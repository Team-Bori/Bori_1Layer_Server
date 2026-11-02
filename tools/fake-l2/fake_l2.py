#!/usr/bin/env python3
"""가짜 L2(릴레이 Pi) 에이전트 — 하드웨어 없이 L1을 개발·테스트할 때 쓴다.

docs/protocol/l1-l2-websocket.md 규격대로 동작한다.
  - 연결 후 hello, 10초마다 ping
  - deploy → 파일 수신·SHA-256 검증 → file_ack → deploy_progress(transfer/install/run)
  - 실행 중 1초마다 metric (MCU는 serial_log도)
  - stop → session_ended (요약 지표 포함)

필요: pip install websockets (15.x에서 확인)

예:
  python3 fake_l2.py --token <파이 토큰> --pi-serial 10000000a1b2c3d4 \
      --board 1421022001387:USB-1:jetson --board 7C:DF:A1:0B:22:E4:USB-2:mcu
"""
import argparse
import asyncio
import hashlib
import json
import random
import struct
import sys
import uuid
from datetime import datetime, timezone

from websockets.asyncio.client import connect
from websockets.exceptions import ConnectionClosed


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def envelope(type_, data, reply_to=None):
    return json.dumps({"type": type_, "id": str(uuid.uuid4()), "replyTo": reply_to, "ts": now_iso(), "data": data})


def parse_board(text):
    # 시리얼에 ':'가 들어갈 수 있어서(MAC) 뒤에서부터 자른다: SERIAL:SLOT:TYPE
    serial, slot, kind = text.rsplit(":", 2)
    return {"serialCode": serial, "usbSlot": slot, "type": kind.lower()}


class FakeL2:
    def __init__(self, args):
        self.args = args
        self.boards = [parse_board(b) for b in args.board]
        self.sessions = {}      # sessionId -> dict
        self.transfers = {}     # transferId(str) -> dict
        self.ws = None
        self.fail_sha_left = args.fail_sha

    def log(self, *a):
        print(f"[fake-l2 {datetime.now().strftime('%H:%M:%S')}]", *a, flush=True)

    async def send(self, type_, data, reply_to=None):
        try:
            await self.ws.send(envelope(type_, data, reply_to))
        except ConnectionClosed:
            return  # 끊긴 동안 보낸 건 버린다 (재연결하면 hello로 상태를 맞춘다)
        if type_ not in ("ping", "metric", "serial_log"):
            self.log("→", type_, json.dumps(data, ensure_ascii=False)[:200])

    async def run_forever(self):
        # 규격 §1 재연결: 1초, 2초, 4초 … 최대 30초. 실행 중 세션은 유지하고 hello로 알린다.
        delay = 1
        while True:
            try:
                await self.run()
                delay = 1
            except (OSError, ConnectionClosed, Exception) as e:  # noqa: BLE001
                self.log("연결 실패:", type(e).__name__, e)
            if self.args.no_reconnect:
                return
            self.log(f"{delay}초 뒤 재연결")
            await asyncio.sleep(delay)
            delay = min(delay * 2, 30)

    async def run(self):
        headers = {"Authorization": f"Bearer {self.args.token}"}
        async with connect(self.args.url, additional_headers=headers, max_size=None) as ws:
            self.ws = ws
            self.log("연결됨", self.args.url)
            await self.send("hello", {
                "agentVersion": "fake-0.1.0",
                "piSerial": self.args.pi_serial,
                "boards": [{"serialCode": b["serialCode"], "usbSlot": b["usbSlot"]} for b in self.boards],
                "runningSessions": [{"sessionId": s, "serialCode": v["serialCode"]}
                                    for s, v in self.sessions.items() if v.get("running")],
            })
            tasks = [asyncio.create_task(self.ping_loop())]
            try:
                async for message in ws:
                    if isinstance(message, bytes):
                        self.on_chunk(message)
                    else:
                        await self.on_text(json.loads(message))
            except ConnectionClosed as e:
                self.log("연결 끊김", e.rcvd)
            finally:
                for t in tasks:
                    t.cancel()
                # 설치·전송 중이던 세션은 버린다. 실행 중인 세션은 계속 돈다 (다음 hello에 보고).
                for sid in [k for k, v in self.sessions.items() if not v.get("running")]:
                    task = self.sessions.pop(sid).get("task")
                    if task:
                        task.cancel()
            self.log("종료 코드", ws.close_code, ws.close_reason)

    async def ping_loop(self):
        while True:
            await asyncio.sleep(self.args.ping_interval)
            await self.send("ping", {})

    def board_of(self, serial):
        return next((b for b in self.boards if b["serialCode"] == serial), None)

    async def on_text(self, msg):
        t, data = msg.get("type"), msg.get("data") or {}
        if t not in ("pong",):
            self.log("←", t, json.dumps(data, ensure_ascii=False)[:200])
        if t == "deploy":
            board = self.board_of(data["serialCode"])
            if board is None:
                await self.send("error", {"code": "BOARD_NOT_FOUND", "message": data["serialCode"]}, msg["id"])
                return
            busy = [s for s, v in self.sessions.items() if v["serialCode"] == data["serialCode"] and v.get("running")]
            if busy:
                await self.send("error", {"code": "BOARD_BUSY", "message": f"running session {busy[0]}"}, msg["id"])
                return
            self.sessions[data["sessionId"]] = {"serialCode": data["serialCode"], "type": board["type"],
                                                "transferId": data["model"]["transferId"], "running": False}
        elif t == "file_begin":
            self.transfers[data["transferId"]] = {**data, "chunks": {}}
        elif t == "file_end":
            await self.on_file_end(data["transferId"])
        elif t == "stop":
            await self.on_stop(data["sessionId"], data.get("reason", "normal"))

    def on_chunk(self, frame):
        tid = str(uuid.UUID(bytes=frame[:16]))
        seq = struct.unpack(">I", frame[16:20])[0]
        tr = self.transfers.get(tid)
        if tr is not None:
            tr["chunks"][seq] = frame[20:]

    async def on_file_end(self, tid):
        tr = self.transfers.pop(tid, None)
        if tr is None:
            return
        session_id = next((s for s, v in self.sessions.items() if v["transferId"] == tid), None)
        if len(tr["chunks"]) != tr["chunkCount"]:
            await self.send("file_ack", {"transferId": tid, "ok": False, "error": "MISSING_CHUNK"})
            return
        blob = b"".join(tr["chunks"][i] for i in range(tr["chunkCount"]))
        if len(blob) != tr["sizeBytes"]:
            await self.send("file_ack", {"transferId": tid, "ok": False, "error": "SIZE_MISMATCH"})
            return
        if hashlib.sha256(blob).hexdigest() != tr["sha256"] or self.fail_sha_left > 0:
            self.fail_sha_left -= 1
            await self.send("file_ack", {"transferId": tid, "ok": False, "error": "SHA256_MISMATCH"})
            return
        await self.send("file_ack", {"transferId": tid, "ok": True})
        self.log(f"파일 수신 완료 {tr['fileName']} {len(blob)} bytes")
        if session_id is not None:
            s = self.sessions[session_id]
            s["task"] = asyncio.create_task(self.install_and_run(session_id, s))

    async def install_and_run(self, session_id, s):
        await self.send("deploy_progress", {"sessionId": session_id, "step": "transfer", "state": "done", "message": None})
        await self.send("deploy_progress", {"sessionId": session_id, "step": "install", "state": "started", "message": None})
        await asyncio.sleep(self.args.step_delay)
        if self.args.fail_install:
            await self.send("deploy_progress", {"sessionId": session_id, "step": "install", "state": "failed", "message": "fake install failure"})
            self.sessions.pop(session_id, None)
            await self.send("session_ended", {"sessionId": session_id, "reason": "failed", "summary": None, "error": "fake install failure"})
            return
        await self.send("deploy_progress", {"sessionId": session_id, "step": "install", "state": "done", "message": None})
        await self.send("deploy_progress", {"sessionId": session_id, "step": "run", "state": "started", "message": None})
        await asyncio.sleep(self.args.step_delay)
        await self.send("deploy_progress", {"sessionId": session_id, "step": "run", "state": "done", "message": None})
        s["running"] = True
        s["fps"], s["acc"], s["count"], s["n"] = [], [], 0, 0
        await self.metric_loop(session_id, s)

    async def metric_loop(self, session_id, s):
        kind = s["type"]
        base_fps = {"jetson": 70.0, "sbc": 110.0, "mcu": 8.0}[kind]
        while True:
            fps = max(0.1, random.gauss(base_fps, base_fps * 0.05))
            acc = round(random.uniform(85, 90), 1)
            sample = {"t": now_iso(), "fps": round(fps, 2), "latencyMs": round(1000 / fps, 2), "accuracyPct": acc,
                      "memPct": round(random.uniform(45, 60), 1)}
            if kind != "mcu":
                sample.update(cpuPct=round(random.uniform(30, 55), 1), tempC=round(random.uniform(48, 58), 1))
            if kind == "jetson":
                sample["powerW"] = round(random.uniform(8, 9), 2)
            s["fps"].append(fps)
            s["acc"].append(acc)
            s["count"] += int(fps)
            await self.send("metric", {"sessionId": session_id, "samples": [sample]})
            if kind == "mcu":
                s["n"] += 1
                await self.send("serial_log", {"sessionId": session_id,
                                               "lines": [f"[{s['n']:04d}] infer #{s['count']} · {1000 / fps:.1f} ms · score {random.uniform(0.7, 0.99):.2f}"]})
            await asyncio.sleep(1)

    async def on_stop(self, session_id, reason):
        s = self.sessions.pop(session_id, None)
        if s is None:
            await self.send("error", {"code": "SESSION_NOT_FOUND", "message": str(session_id)})
            return
        if s.get("task"):
            s["task"].cancel()
        summary = None
        if s.get("fps"):
            summary = {"inferenceCount": s["count"], "avgFps": round(sum(s["fps"]) / len(s["fps"]), 2),
                       "avgAccuracyPct": round(sum(s["acc"]) / len(s["acc"]), 2)}
        await self.send("session_ended", {"sessionId": session_id, "reason": reason, "summary": summary, "error": None})


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--url", default="ws://localhost:8080/ws/l2")
    p.add_argument("--token", required=True)
    p.add_argument("--pi-serial", required=True)
    p.add_argument("--board", action="append", default=[], help="SERIAL:SLOT:TYPE (jetson|sbc|mcu), 여러 번")
    p.add_argument("--ping-interval", type=float, default=10)
    p.add_argument("--step-delay", type=float, default=1.0, help="install/run 단계 사이 지연(초)")
    p.add_argument("--fail-sha", type=int, default=0, help="처음 N번 file_ack를 SHA256_MISMATCH로 실패")
    p.add_argument("--fail-install", action="store_true", help="install 단계를 실패시킨다")
    p.add_argument("--no-reconnect", action="store_true", help="끊기면 다시 연결하지 않고 끝낸다")
    args = p.parse_args()
    try:
        asyncio.run(FakeL2(args).run_forever())
    except KeyboardInterrupt:
        sys.exit(0)


if __name__ == "__main__":
    main()

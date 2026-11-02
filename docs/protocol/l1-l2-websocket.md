# L1 ↔ L2 WebSocket 메시지 규격 (초안 v0.1)

- L1: AWS 백엔드 서버 (이 레포)
- L2: 릴레이 라즈베리파이 에이전트 (`host_pi`)
- L3: 단말 보드 (`cloud_board`) — L2와 UART로 연결, 이 문서 범위 밖

L1과 L2는 **WebSocket 연결 하나로 모든 데이터를 주고받는다** (제어 명령, 지표, 시리얼 출력, 모델 파일).

---

## 1. 연결

```
wss://<L1 호스트>/ws/l2
Authorization: Bearer <파이 토큰>
```

- **L2가 먼저 연결한다.** L1은 L2의 주소를 몰라도 된다.
- 파이 토큰은 관리자가 파이마다 발급한다 (`POST /api/admin/pis` 등록 시, `POST /api/admin/pis/{id}/token` 재발급). 원문은 그 응답에서만 보이고, L1은 SHA-256 해시(`host_pi.token_hash`)만 저장한다. 토큰이 틀리면 L1은 핸드셰이크를 `401`로 거절한다.
- 같은 파이가 두 번 연결하면 L1은 **이전 연결을 닫는다** (close code `4001 replaced`).
- 연결 직후 L2는 반드시 `hello`를 보낸다. `hello` 전에 온 다른 메시지는 무시한다. 10초 안에 `hello`가 없으면 L1이 연결을 닫는다 (`4002 hello timeout`).

### close code

| code | 이유 |
|---|---|
| `4001 replaced` | 같은 파이가 새로 연결함 |
| `4002 hello timeout` | 연결 후 10초 안에 `hello` 없음 |
| `4003 no message for 30s` | 30초 동안 아무 메시지 없음 |
| `4004 pi mismatch` | `hello.piSerial`이 토큰의 파이와 다름 |

### 재연결
- L2는 끊기면 1초, 2초, 4초 … 최대 30초 간격으로 다시 연결한다.
- 다시 연결하면 `hello`에 **현재 붙어 있는 보드 전체와 실행 중인 세션**을 담는다. L1은 이걸 기준으로 상태를 맞춘다 (§4).

### 생존 확인
- L2는 10초마다 `ping`을 보내고 L1은 `pong`으로 답한다.
- L1은 **30초 동안 아무 메시지도 못 받으면** 연결을 끊고 그 파이를 오프라인으로 본다.
- WebSocket 프로토콜 ping 프레임이 아니라 애플리케이션 메시지로 한다 (라이브러리마다 ping 프레임 처리 방식이 달라서).

---

## 2. 프레임 형식

### 텍스트 프레임 — JSON 봉투

모든 제어 메시지는 이 모양이다.

```json
{
  "type": "deploy",
  "id": "9b2f6c1e-1a3d-4c7e-9f00-2b6f1d1f8a10",
  "replyTo": null,
  "ts": "2026-11-01T12:00:00.123Z",
  "data": { }
}
```

| 필드 | 설명 |
|---|---|
| `type` | 메시지 종류 (§3) |
| `id` | 보낸 쪽이 만든 UUID. 응답이 필요한 메시지를 짝지을 때 쓴다 |
| `replyTo` | 응답일 때 원래 메시지의 `id`. 아니면 `null` |
| `ts` | 보낸 시각, UTC ISO-8601 (밀리초) |
| `data` | 종류별 내용 |

- 필드 이름은 camelCase.
- 모르는 `type`이 오면 `error`(`UNKNOWN_TYPE`)로 답하고 연결은 유지한다.
- 모르는 필드는 무시한다 (버전이 달라도 깨지지 않게).

### 바이너리 프레임 — 파일 조각

모델 파일만 바이너리로 보낸다 (JSON에 base64로 넣으면 33% 커진다).

```
[ 16바이트: transferId (UUID 바이너리) ][ 4바이트: seq (big-endian uint32, 0부터) ][ payload ]
```

- 조각 크기는 `file_begin.chunkSize` (기본 256 KiB).
- L1 쪽 WebSocket 바이너리 버퍼는 조각 크기 + 20바이트 이상으로 설정해야 한다 (Spring 기본값은 8 KiB라 그대로면 끊긴다).

---

## 3. 메시지 목록

### L2 → L1

| type | 언제 | 응답 |
|---|---|---|
| `hello` | 연결 직후 1회 | `hello_ack` |
| `ping` | 10초마다 | `pong` |
| `board_attached` | 보드를 새로 꽂았을 때 | — |
| `board_detached` | 보드를 뽑았거나 응답이 없을 때 | — |
| `file_ack` | 파일을 다 받고 검증했을 때 | — |
| `deploy_progress` | 배포 단계가 바뀔 때 | — |
| `metric` | 실행 중 1초마다 | — |
| `serial_log` | MCU 시리얼 출력이 나올 때 | — |
| `session_ended` | 실행이 끝났을 때 (중지 명령·오류 모두) | — |
| `error` | 받은 명령을 처리할 수 없을 때 | — |

### L1 → L2

| type | 언제 | 응답 |
|---|---|---|
| `hello_ack` | `hello`를 받았을 때 | — |
| `pong` | `ping`을 받았을 때 | — |
| `deploy` | 학생이 배포를 눌렀을 때 | `deploy_progress` 들 |
| `file_begin` / (바이너리 조각) / `file_end` | 모델 파일 전송 | `file_ack` |
| `stop` | 학생 종료, 유휴 15분, 관리자 강제 종료 | `session_ended` |
| `error` | L2가 보낸 메시지가 잘못됐을 때 | — |

---

## 4. 메시지 상세

### `hello` (L2 → L1)

```json
{
  "agentVersion": "0.1.0",
  "piSerial": "10000000a1b2c3d4",
  "boards": [
    { "serialCode": "1421022001387", "usbSlot": "USB-1" },
    { "serialCode": "7C:DF:A1:0B:22:E4", "usbSlot": "USB-2" }
  ],
  "runningSessions": [
    { "sessionId": 1042, "serialCode": "1421022001387" }
  ]
}
```

- `piSerial`이 토큰의 파이와 다르면 L1은 `error`(`PI_MISMATCH`)를 보내고 연결을 닫는다.
- `boards`: 지금 USB-UART로 응답하는 보드 **전체**. 보드 식별은 `/dev/ttyUSB*` 순서가 아니라 시리얼 번호로 한다 (재부팅하면 순서가 바뀔 수 있다).

L1이 하는 일:
1. `boards`에 있고 DB에 등록된 보드 → `is_online = true`, `pi_id`·`usb_slot` 갱신 (다른 파이로 옮겨 꽂은 경우 포함). 슬롯은 L2 보고가 기준이라 DB에 `(pi_id, usb_slot)` 유니크 제약을 두지 않는다 (V3).
2. 이 파이에 등록돼 있는데 `boards`에 없는 보드 → `is_online = false`.
3. DB에 없는 시리얼 → 무시하고 `hello_ack.unknownBoards`에 담아 돌려준다. **보드 등록은 관리자 화면에서만 한다.** 관리자가 나중에 그 시리얼을 등록하면 L1이 기억해 둔 보고로 바로 온라인이 된다.
4. `runningSessions` 대조:
   - DB에서도 진행 중 → 그대로 둔다 (지표를 계속 받는다).
   - DB에선 이미 끝난 세션 → `stop`(`reason: "normal"`)을 보낸다.
   - DB에선 진행 중인데 L2가 안 보냄 → 세션을 `failed`로 끝낸다.

### `hello_ack` (L1 → L2)

```json
{
  "piId": 1,
  "pingIntervalSec": 10,
  "metricIntervalMs": 1000,
  "unknownBoards": ["ABX00031-0921"]
}
```

### `board_attached` / `board_detached` (L2 → L1)

```json
{ "serialCode": "ABX00031-0921", "usbSlot": "USB-3" }
```
```json
{ "serialCode": "ABX00031-0921", "reason": "unplugged" }
```

- `reason`: `unplugged` (뽑힘) · `no_response` (꽂혀 있는데 UART 응답 없음)
- 실행 중인 보드가 `board_detached`되면 L1은 그 세션을 `failed`로 끝낸다.

### `deploy` (L1 → L2)

```json
{
  "sessionId": 1042,
  "serialCode": "1421022001387",
  "boardType": "jetson",
  "model": {
    "source": "verified",
    "name": "YOLOv8n",
    "format": "TensorRT",
    "transferId": "3f0c8a5e-7b1d-4b2a-8e6f-0d9c1a2b3c4d"
  }
}
```

- `source`: `verified` (검증 모델) · `upload` (학생 GitHub 모델)
- 바로 뒤에 같은 `transferId`로 파일 전송(§4 `file_begin`)이 이어진다.
- L2는 파일을 다 받고 검증한 뒤(`file_ack`) 설치·실행을 진행한다.
- 이 보드에서 이미 다른 세션이 돌고 있으면 L2는 `error`(`BOARD_BUSY`)로 답한다.

### `file_begin` / 바이너리 조각 / `file_end` (L1 → L2)

```json
{
  "transferId": "3f0c8a5e-7b1d-4b2a-8e6f-0d9c1a2b3c4d",
  "fileName": "model.zip",
  "sizeBytes": 38123520,
  "sha256": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
  "chunkSize": 262144,
  "chunkCount": 146
}
```
```json
{ "transferId": "3f0c8a5e-7b1d-4b2a-8e6f-0d9c1a2b3c4d" }
```

- 파일은 저장소 전체를 묶은 zip 하나다 (§6).
- L2는 `seq` 순서대로 이어 붙이고, `file_end`를 받으면 크기와 SHA-256을 확인한다.
- L1은 파일을 다 보낸 뒤 자기 쪽 사본을 지운다.

### `file_ack` (L2 → L1)

```json
{ "transferId": "3f0c8a5e-...", "ok": true }
```
```json
{ "transferId": "3f0c8a5e-...", "ok": false, "error": "SHA256_MISMATCH" }
```

- `ok: false`면 L1은 **한 번만** 다시 보낸다. 또 실패하면 세션을 `failed`로 끝낸다.
- `error`: `SHA256_MISMATCH` · `SIZE_MISMATCH` · `MISSING_CHUNK` · `DISK_FULL`

### `deploy_progress` (L2 → L1)

```json
{
  "sessionId": 1042,
  "step": "install",
  "state": "done",
  "message": null
}
```

| step | 화면 단계 |
|---|---|
| `transfer` | AWS 서버 → 릴레이 Pi 전송 |
| `install` | 보드에 설치 (SBC는 파일 복사, MCU는 플래싱) |
| `run` | 모델 실행 |

- 첫 단계 "GitHub에서 가져오기"는 L1이 직접 하므로 L2 메시지가 없다.
- `state`: `started` · `done` · `failed`. `failed`면 `message`에 이유를 담고, 이어서 `session_ended`(`reason: "failed"`)를 보낸다.
- `run`이 `done`이 되면 L1은 세션을 "실행 중"으로 보고 15분 유휴 타이머를 시작한다.

### `metric` (L2 → L1)

```json
{
  "sessionId": 1042,
  "samples": [
    {
      "t": "2026-11-01T12:00:01.000Z",
      "fps": 71.4,
      "latencyMs": 14.0,
      "accuracyPct": 88.1,
      "memPct": 48.2,
      "cpuPct": 34.5,
      "tempC": 49.8,
      "powerW": 8.6
    }
  ]
}
```

- 1초마다 1개. 연결이 잠깐 끊겼다 붙으면 밀린 샘플을 한 번에 여러 개 보낼 수 있다 (최대 60개).
- 보드가 측정하지 않는 항목은 **필드를 빼거나 `null`**로 보낸다.

| 보드 종류 | fps | latencyMs | accuracyPct | memPct | cpuPct | tempC | powerW |
|---|---|---|---|---|---|---|---|
| jetson | ○ | ○ | ○ | ○ | ○ | ○ | ○ |
| sbc | ○ | ○ | ○ | ○ | ○ | ○ | — |
| mcu | ○ | ○ | ○ | ○ (SRAM) | — | — | — |

- `accuracyPct`: L2가 계산해서 보낸다. 계산할 수 없는 모델이면 `null`.
- L1은 샘플을 `metric_sample`에 저장하고, 그 세션을 보고 있는 학생 브라우저에 그대로 전달한다.

### `serial_log` (L2 → L1, MCU만)

```json
{
  "sessionId": 1042,
  "lines": ["[00:12] infer #1203 · 8.3 ms · score 0.91"]
}
```

- 실시간 전달만 하고 L1은 저장하지 않는다.

### `stop` (L1 → L2)

```json
{ "sessionId": 1042, "reason": "idle" }
```

- `reason`: `normal` (학생 종료) · `idle` (15분 유휴) · `forced` (관리자 강제 종료) · `failed` (L1 쪽 실패로 정리: 파일 검증 두 번 실패, 배포 시간 초과 등)
- L2는 실행을 멈추고 보드에서 학생 파일을 지운 뒤 `session_ended`를 보낸다.
- L1은 `stop`을 보내자마자 DB 세션을 끝낸다 (L2 응답을 기다리지 않는다). `session_ended`는 요약 지표를 받으려고 쓴다.

### `session_ended` (L2 → L1)

```json
{
  "sessionId": 1042,
  "reason": "normal",
  "summary": {
    "inferenceCount": 106530,
    "avgFps": 70.9,
    "avgAccuracyPct": 88.0
  },
  "error": null
}
```

- `reason`: `stop`에 대한 응답이면 그 `reason` 그대로, 실행 중 오류로 멈췄으면 `failed` (`error`에 이유).
- L1은 `summary`로 `board_model_stat`(평균 FPS, 평균 정확도, 실행 횟수)을 갱신한다. 검증 모델 세션만 반영한다.

### `error` (양방향)

```json
{ "code": "BOARD_BUSY", "message": "board 1421022001387 is running session 1040" }
```

- 응답이면 `replyTo`에 원래 메시지 `id`를 넣는다.

| code | 의미 |
|---|---|
| `UNKNOWN_TYPE` | 모르는 `type` |
| `INVALID_PAYLOAD` | 필수 필드 없음, 타입 틀림 |
| `PI_MISMATCH` | 토큰과 `piSerial`이 다름 |
| `BOARD_NOT_FOUND` | 그 파이에 없는 보드 |
| `BOARD_BUSY` | 보드에서 다른 세션이 실행 중 |
| `SESSION_NOT_FOUND` | 모르는 `sessionId` |

---

## 5. 흐름

### 연결
```
L2                           L1
 │── WS 연결 (Bearer 토큰) ──▶│  토큰 검증
 │── hello ──────────────────▶│  보드 온라인 갱신, 세션 대조
 │◀───────────────── hello_ack│
 │── ping (10초마다) ─────────▶│
 │◀────────────────────── pong│
```

### 배포와 실행
```
학생        L1                          L2
 │─ 배포 ─▶│ GitHub zip 다운로드·크기 검증
 │         │── deploy ─────────────────▶│
 │         │── file_begin, 조각들, file_end ▶│
 │         │◀──────────────── file_ack ok│
 │         │◀── deploy_progress transfer done
 │         │◀── deploy_progress install started/done
 │         │◀── deploy_progress run done │
 │◀ 실행 중 │                             │
 │         │◀── metric (1초마다) ────────│
 │◀ 지표 ── │                             │
```

### 종료
```
학생/관리자/유휴 타이머   L1                 L2
        │── 종료 ──────▶│ DB 세션 종료
                        │── stop ─────────▶│ 실행 중지, 파일 삭제
                        │◀── session_ended │
                        │ board_model_stat 갱신
```

### 끊김
- L2가 끊기면(30초 무응답 포함) L1은 그 파이의 보드를 전부 오프라인으로 바꾼다.
- 진행 중 세션은 **60초 동안 기다린다.** 그 안에 재연결 `hello`의 `runningSessions`에 있으면 이어간다. 없거나 60초가 지나면 `failed`로 끝낸다.
- L1이 다시 시작하면 모든 보드를 오프라인으로 초기화하고 `hello`를 기다린다.

---

## 6. 모델 저장소 규칙

검증 모델(개발팀 저장소)과 학생 모델(GitHub 링크) 모두 같은 규칙을 따른다.

- L1은 `https://github.com/{owner}/{repo}`의 기본 브랜치를 zip으로 받는다. **공개 저장소만** 지원한다.
- 받은 zip 크기가 보드의 `upload_limit_mb`를 넘으면 배포하지 않는다 (학생이 입력한 크기는 믿지 않는다).
- 저장소 최상위에 `bori.yaml`이 있어야 한다. 없으면 배포 전에 L1이 거절한다.

```yaml
# SBC (jetson, sbc)
format: onnx            # onnx | tensorrt | tflite | pytorch
model: models/model.onnx
run: python3 run.py     # 보드에서 실행할 명령
```
```yaml
# MCU
format: tflite-micro    # tflite-micro | tflite
firmware: build/firmware.bin   # 보드에 플래싱할 파일
```

- L2는 zip을 풀어 `bori.yaml`을 읽고 설치·실행한다. 실행 결과에서 지표를 어떻게 뽑는지는 L2↔L3 규격에서 정한다.

---

## 7. 개발용 가짜 L2

`tools/fake-l2/fake_l2.py` — 이 규격대로 움직이는 가짜 에이전트. 하드웨어 없이 L1·프론트를 개발할 때 쓴다.

```
pip install websockets
python3 tools/fake-l2/fake_l2.py --url ws://localhost:8080/ws/l2 --token <토큰> \
    --pi-serial <파이 시리얼> --board <보드 시리얼>:USB-1:jetson --board <시리얼>:USB-2:mcu
```

## 8. 정해지지 않은 것

- 비공개 GitHub 저장소 지원 여부
- `metric_sample` 보관 기간 (제안: 30일)
- L2↔L3 UART 규격 (`bori.yaml`의 `run` 결과에서 지표·정확도를 뽑는 방법 포함)

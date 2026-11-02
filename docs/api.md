# Bori API (프론트용)

- 인증: 세션 쿠키(`JSESSIONID`). 요청은 `credentials: 'include'`. 키는 camelCase.
- 시각: 한국 시간 `LocalDateTime` 문자열 (`2026-11-02T16:12:33.891208`, 타임존 표기 없음).
- 에러: `{ "code": "B005", "message": "…", "errors": [{ "field", "reason" }] }` — 코드 목록은 `ErrorCode.java`.
- enum 값은 대문자 그대로 (`JETSON`, `WAITING`, `RETURN_REQUESTED` …).

## 인증·사용자

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | `{email, password(8~50), name, studentNo(숫자 4자리)}` → 201 |
| POST | `/api/auth/login` | `{email, password}` → 사용자 정보 |
| POST | `/api/auth/logout` | 204 |
| GET | `/api/users/me` | `{id, email, name, studentNo, role, notifyEnabled}` |
| PATCH | `/api/users/me/notify` | `{enabled}` — 브라우저 시스템 알림 허용 |
| GET | `/api/notifications` | `{unreadCount, notifications[최근 30]}` |
| POST | `/api/notifications/read-all` | 204 |

관리자 계정은 가입으로 만들 수 없다. 서버 환경변수 `ADMIN_EMAIL`·`ADMIN_PASSWORD`로 기동할 때 만든다.

## 학생 — 클라우딩

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/cloud/boards?type=JETSON&q=orin` | 보드 목록 |
| GET | `/api/cloud/boards/{id}` | 보드 상세 (목록과 같은 모양) |
| GET | `/api/cloud/me` | `{session, queue}` — 헤더·새로고침 복원용, 없으면 null |
| POST | `/api/cloud/boards/{id}/sessions` | 접속 + 배포 시작 → 201 `MySession` |
| POST | `/api/cloud/sessions/{id}/end` | 학생 종료 → 204 |
| POST | `/api/cloud/sessions/{id}/activity` | 조작 신호 (15분 유휴 타이머 초기화). 30초에 한 번 정도로 묶어서 |
| POST | `/api/cloud/boards/{id}/queue` | 대기 등록 → 201 `MyQueue` |
| DELETE | `/api/cloud/queue` | 내 대기 취소 → 204 |
| GET | `/api/cloud/sessions/{id}/metrics?range=300` | 그래프 (아래) |
| GET | `/api/cloud/me/sessions?days=7\|30` | 내 사용량 세션 목록 |
| GET | `/api/cloud/me/uploads` | 내 업로드 모델 |

### 보드 `availability` (배지)

`OFFLINE` 오프라인 · `MAINTENANCE` 점검중 · `BROKEN` 고장 · `NOT_EXECUTABLE` 실행 불가 · `MY_SESSION` 내 세션 실행 중 · `MY_TURN` 내 차례(5분 안에 접속) · `BUSY` 사용중 · `RESERVED` 비었지만 대기자 차례 · `FREE` 사용가능

`queueCount`/`queueCapacity`(2), `models[] = {id, name, format, description, avgFps, avgAccuracy, runCount}` (실행 기록 없으면 0), `totalRuns`.

### 접속 요청

```json
{ "aiModelId": 1 }
```
```json
{ "githubUrl": "https://github.com/owner/repo", "modelFormat": "ONNX", "sizeMb": 38 }
```

- 둘 중 하나만. 업로드는 공개 저장소, 최상위에 `bori.yaml` 필요 (`docs/protocol/l1-l2-websocket.md` §6).
- `sizeMb`는 입력 검증용이고, 서버가 실제로 받아서 크기를 다시 잰다.
- 주요 거절: `S002` 이미 세션 있음 · `B004` 접속 불가 · `B005` 사용 중 · `B006` 다른 사람 차례 · `Q003` 다른 보드 대기 중 · `M003` 지원 안 하는 모델 · `M004` 한도 초과.

### `MySession`

`{id, boardId, boardName, boardType, host, piName, usbSlot, modelLabel, startedAt, lastActiveAt, idleTimeoutSec(900), phase, steps[]}`

- `phase`: `FETCHING`(GitHub에서 받는 중) → `DEPLOYING` → `RUNNING`
- `steps`: `fetch` · `transfer` · `install` · `run` 각각 `{step, state: WAITING|STARTED|DONE|FAILED, message}` — 화면의 배포 4단계
- 유휴 남은 시간 = `lastActiveAt + idleTimeoutSec - 현재` (`RUNNING`일 때만)

### `MyQueue`

`{entryId, boardId, boardName, state: WAITING|READY|EXPIRED, position, capacity, expiresAt}` — `READY`면 `expiresAt`까지 접속해야 한다.

### 지표 `GET /api/cloud/sessions/{id}/metrics`

- 진행 중 세션: 최근 `range`초 (`300, 900, 1800, 3600, 10800, 21600, 43200`).
- 끝난 세션: 시작~종료 전체 (`range` 무시) — 내 사용량 상세용.
- 응답: `{sessionId, active, from, to, bucketSec, sampleCount, buckets[60], summary, latest}`
  - `buckets[i] = {t, fps, memPct, cpuPct, tempC, powerW, latencyMs, accuracyPct}` — 칸 평균, 없으면 null
  - `summary.fps = {min, avg, max}` … (원래 샘플 기준)
  - MCU는 `cpuPct`·`tempC`·`powerW`가, Jetson 외엔 `powerW`가 null

## 학생 — 대여

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/rental/boards?status=AVAILABLE` | `{id, name, spec, photoUrl, accessories, status}` |
| POST | `/api/rental/boards/{id}/rent` | 즉시 확정 → 201 |
| GET | `/api/rental/me` | 내 대여 `{id, boardId, boardName, boardDeleted, accessories, status, rentedAt, returnRequestedAt, returnedAt}` |
| POST | `/api/rental/rentals/{id}/return` | 반납 신청 (`RENTED` → `RETURN_REQUESTED`, 보드 `RETURN_PENDING`) |

## 관리자 (`/api/admin/**`, ADMIN만)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET/POST | `/api/admin/pis` | 릴레이 Pi 목록(`connected` 포함) / 등록 `{name, serialCode}` → **토큰 원문은 이 응답에만** |
| POST | `/api/admin/pis/{id}/token` | 토큰 재발급 |
| GET/POST | `/api/admin/models` | 검증 모델 `{name, format, description, repoUrl}` |
| PUT | `/api/admin/models/{id}` | 수정 |
| GET/POST | `/api/admin/cloud-boards` | 클라우딩 보드 (목록에 `currentSession`) / 등록 |
| PUT | `/api/admin/cloud-boards/{id}` | `{name, spec, boardType, host, piId, usbSlot, serialCode, uploadLimitMb, executeAble, modelIds}` |
| PATCH | `/api/admin/cloud-boards/{id}/status` | `{status: NORMAL\|MAINTENANCE\|BROKEN}` |
| PATCH | `/api/admin/cloud-boards/{id}/upload-limit` | `{uploadLimitMb}` |
| POST | `/api/admin/cloud-boards/{id}/force-end` | 진행 중 세션 강제 종료 |
| DELETE | `/api/admin/cloud-boards/{id}` | 소프트 삭제 (세션 강제 종료, 대기 취소) |
| GET/POST | `/api/admin/rental-boards` | 대여 보드 (목록에 대여자) / 등록 `{name, spec, accessories}` |
| PUT | `/api/admin/rental-boards/{id}` | 수정 |
| POST | `/api/admin/rental-boards/{id}/photo` | multipart `file` (jpg/png/webp, 5MB) |
| PATCH | `/api/admin/rental-boards/{id}/status` | `AVAILABLE\|MAINTENANCE\|BROKEN`만, 대여 중이면 거절 |
| DELETE | `/api/admin/rental-boards/{id}` | 소프트 삭제 |
| GET | `/api/admin/rentals/return-requests` | 반납 승인 대기 |
| POST | `/api/admin/rentals/{id}/approve` | 반납 승인 → 보드 대여가능, 학생 알림 |
| GET | `/api/admin/history/sessions\|rentals\|logs?q=&page=0&size=20` | 이력 (`{content, page, size, totalElements, totalPages}`) |

관리자의 등록·수정·삭제·상태 변경·강제 종료·반납 승인은 `admin_action_log`에 남는다.

## 실시간 — `ws(s)://<서버>/ws/client`

- 로그인 쿠키로 연결한다 (Origin은 `CORS_ALLOWED_ORIGINS`에 있어야 함). 서버 → 브라우저만.
- 메시지: `{ "type": "...", "data": ... }`

| type | data |
|---|---|
| `deploy_progress` | `{sessionId, step, state, message}` |
| `session_ended` | `{sessionId, reason: NORMAL\|IDLE\|FAILED\|FORCED, message}` |
| `metric` | `{sessionId, samples[{t(UTC ISO), fps, latencyMs, accuracyPct, memPct, cpuPct, tempC, powerW}]}` (1초마다) |
| `serial_log` | `{sessionId, lines[]}` (MCU, 저장 안 됨) |
| `queue` | `MyQueue` 또는 `null`(대기 없어짐) |
| `notification` | `{id, type: QUEUE_READY\|RETURN_APPROVED, message, read, createdAt}` |

끊기면 다시 연결하고 `GET /api/cloud/me`로 상태를 맞춘다.

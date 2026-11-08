# trade-doc-origin-check

수출 서류(인보이스·패킹리스트·B/L·원산지증명서·원산지확인서·수출신고필증)를 대조해 원산지 증명 적용 가능 여부를 사전 점검하는 서비스.

```
trade-doc-origin-check/
├── backend/        Java 21 · Spring Boot 4.1 · Gradle · MySQL(JPA, Flyway) · JWT
├── frontend/       React 19 · TypeScript · Vite · React Router
├── docs/
│   └── API_명세서.md
├── compose.yaml    로컬 개발용 MySQL
└── .env.example    백엔드·compose 공용 환경 변수 (cp .env.example .env)
```

## 흐름

PDF를 올리면 백엔드가 접수(202)하고, 워커가 **비동기로** Upstage 워크플로에 넘겨 결과를 DB에 저장한다. 프론트는 상세를 5초마다 조회하다가 `completed`가 되면 결과 전체를 한 번에 받는다. 자세한 내용은 [docs/API_명세서.md](docs/API_명세서.md).

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/v1/auth/signup` · `/api/v1/auth/login` | 가입·로그인 → JWT |
| GET | `/api/v1/auth/me` | 내 정보 |
| POST | `/api/v1/origin-checks` | PDF 접수 → `202 {id, status}` |
| GET | `/api/v1/origin-checks` | 내 점검 목록 |
| GET | `/api/v1/origin-checks/{id}` | 상태 + 결과(판정·불일치·후속 문서·추출값·좌표) |
| GET | `/api/v1/origin-checks/{id}/file` | 원본 PDF |

## 로컬 실행

필요: Java 21, Node 22, Docker

```sh
cp .env.example .env          # JWT_SECRET, UPSTAGE_API_KEY 채우기 (JWT_SECRET 예: openssl rand -base64 48)
docker compose up -d          # MySQL (3306)
```

```sh
cd backend
./gradlew bootRun             # http://localhost:8080 , 스키마는 Flyway가 만든다
```

```sh
cd frontend
npm install
npm run dev                   # http://localhost:5173 , /api 는 8080 으로 프록시
```

Upstage 키 없이 화면을 만들 때는 `.env`에 `AGENT_MODE=fake`, `AGENT_FAKE_RESULT=src/test/resources/check_result_set04.json`을 넣는다. 업로드한 PDF와 상관없이 10초 뒤 set04 결과가 나온다.

## 테스트·빌드

```sh
cd backend && ./gradlew test      # 60개, 약 25초. Docker 필요(통합 테스트가 MySQL 컨테이너를 띄움)
cd frontend && npm run build && npm run lint
```

백엔드 테스트는 실제 Upstage 를 부르지 않는다(가짜 agent·가짜 HTTP 서버 사용). `.env` 값과도 무관하다.

| 테스트 | 무엇을 보나 |
|---|---|
| `OriginCheckApiIntegrationTest` | 실제 MySQL + 가짜 agent 로 API 전체: 가입·로그인·토큰(없음·위조·만료), 업로드 검증, 접수→제출→완료/실패 흐름, 목록·필터·페이지, 원본 PDF, 남의 건 404 |
| `OriginCheckWorkerTest` | 워커 상태 전환: 제출 성공/실패, 제출 일시 오류 재시도(간격·최대 횟수), 처리 중, 완료 저장, agent 실패, 조회 네트워크 오류 재시도, 해석 실패, 시간 초과, 한 건 오류가 다른 건을 막지 않음 |
| `UpstageClientTest` | Upstage 로 보내는 요청 모양: multipart(file·purpose·한글 파일명 UTF-8 그대로), `x-project-id`, 작업 생성 본문, `include[]=all`, 오류 응답·재시도 대상 구분 |
| `UpstageResponseParserTest` · `AgentCheckResultJsonTest` | Upstage 응답 → 결과 변환, set04 응답 왕복, 코드표 |
| `JwtProviderTest` | 발급·검증, 만료·다른 키·다른 발급자·`alg: none`·변조 거절, 비밀 키 없음 |

## 코드 위치

| 무엇 | 어디 |
|---|---|
| 응답 DTO (판정·추출·메타·enum) | `backend/src/main/java/com/tradedoc/origincheck/agent/dto/` |
| Upstage 호출 · 응답 파싱 | `backend/.../agent/upstage/UpstageClient.java`, `UpstageResponseParser.java` |
| 비동기 처리(제출·조회 워커) | `backend/.../check/OriginCheckWorker.java` |
| 점검 API · 저장 | `backend/.../check/` (`OriginCheckController`, `OriginCheckService`, `OriginCheck` 엔티티) |
| 인증 | `backend/.../auth/` |
| DB 스키마 | `backend/src/main/resources/db/migration/` |
| 프론트 API 타입·호출 | `frontend/src/api/` |
| 상세 폴링 | `frontend/src/hooks/useOriginCheck.ts` |
| 화면 | `frontend/src/pages/` (메인·로그인/가입 모달 · 심사목록 · 심사 상세 — 뼈대만) |

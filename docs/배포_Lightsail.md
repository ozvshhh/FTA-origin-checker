# Lightsail 배포

Lightsail **인스턴스** 1대에 전부 올린다. 구성 파일은 `deploy/`에 있다.

```
인터넷 ─ 80/443 ─▶ Caddy (HTTPS 자동 발급 · 프론트 정적 파일 · /api 프록시)
                     └─▶ backend (Spring Boot, 1개) ─▶ MySQL 8.4
                          └─ 원본 PDF: volume "uploads"
```

**컨테이너 서비스가 아니라 인스턴스로 하는 이유**
- 원본 PDF를 로컬 디스크에 저장한다. Lightsail 컨테이너 서비스는 디스크가 남지 않는다.
- 워커가 행 잠금 없이 대기 건을 가져가므로 백엔드는 1개만 띄워야 한다.
- 한 서버에 DB까지 두면 관리형 DB 비용이 들지 않는다.

## 1. 인스턴스 만들기 (콘솔)

1. **인스턴스를 만든다.**
   - 플랫폼: Linux/Unix, 블루프린트: OS 전용 → **Ubuntu 24.04 LTS**
   - 플랜: **2GB RAM 이상**. 로컬 측정으로 백엔드 약 550MB, MySQL 약 220MB를 쓰고, 서버에서 이미지를 빌드하는 동안에는 더 쓴다. 1GB 플랜은 빌드 중 메모리가 부족할 수 있다.
   - 리전: 서울(ap-northeast-2)
2. **고정 IP를 만들어 인스턴스에 붙인다.** 네트워킹 → 고정 IP 생성.
3. **방화벽을 연다.** 인스턴스 → 네트워킹 → IPv4 방화벽.
   - SSH(22), HTTP(80), HTTPS(443) 3개만 둔다.
   - 8080·3306은 열지 않는다. 백엔드와 MySQL은 컨테이너 내부 네트워크에만 있다.
4. **도메인을 준비한다.**
   - 도메인이 있으면 DNS에 A 레코드(예: `origin.example.com`)를 만들고 고정 IP를 가리키게 한다.
   - 도메인이 없으면 `ip-3-35-1-2.sslip.io`(고정 IP의 점을 `-`로 바꾼 형태)를 쓴다. 별도 설정 없이 이 IP로 연결되고 HTTPS 인증서도 받을 수 있다.
5. **SSH 키를 받아 둔다.** 계정 → SSH 키에서 내려받는다. 예: `~/.ssh/lightsail.pem`, 권한 `chmod 600`

## 2. 인스턴스 첫 준비 (한 번만)

```bash
ssh -i ~/.ssh/lightsail.pem ubuntu@<고정IP>
```

```bash
git clone https://github.com/vector2967/trade-doc-origin-check.git ~/trade-doc-origin-check
```

```bash
cd ~/trade-doc-origin-check && bash deploy/setup-instance.sh   # Docker 설치 + 스왑 2GB
```

설치가 끝나면 SSH를 끊고 다시 접속한다. docker 그룹은 다시 접속해야 적용된다.

**저장소가 비공개일 때**
- 인스턴스에서 clone하려면 읽기 전용 Deploy key 또는 토큰이 필요하다.

**운영 환경 변수**

```bash
cd ~/trade-doc-origin-check && cp deploy/.env.prod.example deploy/.env && chmod 600 deploy/.env && nano deploy/.env
```

| 이름 | 넣을 값 |
|---|---|
| `DOMAIN` | 1-4에서 정한 도메인 |
| `DB_PASSWORD` · `DB_ROOT_PASSWORD` | `openssl rand -base64 24` 로 만든 무작위 값 |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `SIGNUP_ALLOWED_EMAILS` | 가입을 허용할 주소. 예: `kim@acme.com,@team.co.kr`. **비우면 누구나 가입해서 Upstage 호출을 쓸 수 있다** |
| `UPSTAGE_API_KEY` | Upstage 키 |

`deploy/.env`는 인스턴스에만 두고 저장소에 올리지 않는다(`.gitignore`에 들어 있다).

## 3. 배포

**내 컴퓨터에서 배포할 때**

```bash
LIGHTSAIL_HOST=ubuntu@<고정IP> LIGHTSAIL_KEY=~/.ssh/lightsail.pem DEPLOY_BRANCH=main scripts/deploy-lightsail.sh
```

**인스턴스 안에서 직접 배포할 때**

```bash
cd ~/trade-doc-origin-check && docker compose -f deploy/compose.prod.yaml up -d --build
```

**스크립트가 하는 일**
1. 인스턴스에서 브랜치를 받아 `origin/<브랜치>`로 맞춘다.
   - 인스턴스의 로컬 변경은 버린다. 인스턴스는 배포 전용으로 쓴다.
   - PR이 합쳐지기 전이면 `DEPLOY_BRANCH=feat/spring-react-monorepo`로 배포한다.
2. 이미지를 다시 빌드한다(백엔드, 프론트+Caddy).
3. 바뀐 컨테이너만 다시 띄운다.

**첫 배포**
- DB 스키마는 Flyway가 백엔드 시작 시 만든다.
- Caddy가 인증서를 받는 데 몇십 초 걸릴 수 있다.

## 4. 확인

```bash
curl -s https://<DOMAIN>/api/v1/health      # {"ok":true}
```

```bash
docker compose -f deploy/compose.prod.yaml ps          # 3개 모두 healthy
```

```bash
docker compose -f deploy/compose.prod.yaml logs -f backend
```

브라우저에서 `https://<DOMAIN>`에 들어가 허용한 이메일로 가입하고 PDF를 올려 본다.

## 5. 운영

| 할 일 | 방법 |
|---|---|
| 백업 | Lightsail 인스턴스 스냅샷(자동 스냅샷 켜기 권장). DB·원본 PDF·인증서가 모두 인스턴스 디스크의 Docker volume 에 있다 |
| DB 덤프 | `docker compose -f deploy/compose.prod.yaml exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" origin_check' > backup.sql` |
| 가입 허용 추가 | `deploy/.env` 의 `SIGNUP_ALLOWED_EMAILS` 수정 → `docker compose -f deploy/compose.prod.yaml up -d backend` |
| 디스크 확인 | `df -h` , `docker system df` |

**하면 안 되는 것**
- `docker compose ... down -v`: volume을 지우므로 DB, PDF, 인증서가 모두 사라진다.
- 백엔드를 여러 개 띄우기(`--scale backend=2`): 같은 건을 Upstage에 두 번 제출할 수 있다.

## 로컬에서 검증한 것 (2026-11-09)

로컬 환경에서 운영 compose를 테스트 값으로 띄웠다. 포트는 8088/8443, `DOMAIN=localhost`, AI는 fake 모드로 바꿔 Upstage를 호출하지 않았다.

| 항목 | 결과 |
|---|---|
| 컨테이너 상태 | 3개 모두 healthy |
| HTTP | HTTPS로 308 리다이렉트 |
| 프론트 | `index.html`은 no-cache, assets는 1년 캐시, `/checks/1` 같은 SPA 경로도 200 |
| 가입 | 허용 목록에 없는 이메일은 403 `SIGNUP_NOT_ALLOWED`, 허용 도메인은 가입됨 |
| 점검 흐름 | 업로드 202 → 완료 → 목록 요약 필드 채워짐, 원본 PDF 200 |
| 업로드 한도 | 110MB는 Caddy가 413으로 막음 |
| 재기동 | `down` 후 `up` 해도 DB와 원본 PDF 유지 |

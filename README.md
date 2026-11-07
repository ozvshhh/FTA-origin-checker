# trade-doc-origin-check

수출 서류(인보이스·패킹리스트·B/L·원산지증명서·원산지확인서·수출신고필증)를 대조해 원산지 증명 적용 가능 여부를 사전 점검하는 서비스.

## 실행

```sh
cp .env.example .env   # UPSTAGE_API_KEY 입력
docker compose up -d --build
```

- `POST /checks` (multipart `file`: PDF) → `{job_id, status}`
- `GET /checks/{job_id}` → 판정·불일치·추출 결과. `status`가 `completed`/`failed`가 될 때까지 3초 간격으로 조회
- API 문서: http://localhost:8000/docs · 응답 예시 `examples/`

#!/usr/bin/env python3
"""채점용 세트(set01~13)를 백엔드 API 로 돌려 정답(answer.json)과 비교한다.

백엔드가 실제 Upstage 모드(AGENT_MODE=upstage)로 떠 있어야 한다. 세트 수만큼 Upstage 사용량이 든다.
표준 라이브러리만 쓴다.

    python3 scripts/evaluate_sets.py \\
        --answer "/path/사카밤바스피스_미션2_answer.json" \\
        --pdf-dir "/path/final_upload 2" \\
        --base http://localhost:8080 --out eval-report.json

비교 기준(케이스마다):
- decision        : 판정이 같아야 정답
- mismatch_codes  : 코드 집합이 같아야 정답(순서 무관)
- missing_documents: 서류 집합이 같아야 정답(순서 무관)
- invoice_no      : 송장번호가 같아야 정답
"""
import argparse
import json
import mimetypes
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


def call(base, method, path, token=None, body=None, files=None, fields=None):
    headers = {}
    data = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if files:
        boundary = uuid.uuid4().hex
        parts = []
        for name, value in (fields or {}).items():
            parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode())
        for name, path_ in files.items():
            fn = Path(path_).name
            parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename*=UTF-8\'\'{urllib.parse.quote(fn)}\r\n'
                          f'Content-Type: application/pdf\r\n\r\n').encode() + Path(path_).read_bytes() + b"\r\n")
        parts.append(f"--{boundary}--\r\n".encode())
        data = b"".join(parts)
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except ValueError:
            return e.code, {"raw": raw.decode(errors="replace")}


import urllib.parse  # noqa: E402  (multipart 파일명 인코딩)


def login(base, email, password):
    st, body = call(base, "POST", "/api/v1/auth/signup", body={"email": email, "password": password, "name": "정답률 점검"})
    if st == 409:
        st, body = call(base, "POST", "/api/v1/auth/login", body={"email": email, "password": password})
    if st not in (200, 201):
        sys.exit(f"로그인 실패 HTTP {st}: {body}")
    return body["access_token"]


def run_case(base, token, case, pdf_dir, timeout_s):
    pdf = Path(pdf_dir) / case["file"]
    started = time.time()
    st, created = call(base, "POST", "/api/v1/origin-checks", token, files={"file": pdf},
                       fields={"title": f"[정답률] {case['case_id']} {pdf.stem}"})
    if st != 202:
        return {"case": case, "error": f"접수 실패 HTTP {st}: {created}"}
    cid = created["id"]
    while True:
        st, d = call(base, "GET", f"/api/v1/origin-checks/{cid}", token)
        if st == 200 and d["status"] in ("completed", "failed"):
            return {"case": case, "id": cid, "result": d, "seconds": round(time.time() - started)}
        if time.time() - started > timeout_s:
            return {"case": case, "id": cid, "error": f"{timeout_s}초 안에 끝나지 않음 (마지막 상태 {d.get('status')})"}
        time.sleep(5)


def compare(expected, got):
    judge = (got or {}).get("judge") or {}
    checks = {
        "decision": expected["decision"] == judge.get("decision"),
        "mismatch_codes": set(expected["mismatch_codes"]) == set(judge.get("mismatch_codes") or []),
        "missing_documents": set(expected["missing_documents"]) == set(judge.get("missing_documents") or []),
        "invoice_no": (expected.get("invoice_no") or None) == (judge.get("invoice_no") or None),
    }
    return checks, judge


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--answer", required=True, help="answer.json 경로")
    ap.add_argument("--pdf-dir", required=True, help="세트 PDF 가 있는 디렉터리")
    ap.add_argument("--base", default="http://localhost:8080", help="백엔드 주소")
    ap.add_argument("--email", default="eval@example.com")
    ap.add_argument("--password", default="evaluate-sets-123")
    ap.add_argument("--cases", help="일부만: 쉼표로 case_id (예: A,B)")
    ap.add_argument("--timeout", type=int, default=1200, help="케이스당 최대 대기(초)")
    ap.add_argument("--out", help="결과를 JSON 으로 저장할 경로")
    args = ap.parse_args()

    answer = json.loads(Path(args.answer).read_text(encoding="utf-8"))
    cases = answer["cases"]
    if args.cases:
        want = set(args.cases.split(","))
        cases = [c for c in cases if c["case_id"] in want]
    token = login(args.base, args.email, args.password)
    print(f"{len(cases)}건 제출 → 완료까지 기다립니다…", flush=True)

    with ThreadPoolExecutor(max_workers=len(cases)) as pool:
        runs = list(pool.map(lambda c: run_case(args.base, token, c, args.pdf_dir, args.timeout), cases))

    rows, totals = [], {"decision": 0, "mismatch_codes": 0, "missing_documents": 0, "invoice_no": 0, "all": 0}
    print()
    print(f"{'케이스':<4} {'파일':<28} {'시간':>5}  {'정답 판정':<6} {'결과 판정':<6} 판정 코드 누락 송장")
    for run in runs:
        c = run["case"]
        exp = c["answer"]
        if "error" in run or run["result"]["status"] != "completed":
            err = run.get("error") or (run["result"].get("error") or {}).get("message")
            print(f"{c['case_id']:<4} {c['file'][-24:]:<28} {'-':>5}  {exp['decision']:<6} {'오류':<6} {err}")
            rows.append({"case_id": c["case_id"], "file": c["file"], "expected": exp, "error": err})
            continue
        checks, judge = compare(exp, run["result"])
        for k, ok in checks.items():
            totals[k] += ok
        totals["all"] += all(checks.values())
        mark = lambda ok: "O" if ok else "X"
        print(f"{c['case_id']:<4} {c['file'][-24:]:<28} {run['seconds']:>4}s  {exp['decision']:<6} {str(judge.get('decision')):<6} "
              f"  {mark(checks['decision'])}    {mark(checks['mismatch_codes'])}    {mark(checks['missing_documents'])}    {mark(checks['invoice_no'])}")
        if not all(checks.values()):
            if not checks["mismatch_codes"]:
                print(f"       코드 정답 {sorted(exp['mismatch_codes'])} / 결과 {sorted(judge.get('mismatch_codes') or [])}")
            if not checks["missing_documents"]:
                print(f"       누락 정답 {sorted(exp['missing_documents'])} / 결과 {sorted(judge.get('missing_documents') or [])}")
            if not checks["invoice_no"]:
                print(f"       송장 정답 {exp.get('invoice_no')} / 결과 {judge.get('invoice_no')}")
        rows.append({"case_id": c["case_id"], "file": c["file"], "check_id": run["id"], "seconds": run["seconds"],
                     "expected": exp, "got": judge, "checks": checks})

    n = len(cases)
    print()
    print(f"판정 {totals['decision']}/{n} · 불일치 코드 {totals['mismatch_codes']}/{n} · 누락 서류 {totals['missing_documents']}/{n} "
          f"· 송장번호 {totals['invoice_no']}/{n} · 전부 맞음 {totals['all']}/{n}")
    if args.out:
        Path(args.out).write_text(json.dumps({"totals": totals, "n": n, "rows": rows}, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"결과 저장: {args.out}")


if __name__ == "__main__":
    main()

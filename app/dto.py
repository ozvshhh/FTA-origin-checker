"""원산지 서류 점검 API DTO + Upstage Agents API 응답 → DTO 변환.

워크플로우: agt_AvDNsSPfHLp9ZXSjxpcBBc (설정 #7). 노드 이름은 Studio 워크플로우 설정 기준.
실행: uv run --with pydantic python app/dto.py  → 자기 점검 + examples/*.json 생성
"""
from __future__ import annotations

import json
import re
from enum import Enum
from typing import Literal

from pydantic import BaseModel, Field


class Decision(str, Enum):
    PREFERENTIAL = "특혜 적용"
    SUPPLEMENT = "보완 요청"
    NOT_APPLICABLE = "미적용"
    REVIEW = "검토 필요"


class JobStatus(str, Enum):
    QUEUED = "queued"
    RUNNING = "in_progress"
    COMPLETED = "completed"
    FAILED = "failed"  # Upstage failed·cancelled·incomplete 모두 여기로


# 판정 프롬프트(build/judge_prompt.txt)의 코드 → (단계, 화면 문구). 단계: 1 미적용 / 2 보완 요청 / 3 검토 필요
CODES: dict[str, tuple[int, str]] = {
    "SUP_CRITERION_NOT_MET": (1, "원산지확인서 결정기준 미충족(N)"),
    "SUP_ORIGIN_MISMATCH": (1, "원산지확인서 원산지가 한국이 아님"),
    "CO_INVOICE_NO_MISMATCH": (2, "원산지증명서 송장번호 ≠ 인보이스"),
    "CO_QTY_MISMATCH": (2, "원산지증명서 수량 ≠ 인보이스"),
    "CO_CRITERION_MISMATCH": (2, "원산지증명서 결정기준 ≠ 원산지확인서"),
    "CO_ORIGIN_MISMATCH": (2, "원산지증명서 생산국 ≠ 원산지확인서"),
    "CO_CONSIGNEE_MISMATCH": (2, "원산지증명서 수하인이 다른 회사"),
    "CO_DESCRIPTION_MISMATCH": (2, "원산지증명서 품명이 다른 물품"),
    "CO_HS_MISMATCH": (2, "HS 앞 6자리 불일치"),
    "SUP_EXPIRED": (2, "원산지확인서 포괄확인기간 만료"),
    "SUP_RECIPIENT_MISMATCH": (2, "원산지확인서 공급받는 자 ≠ 수출자"),
    "DUPLICATE_DOCUMENT": (3, "같은 종류 서류가 2건 이상"),
    "EXPDEC_INVOICE_NO_MISMATCH": (3, "수출신고필증 송품장부호 ≠ 인보이스"),
    "EXPDEC_QTY_MISMATCH": (3, "수출신고필증 수량 ≠ 인보이스"),
    "CO_HS_MINOR_DIFF": (3, "HS 10자리 중 뒤 4자리 차이"),
    "CO_TRANSPORT_MISMATCH": (3, "원산지증명서 운송 정보 ≠ B/L(직접운송)"),
    "CO_ISSUE_DATE_LATE": (3, "원산지증명서 발급일이 적재일보다 3일 넘게 늦음"),
    "SUP_AGREEMENT_MISMATCH": (3, "원산지확인서 협정명칭 불일치"),
    "CO_CRITERION_NOT_PSR": (3, "원산지증명서 결정기준이 협정 PSR에 없음"),
    "QTY_MISMATCH": (3, "패킹리스트 수량 ≠ 인보이스"),
    "NET_GT_GROSS": (3, "순중량 > 총중량"),
}

JUDGE_NODE = "원산지_판정"
FOLLOW_UP_NODES = ("보완_요청_초안", "미적용_안내", "검토_메모", "미분류_안내")
EXTRACT_NODES = ("인보이스_추출", "패킹리스트_추출", "BL_추출", "원산지증명서_추출", "원산지확인서_추출", "수출신고필증_추출")
FollowUpKind = Literal["보완_요청_초안", "미적용_안내", "검토_메모", "미분류_안내"]


# ---- API DTO ----

class MismatchItem(BaseModel):
    code: str
    stage: int | None = Field(None, description="1 미적용 / 2 보완 요청 / 3 검토 필요. 모르는 코드면 null")
    label: str


class JudgeResult(BaseModel):
    """판정 노드 출력 5키. answer.json 키와 같다."""
    invoice_no: str | None
    decision: Decision
    reason: str
    mismatch_codes: list[str]
    missing_documents: list[str]


class FollowUp(BaseModel):
    kind: FollowUpKind
    text: str


class CheckCreated(BaseModel):
    """POST /checks 응답"""
    job_id: str
    status: JobStatus


class CheckResult(BaseModel):
    """GET /checks/{job_id} 응답. 완료 전에는 judge·follow_up이 null."""
    job_id: str
    status: JobStatus
    judge: JudgeResult | None = None
    mismatches: list[MismatchItem] = []
    follow_up: FollowUp | None = None
    extracted: dict[str, list[dict]] = Field(default_factory=dict, description="Extract 노드 이름 → 서류별 추출 JSON 목록(같은 종류 2건이면 2개)")
    extracted_meta: dict[str, list[dict]] = Field(default_factory=dict, description=(
        "extracted와 같은 모양. 필드마다 {confidence: high|medium|low, score: 0~1, page: 1부터(-1=위치 없음), "
        "boxes: [[{x,y}×4], …] 페이지 기준 0~1 비율 좌표(단어별)}. items는 품목별 같은 구조의 목록"))
    error: str | None = None


# ---- Upstage 응답 → DTO ----

def strip_cites(text: str) -> str:
    """Studio Instruct 출력에 붙는 인용 표시 '【†1】'를 지운다(실측 2026-11-07, 설정 #5)."""
    return re.sub(r"【[^】]*】", "", text)


def parse_judge(text: str) -> JudgeResult:
    """판정 노드 텍스트(5줄 'key: value') 또는 JSON을 받는다. decision이 이상하면 '검토 필요'."""
    text = strip_cites(text)
    try:  # 코드 블록(```json)으로 감싸 와도 첫 { ~ 마지막 } 만 읽는다
        d = json.loads(text[text.index("{"): text.rindex("}") + 1])
    except ValueError:
        d = {}
        for line in text.splitlines():
            k, sep, v = line.partition(":")
            if sep:
                d[k.strip().lower()] = v.strip()
    lists = {}
    for k in ("mismatch_codes", "missing_documents"):
        v = d.get(k) or []
        if isinstance(v, str):
            try:
                v = json.loads(v)
            except ValueError:
                v = [s.strip() for s in v.strip("[]").split(",") if s.strip()]
        lists[k] = [str(s).strip().strip("'\"") for s in v]
    dec = str(d.get("decision", "")).strip()
    return JudgeResult(
        invoice_no=d.get("invoice_no") or None,
        decision=Decision(dec) if dec in {x.value for x in Decision} else Decision.REVIEW,
        reason=str(d.get("reason", "")),
        **lists,
    )


def field_meta(av: dict) -> dict:
    """Extract content.additional_values → 필드별 신뢰도·좌표. 필드가 아닌 항목(step_run_id 등)은 버린다."""
    def one(v):
        if isinstance(v, dict) and "_value" in v:
            return {"confidence": v.get("confidence"), "score": v.get("confidence_score"), "page": v.get("page"),
                    "boxes": v.get("word_coordinates") or ([v["coordinates"]] if v.get("coordinates") else [])}
        if isinstance(v, list) and v and all(isinstance(x, dict) for x in v):  # items 같은 표: 행마다 같은 구조
            return [{k: m for k, x in row.items() if (m := one(x)) is not None} for row in v]
        return None
    return {k: m for k, v in (av or {}).items() if (m := one(v)) is not None}


def _unwrap(text: str) -> str:
    """'"invoice_no: …\\n…"' 처럼 JSON 문자열로 감싼 텍스트를 푼다. 객체 JSON이나 일반 텍스트는 그대로."""
    if text.startswith('"'):
        try:
            v = json.loads(text)
            return v if isinstance(v, str) else text
        except ValueError:
            pass
    return text


def _av(v) -> dict:
    if isinstance(v, str):
        try:
            v = json.loads(v)
        except ValueError:
            return {}
    return v if isinstance(v, dict) else {}


def _status(s: str) -> JobStatus:
    return {"queued": JobStatus.QUEUED, "in_progress": JobStatus.RUNNING,
            "completed": JobStatus.COMPLETED}.get(s, JobStatus.FAILED)


def to_check_result(resp: dict) -> CheckResult:
    """client.responses.retrieve(job_id).model_dump() 를 넣는다(include 없이 — 끝점이 4갈래라 'last'는 판정이 아닐 수 있음)."""
    out = CheckResult(job_id=resp["id"], status=_status(resp["status"]))
    if resp.get("error"):
        out.error = str(resp["error"].get("message") if isinstance(resp["error"], dict) else resp["error"])
    if out.status != JobStatus.COMPLETED:
        return out
    # 실측(2026-11-07 Agents API, include=["all"]): model = 'Information Extract - 인보이스_추출' / 'Instruct - 원산지_판정' 등,
    # Instruct text는 JSON 문자열로 한 번 감싸져 오고, additional_values도 JSON 문자열이다.
    for o in resp.get("output", []):
        name = o.get("model", "")
        text = "".join(_unwrap(c.get("text", "")) for c in o.get("content", []))
        avs = [_av(c.get("additional_values")) for c in o.get("content", [])]
        if JUDGE_NODE in name:
            out.judge = parse_judge(text)
            enum = next((a.get("enum") for a in avs if a.get("enum")), None)  # 분기에 실제로 쓰인 결정값
            if enum in {x.value for x in Decision}:
                out.judge.decision = Decision(enum)
        elif ext := next((k for k in EXTRACT_NODES if k in name), None):
            try:
                out.extracted.setdefault(ext, []).append(json.loads(text))
            except ValueError:
                out.extracted.setdefault(ext, []).append({"_raw": text})
            out.extracted_meta.setdefault(ext, []).append(field_meta(next((a for a in avs if a), {})))
        else:
            kind = next((k for k in FOLLOW_UP_NODES if k in name), None)
            if kind:
                out.follow_up = FollowUp(kind=kind, text=strip_cites(text))
    if out.judge:
        out.mismatches = [MismatchItem(code=c, stage=CODES.get(c, (None,))[0], label=CODES.get(c, (None, c))[1])
                          for c in out.judge.mismatch_codes]
    elif out.follow_up is None:
        out.error = out.error or "판정 노드 출력을 찾지 못함"
    return out


if __name__ == "__main__":
    from pathlib import Path

    fake = {
        "id": "job_demo", "status": "completed", "error": None,
        "output": [
            {"model": "step_9_원산지_판정", "content": [{"type": "output_text", "text":
                "invoice_no: SM-2609-0044\nreason: 원산지증명서 송장번호 SM-2609-0041이 인보이스 SM-2609-0044와 다름\n"
                'mismatch_codes: ["CO_INVOICE_NO_MISMATCH"]\nmissing_documents: []\ndecision: 보완 요청'}]},
            {"model": "step_10_보완_요청_초안", "content": [{"type": "output_text", "text": "제목: 원산지증명서 재발급 요청 …"}]},
            {"model": "step_3_인보이스_추출", "content": [{"type": "output_text", "text":
                '{"invoice_no": "SM-2609-0044", "items": [{"description": "FROZEN MACKEREL", "hs_code": "0303540000", "quantity": 18000}]}'}]},
        ],
    }
    r = to_check_result(fake)
    assert r.extracted["인보이스_추출"][0]["items"][0]["hs_code"] == "0303540000"
    box = [{"x": 0.1, "y": 0.1}, {"x": 0.2, "y": 0.1}, {"x": 0.2, "y": 0.12}, {"x": 0.1, "y": 0.12}]
    m = field_meta({"step_run_id": "x", "invoice_no": {"_value": "A", "confidence": "high", "confidence_score": 0.99, "page": 1,
                    "coordinates": [], "word_coordinates": [box]}, "items": [{"hs_code": {"_value": "1", "confidence": "low", "page": -1}}]})
    assert m == {"invoice_no": {"confidence": "high", "score": 0.99, "page": 1, "boxes": [box]},
                 "items": [{"hs_code": {"confidence": "low", "score": None, "page": -1, "boxes": []}}]}, m
    assert parse_judge('{"invoice_no":"A","reason":"r","mismatch_codes":["CO_CRITERION_NOT_PSR"],"missing_documents":[],"decision":"검토 필요"}').decision == Decision.REVIEW
    assert r.judge.decision == Decision.SUPPLEMENT and r.judge.mismatch_codes == ["CO_INVOICE_NO_MISMATCH"]
    assert r.mismatches[0].stage == 2 and r.follow_up.kind == "보완_요청_초안"
    assert parse_judge('{"invoice_no":"A","decision":"이상값","reason":"","mismatch_codes":[],"missing_documents":["B/L"]}').decision == Decision.REVIEW
    assert to_check_result({"id": "j", "status": "in_progress"}).judge is None
    assert to_check_result({"id": "j", "status": "cancelled"}).status == JobStatus.FAILED

    ex = Path(__file__).parent.parent / "examples"
    ex.mkdir(exist_ok=True)
    dump = lambda p, o: (ex / p).write_text(json.dumps(o, ensure_ascii=False, indent=2), encoding="utf-8", newline="\n")
    dump("check_created.json", CheckCreated(job_id="job_demo", status=JobStatus.QUEUED).model_dump(mode="json"))
    dump("check_result.json", r.model_dump(mode="json"))
    dump("schema.json", {m.__name__: m.model_json_schema() for m in (CheckCreated, CheckResult)})
    dump("codes.json", {c: {"stage": s, "label": l} for c, (s, l) in CODES.items()})
    print("ok", sorted(p.name for p in ex.iterdir()))

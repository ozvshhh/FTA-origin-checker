import os

from fastapi import FastAPI, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI

from dto import CheckCreated, CheckResult, to_check_result, _status

AGENT = os.environ.get("UPSTAGE_AGENT_ID", "agt_5HHQZgemm6hmRVLHyGu4dG")
CONFIG_ID = os.environ.get("UPSTAGE_CONFIG_ID", "5")
client = OpenAI(api_key=os.environ.get("UPSTAGE_API_KEY", ""), base_url="https://api.upstage.ai/v2",
                default_headers={"x-project-id": os.environ.get("UPSTAGE_PROJECT_ID", "proj_UVMuo7ZXt5heYMoei8ocJH")})

app = FastAPI(title="trade-doc-origin-check")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/checks", response_model=CheckCreated)
def create_check(file: UploadFile):
    if file.content_type != "application/pdf":
        raise HTTPException(415, "PDF만 받는다")
    up = client.files.create(file=(file.filename, file.file, "application/pdf"), purpose="user_data")
    resp = client.responses.create(model=AGENT, input=[{"role": "user", "content": [{"type": "input_file", "file_id": up.id}]}],
                                   extra_body={"config_id": CONFIG_ID})
    return CheckCreated(job_id=resp.id, status=_status(resp.status))


@app.get("/checks/{job_id}", response_model=CheckResult)
def get_check(job_id: str):
    # 기본 include=["last"]는 마지막 단계만 준다. 판정·추출까지 받으려면 "all"
    return to_check_result(client.responses.retrieve(job_id, include=["all"]).model_dump())

import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { originChecksApi } from '../api/originChecks'
import type { OriginCheckSummary, PageResponse } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'

/** S-02 심사목록 + M-03 새 심사 만들기. 필터·검색·탭은 화면명세서 §3 에 맞춰 채운다. */
export function ChecksPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<OriginCheckSummary> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showNew, setShowNew] = useState(false)

  useEffect(() => {
    originChecksApi
      .list({ page, size: 20 })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : '목록을 불러오지 못했습니다.'))
  }, [page])

  return (
    <>
      <div className="page-head">
        <h1>심사목록</h1>
        <button type="button" onClick={() => setShowNew(true)}>+ 새 심사</button>
      </div>

      {error && <p className="error">{error}</p>}
      {data && data.items.length === 0 && <p className="muted">아직 심사가 없습니다. 서류 PDF를 올려 시작하세요.</p>}
      {data && data.items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>제목</th>
              <th>송장번호</th>
              <th>상태</th>
              <th>등록일</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((c) => (
              <tr key={c.id}>
                <td><Link to={`/checks/${c.id}`}>{c.title}</Link></td>
                <td>{c.invoice_no ?? '-'}</td>
                <td><StatusBadge status={c.status} decision={c.decision} /></td>
                <td>{new Date(c.created_at).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {data && data.total_pages > 1 && (
        <div className="pager">
          <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>이전</button>
          <span>{page + 1} / {data.total_pages}</span>
          <button type="button" disabled={page + 1 >= data.total_pages} onClick={() => setPage((p) => p + 1)}>다음</button>
        </div>
      )}

      {showNew && <NewCheckModal onClose={() => setShowNew(false)} />}
    </>
  )
}

function NewCheckModal({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    const file = f.get('file')
    if (!(file instanceof File) || file.size === 0) {
      setError('PDF 파일을 선택하세요.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await originChecksApi.create(file, String(f.get('title') ?? '') || undefined)
      navigate(`/checks/${created.id}`) // 상세 화면에서 처리 중 → 완료를 기다린다
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '업로드하지 못했습니다.')
      setBusy(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={submit} className="form">
          <h2>새 심사 만들기</h2>
          <input name="title" placeholder="심사 제목 (비우면 파일명)" maxLength={200} autoFocus />
          <input name="file" type="file" accept="application/pdf" required />
          <p className="muted">한 선적 건의 서류(인보이스·패킹리스트·B/L·원산지증명서·원산지확인서·수출신고필증)를 이어 붙인 PDF, 최대 50MB</p>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={busy}>{busy ? '올리는 중…' : '점검 시작'}</button>
        </form>
      </div>
    </div>
  )
}

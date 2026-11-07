import { Link, useParams } from 'react-router-dom'
import { StatusBadge } from '../components/StatusBadge'
import { useOriginCheck } from '../hooks/useOriginCheck'

/**
 * S-03 심사 상세. 처리 중 / 완료 / 실패 세 상태.
 * 완료 화면 구성(판정 요약 · 불일치 · 후속 문서 · 추출값 · PDF 하이라이트)은 화면명세서 §5 에 맞춰 채운다.
 */
export function CheckDetailPage() {
  const { checkId } = useParams()
  const { data, error, loading } = useOriginCheck(checkId)

  if (loading) return <p className="muted">불러오는 중…</p>
  if (error || !data) return <p className="error">{error?.message ?? '심사를 찾을 수 없습니다.'}</p>

  return (
    <>
      <p><Link to="/checks">← 심사목록</Link></p>
      <div className="page-head">
        <h1>{data.title}</h1>
        <StatusBadge status={data.status} decision={data.judge?.decision} />
      </div>

      {(data.status === 'queued' || data.status === 'in_progress') && (
        <section className="card">
          <p>서류를 점검하고 있습니다. 보통 수 분 걸리며, 이 화면을 닫았다가 다시 와도 됩니다.</p>
        </section>
      )}

      {data.status === 'failed' && (
        <section className="card">
          <p className="error">점검에 실패했습니다. ({data.error?.code})</p>
          <p className="muted">{data.error?.message}</p>
        </section>
      )}

      {data.status === 'completed' && (
        <>
          {data.judge && (
            <section className="card">
              <h2>판정: {data.judge.decision}</h2>
              <p>송장번호 {data.judge.invoice_no ?? '-'}</p>
              <p>{data.judge.reason}</p>
              {data.judge.missing_documents.length > 0 && <p>누락 서류: {data.judge.missing_documents.join(', ')}</p>}
            </section>
          )}

          {data.mismatches.length > 0 && (
            <section className="card">
              <h2>불일치 항목</h2>
              <ul>
                {data.mismatches.map((m) => (
                  <li key={m.code}>[{m.stage ?? '?'}단계] {m.label} <code>{m.code}</code></li>
                ))}
              </ul>
            </section>
          )}

          {data.follow_up && (
            <section className="card">
              <h2>{data.follow_up.kind}</h2>
              <pre className="pre">{data.follow_up.text}</pre>
            </section>
          )}

          <section className="card">
            <h2>추출 결과</h2>
            <pre className="pre">{JSON.stringify(data.extracted, null, 2)}</pre>
          </section>

          {data.error && <p className="muted">참고: {data.error.message}</p>}
        </>
      )}
    </>
  )
}

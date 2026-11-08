import { useLocation, useNavigate, useOutletContext } from 'react-router-dom'
import { GROUPS, useChecks } from '../checks/ChecksContext'
import { keepListParams } from '../components/CheckSidebar'
import { IconInbox, IconPlus } from '../components/icons'

/** /checks — 아직 심사를 고르지 않았을 때 오른쪽 패널. 진행 중 건수를 왼쪽 목록과 같은 묶음·순서로 + 심사 생성 */
export function ChecksPage() {
  const { items, viewOf, groups, view, sort } = useChecks()
  const navigate = useNavigate()
  const { search } = useLocation()
  const open = (items ?? []).filter((c) => viewOf(c) === 'open')

  // 카드를 누르면 그 묶음의 첫 건을 연다. via: 'step' → 왼쪽 목록이 그 묶음을 펼친다
  const openGroup = (key: string) => {
    const shown = view === 'open' ? groups.find((g) => g.key === key)?.items : null
    const list = shown ?? open.filter(GROUPS.find((g) => g.key === key)!.match)
      .sort((a, b) => (sort === 'latest' ? -1 : 1) * a.created_at.localeCompare(b.created_at))
    const first = list[0]
    if (!first) return
    const p = new URLSearchParams(keepListParams(search))
    p.delete('view')
    p.delete('result')
    const qs = p.toString()
    navigate(`/checks/${first.id}${qs ? `?${qs}` : ''}`, { state: { via: 'step' } })
  }
  const { openNew } = useOutletContext<{ openNew: () => void }>()

  return (
    <div className="detail">
      <div className="home">
        <span className="home-icon"><IconInbox size={24} /></span>
        <h1>왼쪽에서 심사를 고르세요</h1>
        <button type="button" className="btn-primary" onClick={openNew}><IconPlus /> 심사 생성</button>

        {items != null && items.length > 0 && (
          <div className="home-cards">
            {GROUPS.map((g) => {
              const n = open.filter(g.match).length
              return (
                <button key={g.key} type="button" className="home-card" disabled={n === 0} onClick={() => openGroup(g.key)}>
                  <span className="home-card-label"><span className={`dot tone-${g.tone}`} />{g.label}</span>
                  <span className="home-card-num num">{n}</span>
                </button>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

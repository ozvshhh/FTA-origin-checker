import { createContext, useContext } from 'react'
import type { OriginCheckSummary } from '../api/types'
import type { CheckGroup, ResultFilter, TabKey } from './groups'

/** 담당자가 '완료 처리'한 심사(done_at 있음)는 진행 중 목록에서 빠지고 '완료' 보기로 옮겨진다 */
export type ListView = 'open' | 'done'

export interface ChecksState {
  items: OriginCheckSummary[] | null
  /** 진행 중(아직 완료 처리 안 함) / 완료 보기 */
  view: ListView
  setView: (v: ListView) => void
  /** 보기별 건수 (검색 전) */
  viewCounts: Record<ListView, number>
  /** 완료 처리 시각. 없으면 진행 중 */
  doneAt: (id: number) => string | null
  setDone: (id: number, done: boolean) => Promise<void>
  /** 완료 처리 실패 같은 짧은 알림 (몇 초 뒤 사라짐) */
  notice: string | null
  /**
   * 사용자가 좌측에서 보기(진행 중/완료)나 검색을 바꾼 직후의 목표 값. 주소가 이 값으로 바뀌면 상세가 새 목록을 따라간다.
   * 처리한 쪽이 consumeIntent() 로 지운다.
   */
  /** 사용자가 바꾼 보기·검색·결과 필터. 주소가 이 값이 된 뒤에 상세가 새 목록을 따라간다 */
  userIntent: { view: ListView; q: string; result?: ResultFilter } | null
  consumeIntent: () => void
  /** 방금 만든 심사를 목록에 바로 넣는다 (서버 목록을 다시 받기 전에 좌측에 보이게) */
  addLocal: (row: OriginCheckSummary) => void
  /** 서버 응답을 기다리지 않고 목록의 한 건을 고친다 (다시 실행 → queued 등) */
  patchLocal: (id: number, patch: Partial<OriginCheckSummary>) => void
  /** 삭제한 건을 목록에서 뺀다 */
  removeLocal: (id: number) => void
  /** 이 심사가 속한 보기 */
  viewOf: (c: OriginCheckSummary) => ListView
  /** 할 일 묶음 (검색 반영) */
  groups: CheckGroup[]
  /** groups 를 위에서부터 이어 붙인 순서 → 상세의 이전/다음 이동이 목록 순서와 같다 */
  filtered: OriginCheckSummary[]
  counts: Record<TabKey, number>
  error: string | null
  truncated: boolean
  tab: TabKey
  q: string
  sort: 'latest' | 'oldest'
  /** 완료 보기에서만 쓰는 결과 필터 */
  result: ResultFilter
  setFilter: (next: Partial<{ tab: TabKey; q: string; sort: 'latest' | 'oldest'; result: ResultFilter }>) => void
  reload: () => void
}

export const ChecksCtx = createContext<ChecksState | null>(null)

export function useChecks() {
  const v = useContext(ChecksCtx)
  if (!v) throw new Error('useChecks 는 ChecksProvider 안에서만 쓸 수 있습니다.')
  return v
}

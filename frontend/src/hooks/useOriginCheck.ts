import { useEffect, useState } from 'react'
import { originChecksApi } from '../api/originChecks'
import type { OriginCheckDetail } from '../api/types'

const POLL_MS = 5000

/**
 * 점검 상세를 불러오고, 처리 중(queued·in_progress)이면 5초마다 다시 조회한다.
 * completed·failed 가 되면 멈춘다 → 결과는 마지막 응답 하나에 모두 들어 있다.
 */
export function useOriginCheck(id: string | undefined) {
  const [data, setData] = useState<OriginCheckDetail | null>(null)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    let timer: ReturnType<typeof setTimeout> | undefined

    const load = async () => {
      try {
        const d = await originChecksApi.get(id)
        if (cancelled) return
        setData(d)
        setError(null)
        if (d.status === 'queued' || d.status === 'in_progress') timer = setTimeout(load, POLL_MS)
      } catch (e) {
        if (cancelled) return
        setError(e as Error)
      }
    }
    load()
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [id])

  return { data, error, loading: !data && !error }
}

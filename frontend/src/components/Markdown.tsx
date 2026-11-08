import { Fragment, type ReactNode } from 'react'

/**
 * 후속 문서(메일 초안 등) 표시용 아주 작은 마크다운 렌더러.
 * 지원: # ~ ####, - 목록, 빈 줄 문단, **굵게**. HTML 은 그대로 글자로 보인다(innerHTML 안 씀).
 */
export function Markdown({ text }: { text: string }) {
  const blocks: ReactNode[] = []
  let list: string[] = []
  let para: string[] = []
  const flush = () => {
    if (list.length) {
      blocks.push(<ul key={blocks.length}>{list.map((l, i) => <li key={i}>{inline(l)}</li>)}</ul>)
      list = []
    }
    if (para.length) {
      blocks.push(<p key={blocks.length}>{para.map((l, i) => <Fragment key={i}>{i > 0 && <br />}{inline(l)}</Fragment>)}</p>)
      para = []
    }
  }
  for (const line of text.split('\n')) {
    const h = line.match(/^(#{1,4})\s+(.*)$/)
    if (h) {
      flush()
      const level = h[1].length
      const Tag = level <= 3 ? 'h3' : 'h4'
      blocks.push(<Tag key={blocks.length}>{inline(h[2])}</Tag>)
    } else if (/^\s*[-*]\s+/.test(line)) {
      if (para.length) flush()
      list.push(line.replace(/^\s*[-*]\s+/, ''))
    } else if (line.trim() === '') {
      flush()
    } else {
      if (list.length) flush()
      para.push(line)
    }
  }
  flush()
  return <div className="md">{blocks}</div>
}

function inline(s: string): ReactNode[] {
  return s.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith('**') && part.endsWith('**') ? <strong key={i}>{part.slice(2, -2)}</strong> : <Fragment key={i}>{part}</Fragment>,
  )
}

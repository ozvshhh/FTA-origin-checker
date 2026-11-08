/* ───────── 메일에 붙여 넣기용 변환 ─────────
 * 메일 프로그램(Gmail·Outlook·네이버 메일)은 class 스타일을 지우므로 모양은 전부 인라인 style 로 준다.
 * html = 서식 있는 붙여넣기, plain = 서식을 못 받는 곳(메모장 등)용 깔끔한 글 */
const FONT = "font-family:'Malgun Gothic','Apple SD Gothic Neo',Arial,sans-serif;"
const S = {
  root: `${FONT}font-size:14px;line-height:1.7;color:#222;`,
  h3: 'margin:0 0 12px;font-size:16px;font-weight:bold;color:#111;',
  h4: 'margin:20px 0 6px;font-size:14px;font-weight:bold;color:#111;',
  p: 'margin:0 0 10px;',
  ul: 'margin:0 0 10px;padding-left:20px;',
  li: 'margin:0 0 4px;',
}

const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
const inlineHtml = (s: string) => esc(s).replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>')
const inlinePlain = (s: string) => s.replace(/\*\*([^*]+)\*\*/g, '$1')

export function markdownToEmail(text: string): { html: string; plain: string } {
  const html: string[] = []
  const plain: string[] = []
  let list: string[] = []
  let para: string[] = []
  const flush = () => {
    if (list.length) {
      html.push(`<ul style="${S.ul}">${list.map((l) => `<li style="${S.li}">${inlineHtml(l)}</li>`).join('')}</ul>`)
      plain.push(...list.map((l) => `• ${inlinePlain(l)}`), '')
      list = []
    }
    if (para.length) {
      html.push(`<p style="${S.p}">${para.map(inlineHtml).join('<br>')}</p>`)
      plain.push(...para.map(inlinePlain), '')
      para = []
    }
  }
  for (const line of text.split('\n')) {
    const h = line.match(/^(#{1,4})\s+(.*)$/)
    if (h) {
      flush()
      html.push(`<p style="${h[1].length <= 3 ? S.h3 : S.h4}">${inlineHtml(h[2])}</p>`)
      plain.push(inlinePlain(h[2]))
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
  return {
    html: `<div style="${S.root}">${html.join('')}</div>`,
    plain: plain.join('\n').replace(/\n{3,}/g, '\n\n').trim(),
  }
}

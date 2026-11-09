/**
 * 서류 코드 칩 (디자인 시스템 '검인'의 DocChip).
 * 서류 종류는 색이 아니라 실무 약어로 구분한다. 빠진 서류는 점선 + '없음'.
 */
const CODE: Record<string, string> = {
  인보이스: 'INV',
  패킹리스트: 'PL',
  'B/L': 'B/L',
  원산지증명서: 'C/O',
  원산지확인서: '확인서',
  수출신고필증: '신고필증',
}

function docCode(name: string) {
  return CODE[name] ?? name
}

export function DocChip({ name, missing = false, title }: { name: string; missing?: boolean; title?: string }) {
  return (
    <span className={`doc-chip${missing ? ' is-missing' : ''}`} title={title ?? name}>
      {docCode(name)}
      {missing && <span className="doc-chip-missing">없음</span>}
    </span>
  )
}

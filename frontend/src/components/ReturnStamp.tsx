import { useState } from 'react'

/**
 * 한국 회사 문서에 찍는 둥근 '반송' 일부인(日附印).
 * 겉 원 두 줄 · 위쪽 둥근 글씨(발신 부서) · 가운데 띠에 큰 오류 번호(404) · 아래 날짜와 '반송'.
 * 인주가 고르게 묻지 않은 느낌은 SVG 필터(가장자리 번짐 + 군데군데 빠진 잉크)로 낸다.
 */
function today() {
  const d = new Date()
  return `${d.getFullYear()}. ${String(d.getMonth() + 1).padStart(2, '0')}. ${String(d.getDate()).padStart(2, '0')}.`
}

export function ReturnStamp({ code = '404', size = 196 }: { code?: string; size?: number }) {
  // 찍은 날짜는 처음 그릴 때 한 번만 정한다
  const [date] = useState(today)
  return (
    <svg className="return-stamp" width={size} height={size} viewBox="0 0 200 200" role="img" aria-label={`${code} 반송 도장`}>
      <defs>
        <filter id="stamp-ink" x="-5%" y="-5%" width="110%" height="110%">
          {/* 가장자리가 종이에 스며든 듯 살짝 울퉁불퉁 */}
          <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed="4" result="grain" />
          <feDisplacementMap in="SourceGraphic" in2="grain" scale="2.4" xChannelSelector="R" yChannelSelector="G" result="rough" />
          {/* 인주가 덜 묻은 자리 */}
          <feTurbulence type="fractalNoise" baseFrequency="0.045" numOctaves="3" seed="11" result="blot" />
          <feColorMatrix in="blot" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  -1.7 0 0 0 1.62" result="blotMask" />
          {/* 잔 점처럼 빠진 자리 */}
          <feTurbulence type="fractalNoise" baseFrequency="1.6" numOctaves="1" seed="7" result="speck" />
          <feColorMatrix in="speck" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  -3 0 0 0 2.95" result="speckMask" />
          <feComposite in="rough" in2="blotMask" operator="in" result="inked" />
          <feComposite in="inked" in2="speckMask" operator="in" />
        </filter>
        <path id="stamp-top-arc" d="M 30 100 A 70 70 0 0 1 170 100" />
      </defs>

      <g filter="url(#stamp-ink)" className="return-stamp-ink">
        {/* 겉 원 두 줄 */}
        <circle cx="100" cy="100" r="93" fill="none" strokeWidth="5" />
        <circle cx="100" cy="100" r="85" fill="none" strokeWidth="1.6" />

        {/* 위쪽 둥근 글씨: 보낸 곳 */}
        <text className="return-stamp-arc" fontSize="15" letterSpacing="2.5">
          <textPath href="#stamp-top-arc" startOffset="50%" textAnchor="middle">FTA 원산지 점검</textPath>
        </text>

        {/* 가운데 띠 */}
        <line x1="17" y1="80" x2="183" y2="80" strokeWidth="2" />
        <line x1="19" y1="128" x2="181" y2="128" strokeWidth="2" />
        <text x="100" y="118" textAnchor="middle" fontSize="40" className="return-stamp-main" letterSpacing="6">{code}</text>

        {/* 아래: 날짜 · 문서번호 */}
        <text x="100" y="149" textAnchor="middle" fontSize="13" className="return-stamp-small">{date}</text>
        <text x="100" y="168" textAnchor="middle" fontSize="13" className="return-stamp-small" letterSpacing="4">반송</text>
      </g>
    </svg>
  )
}

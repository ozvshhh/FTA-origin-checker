/**
 * 국가 표시: 서류 원문(MALAYSIA · 말레이시아 · MYS …) → ISO 코드 · 한글 이름 · 국기.
 * 국기는 flag-icons(MIT) 의 SVG 를 필요한 나라만 가져온다. 이모지 국기는 윈도우에서 글자로 보여서 쓰지 않는다.
 * 목록에 없는 나라는 원문 그대로 보여 준다(국기 없음). 필요하면 아래 표에 한 줄 추가.
 */
import kr from 'flag-icons/flags/4x3/kr.svg?url'
import bn from 'flag-icons/flags/4x3/bn.svg?url'
import kh from 'flag-icons/flags/4x3/kh.svg?url'
import id from 'flag-icons/flags/4x3/id.svg?url'
import la from 'flag-icons/flags/4x3/la.svg?url'
import my from 'flag-icons/flags/4x3/my.svg?url'
import mm from 'flag-icons/flags/4x3/mm.svg?url'
import ph from 'flag-icons/flags/4x3/ph.svg?url'
import sg from 'flag-icons/flags/4x3/sg.svg?url'
import th from 'flag-icons/flags/4x3/th.svg?url'
import vn from 'flag-icons/flags/4x3/vn.svg?url'
import cn from 'flag-icons/flags/4x3/cn.svg?url'
import jp from 'flag-icons/flags/4x3/jp.svg?url'
import in_ from 'flag-icons/flags/4x3/in.svg?url'
import us from 'flag-icons/flags/4x3/us.svg?url'
import ca from 'flag-icons/flags/4x3/ca.svg?url'
import au from 'flag-icons/flags/4x3/au.svg?url'
import nz from 'flag-icons/flags/4x3/nz.svg?url'
import gb from 'flag-icons/flags/4x3/gb.svg?url'
import de from 'flag-icons/flags/4x3/de.svg?url'
import fr from 'flag-icons/flags/4x3/fr.svg?url'
import nl from 'flag-icons/flags/4x3/nl.svg?url'
import it from 'flag-icons/flags/4x3/it.svg?url'
import es from 'flag-icons/flags/4x3/es.svg?url'
import be from 'flag-icons/flags/4x3/be.svg?url'
import pl from 'flag-icons/flags/4x3/pl.svg?url'
import tr from 'flag-icons/flags/4x3/tr.svg?url'
import cl from 'flag-icons/flags/4x3/cl.svg?url'
import pe from 'flag-icons/flags/4x3/pe.svg?url'
import co from 'flag-icons/flags/4x3/co.svg?url'
import il from 'flag-icons/flags/4x3/il.svg?url'
import ae from 'flag-icons/flags/4x3/ae.svg?url'
import tw from 'flag-icons/flags/4x3/tw.svg?url'
import hk from 'flag-icons/flags/4x3/hk.svg?url'

export interface CountryInfo {
  code: string
  ko: string
  flag: string
}

const COUNTRIES: (CountryInfo & { aliases: string[] })[] = [
  { code: 'KR', ko: '대한민국', flag: kr, aliases: ['KOREA', 'REPUBLIC OF KOREA', 'SOUTH KOREA', 'KOR', '한국'] },
  { code: 'BN', ko: '브루나이', flag: bn, aliases: ['BRUNEI', 'BRUNEI DARUSSALAM', 'BRN'] },
  { code: 'KH', ko: '캄보디아', flag: kh, aliases: ['CAMBODIA', 'KHM'] },
  { code: 'ID', ko: '인도네시아', flag: id, aliases: ['INDONESIA', 'IDN'] },
  { code: 'LA', ko: '라오스', flag: la, aliases: ['LAOS', 'LAO PDR', 'LAO PEOPLE\'S DEMOCRATIC REPUBLIC', 'LAO'] },
  { code: 'MY', ko: '말레이시아', flag: my, aliases: ['MALAYSIA', 'MYS'] },
  { code: 'MM', ko: '미얀마', flag: mm, aliases: ['MYANMAR', 'BURMA', 'MMR'] },
  { code: 'PH', ko: '필리핀', flag: ph, aliases: ['PHILIPPINES', 'PHL'] },
  { code: 'SG', ko: '싱가포르', flag: sg, aliases: ['SINGAPORE', 'SGP'] },
  { code: 'TH', ko: '태국', flag: th, aliases: ['THAILAND', 'THA'] },
  { code: 'VN', ko: '베트남', flag: vn, aliases: ['VIETNAM', 'VIET NAM', 'VNM'] },
  { code: 'CN', ko: '중국', flag: cn, aliases: ['CHINA', 'PEOPLE\'S REPUBLIC OF CHINA', 'CHN'] },
  { code: 'JP', ko: '일본', flag: jp, aliases: ['JAPAN', 'JPN'] },
  { code: 'IN', ko: '인도', flag: in_, aliases: ['INDIA', 'IND'] },
  { code: 'US', ko: '미국', flag: us, aliases: ['UNITED STATES', 'USA', 'UNITED STATES OF AMERICA', 'U.S.A.'] },
  { code: 'CA', ko: '캐나다', flag: ca, aliases: ['CANADA', 'CAN'] },
  { code: 'AU', ko: '호주', flag: au, aliases: ['AUSTRALIA', 'AUS'] },
  { code: 'NZ', ko: '뉴질랜드', flag: nz, aliases: ['NEW ZEALAND', 'NZL'] },
  { code: 'GB', ko: '영국', flag: gb, aliases: ['UNITED KINGDOM', 'UK', 'GREAT BRITAIN', 'GBR'] },
  { code: 'DE', ko: '독일', flag: de, aliases: ['GERMANY', 'DEU'] },
  { code: 'FR', ko: '프랑스', flag: fr, aliases: ['FRANCE', 'FRA'] },
  { code: 'NL', ko: '네덜란드', flag: nl, aliases: ['NETHERLANDS', 'THE NETHERLANDS', 'HOLLAND', 'NLD'] },
  { code: 'IT', ko: '이탈리아', flag: it, aliases: ['ITALY', 'ITA'] },
  { code: 'ES', ko: '스페인', flag: es, aliases: ['SPAIN', 'ESP'] },
  { code: 'BE', ko: '벨기에', flag: be, aliases: ['BELGIUM', 'BEL'] },
  { code: 'PL', ko: '폴란드', flag: pl, aliases: ['POLAND', 'POL'] },
  { code: 'TR', ko: '튀르키예', flag: tr, aliases: ['TURKEY', 'TURKIYE', 'TÜRKIYE', 'TUR', '터키'] },
  { code: 'CL', ko: '칠레', flag: cl, aliases: ['CHILE', 'CHL'] },
  { code: 'PE', ko: '페루', flag: pe, aliases: ['PERU', 'PER'] },
  { code: 'CO', ko: '콜롬비아', flag: co, aliases: ['COLOMBIA', 'COL'] },
  { code: 'IL', ko: '이스라엘', flag: il, aliases: ['ISRAEL', 'ISR'] },
  { code: 'AE', ko: '아랍에미리트', flag: ae, aliases: ['UNITED ARAB EMIRATES', 'UAE', 'ARE'] },
  { code: 'TW', ko: '대만', flag: tw, aliases: ['TAIWAN', 'TWN'] },
  { code: 'HK', ko: '홍콩', flag: hk, aliases: ['HONG KONG', 'HKG'] },
]

const norm = (s: string) => s.trim().toUpperCase().replace(/\s+/g, ' ').replace(/^THE /, '')
const INDEX = new Map<string, CountryInfo>()
for (const c of COUNTRIES) {
  for (const k of [c.code, c.ko, ...c.aliases]) INDEX.set(norm(k), { code: c.code, ko: c.ko, flag: c.flag })
}

export function lookupCountry(raw: string | null | undefined): CountryInfo | null {
  if (!raw) return null
  return INDEX.get(norm(raw)) ?? null
}

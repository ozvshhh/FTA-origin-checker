import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

type Modal = 'login' | 'signup' | null

/** S-01 메인(랜딩) + M-01 로그인 / M-02 회원가입. 디자인은 화면명세서 §1·§2 에 맞춰 채운다. */
export function LandingPage() {
  const { user, loading } = useAuth()
  const [modal, setModal] = useState<Modal>(null)

  if (!loading && user) return <Navigate to="/checks" replace />

  return (
    <div className="container">
      <header className="gnb">
        <span className="logo">FTA 원산지 서류 점검</span>
        <div className="gnb-right">
          <button type="button" onClick={() => setModal('login')}>로그인</button>
          <button type="button" onClick={() => setModal('signup')}>회원가입</button>
        </div>
      </header>

      <section className="hero">
        <h1>FTA 원산지 서류, 보내기 전에 한 번에 점검하세요</h1>
        <p>인보이스·원산지증명서·원산지확인서·B/L·수출신고필증을 대조해 협정세율 적용 가능 여부를 알려 드립니다.</p>
        <button type="button" onClick={() => setModal('signup')}>시작하기</button>
      </section>

      <footer className="muted">최종 판단은 담당자가 합니다.</footer>

      {modal && (
        <div className="modal-backdrop" onClick={() => setModal(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            {modal === 'login' ? (
              <LoginForm onSwitch={() => setModal('signup')} />
            ) : (
              <SignupForm onSwitch={() => setModal('login')} />
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function LoginForm({ onSwitch }: { onSwitch: () => void }) {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    setBusy(true)
    setError(null)
    try {
      await login({ email: String(f.get('email')), password: String(f.get('password')) })
      navigate('/checks')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인하지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="form">
      <h2>로그인</h2>
      <input name="email" type="email" placeholder="이메일" required autoFocus />
      <input name="password" type="password" placeholder="비밀번호" required />
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button>
      <button type="button" className="link" onClick={onSwitch}>회원가입으로</button>
    </form>
  )
}

function SignupForm({ onSwitch }: { onSwitch: () => void }) {
  const { signup } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    if (f.get('password') !== f.get('passwordConfirm')) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await signup({
        email: String(f.get('email')),
        password: String(f.get('password')),
        name: String(f.get('name')),
        company: String(f.get('company') ?? '') || undefined,
      })
      navigate('/checks')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '가입하지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="form">
      <h2>회원가입</h2>
      <input name="name" placeholder="이름" required maxLength={50} autoFocus />
      <input name="company" placeholder="회사명 (선택)" maxLength={100} />
      <input name="email" type="email" placeholder="이메일" required />
      <input name="password" type="password" placeholder="비밀번호 (8자 이상)" required minLength={8} />
      <input name="passwordConfirm" type="password" placeholder="비밀번호 확인" required />
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={busy}>{busy ? '가입 중…' : '가입하기'}</button>
      <button type="button" className="link" onClick={onSwitch}>로그인으로</button>
    </form>
  )
}

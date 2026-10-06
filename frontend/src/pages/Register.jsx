import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import './Auth.css'

// Backend CreateUserRequest: { email, password, fullName }
// Sifre kurali @Size(min = 8) — ayni kontrolu formda da yapiyoruz ki
// kullanici 400 beklemeden hatayi aninda gorsun.
const BENEFITS = [
  'Sınırsız varlık ve işlem takibi',
  'Yapay zekâ destekli portföy analizi',
  'Anlık fiyat ve özel alarmlar',
]

const CheckIcon = () => (
  <svg width="16" height="16" viewBox="0 0 256 256" fill="currentColor">
    <path d="M232,128A104,104,0,1,1,128,24,104.11,104.11,0,0,1,232,128Zm-38.34-37.66a8,8,0,0,0-11.32,0L110,162.69,73.66,126.34a8,8,0,0,0-11.32,11.32l42,42a8,8,0,0,0,11.32,0l78-78A8,8,0,0,0,193.66,90.34Z" />
  </svg>
)

export default function Register() {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [password2, setPassword2] = useState('')
  const [accepted, setAccepted] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (password.length < 8) {
      setError('Şifre en az 8 karakter olmalı.')
      return
    }
    if (password !== password2) {
      setError('Şifreler eşleşmiyor.')
      return
    }
    if (!accepted) {
      setError('Devam etmek için kullanım şartlarını kabul etmelisin.')
      return
    }

    setLoading(true)
    try {
      // 1) Hesabi olustur — 201 Created doner, token icermez
      await api.post('/auth/register', { email, password, fullName })
      // 2) Ayni bilgilerle giris yapip token al, dogrudan dashboard'a gec
      const res = await api.post('/auth/login', { email, password })
      login(res.data.token, { email, fullName }, true)
      navigate('/')
    } catch (err) {
      const status = err?.response?.status
      if (status === 409) setError('Bu e-posta adresi zaten kayıtlı.')
      else if (status === 400) setError('Bilgileri kontrol et — e-posta geçersiz veya şifre çok kısa.')
      else setError('Sunucuya ulaşılamadı. Backend çalışıyor mu?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-shell">
      {/* ---------- Sol tanitim paneli ---------- */}
      <div className="panel">
        <div className="brand">
          <div className="mark">
            <svg width="26" height="26" viewBox="0 0 48 48" fill="none">
              <defs>
                <linearGradient id="authLinkGrad" x1="6" y1="42" x2="42" y2="6" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stopColor="#1668a8" />
                  <stop offset="50%" stopColor="#1fa5b4" />
                  <stop offset="100%" stopColor="#35e3bb" />
                </linearGradient>
              </defs>
              <rect x="5.5" y="21" width="26" height="17" rx="8.5"
                transform="rotate(-45 18.5 29.5)"
                stroke="url(#authLinkGrad)" strokeWidth="5" fill="none" strokeLinejoin="round" />
              <rect x="16.5" y="10" width="26" height="17" rx="8.5"
                transform="rotate(-45 29.5 18.5)"
                stroke="url(#authLinkGrad)" strokeWidth="5" fill="none" strokeLinejoin="round" />
            </svg>
          </div>
          <div>
            <div className="brand-name">CryptoTracker</div>
            <div className="brand-sub">AI-Powered Finance Platform</div>
          </div>
        </div>

        <div className="pitch">
          <div className="accent-rule" />
          <h2>Dakikalar içinde portföyünü kur.</h2>
          <p>Ücretsiz hesap aç, varlıklarını ekle ve ilk analizini hemen al.</p>
        </div>

        <div className="benefits">
          {BENEFITS.map(b => (
            <div className="benefit" key={b}><CheckIcon />{b}</div>
          ))}
        </div>
      </div>

      {/* ---------- Sag form sutunu ---------- */}
      <div className="form-col">
        <div className="form-inner wide">
          <h1 className="title">Hesap oluştur</h1>
          <p className="subtitle">Ücretsiz hesabını aç, portföyünü izlemeye hemen başla.</p>

          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="signup-name">Ad Soyad</label>
              <input
                className="input"
                id="signup-name"
                type="text"
                autoComplete="name"
                placeholder="Ad Soyad"
                value={fullName}
                onChange={e => setFullName(e.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="signup-email">E-posta</label>
              <input
                className="input"
                id="signup-email"
                type="email"
                autoComplete="email"
                placeholder="ornek@eposta.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="grid-2">
              <div className="field">
                <label htmlFor="signup-password">Şifre</label>
                <input
                  className="input"
                  id="signup-password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="••••••••"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="signup-password2">Şifre tekrar</label>
                <input
                  className="input"
                  id="signup-password2"
                  type="password"
                  autoComplete="new-password"
                  placeholder="••••••••"
                  value={password2}
                  onChange={e => setPassword2(e.target.value)}
                  required
                />
              </div>
            </div>

            <label className="terms">
              <input
                type="checkbox"
                checked={accepted}
                onChange={e => setAccepted(e.target.checked)}
              />
              <span>
                <a href="#">Kullanım şartlarını</a> ve <a href="#">gizlilik politikasını</a> okudum, kabul ediyorum.
              </span>
            </label>

            {error && <div className="auth-msg error">{error}</div>}

            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? 'Hesap oluşturuluyor...' : 'Kayıt Ol'}
              {!loading && (
                <svg width="14" height="14" viewBox="0 0 256 256" fill="currentColor">
                  <path d="M221.66,133.66l-72,72a8,8,0,0,1-11.32-11.32L196.69,136H40a8,8,0,0,1,0-16H196.69L138.34,61.66a8,8,0,0,1,11.32-11.32l72,72A8,8,0,0,1,221.66,133.66Z" />
                </svg>
              )}
            </button>
          </form>

          <p className="footer-line">Zaten hesabın var mı? <Link to="/login">Giriş yap</Link></p>
        </div>
      </div>
    </div>
  )
}

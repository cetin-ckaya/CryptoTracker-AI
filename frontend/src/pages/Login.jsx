import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useQuery } from '@tanstack/react-query'
import useLivePrices, { changePct } from '../hooks/useLivePrices'
import Coin from '../components/Coin'
import { money } from '../utils/format'
import api from '../api/axios'
import './Auth.css'

// Sol paneldeki canli fiyat seritleri — /topic/prices kanalindan beslenir.
// /ws endpoint'i SecurityConfig'de permitAll oldugu icin giris yapmadan da calisir.
const TICKERS = ['BTC', 'ETH', 'SOL']

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const { prices } = useLivePrices()

  // WebSocket yalnizca scheduler yayin yaptiginda (5 dakikada bir) veri
  // gonderiyor; o yuzden seritler acilista surekli "bekleniyor" yaziyordu.
  // Ilk degerleri /market ucundan cekiyoruz, canli guncelleme yine WebSocket'ten.
  const { data: market = [] } = useQuery({
    queryKey: ['market'],
    queryFn: () => api.get('/market').then(r => r.data),
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

  const marketBySymbol = Object.fromEntries(
    market.map(m => [String(m.symbol).toUpperCase(), m])
  )
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await api.post('/auth/login', { email, password })
      login(res.data.token, { email: res.data.email ?? email, fullName: res.data.fullName }, remember)
      navigate('/')
    } catch (err) {
      setError(err?.response?.status === 401 || err?.response?.status === 404
        ? 'E-posta veya şifre hatalı.'
        : 'Sunucuya ulaşılamadı. Backend çalışıyor mu?')
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
          <h2>Portföyün, tek ekranda ve gerçek zamanlı.</h2>
          <p>Varlıklarını izle, kar/zararını takip et, yapay zekâ destekli analizle kararlarını hızlandır.</p>
        </div>

        <div className="tickers">
          {TICKERS.map(sym => {
            // Oncelik: WebSocket (en taze) > /market ucu
            const entry = prices[sym]
            const m = marketBySymbol[sym]
            const price = entry ? entry.price : m?.price
            const pct = changePct(entry) ?? (m?.change24h != null ? Number(m.change24h) : null)
            return (
              <div className="ticker" key={sym}>
                <Coin sym={sym} src={m?.iconUrl} size={22} />
                <span className="tk-name">{sym}/USDT</span>
                <span className="tk-val">{price != null ? money(price) : '—'}</span>
                <span className={`tk-chg ${pct == null ? '' : pct >= 0 ? 'up' : 'down'}`}>
                  {pct == null ? (price != null ? 'güncel' : 'bekleniyor') : `${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%`}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      {/* ---------- Sag form sutunu ---------- */}
      <div className="form-col">
        <div className="form-inner">
          <h1 className="title">Giriş yap</h1>
          <p className="subtitle">Hesabına giriş yaparak portföyünü takip etmeye devam et.</p>

          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="login-email">E-posta</label>
              <input
                className="input"
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="ornek@eposta.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="login-password">Şifre</label>
              <input
                className="input"
                id="login-password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
              />
            </div>

            <div className="row-between">
              <label className="remember">
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={e => setRemember(e.target.checked)}
                />
                Beni hatırla
              </label>
              <a className="small-link" href="#">Şifremi unuttum</a>
            </div>

            {error && <div className="auth-msg error">{error}</div>}

            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
              {!loading && (
                <svg width="14" height="14" viewBox="0 0 256 256" fill="currentColor">
                  <path d="M221.66,133.66l-72,72a8,8,0,0,1-11.32-11.32L196.69,136H40a8,8,0,0,1,0-16H196.69L138.34,61.66a8,8,0,0,1,11.32-11.32l72,72A8,8,0,0,1,221.66,133.66Z" />
                </svg>
              )}
            </button>
          </form>

          <p className="footer-line">Hesabın yok mu? <Link to="/register">Kayıt ol</Link></p>
        </div>
      </div>
    </div>
  )
}

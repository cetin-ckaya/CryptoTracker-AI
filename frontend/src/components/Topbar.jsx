import { useQuery } from '@tanstack/react-query'
import { Bell, ChevronDown } from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import './Topbar.css'

// Topbar sayfanin tam genisligini kaplar ve logo onun sol ucunda,
// sidebar sutunuyla ayni genislikte bir blok olarak durur.
//
// Fiyat seritleri burada YOK: ayni veri "Canli Fiyatlar" panelinde
// sparkline ve 24 saatlik degisimle birlikte gosteriliyor.
export default function Topbar() {
  const { displayName } = useAuth()

  // Plan etiketi onceden "Premium Üye" olarak SABIT yaziliydi; FREE hesapta
  // bile Premium gorunuyordu. Artik abonelik ucundan okunuyor.
  const { data: subscription } = useQuery({
    queryKey: ['subscription'],
    queryFn: () => api.get('/subscription').then(r => r.data),
    staleTime: 5 * 60 * 1000,
  })

  // Veri gelene kadar bir sey yazmiyoruz: yanlis plan gostermektense
  // bos birakmak dogru.
  const planLabel = subscription?.tier === 'PREMIUM' ? 'Premium Üye'
    : subscription?.tier === 'FREE' ? 'Ücretsiz Üye'
    : ''

  return (
    <header className="topbar">
      <div className="topbar-logo">
        <div className="topbar-logo-icon">
          <svg width="36" height="36" viewBox="0 0 48 48" fill="none">
            <defs>
              <linearGradient id="linkGrad" x1="6" y1="42" x2="42" y2="6" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="#1668a8" />
                <stop offset="50%" stopColor="#1fa5b4" />
                <stop offset="100%" stopColor="#35e3bb" />
              </linearGradient>
            </defs>
            {/* alt-sol halka: merkez (18.5, 29.5) */}
            <rect x="5.5" y="21" width="26" height="17" rx="8.5"
              transform="rotate(-45 18.5 29.5)"
              stroke="url(#linkGrad)" strokeWidth="5" fill="none" strokeLinejoin="round" />
            {/* ust-sag halka: merkez (29.5, 18.5) — kosegende digerinin icinden gecer */}
            <rect x="16.5" y="10" width="26" height="17" rx="8.5"
              transform="rotate(-45 29.5 18.5)"
              stroke="url(#linkGrad)" strokeWidth="5" fill="none" strokeLinejoin="round" />
          </svg>
        </div>
        <div>
          <div className="topbar-logo-title">CryptoTracker</div>
          <div className="topbar-logo-sub">AI-Powered Finance Platform</div>
        </div>
      </div>

      <div className="topbar-spacer" />

      <div className="topbar-user">
        <div className="topbar-bell">
          <Bell size={18} strokeWidth={1.8} />
          <span className="bell-badge">3</span>
        </div>
        <div className="user-avatar">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <circle cx="10" cy="7" r="3.5" fill="#8892b0"/>
            <path d="M3 18c0-3.866 3.134-7 7-7s7 3.134 7 7" stroke="#8892b0" strokeWidth="1.5" fill="none"/>
          </svg>
        </div>
        <div className="user-text">
          <div className="user-name">{displayName}</div>
          <div className={`user-tier ${subscription?.tier === 'PREMIUM' ? 'premium' : 'free'}`}>
            {planLabel}
          </div>
        </div>
        <ChevronDown size={14} color="#5a6280" />
      </div>
    </header>
  )
}

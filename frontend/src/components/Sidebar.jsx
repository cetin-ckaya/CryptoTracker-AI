import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { NavLink, useNavigate } from 'react-router-dom'
import api from '../api/axios'
import UpgradeModal from './UpgradeModal'
import { LayoutDashboard, Wallet, ArrowLeftRight, Bot, LogOut, Activity, Server, ScrollText, Sparkles } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import './Sidebar.css'

// Piyasalar, Alarmlar, Raporlar ve Ayarlar menuden kaldirildi.
// Alarmlar (D002) ve Raporlar (D008) zaten kapsam disi kararlari;
// menude durup bos sayfaya gitmeleri yaniltiyordu.
const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/portfolio', icon: Wallet, label: 'Portföyüm' },
  { to: '/transactions', icon: ArrowLeftRight, label: 'İşlemlerim' },
  { to: '/ai', icon: Bot, label: 'AI Analiz', badge: 'YENİ', badgeType: 'purple' },
]

const systemItems = [
  // API Durumu tasarimda ikon yerine yesil durum noktasiyla gosteriliyor
  { icon: Activity, label: 'API Durumu', dot: true },
  { icon: Server, label: 'Servis Sağlığı' },
  { icon: ScrollText, label: 'Loglar' },
]

export default function Sidebar() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [yukseltAcik, setYukseltAcik] = useState(false)

  const { data: subscription } = useQuery({
    queryKey: ['subscription'],
    queryFn: () => api.get('/subscription').then(r => r.data),
    staleTime: 5 * 60 * 1000,
  })

  const ucretsiz = subscription?.tier === 'FREE'

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        {navItems.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}
          >
            <item.icon size={17} />
            <span>{item.label}</span>
            {item.badge && (
              <span className={`sidebar-badge badge-${item.badgeType}`}>{item.badge}</span>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-section-label">SİSTEM</div>
      <nav className="sidebar-nav sidebar-system">
        {systemItems.map(item => (
          <div key={item.label} className="sidebar-item">
            {item.dot ? <span className="sidebar-dot" /> : <item.icon size={17} />}
            <span>{item.label}</span>
          </div>
        ))}
        <div className="sidebar-item logout" onClick={handleLogout}>
          <LogOut size={17} />
          <span>Çıkış Yap</span>
        </div>
      </nav>

      {/* Yukseltme karti yalnizca ucretsiz kullaniciya gorunur.
          Premium olunca kaybolur; plan degisikligi topbar'dan ve
          AI Analiz sayfasindan yonetilir. */}
      {ucretsiz && (
        <div className="sidebar-upgrade" onClick={() => setYukseltAcik(true)}>
          <div className="su-head">
            <Sparkles size={15} />
            <span>Ücretsiz plan</span>
          </div>
          <div className="su-text">Portföyünüze özel AI analizi için Premium’a geçin.</div>
          <div className="su-btn">Yükselt</div>
        </div>
      )}

      <UpgradeModal
        open={yukseltAcik}
        onClose={() => setYukseltAcik(false)}
        mevcutTier={subscription?.tier}
      />

      <div className="sidebar-status">
        <span className="status-dot green" />
        <div>
          <div className="status-title">Sistem Durumu</div>
          <div className="status-sub">Tüm servisler çalışıyor</div>
          <div className="status-time">Son güncelleme: {new Date().toLocaleTimeString('tr-TR')}</div>
        </div>
      </div>
    </aside>
  )
}

import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Wallet, ArrowLeftRight, Bot, LogOut, Activity, Server, ScrollText } from 'lucide-react'
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

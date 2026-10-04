import { useState, useEffect } from 'react'
import './Statusbar.css'

export default function Statusbar() {
  const [time, setTime] = useState(new Date())

  useEffect(() => {
    const t = setInterval(() => setTime(new Date()), 1000)
    return () => clearInterval(t)
  }, [])

  const fmt = (d) => d.toLocaleString('tr-TR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  }).replace(',', '')

  return (
    <footer className="statusbar">
      <div className="sb-item">
        <span className="sb-label">Sistem Saati:</span>
        <span>{time.toLocaleTimeString('tr-TR')}</span>
      </div>
      <div className="sb-item">
        <span className="sb-label">API Durumu:</span>
        <span className="sb-dot green" />
        <span className="sb-ok">Aktif</span>
      </div>
      <div className="sb-item">
        <span className="sb-label">WebSocket:</span>
        <span className="sb-dot green" />
        <span className="sb-ok">Bağlı</span>
      </div>
      <div className="sb-item">
        <span className="sb-label">Cache:</span>
        <span className="sb-dot green" />
        <span className="sb-ok">Redis Aktif</span>
      </div>
      <div className="sb-item">
        <span className="sb-label">Son Güncelleme:</span>
        <span>{fmt(time)}</span>
      </div>
      <div className="sb-item sb-right">
        <span style={{ color: '#626d88' }}>CryptoTracker</span>
      </div>
    </footer>
  )
}

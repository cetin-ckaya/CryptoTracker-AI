import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { X, Check, Sparkles, Loader2, AlertTriangle } from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import './UpgradeModal.css'

// Planda gercekten ne var, ne yok.
// Olmayan ozellik yazmiyoruz: Alarmlar ve Raporlar kapsam disi kararlariydi
// (D002, D008), onlari Premium vaadi diye listelemek yaniltici olurdu.
const PLANLAR = [
  {
    ad: 'Ücretsiz',
    tier: 'FREE',
    maddeler: [
      { metin: 'Sınırsız varlık ve işlem takibi', var: true },
      { metin: 'Canlı fiyatlar ve portföy grafiği', var: true },
      { metin: 'BTC, ETH, SOL için genel AI yorumu', var: true },
      { metin: 'Portföyünüze özel AI analizi', var: false },
    ],
  },
  {
    ad: 'Premium',
    tier: 'PREMIUM',
    maddeler: [
      { metin: 'Sınırsız varlık ve işlem takibi', var: true },
      { metin: 'Canlı fiyatlar ve portföy grafiği', var: true },
      { metin: 'BTC, ETH, SOL için genel AI yorumu', var: true },
      { metin: 'Portföyünüzdeki her coin için kişiselleştirilmiş analiz', var: true },
    ],
  },
]

export default function UpgradeModal({ open, onClose, mevcutTier }) {
  const [yukleniyor, setYukleniyor] = useState(false)
  const [hata, setHata] = useState('')
  const queryClient = useQueryClient()
  const { setToken } = useAuth()

  if (!open) return null

  const premium = mevcutTier === 'PREMIUM'

  async function planDegistir(yol) {
    setYukleniyor(true)
    setHata('')
    try {
      const res = await api.post(`/subscription/${yol}`)

      // TAZE TOKEN. Yetki JWT'nin icinde tasiniyor; eski token yerinde
      // kalirsa veritabani PREMIUM dese de kullanici degisikligi gormez.
      if (res.data?.token) setToken(res.data.token)

      // Plana bagli ekranlar yeniden cekilsin
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      queryClient.invalidateQueries({ queryKey: ['ai-analyze'] })
      onClose()
    } catch (e) {
      setHata(e?.response?.data?.message ?? 'İşlem tamamlanamadı, tekrar deneyin.')
    } finally {
      setYukleniyor(false)
    }
  }

  return (
    <div className="up-overlay" onClick={onClose}>
      <div className="up-modal" onClick={e => e.stopPropagation()}>
        <button className="up-close" onClick={onClose} aria-label="Kapat"><X size={18} /></button>

        <div className="up-head">
          <div className="up-ico"><Sparkles size={22} /></div>
          <div>
            <h2>{premium ? 'Planınız: Premium' : 'Premium’a yükselt'}</h2>
            <p>Portföyünüzdeki her coin için kişiselleştirilmiş yapay zekâ analizi.</p>
          </div>
        </div>

        <div className="up-plans">
          {PLANLAR.map(plan => (
            <div
              key={plan.tier}
              className={`up-plan ${plan.tier === 'PREMIUM' ? 'one-cikan' : ''} ${
                plan.tier === mevcutTier ? 'mevcut' : ''}`}
            >
              <div className="up-plan-head">
                <span className="up-plan-ad">{plan.ad}</span>
                {plan.tier === mevcutTier && <span className="up-rozet">Mevcut plan</span>}
              </div>
              <ul>
                {plan.maddeler.map(m => (
                  <li key={m.metin} className={m.var ? '' : 'yok'}>
                    {m.var ? <Check size={14} /> : <X size={14} />}
                    <span>{m.metin}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="up-demo">
          <AlertTriangle size={14} />
          <span>
            Bu bir demo. Ödeme alınmaz, kart bilgisi istenmez; plan anında değişir.
          </span>
        </div>

        {hata && <div className="up-hata">{hata}</div>}

        <div className="up-actions">
          <button className="up-btn-ghost" onClick={onClose} disabled={yukleniyor}>
            Vazgeç
          </button>
          {premium ? (
            <button className="up-btn-ghost" onClick={() => planDegistir('downgrade')} disabled={yukleniyor}>
              {yukleniyor ? <><Loader2 size={15} className="spin" /> İşleniyor</> : 'Ücretsiz plana dön'}
            </button>
          ) : (
            <button className="up-btn-solid" onClick={() => planDegistir('upgrade')} disabled={yukleniyor}>
              {yukleniyor ? <><Loader2 size={15} className="spin" /> İşleniyor</> : 'Premium’a geç'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

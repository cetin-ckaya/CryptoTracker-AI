import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { X, Search, Loader2 } from 'lucide-react'
import api from '../api/axios'
import Coin from './Coin'
import './AddTransactionModal.css'

// Backend CreateTransactionRequest: { coinId, type, quantity, pricePerUnit }
// coinId gerektigi icin coin listesini GET /coins ile cekiyoruz.

export default function AddTransactionModal({ open, onClose }) {
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(null)
  const [showList, setShowList] = useState(false)
  const [type, setType] = useState('BUY')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const searchRef = useRef(null)
  const queryClient = useQueryClient()

  // Coin katalogu — modal ilk acildiginda bir kez cekilir, sonra cache'ten gelir
  const { data: coins = [], isLoading: coinsLoading, isError: coinsError } = useQuery({
    queryKey: ['coins'],
    queryFn: () => api.get('/coins').then(r => r.data),
    enabled: open,
    staleTime: 60 * 60 * 1000,
  })

  // Modal her acildiginda formu sifirla ve arama kutusuna odaklan
  useEffect(() => {
    if (!open) return
    setQuery(''); setSelected(null); setShowList(false)
    setType('BUY'); setQuantity(''); setPrice(''); setError(''); setSaving(false)
    const t = setTimeout(() => searchRef.current?.focus(), 50)
    return () => clearTimeout(t)
  }, [open])

  // Escape ile kapat
  useEffect(() => {
    if (!open) return
    const onKey = e => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  // "BT" yazilinca hem sembolde hem isimde eslesenler listelenir
  const matches = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return coins.slice(0, 8)
    return coins
      .filter(c =>
        c.symbol?.toLowerCase().includes(q) ||
        c.name?.toLowerCase().includes(q))
      // Sembolu sorguyla baslayanlar once gelsin
      .sort((a, b) => {
        const as = a.symbol?.toLowerCase().startsWith(q) ? 0 : 1
        const bs = b.symbol?.toLowerCase().startsWith(q) ? 0 : 1
        return as - bs
      })
      .slice(0, 8)
  }, [coins, query])

  function pick(coin) {
    setSelected(coin)
    setQuery(`${coin.name} (${coin.symbol})`)
    setShowList(false)
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (!selected) return setError('Listeden bir coin seçmelisin.')
    const q = Number(String(quantity).replace(',', '.'))
    const p = Number(String(price).replace(',', '.'))
    if (!Number.isFinite(q) || q <= 0) return setError('Miktar sıfırdan büyük bir sayı olmalı.')
    if (!Number.isFinite(p) || p <= 0) return setError('Fiyat sıfırdan büyük bir sayı olmalı.')

    setSaving(true)
    try {
      await api.post('/transactions', {
        coinId: selected.id,
        type,
        quantity: q,
        pricePerUnit: p,
      })
      // Islem hem islem gecmisini hem portfoyu degistirir — ikisini de tazele
      queryClient.invalidateQueries({ queryKey: ['transactions'] })
      queryClient.invalidateQueries({ queryKey: ['portfolio'] })
      onClose()
    } catch (err) {
      const s = err?.response?.status
      if (s === 400) setError('Bilgileri kontrol et — eksik veya geçersiz alan var.')
      else if (s === 404) setError('Coin veya portföy bulunamadı.')
      else if (s === 401 || s === 403) setError('Oturumun düşmüş olabilir, tekrar giriş yap.')
      else setError('Sunucuya ulaşılamadı. Backend çalışıyor mu?')
    } finally {
      setSaving(false)
    }
  }

  if (!open) return null

  return (
    <div className="modal-overlay" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className="modal-card" role="dialog" aria-modal="true" aria-label="Yeni İşlem Ekle">
        <div className="modal-head">
          <h2>Yeni İşlem Ekle</h2>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Kapat">
            <X size={18} />
          </button>
        </div>

        <form className="modal-body" onSubmit={handleSubmit}>
          {/* ---------- Coin arama + otomatik tamamlama ---------- */}
          <div className="combo">
            <div className="combo-input">
              <Search size={15} className="combo-icon" />
              <input
                ref={searchRef}
                type="text"
                placeholder="Coin ara (isim veya sembol)"
                value={query}
                autoComplete="off"
                onChange={e => { setQuery(e.target.value); setSelected(null); setShowList(true) }}
                onFocus={() => setShowList(true)}
              />
              {selected && <span className="combo-badge">{selected.symbol}</span>}
            </div>

            {showList && (
              <div className="combo-list">
                {coinsLoading && (
                  <div className="combo-state"><Loader2 size={14} className="spin" /> Coinler yükleniyor...</div>
                )}
                {coinsError && (
                  <div className="combo-state err">Coin listesi alınamadı. Backend'de /api/v1/coins ucu var mı?</div>
                )}
                {!coinsLoading && !coinsError && matches.length === 0 && (
                  <div className="combo-state">"{query}" ile eşleşen coin yok.</div>
                )}
                {matches.map(c => (
                  <button type="button" className="combo-item" key={c.id} onClick={() => pick(c)}>
                    <Coin sym={c.symbol} size={24} />
                    <span className="ci-name">{c.name}</span>
                    <span className="ci-sym">{c.symbol}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* ---------- Islem turu ---------- */}
          <select className="modal-field" value={type} onChange={e => setType(e.target.value)}>
            <option value="BUY">Alış İşlemi (BUY)</option>
            <option value="SELL">Satış İşlemi (SELL)</option>
          </select>

          {/* ---------- Miktar ve fiyat ---------- */}
          <input
            className="modal-field"
            type="text"
            inputMode="decimal"
            placeholder="Miktar (Örn: 0.5)"
            value={quantity}
            onChange={e => setQuantity(e.target.value)}
          />

          <input
            className="modal-field"
            type="text"
            inputMode="decimal"
            placeholder="İşlemi Yaptığınız Fiyat ($)"
            value={price}
            onChange={e => setPrice(e.target.value)}
          />

          {/* Secili coin ve miktar varsa toplam tutari onizle */}
          {selected && Number(quantity) > 0 && Number(price) > 0 && (
            <div className="modal-preview">
              Toplam tutar:{' '}
              <b>
                $ {(Number(String(quantity).replace(',', '.')) * Number(String(price).replace(',', '.')))
                  .toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </b>
            </div>
          )}

          {error && <div className="modal-error">{error}</div>}

          <div className="modal-actions">
            <button type="button" className="btn-cancel" onClick={onClose} disabled={saving}>
              İptal
            </button>
            <button type="submit" className="btn-save" disabled={saving}>
              {saving ? 'Kaydediliyor...' : 'İşlemi Kaydet'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

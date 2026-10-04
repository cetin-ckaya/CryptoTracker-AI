import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, Plus, MoreHorizontal } from 'lucide-react'
import api from '../api/axios'
import Coin from '../components/Coin'
import AddTransactionModal from '../components/AddTransactionModal'
import useLivePrices from '../hooks/useLivePrices'
import { money, money0, amount } from '../utils/format'
import './Pages.css'

const num = v => (v == null ? 0 : Number(v))
const tl = money
const tl0 = money0
const pct = n => (num(n) >= 0 ? '+' : '−') + Math.abs(num(n)).toFixed(2).replace('.', ',') + '%'
const qty = amount

export default function Portfolio() {
  const [open, setOpen] = useState({})
  const [addOpen, setAddOpen] = useState(false)
  const [sortByValue, setSortByValue] = useState(true)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { prices: live } = useLivePrices()

  const { data: portfolio, isLoading, isFetching } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => api.get('/portfolio').then(r => r.data),
  })

  // Islem gecmisi — "Ilk Alim" ve "Islem Sayisi" detaylari buradan hesaplanir
  const { data: transactions = [] } = useQuery({
    queryKey: ['transactions'],
    queryFn: () => api.get('/transactions').then(r => r.data),
  })

  // Piyasa fiyatlari — WebSocket 5 dakikada bir yayin yaptigi icin sayfa
  // acilisinda fiyatlar bos kalmasin diye bu uctan da cekiyoruz.
  // Dashboard ile ayni anahtari paylasir, ikinci bir istek atilmaz.
  const { data: market = [] } = useQuery({
    queryKey: ['market'],
    queryFn: () => api.get('/market').then(r => r.data),
    refetchInterval: 5 * 60 * 1000,
  })

  const holdings = portfolio?.holdings ?? []

  // Her holding icin: maliyet her zaman hesaplanabilir (miktar x ort. alis),
  // guncel deger ise ancak WebSocket'ten o coinin fiyati geldiyse bilinir.
  const marketBySymbol = Object.fromEntries(
    market.map(m => [String(m.symbol).toUpperCase(), m])
  )

  const rows = holdings.map(h => {
    const sym = h.coinSymbol
    const key = String(sym ?? '').toUpperCase()
    const quantity = num(h.quantity)
    const avg = num(h.averageBuyPrice)
    const cost = quantity * avg

    // Fiyat onceligi: WebSocket (en taze) > /market ucu > backend holding alani
    const m = marketBySymbol[key]
    const price = live[key]?.price
      ?? (m ? num(m.price) : (h.currentPrice != null ? num(h.currentPrice) : null))
    const change24h = m ? num(m.change24h) : null
    const value = price != null ? quantity * price : null
    const coinTx = transactions.filter(t => t.coinSymbol === sym)
    const firstBuy = coinTx.length
      ? coinTx.map(t => new Date(t.transactionDate)).sort((a, b) => a - b)[0]
      : null
    return {
      id: h.id ?? sym,
      sym,
      name: h.coinName ?? sym,
      quantity, avg, cost, price, value, change24h,
      pnl: value != null ? value - cost : null,
      pnlPct: value != null && cost > 0 ? ((value - cost) / cost) * 100 : null,
      txCount: coinTx.length,
      firstBuy,
    }
  })

  // Pay yuzdesi: canli fiyat varsa guncel degere, yoksa maliyete gore
  const hasLive = rows.some(r => r.value != null)
  const base = r => (hasLive ? (r.value ?? 0) : r.cost)
  const totalBase = rows.reduce((s, r) => s + base(r), 0)
  const share = r => (totalBase ? (base(r) / totalBase) * 100 : 0)

  const sorted = sortByValue
    ? [...rows].sort((a, b) => base(b) - base(a))
    : [...rows].sort((a, b) => a.name.localeCompare(b.name, 'tr'))

  const syncTime = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })

  function toggle(id) {
    setOpen(o => ({ ...o, [id]: !o[id] }))
  }

  return (
    <div className="page">
      <AddTransactionModal open={addOpen} onClose={() => setAddOpen(false)} />
      <div className="page-head">
        <div>
          <h1>Portföyüm</h1>
          <div className="sub">Portföyünüzün genel görünümü · son senkronizasyon {syncTime}</div>
        </div>
        <div className="head-actions">
          <button
            className="btn-ghost"
            disabled={isFetching}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['portfolio'] })}
          >
            <RefreshCw size={15} /> {isFetching ? 'Yenileniyor' : 'Yenile'}
          </button>
          <button className="btn-solid" onClick={() => setAddOpen(true)}>
            <Plus size={15} /> İşlem Ekle
          </button>
        </div>
      </div>

      {/* ---------- Varliklarim ---------- */}
      <div className="panel-box">
        <div className="panel-head">
          <div>
            <div className="panel-title">Varlıklarım</div>
            <div className="panel-hint">Satıra tıklayarak detayı aç</div>
          </div>
          <span className="pill" onClick={() => setSortByValue(v => !v)}>
            {sortByValue ? 'Değere göre' : 'İsme göre'} ▾
          </span>
        </div>

        <div className="scroll-x">
          <div className="holding-grid holding-head">
            <div />
            <div>VARLIK</div>
            <div className="right">GÜNCEL DEĞER</div>
            <div className="right">MİKTAR</div>
            <div className="right">KAR / ZARAR</div>
            <div className="right">K/Z %</div>
            <div className="right">ORT. ALIŞ</div>
            <div />
          </div>

          {isLoading && <div className="state-box"><span className="spinner" />Yükleniyor</div>}

          {!isLoading && sorted.length === 0 && (
            <div className="state-box">Portföyünüzde henüz varlık yok.</div>
          )}

          {sorted.map(r => (
            <div key={r.id}>
              <div className="holding-grid holding-row" onClick={() => toggle(r.id)}>
                <div><span className={`chev ${open[r.id] ? 'open' : ''}`}>›</span></div>

                <div className="coin-cell">
                  <Coin sym={r.sym} size={32} />
                  <div><div className="nm">{r.name}</div><div className="sy">{r.sym}</div></div>
                </div>

                <div className="right">
                  <div className="strong num">{r.value != null ? tl(r.value) : tl(r.cost)}</div>
                  <div className="dim">
                    {share(r).toFixed(1).replace('.', ',')}% pay{r.value == null ? ' · maliyet' : ''}
                  </div>
                </div>

                <div className="right num soft">{qty(r.quantity, r.sym)}</div>

                <div className="right">
                  {r.pnl == null ? (
                    <div className="dim">fiyat bekleniyor</div>
                  ) : (
                    <div className={`strong num ${r.pnl >= 0 ? 'green' : 'red'}`}>
                      {r.pnl >= 0 ? '+ ' : '− '}{tl(Math.abs(r.pnl))}
                    </div>
                  )}
                  <div className="dim">maliyet {tl(r.cost)}</div>
                </div>

                <div className={`right strong num ${r.pnlPct == null ? 'dim' : r.pnlPct >= 0 ? 'green' : 'red'}`}>
                  {r.pnlPct == null ? '—' : pct(r.pnlPct)}
                </div>

                <div className="right num soft">{tl(r.avg)}</div>

                <div className="right"><MoreHorizontal size={16} color="#5f6b80" /></div>
              </div>

              {open[r.id] && (
                <div className="holding-detail">
                  <div className="detail-card">
                    <div><div className="k">Ort. Alış Fiyatı</div><div className="v">{tl(r.avg)}</div></div>
                    <div>
                      <div className="k">Güncel Fiyat</div>
                      <div className="v">
                        {r.price != null ? tl(r.price) : '—'}
                        {r.change24h != null && (
                          <span className={`chg ${r.change24h >= 0 ? 'green' : 'red'}`}>
                            {pct(r.change24h)}
                          </span>
                        )}
                      </div>
                    </div>
                    <div>
                      <div className="k">İlk Alım</div>
                      <div className="v">
                        {r.firstBuy
                          ? r.firstBuy.toLocaleDateString('tr-TR', { day: '2-digit', month: 'short', year: 'numeric' })
                          : '—'}
                      </div>
                    </div>
                    <div>
                      <div className="k">İşlem Sayısı</div>
                      <div className="v">{r.txCount} işlem</div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                      <span className="link-purple" onClick={e => { e.stopPropagation(); navigate('/transactions') }}>
                        İşlemleri gör ›
                      </span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="panel-foot">
          <span>{sorted.length} varlık · toplam {tl0(portfolio?.totalValue)}</span>
          <span className="link-purple" onClick={() => navigate('/transactions')}>
            İşlem geçmişini görüntüle ›
          </span>
        </div>
      </div>

    </div>
  )
}

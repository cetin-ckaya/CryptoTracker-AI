import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowUpRight, ArrowDownRight, Plus } from 'lucide-react'
import api from '../api/axios'
import Coin from '../components/Coin'
import AddTransactionModal from '../components/AddTransactionModal'
import { money, amount } from '../utils/format'
import './Pages.css'

// Tarih araligi secenekleri — gun sayisi null ise filtre uygulanmaz
const RANGES = [
  { key: '7', label: 'Son 7 Gün', days: 7 },
  { key: '30', label: 'Son 30 Gün', days: 30 },
  { key: '365', label: 'Son 1 Yıl', days: 365 },
  { key: 'all', label: 'Tümü', days: null },
]

const num = v => (v == null ? 0 : Number(v))
const tl = money
const qty = n => amount(n)

// "24 Tem, 11:32" formati
function fmtDate(iso) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const date = d.toLocaleDateString('tr-TR', { day: '2-digit', month: 'short' })
  const time = d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
  return `${date}, ${time}`
}

export default function Transactions() {
  const [addOpen, setAddOpen] = useState(false)
  const [range, setRange] = useState('30')
  const [coinFilter, setCoinFilter] = useState('all')
  const [typeFilter, setTypeFilter] = useState('all')

  const { data: transactions = [], isLoading } = useQuery({
    queryKey: ['transactions'],
    queryFn: () => api.get('/transactions').then(r => r.data),
  })

  // Filtre kutusundaki varlik listesi — kullanicinin gercek islemlerinden uretilir
  const coins = useMemo(
    () => [...new Set(transactions.map(t => t.coinSymbol))].sort(),
    [transactions]
  )

  const filtered = useMemo(() => {
    const days = RANGES.find(r => r.key === range)?.days
    const limit = days ? Date.now() - days * 24 * 60 * 60 * 1000 : null
    return transactions
      .filter(t => (limit ? new Date(t.transactionDate).getTime() >= limit : true))
      .filter(t => (coinFilter === 'all' ? true : t.coinSymbol === coinFilter))
      .filter(t => (typeFilter === 'all' ? true : t.type === typeFilter))
      .sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate))
  }, [transactions, range, coinFilter, typeFilter])

  // Ozet: secili aralikta ne kadar alim, ne kadar satim yapilmis
  const bought = filtered.filter(t => t.type === 'BUY').reduce((s, t) => s + num(t.totalAmount), 0)
  const sold = filtered.filter(t => t.type === 'SELL').reduce((s, t) => s + num(t.totalAmount), 0)

  return (
    <div className="page">
      <AddTransactionModal open={addOpen} onClose={() => setAddOpen(false)} />

      <div className="page-head">
        <div>
          <h1>İşlemlerim</h1>
          <div className="sub">Tüm alım ve satım işlemlerinizin listesi</div>
        </div>
        <div className="head-actions">
          <button className="btn-solid" onClick={() => setAddOpen(true)}>
            <Plus size={15} /> İşlem Ekle
          </button>
        </div>
      </div>

      <div className="panel-box">
        {/* ---------- Filtreler ---------- */}
        <div className="filters">
          <div>
            <div className="filter-label">Tarih Aralığı</div>
            <div className="seg">
              {RANGES.map(r => (
                <button
                  key={r.key}
                  className={range === r.key ? 'on' : ''}
                  onClick={() => setRange(r.key)}
                >
                  {r.label}
                </button>
              ))}
            </div>
          </div>

          <div>
            <div className="filter-label">Varlık</div>
            <select className="select-box" value={coinFilter} onChange={e => setCoinFilter(e.target.value)}>
              <option value="all">Tüm varlıklar</option>
              {coins.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>

          <div>
            <div className="filter-label">Tip</div>
            <select className="select-box" value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
              <option value="all">Satın Al, Satış</option>
              <option value="BUY">Satın Al</option>
              <option value="SELL">Satış</option>
            </select>
          </div>
        </div>

        {/* ---------- Tablo ---------- */}
        <div className="scroll-x">
          <div className="tx-grid tx-head">
            <div>TARİH &amp; SAAT</div>
            <div>VARLIK</div>
            <div>İŞLEM TÜRÜ</div>
            <div className="right">MİKTAR</div>
            <div className="right">BİRİM FİYAT</div>
            <div className="right">TOPLAM</div>
          </div>

          {isLoading && <div className="state-box"><span className="spinner" />Yükleniyor</div>}

          {!isLoading && filtered.length === 0 && (
            <div className="state-box">
              {transactions.length === 0
                ? 'Henüz işlem kaydınız yok.'
                : 'Seçili filtrelere uyan işlem bulunamadı.'}
            </div>
          )}

          {filtered.map(t => {
            const buy = t.type === 'BUY'
            return (
              <div className="tx-grid tx-row" key={t.id}>
                <div className="tx-date">{fmtDate(t.transactionDate)}</div>

                <div className="tx-coin">
                  <Coin sym={t.coinSymbol} size={28} />
                  <span>{t.coinSymbol}</span>
                </div>

                <div className={`tx-type ${buy ? 'green' : 'red'}`}>
                  {buy ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
                  {buy ? 'Satın Al' : 'Satış'}
                </div>

                <div className={`right strong num ${buy ? 'green' : 'red'}`}>
                  {buy ? '+' : '−'}{qty(t.quantity)} {t.coinSymbol}
                </div>

                <div className="right num soft">{tl(t.pricePerUnit)}</div>

                <div className="right strong num">{tl(t.totalAmount)}</div>
              </div>
            )
          })}
        </div>

        <div className="panel-foot">
          <span>{filtered.length} işlem</span>
          <span>
            <span className="green">Alım {tl(bought)}</span>
            {'  ·  '}
            <span className="red">Satım {tl(sold)}</span>
          </span>
        </div>
      </div>
    </div>
  )
}

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  AreaChart, Area, LineChart, Line, BarChart, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts'
import {
  DollarSign, TrendingUp, TrendingDown, Wallet, Layers, Plus,
  Info, ChevronRight, FileText, Loader2,
} from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import useLivePrices from '../hooks/useLivePrices'
import Coin from '../components/Coin'
import AddTransactionModal from '../components/AddTransactionModal'
import { money, money0, moneyShort, percent } from '../utils/format'
import './Dashboard.css'

/* ---------- Yardimcilar ---------- */
const num = v => (v == null ? 0 : Number(v))
// Para bicimleme utils/format.js'ten gelir — fiyatlar CoinGecko'dan dolar olarak cekiliyor
const tl = money
const tl0 = money0
const shortTl = moneyShort
const pctStr = percent

// Dagilim halkasi renkleri
const SLICE = ['#F7931A', '#7b6cf0', '#34d399', '#6c5ce7', '#8fb3ff', '#f0b429', '#f87171', '#22d3ee']

function Spark({ data, color }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data.map((v, i) => ({ i, v }))}
        margin={{ top: 2, right: 0, bottom: 2, left: 0 }}>
        {/* YAxis yazilmazsa recharts varsayilan olarak 0'dan baslayan bir
            alan kuruyor; 84.000-85.000 arasi gezinen fiyat o olcekte duz
            bir cizgi gibi gorunuyordu. domain'i veriye daraltinca
            7 gunluk hareket gercekten gorunur hale geliyor. */}
        <YAxis hide domain={['dataMin', 'dataMax']} />
        <Line type="monotone" dataKey="v" stroke={color} strokeWidth={1.6} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  )
}

// "Canli Fiyatlar" panelinde gosterilecek sabit liste ve sirasi.
// Katalogda 100 coin var ama bu panel bir izleme listesi; hepsini
// listelemek paneli kullanilmaz hale getirirdi.
const LIVE_SYMBOLS = ['BTC', 'ETH', 'BNB', 'SOL', 'XRP']

// Portfoy deger grafigindeki aralik butonlari
const RANGES = ['1G', '1H', '1A', '3A', '1Y', 'Tümü']

export default function Dashboard() {
  const [addOpen, setAddOpen] = useState(false)
  const [range, setRange] = useState('1G')
  const navigate = useNavigate()
  const { firstName } = useAuth()
  const { prices: live, connected } = useLivePrices()

  // react-query ile cekiyoruz ki islem eklendiginde modal'in gonderdigi
  // invalidateQueries(['portfolio']) sinyali buraya da ulassin ve
  // sayfa yenilenmeden kartlar guncellensin.
  const { data: portfolio, isLoading: loading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => api.get('/portfolio').then(r => r.data),
  })

  // Canli fiyatlar paneli: takip edilen coinlerin fiyati, 24 saatlik degisimi
  // ve sparkline'i. WebSocket 5 dakikada bir yayin yaptigi icin sayfa acilisinda
  // ekranin bos kalmamasi adina once bu uctan cekiliyor.
  const { data: market = [], isLoading: marketLoading } = useQuery({
    queryKey: ['market'],
    queryFn: () => api.get('/market').then(r => r.data),
    refetchInterval: 5 * 60 * 1000,
  })

  // Portfoy deger grafigi: GERCEK snapshot'lar. Aralik anahtarin parcasi
  // oldugu icin butona basildiginda react-query yeni veriyi kendisi cekiyor.
  const { data: history = [], isLoading: historyLoading } = useQuery({
    queryKey: ['portfolio-history', range],
    queryFn: () => api
      .get('/portfolio/history', { params: { range: range === 'Tümü' ? 'TUMU' : range } })
      .then(r => r.data),
  })

  // AI analizi her portfoy degisiminde yeniden cagrilmasin diye ayri bir
  // anahtarda ve uzun staleTime ile tutuluyor — her istek Groq'a gidiyor.
  const { data: aiText, isLoading: aiLoading, error: aiErr } = useQuery({
    queryKey: ['ai-analyze'],
    queryFn: () => api.get('/ai/analyze').then(r => r.data),
    staleTime: 10 * 60 * 1000,
    retry: false,
  })

  const ai = aiLoading
    ? { state: 'loading' }
    : aiErr
      ? { state: aiErr?.response?.status === 403 ? 'locked' : 'error' }
      : { state: 'ok' }

  // Sinyaller ve maddeler artik backend'den yapilandirilmis geliyor;
  // metni regex ile ayiklamaya gerek kalmadi.
  const signals = aiText?.signals ?? []
  const bullets = signals.map(s => s.comment).filter(Boolean).slice(0, 4)

  /* ----- Tum ozet degerleri kullanicinin gercek portfoyunden ----- */
  const holdings = portfolio?.holdings ?? []
  const totalValue = num(portfolio?.totalValue)
  const invested = num(portfolio?.totalInvested)
  const pnl = num(portfolio?.totalProfitLoss)
  const pnlPct = num(portfolio?.totalProfitLossPercentage)
  const up = pnl >= 0
  const assetCount = holdings.length

  // Gunluk kar/zarar: 24 saat onceki portfolio_value_history snapshot'ina gore.
  // Backend bu alanlari doldurana kadar null kalir — kart "—" gosterir.
  const daily = portfolio?.dailyProfitLoss != null ? Number(portfolio.dailyProfitLoss) : null
  const dailyPct = portfolio?.dailyProfitLossPercentage != null
    ? Number(portfolio.dailyProfitLossPercentage)
    : null
  const dailyUp = (daily ?? 0) >= 0

  // Sembol -> piyasa verisi sozlugu. /market ucu coin basina guncel fiyat ve
  // 24 saatlik degisim donuyor; HoldingResponse'ta bu alanlar olmadigi icin
  // tablodaki "Guncel Fiyat" ve "Toplam Deger" kolonlari buradan hesaplaniyor.
  const marketBySymbol = Object.fromEntries(
    market.map(m => [String(m.symbol).toUpperCase(), m])
  )

  // Panel sirasi LIVE_SYMBOLS'un sirasini izler; CoinGecko'nun piyasa
  // degeri sirasina birakilsa liste her cagri sonrasi yer degistirebilirdi.
  const livePanel = LIVE_SYMBOLS
    .map(sym => marketBySymbol[sym])
    .filter(Boolean)

  const rows = holdings.map((h, i) => {
    const qty = num(h.quantity)
    const avg = num(h.averageBuyPrice)
    const cost = qty * avg
    const sym = h.coinSymbol

    // Fiyat onceligi: WebSocket (en taze) > /market ucu > backend holding alani
    const m = marketBySymbol[String(sym).toUpperCase()]
    const wsPrice = live[String(sym).toUpperCase()]?.price
    const price = wsPrice ?? (m ? num(m.price) : (h.currentPrice != null ? num(h.currentPrice) : null))

    const value = price != null ? qty * price : null
    const change24h = m ? num(m.change24h) : null
    // Varligin gunluk degisimi = guncel deger x 24 saatlik yuzde degisim
    const dailyChange = value != null && change24h != null ? value * (change24h / 100) : null

    return {
      id: h.id ?? sym,
      sym,
      name: h.coinName ?? sym,
      qty, avg, cost, price, value,
      change24h,
      dailyChange,
      color: SLICE[i % SLICE.length],
    }
  })

  const hasLivePrices = rows.length > 0 && rows[0].price != null
  const costTotal = rows.reduce((s, r) => s + r.cost, 0)

  // Varlik dagilimi: canli fiyat varsa guncel degere, yoksa maliyete gore
  const allAllocation = rows.map(r => {
    const base = hasLivePrices ? (r.value ?? 0) : r.cost
    const denom = hasLivePrices ? totalValue : costTotal
    return {
      name: `${r.name} (${r.sym})`,
      pct: denom ? Number(((base / denom) * 100).toFixed(1)) : 0,
      value: base,
      color: r.color,
    }
  }).sort((a, b) => b.value - a.value)

  // Tasarimdaki gibi: en buyuk 4 varlik ayri, kalanlar tek bir "Diger"
  // diliminde toplanir. Cok varlikli portfoyde lejant tasmasin diye.
  const TOP_N = 4
  const allocation = allAllocation.length > TOP_N + 1
    ? [
        ...allAllocation.slice(0, TOP_N),
        {
          name: 'Diğer',
          pct: Number(allAllocation.slice(TOP_N)
            .reduce((s, a) => s + a.pct, 0).toFixed(1)),
          value: allAllocation.slice(TOP_N).reduce((s, a) => s + a.value, 0),
          color: '#8fa0c4',
        },
      ]
    : allAllocation

  // Satirin portfoy payi. allocation listesi siralanip gruplandigi icin
  // tablo satiriyla indeks eslesmesi kurulamaz, paydan yeniden hesaplaniyor.
  const share = r => {
    const base = hasLivePrices ? (r.value ?? 0) : r.cost
    const denom = hasLivePrices ? totalValue : costTotal
    return denom ? (base / denom) * 100 : 0
  }

  // Etiket formati araliga gore degisir: 1 gunde saat, uzun araliklarda tarih.
  const series = history.map(p => {
    const d = new Date(p.recordedAt)
    return {
      t: range === '1G'
        ? d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
        : d.toLocaleDateString('tr-TR', { day: '2-digit', month: 'short' }),
      v: Number(p.totalValue),
    }
  })
  const sparkSeed = up ? [12, 15, 13, 18, 16, 21, 19, 24, 22, 27, 25, 30] : [30, 26, 28, 23, 25, 20, 22, 17, 19, 14, 16, 12]
  const barSeed = [22, 30, 18, 36, 26, 42, 30, 48, 38, 55, 44, 62, 50, 68]

  // Gruplanmamis listeden: aksi halde en buyuk varlik "Diger" cikabilirdi
  const topAsset = allAllocation[0]

  const dash = <span className="muted">—</span>

  return (
    <div className="dash">
      <AddTransactionModal open={addOpen} onClose={() => setAddOpen(false)} />
      {/* Baslik */}
      <div className="dash-head">
        <div>
          <h1>Dashboard</h1>
          <p>Portföyünüzün genel görünümü</p>
        </div>
        <div className="dash-head-actions">
          <button className="btn-add" onClick={() => setAddOpen(true)}><Plus size={16} /> İşlem Ekle</button>
        </div>
      </div>

      {/* Ozet kartlari — hepsi kullanicinin portfoyunden */}
      <div className="stats">
        <div className="card stat">
          <div className="stat-top">
            <span className="stat-label">Toplam Portföy Değeri</span>
            <div className="stat-ico green"><DollarSign size={20} /></div>
          </div>
          <div className="stat-value">{loading ? '—' : tl(totalValue)}</div>
          {!loading && (
            <div className={`stat-sub ${up ? 'green' : 'red'}`}>
              {up ? '+ ' : '- '}{tl(Math.abs(pnl))} ({pnlPct.toFixed(2)}%)
            </div>
          )}
          {totalValue > 0 && (
            <div className="stat-spark">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={sparkSeed.map((v, i) => ({ i, v }))}>
                  <defs>
                    <linearGradient id="sg" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={up ? '#34d399' : '#f87171'} stopOpacity="0.35" />
                      <stop offset="100%" stopColor={up ? '#34d399' : '#f87171'} stopOpacity="0" />
                    </linearGradient>
                  </defs>
                  <Area type="monotone" dataKey="v" stroke={up ? '#34d399' : '#f87171'} strokeWidth={2} fill="url(#sg)" dot={false} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="card stat">
          <div className="stat-top">
            <span className="stat-label">Günlük Kar/Zarar</span>
            <div className={`stat-ico ${dailyUp ? 'blue' : 'red'}`}>
              {dailyUp ? <TrendingUp size={20} /> : <TrendingDown size={20} />}
            </div>
          </div>
          <div className={`stat-value ${daily == null ? '' : dailyUp ? 'green' : 'red'}`}>
            {loading || daily == null ? '—' : (dailyUp ? '+ ' : '- ') + tl(Math.abs(daily))}
          </div>
          {!loading && (
            daily == null
              ? <div className="stat-sub">Son 24 saatlik kayıt bekleniyor</div>
              : <div className={`stat-sub ${dailyUp ? 'green' : 'red'}`}>{pctStr(dailyPct)}</div>
          )}
          {daily != null && (
            <div className="stat-spark">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={barSeed.map((v, i) => ({ i, v }))}>
                  <Bar dataKey="v" fill={dailyUp ? '#4a8ef0' : '#f87171'} fillOpacity={0.75} radius={[2, 2, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="card stat">
          <div className="stat-top">
            <span className="stat-label">Toplam Yatırım</span>
            <div className="stat-ico amber"><Wallet size={20} /></div>
          </div>
          <div className="stat-value">{loading ? '—' : tl(invested)}</div>
          {!loading && <div className="stat-sub">Yatırılan toplam tutar</div>}
        </div>

        <div className="card stat">
          <div className="stat-top">
            <span className="stat-label">Aktif Varlık Sayısı</span>
            <div className="stat-ico violet"><Layers size={20} /></div>
          </div>
          <div className="stat-value">{loading ? '—' : assetCount}</div>
          {!loading && <div className="stat-sub">Farklı kripto para</div>}
        </div>
      </div>

      {/* Grafik + Dagilim */}
      <div className="mid-grid">
        <div className="card">
          <div className="card-head">
            <div className="card-title">Portföy Değeri Grafiği <Info size={14} color="#6f7b96" /></div>
            <div className="ranges">
              {RANGES.map(r => (
                <button
                  key={r}
                  className={r === range ? 'on' : ''}
                  onClick={() => setRange(r)}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>
          <div className="big-chart">
            {historyLoading ? (
              <div className="empty">Yükleniyor...</div>
            ) : series.length < 2 ? (
              <div className="empty">
                Bu aralık için yeterli kayıt yok.<br />
                Portföy değeri saat başı kaydediliyor; grafik zamanla dolacak.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={series} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                  <defs>
                    <linearGradient id="pg" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#6c5ce7" stopOpacity="0.28" />
                      <stop offset="100%" stopColor="#6c5ce7" stopOpacity="0" />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 4" stroke="#1e2334" vertical={false} />
                  {/* interval sabit 15'ti: 96 noktali uydurma seride dogruydu ama gercek
                      veride nokta sayisi arakliga gore degisiyor ve tek etiket kaliyordu.
                      minTickGap, sigdigi kadar etiketi kendisi seciyor. */}
                  <XAxis dataKey="t" tick={{ fontSize: 11, fill: '#6f7b96' }} axisLine={false}
                    tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                  <YAxis tick={{ fontSize: 11, fill: '#6f7b96' }} axisLine={false} tickLine={false} width={62}
                    domain={['dataMin', 'dataMax']} tickFormatter={shortTl} />
                  <Tooltip formatter={v => [tl(v), '']}
                    contentStyle={{ background: '#1b2033', border: '1px solid #2b3149', borderRadius: 8, fontSize: 12 }}
                    labelStyle={{ color: '#8b95b0', fontSize: 11 }} itemStyle={{ color: '#f1f5f9' }} />
                  <Area type="monotone" dataKey="v" stroke="#7b6cf0" strokeWidth={2} fill="url(#pg)"
                    dot={false} activeDot={{ r: 4, fill: '#7b6cf0', stroke: '#fff', strokeWidth: 1.5 }} />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-head"><div className="card-title">Varlık Dağılımı</div></div>
          {allocation.length === 0 ? (
            <div className="empty tall">Henüz varlığınız yok.</div>
          ) : (
            <div className="alloc-body">
              <div className="donut">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={allocation} dataKey="pct" nameKey="name"
                      innerRadius={62} outerRadius={98} paddingAngle={1} stroke="none">
                      {allocation.map(a => <Cell key={a.name} fill={a.color} />)}
                    </Pie>
                  </PieChart>
                </ResponsiveContainer>
                <div className="donut-mid">
                  <small>Toplam</small>
                  <b>{shortTl(totalValue)}</b>
                </div>
              </div>
              <div className="legend">
                {allocation.map(a => (
                  <div className="legend-row" key={a.name}>
                    <i style={{ background: a.color }} />
                    <span className="legend-name">{a.name}</span>
                    <span className="legend-pct">{a.pct}%</span>
                    <span className="legend-val">{tl0(a.value)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Varliklar + AI + Canli fiyatlar */}
      <div className="bot-grid">
        <div className="card">
          <div className="card-head"><div className="card-title">Portföy Varlıklarım</div></div>
          <table className="assets-table">
            <thead>
              <tr>
                <th>Varlık</th><th>Miktar</th>
                <th>{hasLivePrices ? 'Güncel Fiyat' : 'Ort. Alış Fiyatı'}</th>
                <th>{hasLivePrices ? 'Toplam Değer' : 'Toplam Maliyet'}</th>
                <th>{hasLivePrices ? 'Günlük Değişim' : 'Portföy Payı'}</th>
              </tr>
            </thead>
            <tbody>
              {rows.slice(0, 5).map(r => (
                <tr key={r.id}>
                  <td>
                    <div className="asset-cell">
                      <Coin sym={r.sym} size={30} />
                      <div><b>{r.name}</b><span>({r.sym})</span></div>
                    </div>
                  </td>
                  <td className="muted">{r.qty} {r.sym}</td>
                  <td>
                    {tl(hasLivePrices ? r.price : r.avg)}
                    {r.change24h != null && (
                      <span className={`cell-sub ${r.change24h >= 0 ? 'pos' : 'neg'}`}>
                        {pctStr(r.change24h)}
                      </span>
                    )}
                  </td>
                  <td>{tl(hasLivePrices ? r.value : r.cost)}</td>
                  {r.dailyChange != null ? (
                    <td className={r.dailyChange >= 0 ? 'pos' : 'neg'}>
                      {r.dailyChange >= 0 ? '+ ' : '- '}{tl(Math.abs(r.dailyChange))}
                      <span className={`cell-sub ${r.change24h >= 0 ? 'pos' : 'neg'}`}>
                        {pctStr(r.change24h)}
                      </span>
                    </td>
                  ) : (
                    <td>{share(r).toFixed(1)}%</td>
                  )}
                </tr>
              ))}
              {!loading && rows.length === 0 && (
                <tr><td colSpan={5}><div className="empty">Portföyünüzde henüz varlık yok.</div></td></tr>
              )}
            </tbody>
          </table>
          <div className="card-foot" onClick={() => navigate('/portfolio')}>
            Tüm Varlıkları Görüntüle <ChevronRight size={15} />
          </div>
        </div>

        <div className="card">
          <div className="card-head">
            <div className="card-title">AI Destekli Portföy Analizi <span className="badge-new">YENİ</span></div>
            <span className="ai-model">Groq · gpt-oss</span>
          </div>
          <div className="ai-box">
            <div className="ai-hi">Merhaba {firstName}! 👋</div>
            {ai.state === 'loading' && (
              <div className="ai-lead"><Loader2 size={14} className="spin" /> Portföyünüz analiz ediliyor...</div>
            )}
            {ai.state === 'locked' && (
              <div className="ai-lead">AI portföy analizi Premium üyeliğe özeldir. Yükseltmek için AI Analiz sayfasına göz atın.</div>
            )}
            {ai.state === 'error' && (
              <div className="ai-lead">Analiz şu anda alınamadı. AI Analiz sayfasından tekrar deneyebilirsiniz.</div>
            )}
            {ai.state === 'ok' && (
              <>
                <div className="ai-lead">
                  {aiText?.headline ?? (assetCount > 0
                    ? `Portföyünüzde ${assetCount} farklı varlık var${topAsset ? `, en büyüğü ${topAsset.name} (%${topAsset.pct})` : ''}.`
                    : 'Henüz varlığınız yok. İlk işleminizi ekleyince analiz burada görünecek.')}
                </div>
                <div className="ai-list">
                  {bullets.map((b, i) => (
                    <div className="ai-item" key={i}><span>{['✅', '✅', '⚠️', '💡'][i] ?? '•'}</span><span>{b}</span></div>
                  ))}
                </div>
              </>
            )}
          </div>
          {signals.length > 0 && (
            <>
              <div className="ai-sign-title">Al/Sat Önerisi:</div>
              <div className="ai-signals">
                {signals.map(s => (
                  <span key={s.symbol} className={`sig ${s.action === 'AL' ? 'buy' : s.action === 'SAT' ? 'sell' : 'hold'}`}>
                    {s.symbol}: {s.action}
                  </span>
                ))}
              </div>
            </>
          )}
          <div className="ai-note">* Yatırım tavsiyesi değildir.</div>
          <button className="ai-btn" onClick={() => navigate('/ai')}>
            Detaylı Analiz Raporu <FileText size={16} />
          </button>
        </div>

        <div className="card">
          <div className="card-head">
            <div className="card-title">Canlı Fiyatlar</div>
            <span className={`live-badge ${connected ? '' : 'off'}`}>
              <i /> {connected ? 'CANLI' : 'BAĞLANIYOR'}
            </span>
          </div>
          <div className="live-list">
            {marketLoading && <div className="empty">Piyasa verisi yükleniyor...</div>}
            {!marketLoading && livePanel.length === 0 && (
              <div className="empty">Piyasa verisi alınamadı.</div>
            )}
            {livePanel.map(m => {
              // WebSocket'ten o sembol icin daha yeni bir fiyat geldiyse onu kullan
              const wsPrice = live[m.symbol?.toUpperCase()]?.price
              const shown = wsPrice ?? num(m.price)
              const chg = num(m.change24h)
              const up = chg >= 0
              return (
                <div className="live-row" key={m.symbol}>
                  <Coin sym={m.symbol} size={30} />
                  <span className="live-pair">{m.symbol}/USDT</span>
                  <div className="live-nums">
                    <span className="live-price">{money(shown)}</span>
                    <span className={`live-chg ${up ? 'pos' : 'neg'}`}>{pctStr(chg)}</span>
                  </div>
                  <div className="live-spark">
                    {m.sparkline?.length > 1 && (
                      <Spark data={m.sparkline.map(Number)} color={up ? '#34d399' : '#f87171'} />
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}

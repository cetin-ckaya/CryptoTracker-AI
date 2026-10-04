import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Sparkles, RefreshCw, AlertTriangle, Loader2 } from 'lucide-react'
import api from '../api/axios'
import Coin from '../components/Coin'
import { money } from '../utils/format'
import './AiAnalysis.css'

// Sinyal rozetinin rengi: AL yesil, SAT kirmizi, TUT amber
const ACTION_CLASS = { AL: 'buy', SAT: 'sell', TUT: 'hold' }

export default function AiAnalysis() {
  const queryClient = useQueryClient()

  // Dashboard'daki kartla AYNI anahtar: iki ekran tek analizi paylasir,
  // sayfalar arasi gecis her seferinde Groq'a yeni istek atmaz.
  const { data, isLoading, isFetching, error } = useQuery({
    queryKey: ['ai-analyze'],
    queryFn: () => api.get('/ai/analyze').then(r => r.data),
    staleTime: 10 * 60 * 1000,
    retry: false,
  })

  const signals = data?.signals ?? []
  const say = action => signals.filter(s => s.action === action).length

  const sonAnaliz = data?.generatedAt
    ? new Date(data.generatedAt).toLocaleString('tr-TR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      })
    : '—'

  return (
    <div className="ai-page">
      <div className="ai-head">
        <div>
          <div className="ai-head-title">
            <h1>AI Analiz</h1>
            <span className="badge-new">YENİ</span>
          </div>
          <div className="ai-head-sub">
            Groq AI destekli portföy analizi · {data?.model ?? 'openai/gpt-oss-20b'}
          </div>
        </div>

        <div className="ai-head-actions">
          <div className="ai-stamp">Son analiz: {sonAnaliz}</div>
          <button
            className="ai-refresh"
            disabled={isFetching}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['ai-analyze'] })}
          >
            {isFetching
              ? <><Loader2 size={15} className="spin" /> Analiz ediliyor</>
              : <><RefreshCw size={15} /> Yeniden Analiz Et</>}
          </button>
        </div>
      </div>

      {isLoading && (
        <div className="ai-state"><Loader2 size={16} className="spin" /> Portföyünüz analiz ediliyor...</div>
      )}

      {error && (
        <div className="ai-state error">
          Analiz alınamadı. {error?.response?.status === 403
            ? 'Bu analiz için Premium üyelik gerekiyor.'
            : 'Lütfen biraz sonra tekrar deneyin.'}
        </div>
      )}

      {data && (
        <>
          {/* ---- Genel gorunum ---- */}
          <div className="ai-overview">
            <div className="ai-overview-ico"><Sparkles size={24} /></div>

            <div className="ai-overview-text">
              <div className="k">Genel Görünüm</div>
              <div className="headline">{data.headline ?? 'Analiz hazır'}</div>
              {data.summary && <div className="summary">{data.summary}</div>}
              {!data.personalized && (
                <div className="summary free-note">
                  Ücretsiz planda BTC, ETH ve SOL için genel değerlendirme gösterilir.
                  Portföyünüze özel analiz Premium üyelikte.
                </div>
              )}
            </div>

            <div className="ai-counts">
              <div className="ai-count"><div className="k">Al</div><div className="v buy">{say('AL')}</div></div>
              <div className="ai-count"><div className="k">Tut</div><div className="v hold">{say('TUT')}</div></div>
              <div className="ai-count"><div className="k">Sat</div><div className="v sell">{say('SAT')}</div></div>
            </div>
          </div>

          {/* ---- Coin kartlari ---- */}
          {signals.length > 0 && (
            <div className="ai-grid">
              {signals.map(s => (
                <div className="ai-card" key={s.symbol}>
                  <div className="ai-card-head">
                    <Coin sym={s.symbol} size={36} />
                    <div className="ai-card-name">
                      <div className="nm">{s.name ?? s.symbol}</div>
                      <div className="sy">
                        {s.symbol}{s.price != null && ` · ${money(s.price)}`}
                      </div>
                    </div>
                    <span className={`ai-action ${ACTION_CLASS[s.action] ?? 'hold'}`}>{s.action}</span>
                  </div>

                  {s.comment && <div className="ai-card-text">{s.comment}</div>}

                  <div className="ai-card-foot">
                    <div>
                      <div className="k">Hedef / Seviye</div>
                      <div className="v">{s.target ?? '—'}</div>
                    </div>
                    <div>
                      <div className="k">Model güveni</div>
                      {s.confidence == null ? (
                        <div className="v">—</div>
                      ) : (
                        <div className="ai-conf">
                          <span className="bar"><span style={{ width: `${s.confidence}%` }} /></span>
                          <span className="pct">%{s.confidence}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Model JSON yerine duz metin dondurduyse ekran bos kalmasin */}
          {signals.length === 0 && data.rawText && (
            <div className="ai-raw">{data.rawText}</div>
          )}
        </>
      )}

      <div className="ai-warn">
        <AlertTriangle size={15} />
        <span>
          Bu analiz yatırım tavsiyesi değildir, yalnızca bilgi amaçlıdır.
          “Model güveni” modelin kendi beyanıdır, ölçülmüş bir olasılık değildir.
        </span>
      </div>
    </div>
  )
}

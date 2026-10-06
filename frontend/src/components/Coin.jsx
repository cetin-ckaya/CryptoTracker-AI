import { useState } from 'react'
import useCoinIcons from '../hooks/useCoinIcons'

// Coin ikonu. Dashboard, Portfoyum, Islemlerim ve islem ekleme penceresinin
// ortak bileseni.
//
// Once CoinGecko'nun gercek logosunu dener (coins.icon_url). Adres yoksa veya
// gorsel yuklenemezse asagidaki elle cizilmis SVG'lere duser — boylece ag
// erisimi olmadiginda da ekran bos kalmaz.
// src: ikon adresi disaridan verilebilir. Giris ekrani /api/v1/coins ucuna
// erisemiyor (kimlik dogrulama istiyor), ama /market yanitinda iconUrl zaten
// var — oradan gecirilir.
export default function Coin({ sym, size = 32, src }) {
  const icons = useCoinIcons()
  const [hata, setHata] = useState(false)

  const url = src ?? icons[String(sym ?? '').toUpperCase()]
  if (url && !hata) {
    return (
      <img
        src={url}
        alt={sym}
        width={size}
        height={size}
        loading="lazy"
        onError={() => setHata(true)}
        style={{ borderRadius: '50%', flexShrink: 0, display: 'block' }}
      />
    )
  }

  return <CoinSvg sym={sym} size={size} />
}

// Yedek: elle cizilmis SVG'ler
function CoinSvg({ sym, size }) {
  const p = { width: size, height: size, viewBox: '0 0 40 40', xmlns: 'http://www.w3.org/2000/svg' }
  switch ((sym ?? '').toUpperCase()) {
    case 'BTC': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#F7931A"/><path fill="#fff" d="M25.5 12.2c2.4.7 3.7 2.3 3.3 4.5-.2 1.2-.9 2.1-2 2.6 1.8.8 2.5 2.3 2 4.1-.7 2.6-3 3.8-6.2 3.6l-.5 2.7-2-.4.5-2.7-1.6-.3-.5 2.7-2-.4.5-2.7-2-.4.4-2.2 1.6.3 1.8-9.1-1.6-.3.4-2.2 2 .4.5-2.7 2 .4-.5 2.7c.6.1 1.2.2 1.6.3l.5-2.7 2 .4-.5 2.7Zm-4.4 3-1 5c1.9.3 4.5.5 4.9-1.9.4-2.3-2.1-2.8-3.9-3.1Zm-1.4 7-.8 4.4c2.1.4 5.1.7 5.5-1.9.4-2.5-2.5-3-4.7-2.5Z"/></svg>
    case 'ETH': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#3C3C3D"/><path fill="#fff" d="m20 6 8.5 14.1L20 24.3l-8.5-4.2L20 6Z"/><path fill="#C9CCD1" d="M20 6v18.3l8.5-4.2L20 6Z"/><path fill="#fff" d="m11.5 21.8 8.5 4.2 8.5-4.2L20 34l-8.5-12.2Z"/></svg>
    case 'BNB': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#F3BA2F"/><path fill="#fff" d="M20 16.6 23.4 20 20 23.4 16.6 20 20 16.6Zm0-8.1 3.4 3.5-3.4 3.4-3.4-3.4L20 8.5Zm-8 8.1 3.4-3.4 3.4 3.4-3.4 3.4L12 16.6Zm16 0 3.4-3.4 3.4 3.4-3.4 3.4L28 16.6Z" transform="translate(-3.4 3.4)"/><path fill="#fff" d="m20 24.5 3.4 3.4-3.4 3.6-3.4-3.6 3.4-3.4Z"/></svg>
    case 'SOL': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#9945FF"/><path fill="#fff" d="M11 13.4h17.3c.9 0 1.3 1.1.6 1.7l-2.9 2.3H8.7c-.9 0-1.3-1.1-.6-1.7l2.9-2.3Zm0 5.7h17.3c.9 0 1.3 1.1.6 1.7l-2.9 2.3H8.7c-.9 0-1.3-1.1-.6-1.7l2.9-2.3Zm0 5.7h17.3c.9 0 1.3 1.1.6 1.7l-2.9 2.3H8.7c-.9 0-1.3-1.1-.6-1.7l2.9-2.3Z"/></svg>
    case 'USDT': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#26A17B"/><path fill="#fff" d="M9 11h22v4.2h-8.6v2.4c4.8.3 8.4 1.3 8.4 2.5s-3.6 2.2-8.4 2.5v6.4h-4.8v-6.4c-4.8-.3-8.4-1.3-8.4-2.5s3.6-2.2 8.4-2.5v-2.4H9V11Zm11 9.6c4.3 0 7.9-.6 7.9-1.4s-3.6-1.4-7.9-1.4-7.9.6-7.9 1.4 3.6 1.4 7.9 1.4Z"/></svg>
    case 'ADA': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#0033AD"/><g fill="#fff"><circle cx="20" cy="20" r="3"/><circle cx="20" cy="10" r="2"/><circle cx="20" cy="30" r="2"/><circle cx="11.5" cy="15" r="2"/><circle cx="28.5" cy="15" r="2"/><circle cx="11.5" cy="25" r="2"/><circle cx="28.5" cy="25" r="2"/></g></svg>
    case 'XRP': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#23292F"/><path fill="#fff" d="M12 12h3.4l4.6 4.7 4.6-4.7H28l-6.3 6.5c-1 1-2.5 1-3.4 0L12 12Zm0 16h3.4l4.6-4.7 4.6 4.7H28l-6.3-6.5c-1-1-2.5-1-3.4 0L12 28Z"/></svg>
    case 'AVAX': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#E84142"/><path fill="#fff" d="M23.6 13.4c.9-1.5 2.3-1.5 3.2 0l5.4 9.5c.9 1.5.2 2.8-1.5 2.8H19.8c-1.7 0-2.4-1.3-1.5-2.8l5.3-9.5Zm-8.9 4.2c.8-1.4 2.1-1.4 2.9 0l1.2 2.2c.6 1.1.6 2.3 0 3.4l-2.5 4.3c-.6 1-1.6 1.6-2.7 1.6H9.9c-1.7 0-2.4-1.3-1.5-2.8l6.3-8.7Z"/></svg>
    case 'DOT': return <svg {...p}><circle cx="20" cy="20" r="20" fill="#E6007A"/><ellipse cx="20" cy="11" rx="5" ry="3" fill="#fff"/><ellipse cx="12" cy="24.5" rx="5" ry="3" fill="#fff" transform="rotate(-60 12 24.5)"/><ellipse cx="28" cy="24.5" rx="5" ry="3" fill="#fff" transform="rotate(60 28 24.5)"/></svg>
    default: return <svg {...p}><circle cx="20" cy="20" r="20" fill="#3f4a63"/><text x="20" y="26" textAnchor="middle" fill="#c3cad9" fontSize="15" fontWeight="700">{(sym ?? '?')[0]}</text></svg>
  }
}

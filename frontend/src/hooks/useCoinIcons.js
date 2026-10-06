import { useQuery } from '@tanstack/react-query'
import api from '../api/axios'

// Sembol -> ikon adresi sozlugu.
//
// Ikonlar coins tablosunda duruyor (MarketScheduler CoinGecko'dan alip yaziyor)
// ve /api/v1/coins ucu zaten iconUrl donduruyordu.
//
// Tek bir react-query anahtari kullaniyoruz: katalog uygulama boyunca bir kez
// cekilir, butun sayfalar ayni veriyi paylasir. Katalog nadiren degistigi icin
// staleTime uzun tutuldu.
export default function useCoinIcons() {
  const { data: coins = [] } = useQuery({
    queryKey: ['coins'],
    queryFn: () => api.get('/coins').then(r => r.data),
    staleTime: 60 * 60 * 1000,   // 1 saat
    retry: false,
  })

  return Object.fromEntries(
    coins
      .filter(c => c.iconUrl)
      .map(c => [String(c.symbol).toUpperCase(), c.iconUrl])
  )
}

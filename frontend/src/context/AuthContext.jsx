import { createContext, useContext, useEffect, useState } from 'react'
import api from '../api/axios'

const AuthContext = createContext(null)

// "Beni hatirla" isaretliyse localStorage (tarayici kapansa da kalir),
// degilse sessionStorage (sekme kapaninca silinir) kullanilir.
function read(key) {
  return sessionStorage.getItem(key) ?? localStorage.getItem(key)
}

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => read('token'))
  const [user, setUser] = useState(() => {
    const u = read('user')
    return u ? JSON.parse(u) : null
  })

  function login(token, userData, remember = true) {
    const store = remember ? localStorage : sessionStorage
    // Once her iki depoyu da temizle ki eski oturumdan artik kalmasin
    logoutStorage()
    store.setItem('token', token)
    store.setItem('user', JSON.stringify(userData))
    setTokenState(token)
    setUser(userData)
  }

  // Oturumu bozmadan YALNIZCA token'i degistirir.
  //
  // Plan degisiminde gerekiyor: yetki JWT'nin icinde tasindigi icin
  // yukseltmeden sonra sunucu yeni rolu tasiyan taze bir token donuyor ve
  // eskisinin yerine yazilmasi gerekiyor. Kullanici cikis yapmadan yeni
  // yetkisini kullanabiliyor.
  function setToken(yeniToken) {
    // Oturum hangi depodaysa orada kalsin ("Beni hatirla" secimi bozulmasin)
    const store = localStorage.getItem('token') ? localStorage : sessionStorage
    store.setItem('token', yeniToken)
    setTokenState(yeniToken)
  }

  function logoutStorage() {
    for (const store of [localStorage, sessionStorage]) {
      store.removeItem('token')
      store.removeItem('user')
    }
  }

  function logout() {
    logoutStorage()
    setTokenState(null)
    setUser(null)
  }

  // Eski oturum onarimi.
  //
  // Ad soyad, giris yanitiyla birlikte tarayiciya kaydediliyor. AuthResponse'a
  // fullName eklenmeden once giris yapmis bir kullanicinin kaydinda bu alan yok
  // ve arayuz e-postanin @ oncesine dusup "cetolamak" gosteriyordu. Boyle bir
  // kayit gorursek sunucudan guncelini cekip yerine yaziyoruz; kullanicinin
  // cikis yapip tekrar girmesine gerek kalmiyor.
  useEffect(() => {
    if (!token || user?.fullName) return

    let iptal = false
    api.get('/auth/me')
      .then(res => {
        if (iptal || !res.data?.fullName) return
        const guncel = { email: res.data.email, fullName: res.data.fullName }
        // Oturumun hangi depoda tutuldugunu bozmadan guncelle
        const store = localStorage.getItem('token') ? localStorage : sessionStorage
        store.setItem('user', JSON.stringify(guncel))
        setUser(guncel)
      })
      .catch(() => {})   // basarisizsa eski davranis surer, ekran bozulmaz

    return () => { iptal = true }
  }, [token, user?.fullName])

  return (
    <AuthContext.Provider value={{ token, user, login, logout, setToken }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  const user = ctx?.user

  // Gosterilecek ad: once kayitta girilen ad soyad, yoksa e-postanin @ oncesi.
  // fullName kayitta opsiyonel oldugu icin her zaman dolu olmayabilir.
  const displayName =
    user?.fullName?.trim() ||
    user?.email?.split('@')[0] ||
    'Kullanıcı'

  // Selamlama icin sadece ilk ad: "Çetin Çetinkaya" -> "Çetin"
  const firstName = displayName.split(/\s+/)[0].replace(/^./, c => c.toLocaleUpperCase('tr'))

  return { ...ctx, displayName, firstName }
}

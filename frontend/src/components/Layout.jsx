import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import Statusbar from './Statusbar'
import './Layout.css'

// Yerlesim tasarimdaki gibi: topbar ve durum cubugu sayfanin TAM genisligini
// kaplar, sidebar ikisinin arasinda kalir. Bu yuzden ucu de sabit konumlu
// (fixed) ve icerik alani padding ile onlarin altina/yanina itiliyor.
export default function Layout() {
  return (
    <div className="layout">
      <Topbar />
      <Sidebar />
      <main className="layout-content">
        <Outlet />
      </main>
      <Statusbar />
    </div>
  )
}

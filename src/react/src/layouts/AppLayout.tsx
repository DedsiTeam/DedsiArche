import { notify } from '@/lib/notify'
import { useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { PanelLeftClose, PanelLeftOpen, Layers2, Users, Settings2, UserRound, LogOut, LogIn, LoaderCircle } from 'lucide-react'
import { SidebarMenu, type SidebarMenuItem } from '@/components/SidebarMenu'
import { Button } from '@/components/ui/button'

import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'

import { cn } from '@/lib/utils'
import { logout, useSession } from '@/services/auth'

function Navigation({ collapsed }: { collapsed: boolean }) {
  const session = useSession()
  const items: SidebarMenuItem[] = session?.user.canManageUsers
    ? [{ id: 'system', label: '系统管理', icon: Settings2, children: [
      { id: 'users', label: '用户管理', icon: Users, to: '/users' },
    ] }]
    : []
  return <>
    <div className="flex h-16 shrink-0 items-center justify-center px-3">
      <Link to="/" aria-label="DedsiArche 首页" className="flex items-center justify-center gap-3 text-xl font-bold tracking-tight"><Layers2 className="size-8 shrink-0" />{!collapsed && <span>DedsiArche</span>}</Link>
    </div>
    <SidebarMenu items={items} collapsed={collapsed} />
  </>
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const session = useSession()
  const navigate = useNavigate()

  const [loggingOut, setLoggingOut] = useState(false)
  const displayName = session?.user.displayName || session?.user.username || '访客'
  async function handleLogout() {
    setLoggingOut(true)

    try { await logout(); navigate('/login') }
    catch { notify.error('退出失败，请稍后重试。') }
    finally { setLoggingOut(false) }
  }

  return <div className="desktop-shell min-h-screen bg-background" data-collapsed={collapsed}>
    <aside className="desktop-sidebar border-r bg-black/[0.025]">
      <Navigation collapsed={collapsed} />
      <div className={cn('flex shrink-0 items-center gap-1 border-t p-3', collapsed && 'flex-col')}>
        {!collapsed && <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className={cn('h-auto min-w-0 flex-1 justify-start gap-2 px-2 py-3 text-left', collapsed && 'w-full flex-none justify-center px-0')} title={displayName} aria-label={`当前用户：${displayName}`}>
              <Avatar className="size-10 shrink-0"><AvatarFallback className="bg-secondary font-semibold text-primary">{session ? displayName.slice(0, 2).toUpperCase() : 'DA'}</AvatarFallback></Avatar>
              {!collapsed && <span className="min-w-0 flex-1 truncate text-sm font-medium">{displayName}</span>}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" align="start" sideOffset={8} className="w-60 rounded-xl p-1.5 shadow-lg">
            <DropdownMenuLabel className="flex items-center gap-3 px-2.5 py-3">
              <Avatar className="size-9 shrink-0"><AvatarFallback className="bg-primary/10 text-xs font-semibold text-primary">{session ? displayName.slice(0, 2).toUpperCase() : 'DA'}</AvatarFallback></Avatar>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold" title={displayName}>{displayName}</span>
                <span className="mt-0.5 block truncate text-xs font-normal text-muted-foreground" title={session?.user.username}>{session ? session.user.username : '尚未登录'}</span>
              </span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator className="mx-2 my-1" />
            <DropdownMenuItem asChild className="h-9 cursor-pointer gap-3 rounded-md px-2.5 focus:bg-primary/10">
              <Link to="/"><UserRound className="size-4" />账户中心</Link>
            </DropdownMenuItem>
            {session ? <DropdownMenuItem className="h-9 cursor-pointer gap-3 rounded-md px-2.5 focus:bg-primary/10" disabled={loggingOut} onSelect={(event) => { event.preventDefault(); void handleLogout() }}>
              {loggingOut ? <LoaderCircle className="size-4 animate-spin" /> : <LogOut className="size-4" />}{loggingOut ? '正在退出…' : '退出登录'}
            </DropdownMenuItem> : <DropdownMenuItem asChild className="h-9 cursor-pointer gap-3 rounded-md px-2.5 focus:bg-primary/10"><Link to="/login"><LogIn className="size-4" />登录账户</Link></DropdownMenuItem>}
          </DropdownMenuContent>
        </DropdownMenu>}
          <Button variant="ghost" size="icon" onClick={() => setCollapsed((value) => !value)} aria-label={collapsed ? '展开菜单' : '折叠菜单'} title={collapsed ? '展开菜单' : '折叠菜单'} aria-expanded={!collapsed} aria-controls="main-navigation" className="shrink-0 text-muted-foreground">
            {collapsed ? <PanelLeftOpen /> : <PanelLeftClose />}
          </Button>
      </div>
    </aside>
    <div className="desktop-workspace">
      <main className="page-padding"><Outlet /></main>
    </div>
  </div>
}

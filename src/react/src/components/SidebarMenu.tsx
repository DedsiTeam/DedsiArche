import { useId, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ChevronDown, Folder, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel,
  DropdownMenuPortal, DropdownMenuSub, DropdownMenuSubContent,
  DropdownMenuSubTrigger, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export type SidebarMenuItem = {
  id: string
  label: string
  icon?: LucideIcon
} & ({ to: string; children?: never } | { to?: never; children: SidebarMenuItem[] })

function isActive(item: SidebarMenuItem, pathname: string): boolean {
  return item.children
    ? item.children.some((child) => isActive(child, pathname))
    : pathname === item.to || (item.to !== '/' && pathname.startsWith(`${item.to}/`))
}

const rowClass = 'flex h-9 w-full items-center gap-2 rounded-md px-3 text-sm transition-colors hover:bg-black/5'
const activeClass = 'bg-primary text-white hover:bg-primary/90'

function FlyoutItems({ items, pathname }: { items: SidebarMenuItem[]; pathname: string }) {
  return items.map((item) => {
    const Icon = item.icon
    const active = isActive(item, pathname)
    if (item.children) return <DropdownMenuSub key={item.id}>
      <DropdownMenuSubTrigger className={cn('min-h-9', active && 'bg-primary/10 text-primary')}>
        {Icon && <Icon className="size-4" />}{item.label}
      </DropdownMenuSubTrigger>
      <DropdownMenuPortal><DropdownMenuSubContent className="max-h-80 w-48 overflow-y-auto">
        <FlyoutItems items={item.children} pathname={pathname} />
      </DropdownMenuSubContent></DropdownMenuPortal>
    </DropdownMenuSub>
    return <DropdownMenuItem key={item.id} asChild className={cn('min-h-9', active && 'bg-primary/10 text-primary')}>
      <Link to={item.to} aria-current={active ? 'page' : undefined}>
        {Icon && <Icon className="size-4" />}{item.label}
      </Link>
    </DropdownMenuItem>
  })
}

function MenuNode({ item, pathname, collapsed }: { item: SidebarMenuItem; pathname: string; collapsed: boolean }) {
  const active = isActive(item, pathname)
  const [open, setOpen] = useState(active)
  const childrenId = useId()
  const Icon = item.icon ?? Folder
  if (!item.children) return <li>
    <Link to={item.to} title={item.label} aria-label={item.label} aria-current={active ? 'page' : undefined}
      className={cn(rowClass, active && activeClass, collapsed && 'justify-center px-0')}>
      <Icon className="size-4 shrink-0" />
      {!collapsed && <span className="truncate">{item.label}</span>}
    </Link>
  </li>

  if (collapsed) return <li><DropdownMenu>
    <DropdownMenuTrigger asChild>
      <button type="button" aria-label={item.label} title={item.label}
        className={cn(rowClass, 'justify-center px-0', active && activeClass)}>
        <Icon className="size-4 shrink-0" />
      </button>
    </DropdownMenuTrigger>
    <DropdownMenuContent side="right" align="start" sideOffset={12} className="w-48">
      <DropdownMenuLabel>{item.label}</DropdownMenuLabel>
      <FlyoutItems items={item.children} pathname={pathname} />
    </DropdownMenuContent>
  </DropdownMenu></li>

  return <li>
    <button type="button" onClick={() => setOpen(!open)} aria-expanded={open} aria-controls={childrenId}
      className={cn(rowClass, active && 'text-primary')}>
      <Icon className="size-4 shrink-0" /><span className="min-w-0 flex-1 truncate text-left">{item.label}</span>
      <ChevronDown className={cn('size-3.5 shrink-0 transition-transform', !open && '-rotate-90')} />
    </button>
    <ul id={childrenId} hidden={!open} className="ml-4 space-y-1 pt-1">
      {item.children.map((child) => <MenuNode key={child.id} item={child} pathname={pathname} collapsed={false} />)}
    </ul>
  </li>
}

export function SidebarMenu({ items, collapsed }: { items: SidebarMenuItem[]; collapsed: boolean }) {
  const { pathname } = useLocation()
  return <nav id="main-navigation" aria-label="主导航" className="min-h-0 flex-1 overflow-y-auto px-2 py-2">
    <ul className="space-y-1">
      {items.map((item) => <MenuNode key={`${item.id}:${pathname}`} item={item} pathname={pathname} collapsed={collapsed} />)}
    </ul>
  </nav>
}

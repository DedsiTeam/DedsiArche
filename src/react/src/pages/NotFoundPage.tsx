import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
export function NotFoundPage() {
  return <div className="flex min-h-100 flex-col items-center justify-center gap-5 text-center"><h1 className="text-2xl font-semibold">页面不存在</h1><p className="text-sm text-muted-foreground">请检查访问地址，或返回账户中心。</p><Button asChild><Link to="/">返回账户中心</Link></Button></div>
}

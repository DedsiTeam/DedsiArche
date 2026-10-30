import { useRef, useState, type ComponentProps } from 'react'
import { Copy, Eye, EyeOff, RefreshCw } from 'lucide-react'
import { Input } from './input'
import { Button } from './button'
import { generatePassword } from '@/lib/password-generator'
import { notify } from '@/lib/notify'
import { cn } from '@/lib/utils'

type PasswordGeneratorInputProps = Omit<ComponentProps<typeof Input>, 'type' | 'value' | 'defaultValue' | 'onChange'> & {
  value: string
  onValueChange: (value: string) => void
  /** 仅成功生成时调用，供表单清空旧的确认密码。 */
  onGenerate?: () => void
}

export function PasswordGeneratorInput({ value, onValueChange, onGenerate, disabled, readOnly, className, ...props }: PasswordGeneratorInputProps) {
  const [visible, setVisible] = useState(false)
  const [copying, setCopying] = useState(false)
  const copyLock = useRef(false)
  function generate() {
    if (disabled || readOnly) return
    try {
      const password = generatePassword()
      onValueChange(password)
      onGenerate?.()
    } catch { notify.error('密码生成失败，请稍后重试。') }
  }
  async function copy() {
    if (disabled || !value || copyLock.current) return
    copyLock.current = true
    setCopying(true)
    try {
      await navigator.clipboard.writeText(value)
      notify.success('密码已复制，请粘贴到确认密码框。')
    } catch { notify.error('复制失败，请允许浏览器访问剪贴板后重试。') }
    finally { copyLock.current = false; setCopying(false) }
  }
  return <div className="space-y-2">
    <div className="relative">
      <Input {...props} hasEndAction className={cn(className, 'pr-11')} type={visible ? 'text' : 'password'} autoComplete="new-password" value={value} disabled={disabled} readOnly={readOnly} onChange={(event) => onValueChange(event.target.value)} />
      <Button type="button" variant="ghost" size="icon-sm" className="absolute right-1 top-1/2 -translate-y-1/2 text-muted-foreground" disabled={disabled}
        aria-label={visible ? '隐藏密码' : '显示密码'} title={visible ? '隐藏密码' : '显示密码'} aria-pressed={visible} aria-controls={props.id}
        onClick={() => setVisible((current) => !current)}>
        {visible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
      </Button>
    </div>
    <div className="flex flex-wrap items-center gap-2">
      <Button type="button" variant="outline" size="sm" disabled={disabled || readOnly || copying} onClick={generate}><RefreshCw />生成密码</Button>
      <Button type="button" variant="outline" size="sm" disabled={disabled || !value || copying} onClick={() => void copy()}><Copy />{copying ? '正在复制…' : '复制密码'}</Button>
      <span className="text-xs text-muted-foreground">20 位，含大小写字母、数字及符号</span>
    </div>
  </div>
}

import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'
import { Form, type FormFieldConfig } from '@/components/ui/form'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { PasswordGeneratorInput } from '@/components/ui/password-generator-input'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { validateDisplayName, validateUsername, validatePasswordChange, validatePhone, validateEmail } from '@/lib/user-validation'
import { userError } from '@/lib/api-error'
import { createUser, editUser, type IManagedUser } from '@/services/users'

type UserFormValues = {
  username: string
  displayName: string
  phone: string
  email: string
  password: string
  confirmPassword: string
  enabled: boolean
  confirmed: boolean
}

export function UserFormDialog({ user, currentUserId, onClose, onSuccess }: {
  user: IManagedUser | null
  currentUserId?: string
  onClose: () => void
  onSuccess: (message: string) => void
}) {
  const form = useForm<UserFormValues>({
    mode: 'onBlur', reValidateMode: 'onChange',
    defaultValues: {
      username: user?.username ?? '', displayName: user?.displayName ?? '',
      phone: user?.phone ?? '', email: user?.email ?? '', password: '', confirmPassword: '', enabled: user?.enabled ?? true, confirmed: false,
    },
  })
  const saving = form.formState.isSubmitting
  const fields: FormFieldConfig<UserFormValues>[] = [
    { name: 'username', label: '账号', readOnly: Boolean(user), required: true,
      inputProps: { autoComplete: 'off' },
      description: user ? '账号不能修改。' : '3～50 位英文字母、数字或下划线。',
      rules: { validate: (value) => user ? true : validateUsername(String(value)) || true } },
    { name: 'displayName', label: '姓名', required: true, inputProps: { autoComplete: 'off' },
      rules: { validate: (value) => validateDisplayName(String(value)) || true } },
    { name: 'phone', label: '手机号', inputProps: { type: 'tel', autoComplete: 'tel', maxLength: 16 },
      rules: { validate: (value) => validatePhone(String(value ?? '')) || true } },
    { name: 'email', label: '邮箱', inputProps: { type: 'email', autoComplete: 'email', maxLength: 254 },
      rules: { validate: (value) => validateEmail(String(value ?? '')) || true } },
    { name: 'password', label: '初始密码', required: true, visible: () => !user,
      inputProps: { type: 'password', autoComplete: 'new-password' }, description: '12～64 个字符，UTF-8 最多 72 字节。', descriptionTone: 'error',
      render: ({ field, controlProps }) => <PasswordGeneratorInput {...controlProps} ref={field.ref} name={field.name} value={String(field.value ?? '')} onBlur={field.onBlur}
        onValueChange={(value) => form.setValue('password', value, { shouldDirty: true, shouldValidate: true })}
        onGenerate={() => { form.setValue('confirmPassword', '', { shouldDirty: true }); form.clearErrors('confirmPassword') }} />,
      rules: { validate: (value) => validatePasswordChange(String(value), String(value)).password || true } },
    { name: 'confirmPassword', label: '确认密码', required: true, visible: () => !user,
      inputProps: { type: 'password', autoComplete: 'new-password' }, dependencies: ['password'],
      rules: { validate: (value, values) => validatePasswordChange(values.password, String(value)).confirmPassword || true } },
    { name: 'enabled', label: '账户状态', visible: () => Boolean(user), disabled: user?.id === currentUserId,
      description: user?.id === currentUserId ? '不能停用当前登录账户。' : undefined,
      render: ({ field, controlProps }) => <Select {...controlProps} clearable={false} ref={field.ref} name={field.name} value={String(field.value)} onBlur={field.onBlur}
        onValueChange={(value) => { field.onChange(value === 'true'); form.setValue('confirmed', false) }} className="w-full"
        options={[{ value: 'true', label: '已启用' }, { value: 'false', label: '已停用' }]} /> },
    { name: 'confirmed', label: '确认停用该用户，并撤销其刷新会话。', kind: 'checkbox',
      visible: (values) => Boolean(user?.enabled && !values.enabled), className: 'rounded-lg bg-secondary p-3' },
  ]
  return <Dialog open onOpenChange={(open) => { if (!open && !saving) onClose() }}>
    <DialogContent aria-describedby={undefined} showCloseButton={!saving}>
      <DialogHeader>
        <DialogTitle>{user ? '编辑用户' : '新增用户'}</DialogTitle>
      </DialogHeader>
      <Form form={form} fields={fields} aria-label={user ? '编辑用户' : '新增用户'}
        validate={(values) => {
          if (user?.id === currentUserId && !values.enabled) return [{ name: 'enabled', message: '不能停用当前登录账户。' }]
          if (user?.enabled && !values.enabled && !values.confirmed) return [{ name: 'confirmed', message: '请确认停用此用户。' }]
          return []
        }}
        mapError={(cause) => {
          const status = isAxiosError(cause) ? cause.response?.status : undefined
          if (!user && status === 409) return [{ name: 'username', message: '账号已存在，请换一个账号。' }]
          return [{ message: status === 409 && user ? '资料已被其他操作修改。请关闭此窗口、刷新列表后重试。' : status === 404 ? '该用户不存在，请刷新列表。' : userError(cause) }]
        }}
        onSubmit={async (values) => {
          if (user) await editUser(user.id, { displayName: values.displayName.trim(), phone: values.phone.trim(), email: values.email.trim(), enabled: values.enabled, version: user.version })
          else await createUser({ username: values.username.trim(), displayName: values.displayName.trim(), phone: values.phone.trim(), email: values.email.trim(), password: values.password })
          onSuccess(user ? '用户资料已更新。' : '用户已创建，可使用设置的密码登录。')
        }}>
        <div className="flex gap-3">
          <Button type="submit" disabled={saving}>{saving ? '正在保存…' : user ? '保存修改' : '创建用户'}</Button>
          <Button type="button" variant="outline" disabled={saving} onClick={onClose}>取消</Button>
        </div>
      </Form>
    </DialogContent>
  </Dialog>
}

import { UserPositionSelect } from '@/components/UserPositionSelect'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'
import { Form, type FormFieldConfig } from '@/components/ui/form'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { PasswordGeneratorInput } from '@/components/ui/password-generator-input'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { validateDisplayName, validateUsername, validatePasswordChange, validatePhone, validateEmail } from '@/lib/user-validation'
import { userError } from '@/lib/api-error'
import { createUser, editUser, type IManagedUserDetail } from '@/services/users'

type UserFormValues = {
  username: string
  displayName: string
  phone: string
  email: string
  password: string
  confirmPassword: string
  enabled: boolean
  confirmed: boolean
  positionIds: string[]
}

export function UserFormDialog({ user, currentUserId, onClose, onSuccess }: {
  user: IManagedUserDetail | null
  currentUserId?: string
  onClose: () => void
  onSuccess: (message: string) => void
}) {
  const form = useForm<UserFormValues>({
    mode: 'onBlur', reValidateMode: 'onChange',
    defaultValues: {
      positionIds: user?.positions.map((item) => item.positionId) ?? [],
      username: user?.username ?? '', displayName: user?.displayName ?? '',
      phone: user?.phone ?? '', email: user?.email ?? '', password: '', confirmPassword: '', enabled: user?.enabled ?? true, confirmed: false,
    },
  })
  const saving = form.formState.isSubmitting || form.formState.isValidating
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
    {
      name: 'positionIds', label: '用户岗位',
      className: 'flex min-h-0 flex-1 flex-col space-y-0 gap-3 [&>label]:font-semibold',
      render: ({ field, controlProps }) => (
        <UserPositionSelect
          {...controlProps}
          value={field.value as string[]}
          onChange={field.onChange}
          onBlur={field.onBlur}
          inputRef={field.ref}
          initialPositions={user?.positions ?? []}
        />
      ),
    },
    { name: 'confirmed', label: '确认停用该用户，并撤销其刷新会话。', kind: 'checkbox',
      visible: (values) => Boolean(user?.enabled && !values.enabled), className: 'rounded-lg bg-secondary p-3' },
  ]
  return <Dialog open onOpenChange={(open) => { if (!open && !saving) onClose() }}>
    <DialogContent className="flex h-[82vh] max-h-[760px] w-[calc(100vw-48px)] max-w-[1280px] flex-col gap-5 overflow-hidden bg-card" aria-describedby={undefined} showCloseButton={!saving}>
      <DialogHeader className="shrink-0 border-b pb-4">
        <DialogTitle>{user ? '编辑用户' : '新增用户'}</DialogTitle>
      </DialogHeader>
      <Form form={form} fields={fields.map((field) => ({ ...field, disabled: saving || field.disabled }))} aria-label={user ? '编辑用户' : '新增用户'}
        className="flex min-h-0 flex-1 flex-col space-y-0 gap-4"
        fieldsClassName="min-h-0 flex-1 space-y-0 overflow-hidden"
        renderFields={(items) => (
          <div className="grid h-full min-h-0 grid-cols-[minmax(260px,0.85fr)_minmax(0,2fr)] gap-5">
            <section aria-label="基本信息" className="min-w-0 space-y-4 overflow-y-auto overscroll-contain py-1 pr-4">
              <h2 className="text-sm font-semibold">基本信息</h2>
              {items.filter((item) => item.name !== 'positionIds').map((item) => item.node)}
            </section>
            <section aria-label="用户岗位" className="flex min-h-0 min-w-0 flex-col border-l py-1 pl-5">
              {items.filter((item) => item.name === 'positionIds').map((item) => item.node)}
            </section>
          </div>
        )}
        validate={(values) => {
          if (user?.id === currentUserId && !values.enabled) return [{ name: 'enabled', message: '不能停用当前登录账户。' }]
          if (user?.enabled && !values.enabled && !values.confirmed) return [{ name: 'confirmed', message: '请确认停用此用户。' }]
          return []
        }}
        mapError={(cause) => {
          const status = isAxiosError(cause) ? cause.response?.status : undefined
          const code = isAxiosError(cause) ? cause.response?.data?.code : undefined
          if (code === 'USER_POSITION_NOT_FOUND') return [{ name: 'positionIds', message: '所选岗位已不存在，请移除该项或重新选择后保存。' }]
          if (code === 'DUPLICATE_USER_POSITION' || code === 'INVALID_USER_POSITION') return [{ name: 'positionIds', message: '岗位选择无效，请检查后重新选择。' }]
          if (!user && status === 409 && code === 'USERNAME_EXISTS') return [{ name: 'username', message: '账号已存在，请换一个账号。' }]
          return [{ message: status === 409 ? '用户资料或关联岗位已变化，请关闭窗口并重新读取后提交。' : status === 404 ? '该用户不存在，请刷新列表。' : userError(cause) }]
        }}
        onSubmit={async (values) => {
          const valuesToSave = {
            displayName: values.displayName.trim(),
            phone: values.phone.trim(),
            email: values.email.trim(),
            positionIds: values.positionIds,
          }
          if (user) {
            await editUser(user.id, {
              ...valuesToSave, enabled: values.enabled, version: user.version,
            })
          } else {
            await createUser({
              ...valuesToSave, username: values.username.trim(), password: values.password,
            })
          }
          onSuccess(user ? '用户资料已更新。' : '用户已创建，可使用设置的密码登录。')
        }}>
        <div className="flex shrink-0 gap-3 border-t pt-4">
          <Button type="submit" disabled={saving}>{saving ? '正在保存…' : user ? '保存修改' : '创建用户'}</Button>
          <Button type="button" variant="outline" disabled={saving} onClick={onClose}>取消</Button>
        </div>
      </Form>
    </DialogContent>
  </Dialog>
}

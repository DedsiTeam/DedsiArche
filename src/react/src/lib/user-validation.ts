export function validateDisplayName(value: string) {
  return !value.trim() ? '请输入姓名。' : value.trim().length > 100 ? '姓名不能超过 100 个字符。' : ''
}

export function validateUsername(value: string) {
  return /^[a-zA-Z0-9_]{3,50}$/.test(value.trim()) ? '' : '账号需为 3～50 位英文字母、数字或下划线。'
}

export function validateRegistration(values: { username: string; displayName: string; password: string; confirmPassword: string }) {
  return {
    username: validateUsername(values.username),
    displayName: validateDisplayName(values.displayName),
    ...validatePasswordChange(values.password, values.confirmPassword),
  }
}

export function validatePasswordChange(password: string, confirmPassword: string) {
  return {
    password: !password.trim() ? '请输入密码。' : password.length < 12 || password.length > 64
      ? '密码需为 12～64 个字符。' : new TextEncoder().encode(password).length > 72 ? '密码 UTF-8 编码不能超过 72 字节，请减少中文等多字节字符。' : '',
    confirmPassword: !confirmPassword ? '请再次输入密码。' : password !== confirmPassword ? '两次输入的密码不一致。' : '',
  }
}

export function validatePhone(value: string) {
  return !value.trim() || /^\+?[0-9]{7,15}$/.test(value.trim()) ? '' : '手机号需为 7～15 位数字，可使用 + 国际区号。'
}

export function validateEmail(value: string) {
  const email = value.trim()
  return !email || (email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) ? '' : '请输入有效邮箱，最多 254 个字符。'
}

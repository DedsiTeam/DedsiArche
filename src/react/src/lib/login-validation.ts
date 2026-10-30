export function validateLogin(username: string, password: string) {
  return {
    username: username.trim() ? '' : '请输入账号。',
    password: password.trim() ? '' : '请输入密码。',
  }
}

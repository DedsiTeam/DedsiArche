export function validateLogin(username: string, password: string) {
  return {
    username: username.trim() ? '' : '请输入用户名。',
    password: password.trim() ? '' : '请输入密码。',
  }
}

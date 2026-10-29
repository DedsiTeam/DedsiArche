import { isAxiosError } from 'axios'

export function userError(error: unknown) {
  if (!isAxiosError(error)) return '操作失败，请稍后重试。'
  const status = error.response?.status
  if (status === 409) return '用户名已存在，请换一个用户名。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无法执行此操作，请联系管理员。'
  if (status === 400) return '提交的信息不符合要求，请检查后重试。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '服务暂时不可用，请稍后重试。'
}

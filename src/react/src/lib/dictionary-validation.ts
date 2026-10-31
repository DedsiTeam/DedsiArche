import { isAxiosError } from 'axios'
import type { DictionaryEnabled } from '@/services/dictionaries'

/** 表单内部使用 itemId，避免与 useFieldArray 生成的渲染 id 混淆。 */
export interface IDictionaryItemFormValue {
  itemId?: string
  code: string
  name: string
  sortOrder: string
  isDefault: boolean
  enabled: boolean
}

export interface IDictionaryFormValues {
  systemCode: string
  code: string
  name: string
  description: string
  enabled: string
  items: IDictionaryItemFormValue[]
}

export function isDictionaryEnabled(value: string): value is DictionaryEnabled {
  return value === '' || value === 'true' || value === 'false'
}

export function validateDictionaryText(value: string, label: string) {
  if (!value.trim()) return `请输入${label}。`
  return value.length > 100 ? `${label}最多 100 个字符。` : ''
}

export function validateDictionarySort(value: string) {
  const number = Number(value)
  return value.trim() && Number.isInteger(number) && number >= 0 && number <= 2147483647
    ? '' : '排序必须为 0～2147483647 的整数。'
}

export function validateDictionaryItemCode(value: string, index: number, items: IDictionaryItemFormValue[]) {
  const message = validateDictionaryText(value, '字典项编码')
  if (message) return message
  return items.some((item, other) => other !== index && item.code.trim() === value.trim())
    ? '同一字典内的字典项编码不能重复。' : ''
}

export function dictionaryError(cause: unknown, operation: 'load' | 'create' | 'edit' | 'delete' = 'load') {
  if (!isAxiosError(cause)) return '操作失败，请稍后重试。'
  const status = cause.response?.status
  const code = cause.response?.data?.code
  if (code === 'DICTIONARY_SYSTEM_NOT_FOUND') return '所属系统已不存在，请重新选择系统后保存。'
  if (code === 'INVALID_DICTIONARY') return '请检查字典和字典项：编码不能重复，最多一个默认项且默认项必须启用。'
  if (status === 409) return operation === 'create'
    ? '该系统下的字典编码已存在，请更换编码。'
    : '字典资料已变化，请关闭弹窗、刷新列表后重新编辑，避免覆盖其他修改。'
  if (status === 404) return '该字典已不存在，请关闭弹窗并刷新列表。'
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '当前账户无权管理字典，请联系管理员。'
  if (status === 400) return '请检查必填字段、长度、排序和默认项设置。'
  if (status === 429) return '操作过于频繁，请稍后重试。'
  return '字典服务暂时不可用，请稍后重试。'
}

import { expect, test } from 'bun:test'
import { validateLogin } from '../src/lib/login-validation'

test('blank and whitespace-only fields are rejected independently', () => {
  expect(validateLogin('', 'secret')).toEqual({ username: '请输入用户名。', password: '' })
  expect(validateLogin('alice', ' \t ')).toEqual({ username: '', password: '请输入密码。' })
  expect(validateLogin('   ', '')).toEqual({ username: '请输入用户名。', password: '请输入密码。' })
})
test('login does not impose length or registration-format rules', () => {
  for (const value of ['a', 'x'.repeat(200), ' name with spaces ']) {
    expect(validateLogin(value, value)).toEqual({ username: '', password: '' })
  }
})

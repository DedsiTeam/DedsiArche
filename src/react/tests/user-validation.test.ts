import { expect, test } from 'bun:test'
import { validateRegistration, validateDisplayName } from '../src/lib/user-validation'

test('registration respects API rules including UTF-8 bytes and confirmation', () => {
  const valid = { username: 'alice_123', displayName: 'Alice', password: 'abcdefgh1234', confirmPassword: 'abcdefgh1234' }
  expect(Object.values(validateRegistration(valid)).every((error) => !error)).toBe(true)
  expect(validateRegistration({ ...valid, username: 'bad name' }).username).not.toBe('')
  expect(validateRegistration({ ...valid, password: '中'.repeat(25) }).password).toContain('72')
  expect(validateRegistration({ ...valid, password: 'x'.repeat(65) }).password).toContain('64')
  expect(validateRegistration({ ...valid, confirmPassword: 'different' }).confirmPassword).not.toBe('')
  expect(validateDisplayName(' ')).not.toBe('')
  expect(validateDisplayName('x'.repeat(101))).not.toBe('')
  expect(validateDisplayName('  Alice  ')).toBe('')
})

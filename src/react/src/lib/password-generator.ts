const groups = [
  'abcdefghijklmnopqrstuvwxyz',
  'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
  '0123456789',
  '!@#$%^&*()-_=+[]{}:;,.?',
] as const
const alphabet = groups.join('')

/** Web Crypto + rejection sampling，避免取模偏差；固定 20 位 ASCII，至少包含四类各一位。 */
export function generatePassword(): string {
  const limit = Math.floor(0x100000000 / alphabet.length) * alphabet.length
  const random = new Uint32Array(1)
  for (;;) {
    let password = ''
    while (password.length < 20) {
      crypto.getRandomValues(random)
      if (random[0] < limit) password += alphabet[random[0] % alphabet.length]
    }
    if (groups.every((group) => [...password].some((character) => group.includes(character)))) return password
  }
}

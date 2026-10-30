const alphabet = '0123456789ABCDEFGHJKMNPQRSTVWXYZ'

/** 48 位毫秒时间戳与 80 位安全随机数，编码为 26 位大写 Crockford Base32。 */
export function createUlid(): string {
  const timestamp = Date.now()
  if (!Number.isSafeInteger(timestamp) || timestamp < 0 || timestamp > 0xffffffffffff) {
    throw new Error('当前时间超出 ULID 的有效范围。')
  }
  const random = crypto.getRandomValues(new Uint8Array(10))
  let value = BigInt(timestamp)
  for (const byte of random) {
    value = (value << 8n) | BigInt(byte)
  }
  let result = ''
  for (let index = 0; index < 26; index += 1) {
    result = alphabet[Number(value & 31n)] + result
    value >>= 5n
  }
  return result
}

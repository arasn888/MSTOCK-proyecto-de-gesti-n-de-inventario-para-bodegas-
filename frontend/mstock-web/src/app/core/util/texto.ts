/** Texto en blanco -> null (la API guarda null en los campos opcionales, no cadenas vacías). */
export function vacioANull(texto: string | null | undefined): string | null {
  const limpio = (texto ?? '').trim();
  return limpio === '' ? null : limpio;
}

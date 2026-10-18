import { describe, expect, it } from 'vitest';
import { vacioANull } from './texto';

describe('vacioANull', () => {
  it('convierte el texto vacío, en blanco, nulo o indefinido en null', () => {
    expect(vacioANull('')).toBeNull();
    expect(vacioANull('   ')).toBeNull();
    expect(vacioANull(null)).toBeNull();
    expect(vacioANull(undefined)).toBeNull();
  });

  it('recorta los espacios del texto con contenido', () => {
    expect(vacioANull('  987654321 ')).toBe('987654321');
  });
});

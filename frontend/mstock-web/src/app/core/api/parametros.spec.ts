import { describe, expect, it } from 'vitest';
import { parametros } from './parametros';

describe('parametros', () => {
  it('omite los valores vacíos, nulos y los textos en blanco', () => {
    const params = parametros({ nombre: '   ', categoriaId: undefined, desde: null, hasta: '' });
    expect(params.keys()).toEqual([]);
  });

  it('conserva el cero y el false, que son valores válidos', () => {
    const params = parametros({ cantidad: 0, activo: false });
    expect(params.get('cantidad')).toBe('0');
    expect(params.get('activo')).toBe('false');
  });

  it('recorta los espacios del texto y convierte los números', () => {
    const params = parametros({ nombre: '  arroz ', categoriaId: 3 });
    expect(params.get('nombre')).toBe('arroz');
    expect(params.get('categoriaId')).toBe('3');
  });
});

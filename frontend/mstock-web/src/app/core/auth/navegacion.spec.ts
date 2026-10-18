import { describe, expect, it } from 'vitest';
import { MENU, menuPara } from './navegacion';

const rutas = (rol: Parameters<typeof menuPara>[0]) => menuPara(rol).map((item) => item.ruta);

describe('menú por rol', () => {
  it('sin rol no hay menú', () => {
    expect(menuPara(null)).toEqual([]);
  });

  it('el cajero vende pero no gestiona compras, inventario ni usuarios', () => {
    const cajero = rutas('CAJERO');
    expect(cajero).toContain('/ventas/nueva');
    expect(cajero).toContain('/cierre-caja');
    expect(cajero).not.toContain('/compras');
    expect(cajero).not.toContain('/inventario');
    expect(cajero).not.toContain('/usuarios');
  });

  it('el almacenero gestiona inventario y compras pero no vende', () => {
    const almacenero = rutas('ALMACENERO');
    expect(almacenero).toContain('/inventario');
    expect(almacenero).toContain('/compras');
    expect(almacenero).not.toContain('/ventas/nueva');
    expect(almacenero).not.toContain('/reportes');
  });

  it('solo el administrador ve el dashboard y los usuarios', () => {
    expect(rutas('ADMIN')).toContain('/reportes');
    expect(rutas('ADMIN')).toContain('/usuarios');
    expect(rutas('CAJERO')).not.toContain('/reportes');
  });

  it('todos los roles ven el inicio y las rutas del menú no se repiten', () => {
    expect(rutas('ADMIN')).toContain('/inicio');
    expect(rutas('CAJERO')).toContain('/inicio');
    expect(rutas('ALMACENERO')).toContain('/inicio');
    const todas = MENU.map((item) => item.ruta);
    expect(new Set(todas).size).toBe(todas.length);
  });
});

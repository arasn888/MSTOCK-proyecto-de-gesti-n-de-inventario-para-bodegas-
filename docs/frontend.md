# Frontend (Angular): permisos y pruebas manuales

El frontend solo muestra lo que cada rol puede usar, pero **la seguridad real está en los servicios**: cada uno valida el JWT y el
rol en cada petición. Si alguien abre a mano una URL que su rol no tiene, el guard lo devuelve al inicio; y si llamara a la API
directamente, el servicio respondería 403.

## Matriz de permisos

| Acción | ADMIN | CAJERO | ALMACENERO |
|---|:-:|:-:|:-:|
| Ver productos, categorías y proveedores | sí | productos | sí |
| Crear, editar o desactivar productos, categorías y proveedores | sí | no | no |
| Ver stock, kardex y alertas | sí | no (menú) | sí |
| Entradas, ajustes y stock mínimo | sí | no | sí |
| Descargar kardex CSV | sí | no | sí |
| Registrar una venta (punto de venta) | sí | sí | no |
| Ver ventas y abrir la boleta PDF | sí | sí | no |
| Anular una venta | sí | no | no |
| Clientes: crear y editar | sí | sí | no |
| Clientes: eliminar | sí | no | no |
| Cierre de caja | todos los cajeros | solo el suyo | no |
| Compras | sí | no | sí |
| Notificaciones | sí | no | sí |
| Dashboard | sí | no | no |
| Usuarios | sí | no | no |

Notas: el servicio de inventario deja que el CAJERO consulte el stock (lo necesita el punto de venta) y el descuento por venta lo
hace ventas-service; por eso el menú de inventario no se le muestra aunque la API de lectura se lo permita.

## Antes de probar

Deben estar corriendo: Docker (MySQL y RabbitMQ), eureka-server, config-server, api-gateway, auth, catálogo, inventario, ventas,
compras y notificaciones. Si falta un servicio, el gateway responde "el servicio X no está respondiendo" en la pantalla que lo usa.

```powershell
cd frontend\mstock-web
ng serve
```

## Lista de pruebas por rol

**ADMIN** (usuario `admin`)
1. Inicio muestra "Conectado al gateway" y todos los atajos.
2. Categorías, proveedores y productos: crear, editar y desactivar; probar el precio de venta menor al de compra (debe rechazarlo).
3. Inventario: registrar una entrada, un ajuste con conteo físico y subir el stock mínimo; abrir el kardex y descargar el CSV.
4. Compras: registrar una compra; pasa de Pendiente a Recibida sola y el stock sube.
5. Punto de venta: vender buscando por código y por nombre; abrir la boleta; intentar vender más que el stock (debe avisar).
6. Ventas: abrir el detalle y anular una venta con motivo; el stock vuelve y la venta sale tachada.
7. Cierre de caja y Dashboard: los totales coinciden con la lista de Ventas.
8. Notificaciones: aparecen la venta, la anulación y el stock bajo; la campana baja al marcarlas leídas.
9. Usuarios: crear un cajero y un almacenero; comprobar que no puede eliminarse ni desactivarse a sí mismo.

**CAJERO**
1. El menú solo tiene Inicio, Punto de venta, Ventas, Clientes, Cierre de caja y Productos.
2. Vender y abrir la boleta; en el detalle de la venta no aparece "Anular venta".
3. En Productos no hay botones de crear ni editar.
4. Cierre de caja muestra solo sus ventas.
5. Escribir a mano `/usuarios` o `/reportes` en la barra del navegador lo devuelve al inicio.

**ALMACENERO**
1. El menú tiene Productos, Categorías, Proveedores, Inventario, Compras y Notificaciones.
2. Registrar entradas y ajustes, registrar una compra y descargar el kardex.
3. En Categorías y Proveedores no hay botones de crear ni editar.
4. Escribir a mano `/ventas` o `/usuarios` lo devuelve al inicio.

**Sesión**
- Cerrar sesión y volver a entrar; borrar el token del navegador (`localStorage`, clave `mstock.sesion`) y recargar: debe volver al login.
- Apagar el gateway: las pantallas muestran "No se pudo conectar con el servidor".

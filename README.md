# MSTOCK

Sistema de gestión de inventario y ventas para bodegas y minimarkets, construido con arquitectura de
microservicios (una base de datos por servicio).

## Stack
Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.2, Gradle, MySQL 8.4 (Docker), Feign y Resilience4J.

## Módulos
| Módulo | Puerto | Base de datos | Descripción |
|---|---|---|---|
| config-server | 9888 | - | Configuración centralizada |
| eureka-server | 9761 | - | Registro y descubrimiento de servicios |
| auth-service | 9001 | mstock_auth | Usuarios, login (BCrypt + JWT) y roles |
| catalogo-service | 9002 | mstock_catalogo | Productos, categorías y proveedores |
| inventario-service | 9003 | mstock_inventario | Stock, kardex y alertas; valida productos con el catálogo (Feign) |
| ventas-service | 9004 | mstock_ventas | Ventas y clientes; precio del catálogo y descuento de stock del inventario |

Roles: `ADMIN`, `ALMACENERO`, `CAJERO`. Todos los servicios validan el mismo JWT.

## Preparación
1. Copiar `.env.example` como `.env` y completar las claves (`.env` no se sube a Git).
2. Levantar MySQL: `docker compose up -d` (queda en `localhost:3307`).
3. En cada terminal nueva, cargar las claves: `Get-Content .\scripts\cargar-env.ps1 -Raw | Invoke-Expression`

## Cómo ejecutar (en este orden, una terminal por servicio)
    .\gradlew.bat :eureka-server:bootRun
    .\gradlew.bat :config-server:bootRun
    .\gradlew.bat :auth-service:bootRun
    .\gradlew.bat :catalogo-service:bootRun
    .\gradlew.bat :inventario-service:bootRun
    .\gradlew.bat :ventas-service:bootRun

Eureka: http://localhost:9761. Usuario inicial: `admin` (contraseña en `ADMIN_PASSWORD`; por defecto `Admin123*`,
solo para desarrollo).

## Flujo de una venta
`POST /api/ventas` en ventas-service:
1. Toma el precio vigente de cada producto del catálogo (Feign; el cliente nunca decide el precio).
2. Descuenta el stock en el inventario con una sola operación todo-o-nada (Feign).
3. Guarda la venta y su detalle. Si guardar falla después de descontar, devuelve el stock (compensación).

Resiliencia: el catálogo se reintenta (lectura) y tiene circuit breaker; el inventario solo tiene circuit
breaker, porque descontar stock no debe repetirse. Si un servicio está caído, la venta se rechaza con 503.

## Pruebas
    .\gradlew.bat test
    .\gradlew.bat :ventas-service:test
Pruebas de la API: importar en Postman las colecciones de `docs/postman/` y ejecutarlas con el Runner.
La carpeta de resiliencia de cada colección es manual (requiere apagar un servicio).

## Pendiente
RabbitMQ y notificaciones, compras-service, anulación de ventas, api-gateway, Angular, Docker/Kubernetes
y observabilidad. Mejora anotada: cuenta de servicio para las llamadas entre microservicios.

# MSTOCK

Sistema de gestión de inventario y ventas para bodegas y minimarkets,
construido con arquitectura de microservicios.

## Stack
- Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.2, Gradle

## Módulos
| Módulo | Puerto | Descripción |
|---|---|---|
| config-server | 8888 | Configuración centralizada |
| eureka-server | 8761 | Registro y descubrimiento de servicios |

## Cómo ejecutar
    .\gradlew.bat :eureka-server:bootRun
    .\gradlew.bat :config-server:bootRun
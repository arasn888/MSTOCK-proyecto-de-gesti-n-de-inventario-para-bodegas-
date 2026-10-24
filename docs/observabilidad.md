# Observabilidad: metricas con Prometheus y Grafana

Cuando el sistema corre en varios contenedores, "esta lento" o "algo falla" ya no se descubre mirando una consola. La observabilidad
responde tres preguntas sin entrar a ningun servicio: **¿esta vivo?**, **¿cuanto trafico y cuantos errores tiene?** y **¿le falta memoria?**

## Como funciona

1. Cada servicio Spring Boot publica sus metricas en `/actuator/prometheus` (peticiones HTTP, memoria, CPU, conexiones a MySQL, circuit
   breakers…). Eso lo da **Micrometer**, que ya viene con Actuator; solo se agrega el formato de Prometheus.
2. **Prometheus** consulta ese endpoint de cada servicio cada 15 segundos y guarda los valores en el tiempo. En Compose conoce a los
   servicios por nombre (`prometheus-compose.yml`); en Kubernetes los descubre solo, buscando los pods del namespace (`prometheus-k8s.yml`).
3. **Grafana** dibuja esos datos. El dashboard **MSTOCK - Servicios** ya viene cargado: no hay que configurar nada a mano.

`/actuator/prometheus` se permite sin token (clase `MetricasSecurityConfig` en cada servicio, solo para esa ruta). No queda publico: el
gateway no tiene ninguna ruta hacia `/actuator`, y los servicios solo son alcanzables desde la red de Docker o del cluster.

## Cambios en el codigo

- `build.gradle` (raiz): una linea nueva dentro de `subprojects { dependencies { … } }`:
  ```groovy
  runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
  ```
- Una clase `MetricasSecurityConfig` nueva en el paquete `config` de cada servicio con seguridad (auth, catalogo, inventario, ventas,
  compras, notificaciones y gateway). No se modifica ninguna clase existente.
- Variable `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus` en Compose y en Kubernetes, que activa el endpoint solo ahi.
  En desarrollo con `bootRun` no cambia nada.

## Docker Compose

```powershell
docker compose --profile full --profile observabilidad up -d --build
```

- Grafana: **http://localhost:3000** (usuario `admin`, clave `GRAFANA_PASSWORD` de tu `.env`; si no la defines, `admin`).
  Abre *Dashboards → MSTOCK → MSTOCK - Servicios*.
- Prometheus: **http://localhost:9090**. En *Status → Targets* cada servicio debe estar `UP`.

## Kubernetes

```powershell
docker compose --profile full build          # reconstruye las imagenes con las metricas
.\k8s\desplegar.ps1                          # actualiza los pods y crea Prometheus y Grafana
.\k8s\acceso.ps1                             # agrega los tuneles 3000 (Grafana) y 9090 (Prometheus)
```

Si despues de desplegar algun servicio no aparece en Prometheus, reinicialo: `kubectl rollout restart deploy -n mstock`.
En Prometheus, *Status → Targets* debe listar 9 servicios en `UP`.

## Que muestra el dashboard

| Panel | Que dice | Como leerlo |
|---|---|---|
| Servicios arriba / caidos | Cuantos servicios responden a Prometheus | "caidos" debe ser 0 |
| Peticiones por segundo | Trafico total (y por servicio, y por codigo 200/401/500…) | Sube al usar el sistema |
| Errores 5xx por segundo | Fallos del servidor | Lo normal es 0; sube si un servicio se cae |
| Latencia media | Tiempo medio de respuesta | Picos = algo esta lento |
| Circuit breakers abiertos | Circuitos del gateway que cortaron el paso a un servicio | 0 en operacion normal |
| Memoria heap, CPU, hilos | Salud de cada JVM | Si la memoria sube sin parar o toca el tope, el pod terminara en `OOMKilled` |
| Conexiones activas a MySQL | Uso del pool de cada servicio | Cerca del maximo = cuellos de botella |

Si un panel dice "No data": todavia no hubo trafico de ese tipo (por ejemplo, errores 5xx) o el servicio no esta en `Targets`.
El panel de circuit breakers solo muestra datos si el gateway publica las metricas de Resilience4J.

## Probarlo

1. Genera trafico (con el sistema abierto y sesion iniciada, usa la app unos minutos) o, mas rapido:
   ```powershell
   1..300 | ForEach-Object { try { Invoke-WebRequest http://localhost:8080/api/productos -UseBasicParsing | Out-Null } catch { } ; Start-Sleep -Milliseconds 100 }
   ```
   Sin token cada peticion devuelve 401: veras subir *Peticiones por segundo* y el codigo `401` en el panel por codigo.
2. Provoca una falla. En Kubernetes: `kubectl scale deploy/ventas-service -n mstock --replicas=0` y refresca la pantalla de Ventas varias
   veces. *Servicios caidos* pasa a 1 y aparecen errores 5xx del gateway. Restauralo con `--replicas=1`.
   En Compose: `docker compose stop ventas-service` y `docker compose start ventas-service`.
3. Haz una venta en el Punto de venta y mira que *Peticiones por servicio* muestre actividad en `ventas-service` y `inventario-service`.

## Lo que no incluye (todavia)

Trazas distribuidas (seguir una peticion entre servicios con un identificador comun) y logs centralizados (Loki). Son el siguiente
paso natural; se pueden agregar sin tocar lo de aqui.

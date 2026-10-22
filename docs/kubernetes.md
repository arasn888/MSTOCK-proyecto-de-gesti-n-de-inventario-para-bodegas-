# Kubernetes

Los mismos contenedores de `docs/docker.md`, ahora administrados por Kubernetes: si un servicio se cae lo reinicia, no deja pasar
tráfico a un servicio que aún no está listo y permite escalar con un comando.

## Piezas que se usan (y qué hace cada una)

| Pieza | Para qué |
|---|---|
| **Namespace** `mstock` | Carpeta lógica: todo el sistema vive ahí y se borra de una vez |
| **Deployment** | Mantiene encendido el número de copias (*pods*) de un servicio; si un pod muere, crea otro |
| **Service** | Nombre estable dentro del cluster (`mysql`, `eureka-server`, …). Es lo que reemplaza a `localhost` |
| **StatefulSet** + disco (PVC) | MySQL y RabbitMQ: necesitan conservar sus datos aunque el pod se reinicie |
| **Secret** `mstock-secretos` | Claves (JWT, base de datos, RabbitMQ). Lo crea el script desde tu `.env`; no hay claves en los archivos |
| **ConfigMap** `mysql-init` | El `init.sql` que crea las seis bases de datos |
| **initContainer** | Antes de arrancar, cada servicio espera a MySQL, RabbitMQ, Config Server y Eureka. Importante: sin Config Server un servicio arrancaría con la configuración local (`localhost`) y quedaría roto |
| **Probes** | `startup` da hasta 3 min de arranque; `readiness` decide si recibe tráfico; `liveness` reinicia el pod si se cuelga. Los tres consultan `/actuator/health` |
| **resources** | Memoria reservada y tope por pod; evita que un servicio se coma toda la máquina |

Archivos en `k8s/`: `00-namespace`, `10-mysql`, `11-rabbitmq`, `20-plataforma` (Eureka y Config Server), `30-servicios` (los seis de
negocio), `40-gateway-web`, más `desplegar.ps1` y `acceso.ps1`.

## Requisitos

- Un cluster local: **Docker Desktop** (Settings → Kubernetes → *Enable Kubernetes*) o **minikube**.
- Memoria: 8 GB o más para el cluster (son 10 pods con Java más MySQL y RabbitMQ).
- Las imágenes `mstock/*`: ya las construyó `docker compose --profile full build` (o usa `-ConstruirImagenes`).
- Apaga antes el modo Docker Compose (`docker compose --profile full down`): usa los mismos puertos que `acceso.ps1`.

## Desplegar

```powershell
cd "D:\Desarrollo de aplicaciones web II\MSTOCK"
kubectl config current-context              # debe ser docker-desktop (o minikube)
.\k8s\desplegar.ps1                         # con minikube:  .\k8s\desplegar.ps1 -Minikube
.\k8s\acceso.ps1                            # abre los túneles; luego http://localhost:4200
```

El despliegue tarda varios minutos. Para seguirlo: `kubectl get pods -n mstock -w` (Ctrl+C para salir). Un pod pasa por `Init`
(esperando a la plataforma), luego `Running` con `0/1` y finalmente `1/1`.

## Comandos útiles

```powershell
kubectl get pods -n mstock                                  # estado de todo
kubectl logs -n mstock deploy/ventas-service --tail 50      # logs de un servicio
kubectl describe pod -n mstock <pod>                        # por qué un pod no arranca (sección Events)
kubectl rollout restart deploy/ventas-service -n mstock     # reiniciar un servicio
kubectl scale deploy/catalogo-service -n mstock --replicas=2   # 2 copias; el gateway reparte entre ellas
kubectl delete pod -n mstock -l app=catalogo-service        # matar pods: Kubernetes los recrea solo
```

Prueba de resiliencia: con el sistema abierto, borra el pod de `ventas-service` y refresca Ventas; durante unos segundos el gateway
responde con el mensaje de servicio no disponible (Resilience4J) y vuelve solo cuando el pod nuevo está `1/1`.

## Quitarlo

```powershell
Get-Process kubectl | Stop-Process          # cierra los túneles
kubectl delete namespace mstock             # borra TODO, incluidas las bases de datos
```

## Problemas frecuentes

- **`ImagePullBackOff` / `ErrImagePull` en `mstock/...`:** el cluster no ve tus imágenes locales. En minikube usa
  `.\k8s\desplegar.ps1 -Minikube` (las copia al cluster). En Docker Desktop, si el tipo de cluster es *kind*, las imágenes locales no
  se comparten: cambia a *kubeadm* en Settings → Kubernetes, o avísame y se usa un registro local.
- **Pod en `Init` mucho rato:** el `initContainer` está esperando a otro servicio; `kubectl logs -n mstock <pod> -c esperar-plataforma`
  muestra a cuál.
- **`OOMKilled` en `describe pod`:** poca memoria; sube `limits.memory` en el manifiesto o la memoria del cluster.
- **El frontend carga pero no inicia sesión:** falta el túnel del gateway (`acceso.ps1`); el navegador llama a `localhost:8080`.

## Qué queda fuera de este paso

Hoy se entra por `port-forward` y el frontend apunta a `http://localhost:8080` (valor fijo de `environment.ts`). Para un acceso real
(un dominio, HTTPS) haría falta un **Ingress** y que el frontend lea la URL del gateway al arrancar. Se puede agregar después.

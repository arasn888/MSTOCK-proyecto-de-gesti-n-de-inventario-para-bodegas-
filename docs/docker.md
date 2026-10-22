# Docker: todo el sistema con un comando

Hay dos formas de trabajar, y **no se mezclan** (usan los mismos puertos):

| Modo | Comando | Para qué |
|---|---|---|
| Desarrollo | `docker compose up -d` y luego `bootRun` / `ng serve` | Programar: solo MySQL y RabbitMQ en Docker; el resto corre desde el IDE |
| Todo en contenedores | `docker compose --profile full up -d --build` | Probar el sistema completo, como se despliega |

Antes de cambiar de modo, apaga el otro (cierra los `bootRun` y `ng serve`, o `docker compose --profile full down`).

## Requisitos

- Docker Desktop con **al menos 6 GB de memoria** asignados (Settings → Resources): son 9 JVM más MySQL y RabbitMQ.
- El archivo `.env` de la raíz (ver `.env.example`). Docker lo lee solo; no hace falta `cargar-env.ps1`.

## Levantar todo

```powershell
docker compose --profile full up -d --build
```

La primera vez tarda varios minutos (descarga imágenes y compila cada servicio con Gradle dentro del contenedor). Las siguientes veces
usa la caché y es mucho más rápido. Los servicios arrancan por capas, esperando a que la anterior esté sana:

1. MySQL y RabbitMQ
2. eureka-server y config-server
3. auth, catálogo, inventario, ventas, compras y notificaciones
4. api-gateway y, por último, el frontend

Seguimiento:

```powershell
docker compose --profile full ps          # el estado debe pasar a "healthy" en cada servicio
docker compose --profile full logs -f ventas-service
```

Cuando todo esté `healthy`: **http://localhost:4200** (frontend), http://localhost:8080 (gateway), http://localhost:9761 (Eureka) y
http://localhost:15673 (RabbitMQ, con el usuario y clave de tu `.env`). El usuario inicial es `admin` con la clave de `ADMIN_PASSWORD`
(por defecto `Admin123*`; solo se aplica la primera vez, cuando la base de auth está vacía).

## Apagar

```powershell
docker compose --profile full down        # apaga y conserva los datos (volúmenes)
docker compose --profile full down -v     # apaga y BORRA las bases de datos
```

## Cómo se configura cada contenedor

- **Un solo `Dockerfile`** en la raíz sirve para los nueve servicios: compila con Gradle (`bootJar`, sin tests) y deja una imagen
  final con solo el JRE 21, sin root.
- **Variables de entorno en lugar de `localhost`.** Dentro de Docker cada servicio vive en su propio contenedor, así que las
  direcciones cambian por los nombres del compose. El código no se toca: Spring permite sobrescribir cualquier propiedad con una
  variable de entorno.

  | Variable | Valor en Docker | Reemplaza |
  |---|---|---|
  | `SPRING_CONFIG_IMPORT` | `optional:configserver:http://config-server:9888` | `localhost:9888` |
  | `EUREKA_DEFAULT_ZONE` | `http://eureka-server:9761/eureka/` | `localhost:9761` |
  | `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/mstock_<servicio>…` | `localhost:3307` |
  | `SPRING_RABBITMQ_HOST` / `_PORT` | `rabbitmq` / `5672` | `localhost:5673` |
  | `EUREKA_INSTANCE_PREFER_IP_ADDRESS` | `true` | se registran con su IP, no con el id del contenedor |

  (Los puertos 3307 y 5673 son los de *tu máquina*; dentro de la red de Docker MySQL y RabbitMQ usan sus puertos normales.)
- `config-server/.../configs/application.yml` ahora lee la dirección de Eureka de `EUREKA_DEFAULT_ZONE` (con `localhost` como valor
  por defecto, así el modo desarrollo sigue igual).
- **Healthchecks** con `/actuator/health` en cada servicio; el compose usa `depends_on: condition: service_healthy`.
- **Frontend:** se compila con Node 22 y se sirve con nginx, que devuelve `index.html` para cualquier ruta de Angular. El navegador
  sigue llamando al gateway en `http://localhost:8080` (por eso el CORS del gateway ya permite `http://localhost:4200`).

## Problemas frecuentes

- **Un servicio queda `unhealthy` o reinicia sin parar:** `docker compose --profile full logs <servicio>`. Lo más común es poca memoria
  en Docker Desktop (aparece `Killed` o `OutOfMemoryError`); sube la memoria y repite.
- **"port is already allocated":** hay algo corriendo en ese puerto (un `bootRun` o `ng serve` del modo desarrollo).
- **Cambiaste código de un servicio:** `docker compose --profile full up -d --build <servicio>` reconstruye solo ese.
- **Tras cambiar `JWT_SECRET` o claves en `.env`:** `docker compose --profile full up -d` recrea los contenedores afectados. Si
  cambias `MYSQL_PASSWORD` o `RABBITMQ_PASSWORD` con volúmenes ya creados, MySQL y RabbitMQ conservan la clave anterior: hay que
  borrar los volúmenes (`down -v`) para que tomen la nueva.

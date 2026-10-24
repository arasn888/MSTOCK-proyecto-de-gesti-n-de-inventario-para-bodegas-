# Construir las imagenes en GitHub (sin cargar tu computadora)

Compilar los nueve servicios con Gradle dentro de Docker es lo que mas memoria y red consume. El workflow
`.github/workflows/imagenes.yml` lo hace en los servidores de GitHub cada vez que haces `git push` a `main`, y deja las imagenes
publicadas en GitHub Container Registry:

```
ghcr.io/<usuario>/mstock-eureka-server:latest      ...  mstock-web:latest   (10 imagenes)
```

Ademas cada imagen queda etiquetada con el commit (`:<sha>`), para volver a una version anterior si algo sale mal. Una ventaja extra:
si el codigo Java no compila, el error aparece en la pestana **Actions** de GitHub.

## Usarlo

1. `git add .`, `git commit`, `git push` (el push dispara el workflow).
2. En GitHub, pestana **Actions → Imagenes**: cada servicio es un trabajo; tardan unos 5 a 8 minutos y corren en paralelo.
3. Cuando todos esten en verde, en tu computadora:
   ```powershell
   .\scripts\descargar-imagenes.ps1
   docker compose --profile full up -d        # sin --build
   ```
   o, en Kubernetes: `.\k8s\desplegar.ps1`. El script descarga las imagenes y las renombra a `mstock/<servicio>:latest`, que es lo que
   esperan el compose y los manifiestos; por eso no hubo que tocar ninguno de los dos.

Para reconstruir a mano sin hacer un cambio: **Actions → Imagenes → Run workflow**.

## Si las imagenes son privadas

Si el repositorio es privado, las imagenes tambien. Se resuelve de una de dos formas:
- **Iniciar sesion una vez** (recomendado): en GitHub, *Settings → Developer settings → Personal access tokens → Tokens (classic)*, crea uno
  con el permiso `read:packages` y ejecuta `docker login ghcr.io -u <tu-usuario>` usando el token como contrasena.
- Hacer publicas las imagenes: *tu perfil → Packages → cada paquete → Package settings → Change visibility*.

## Problemas frecuentes

- **El trabajo falla al publicar ("denied" o "permission_denied: write_package"):** *Settings → Actions → General → Workflow permissions*
  debe estar en *Read and write permissions*.
- **Un servicio falla al compilar:** abre ese trabajo en Actions y mira el paso "Construir y publicar"; el error de Java aparece ahi.
- **`docker pull` dice "unauthorized" o "not found":** el workflow aun no termino, o es privada y falta el `docker login`.
- **Quieres correrlo en una maquina con procesador ARM** (por ejemplo, la nube gratuita de Oracle): agrega
  `platforms: linux/amd64,linux/arm64` al paso "Construir y publicar".

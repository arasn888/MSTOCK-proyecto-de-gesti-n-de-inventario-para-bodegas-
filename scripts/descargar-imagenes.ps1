<#
Descarga desde GitHub las imagenes que construyo GitHub Actions y las deja con los nombres que usan
docker-compose.yml y k8s/ (mstock/<servicio>:latest), asi no hay que compilar nada en esta computadora.

Uso (desde la raiz de MSTOCK):
    .\scripts\descargar-imagenes.ps1                          # usuario por defecto: arasn888
    .\scripts\descargar-imagenes.ps1 -Usuario otro-usuario

Si las imagenes son privadas, antes inicia sesion una vez:
    docker login ghcr.io -u <tu-usuario>        (la contrasena es un token de GitHub con el permiso read:packages)
#>
param(
    [string]$Usuario = 'arasn888',
    [string]$Etiqueta = 'latest'
)
$ErrorActionPreference = 'Stop'
$dueno = $Usuario.ToLower()
$servicios = 'eureka-server', 'config-server', 'auth-service', 'catalogo-service', 'inventario-service',
             'ventas-service', 'compras-service', 'notificaciones-service', 'api-gateway', 'web'

foreach ($s in $servicios) {
    $remota = "ghcr.io/${dueno}/mstock-${s}:${Etiqueta}"
    Write-Host "Descargando $remota" -ForegroundColor Cyan
    docker pull $remota
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo descargar $remota. Revisa que el workflow haya terminado en GitHub y, si es privada, el docker login."
    }
    docker tag $remota "mstock/${s}:latest"
}
Write-Host 'Listo. Ahora: docker compose --profile full up -d   (sin --build)   o   .\k8s\desplegar.ps1' -ForegroundColor Green

<#
Despliega MSTOCK en Kubernetes (Docker Desktop o minikube). Ejecutar desde la raiz del proyecto:
    .\k8s\desplegar.ps1                       # usa las imagenes mstock/* que ya existen en Docker
    .\k8s\desplegar.ps1 -ConstruirImagenes    # las construye antes (docker compose build)
    .\k8s\desplegar.ps1 -Minikube             # ademas las copia al cluster de minikube
Lee las claves del archivo .env y crea el Secret; no hay claves escritas en los manifiestos.
#>
param(
    [switch]$ConstruirImagenes,
    [switch]$Minikube
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw 'No se encontro kubectl. Activa Kubernetes en Docker Desktop (Settings > Kubernetes) o instala minikube.'
}
if (-not (Test-Path '.env')) { throw 'No se encontro .env. Ejecuta este script desde la raiz de MSTOCK.' }

# --- Claves desde .env ---
$vars = @{}
Get-Content '.env' | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') { $vars[$Matches[1]] = $Matches[2] }
}
if (-not $vars['ADMIN_PASSWORD']) { $vars['ADMIN_PASSWORD'] = 'Admin123*' }
if (-not $vars['GRAFANA_PASSWORD']) { $vars['GRAFANA_PASSWORD'] = 'admin' }   # clave de Grafana (opcional en .env)
$claves = 'MYSQL_ROOT_PASSWORD', 'MYSQL_USER', 'MYSQL_PASSWORD', 'JWT_SECRET', 'ADMIN_PASSWORD', 'RABBITMQ_USER', 'RABBITMQ_PASSWORD', 'GRAFANA_PASSWORD'
$faltan = $claves | Where-Object { -not $vars[$_] }
if ($faltan) { throw ('Faltan variables en .env: ' + ($faltan -join ', ')) }

Write-Host ('Cluster: ' + (kubectl config current-context)) -ForegroundColor Cyan

# --- Imagenes ---
if ($ConstruirImagenes) {
    docker compose --profile full build
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la construccion de las imagenes.' }
}
if ($Minikube) {
    foreach ($n in 'eureka-server', 'config-server', 'auth-service', 'catalogo-service', 'inventario-service',
                   'ventas-service', 'compras-service', 'notificaciones-service', 'api-gateway', 'web') {
        Write-Host "Cargando mstock/${n} en minikube..."
        minikube image load "mstock/${n}:latest"
    }
}

# --- Namespace, Secret y ConfigMap (los dos ultimos se generan, no se escriben a mano) ---
kubectl apply -f k8s/00-namespace.yaml
$literales = $claves | ForEach-Object { "--from-literal=$_=$($vars[$_])" }
kubectl create secret generic mstock-secretos -n mstock @literales --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap mysql-init -n mstock --from-file=init.sql=docker/mysql/init.sql --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap prometheus-config -n mstock --from-file=prometheus.yml=observabilidad/prometheus/prometheus-k8s.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-config -n mstock `
    --from-file=datasource.yml=observabilidad/grafana/provisioning/datasources/datasource.yml `
    --from-file=dashboards.yml=observabilidad/grafana/provisioning/dashboards/dashboards.yml `
    --from-file=mstock.json=observabilidad/grafana/dashboards/mstock.json `
    --dry-run=client -o yaml | kubectl apply -f -

# --- Resto de manifiestos ---
kubectl apply -f k8s/

Write-Host 'Esperando a que arranquen los servicios (puede tardar varios minutos)...' -ForegroundColor Cyan
kubectl wait --for=condition=available deployment --all -n mstock --timeout=600s
kubectl get pods -n mstock
Write-Host "Listo. Ahora ejecuta .\k8s\acceso.ps1 para abrir el sistema en http://localhost:4200" -ForegroundColor Green

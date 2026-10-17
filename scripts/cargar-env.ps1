# Carga las variables del archivo .env en la terminal actual.
# Uso (desde la raiz de MSTOCK):
#     Get-Content .\scripts\cargar-env.ps1 -Raw | Invoke-Expression
if (-not (Test-Path ".\.env")) {
    Write-Host "No se encontro .env. Ejecuta este comando desde la raiz de MSTOCK." -ForegroundColor Red
} else {
    Get-Content ".\.env" | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
        }
    }
    $env:DB_PASSWORD = $env:MYSQL_PASSWORD
    $faltan = @("DB_PASSWORD", "JWT_SECRET", "RABBITMQ_PASSWORD") | Where-Object { -not [Environment]::GetEnvironmentVariable($_) }
    if ($faltan) {
        Write-Host ("Faltan variables en .env: " + ($faltan -join ", ")) -ForegroundColor Yellow
    } else {
        Write-Host "Variables cargadas: DB_PASSWORD, JWT_SECRET y RABBITMQ_PASSWORD" -ForegroundColor Green
    }
}

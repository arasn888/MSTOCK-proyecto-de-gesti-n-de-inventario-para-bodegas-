<#
Publica el sistema en tu maquina con "port-forward" (el cluster no expone puertos por si solo):
    4200 -> frontend, 8080 -> gateway, 9761 -> Eureka, 15672 -> panel de RabbitMQ
Cada tunel corre en una ventana minimizada. Para cerrarlos:  Get-Process kubectl | Stop-Process
#>
$tuneles = @(
    @('svc/web', '4200:80'),
    @('svc/api-gateway', '8080:8080'),
    @('svc/eureka-server', '9761:9761'),
    @('svc/rabbitmq', '15672:15672')
)
foreach ($t in $tuneles) {
    Start-Process kubectl -WindowStyle Minimized -ArgumentList 'port-forward', '-n', 'mstock', $t[0], $t[1]
}
Write-Host 'Frontend: http://localhost:4200   Gateway: http://localhost:8080   Eureka: http://localhost:9761' -ForegroundColor Green

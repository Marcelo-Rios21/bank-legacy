# Instrucciones de ejecución - Bank Legacy

## 1. Descripción

Bank Legacy moderniza procesos bancarios legacy mediante una arquitectura compuesta por:

- Spring Batch para procesamiento masivo.
- BFF independientes para Web, Mobile y ATM.
- Microservicios de cuentas, transacciones y movimientos.
- Spring Cloud Config.
- Netflix Eureka.
- OAuth 2.0.
- Resilience4j.
- Apache Kafka.
- Oracle Database.
- Docker y Docker Compose.

Repositorio:

https://github.com/Marcelo-Rios21/bank-legacy.git

## 2. Requisitos

Para ejecutar el proyecto se requiere:

- Java 21.
- Maven Wrapper incluido en el repositorio.
- Docker Desktop o Docker Engine.
- Docker Compose.
- Acceso a la base de datos Oracle configurada para el proyecto.
- PowerShell para los comandos indicados en este documento.

Verificación:

```powershell
java -version
.\mvnw.cmd -version
docker --version
docker compose version
```

## 3. Dataset oficial EFT

Los archivos utilizados por Spring Batch se encuentran en:

```text
data/eft/
├── movimientos_financieros_diarios.csv
├── intereses_trimestrales.csv
└── estados_financieros_anuales.csv
```

Fuente del dataset:

```text
https://github.com/KariVillagran/fin_legacy_data
```

## 4. Variables sensibles

Las credenciales y secretos no deben almacenarse directamente en Git.

La arquitectura utiliza, entre otras, las siguientes variables:

```text
OAUTH2_CLIENT_ID
OAUTH2_CLIENT_SECRET
OAUTH2_ISSUER_URI

BFF_WEB_JWT_SECRET
BFF_MOBILE_JWT_SECRET
BFF_ATM_JWT_SECRET
```

Las variables asociadas a Oracle deben configurarse según el entorno utilizado.

Para revisar las variables referenciadas actualmente por la solución:

```powershell
Get-ChildItem `
    -Path . `
    -Recurse `
    -Include *.properties,*.yml,*.yaml `
    -File |
    Where-Object {
        $_.FullName -notmatch '\\target\\|\\.git\\'
    } |
    Select-String -Pattern '\$\{[A-Z0-9_]+'
```

No se deben publicar contraseñas, secretos OAuth, secretos JWT ni tokens.

## 5. Pruebas Batch

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd clean test
```

Resultado validado para el módulo Batch:

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

## 6. Ejecución de dailyTransactionJob

```powershell
$env:SPRING_BATCH_JOB_NAME = "dailyTransactionJob"

$runId = Get-Date -Format "yyyyMMddHHmmss"

.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.arguments=run.id=$runId"
```

Resultado obtenido con el dataset oficial:

```text
Registros leídos: 1000
Registros persistidos: 392
Registros inválidos: 608
Filas finales Oracle: 392
```

También se identificaron:

```text
13 grupos de posibles duplicados
27 registros involucrados
```

## 7. Ejecución de monthlyInterestJob

```powershell
$env:SPRING_BATCH_JOB_NAME = "monthlyInterestJob"

$runId = Get-Date -Format "yyyyMMddHHmmss"

.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.arguments=run.id=$runId"
```

Resultado obtenido:

```text
Registros leídos: 1000
Registros persistidos: 296
Registros inválidos: 704
Cuentas finales Oracle: 50
```

El dataset oficial entrega el archivo:

```text
intereses_trimestrales.csv
```

El Job conserva el nombre `monthlyInterestJob` porque corresponde al proceso mensual implementado en el proyecto.

## 8. Ejecución de annualAccountJob

```powershell
$env:SPRING_BATCH_JOB_NAME = "annualAccountJob"

$runId = Get-Date -Format "yyyyMMddHHmmss"

.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.arguments=run.id=$runId"
```

Resultado obtenido:

```text
Periodo: 2024-01-01 a 2024-12-30

Registros leídos: 1000
Registros persistidos: 779
Registros inválidos: 221
Cuentas distintas: 20
```

El reporte generado queda disponible en:

```text
output/auditoria_anual.txt
```

Al terminar las ejecuciones Batch:

```powershell
Remove-Item Env:SPRING_BATCH_JOB_NAME -ErrorAction SilentlyContinue
```

## 9. Restart y checkpoint

`dailyTransactionJob` incluye una prueba controlada para demostrar recuperación desde checkpoint.

La evidencia final se encuentra en:

```text
evidencias/semana_9/batch_restart_checkpoint.txt
```

La validación realizada utilizó la misma `JobInstance`.

La primera ejecución finalizó:

```text
FAILED
```

La segunda ejecución finalizó:

```text
COMPLETED
```

Las particiones ya completadas no fueron procesadas nuevamente. Solo se retomó la partición que había fallado.

El resultado final de la tabla `DAILY_TRANSACTION` fue de 392 registros.

## 10. Pruebas de los BFF

Los tres BFF pueden probarse individualmente:

```powershell
.\mvnw.cmd -f .\bff\web\pom.xml test

.\mvnw.cmd -f .\bff\mobile\pom.xml test

.\mvnw.cmd -f .\bff\atm\pom.xml test
```

Los canales implementados son:

```text
Web
Mobile
ATM
```

Cada BFF dispone de endpoints y autorización específicos para su canal.

## 11. Microservicios

La solución implementa:

```text
account-service
transaction-service
movement-service
```

Puertos utilizados:

```text
account-service       8091
transaction-service   8092
movement-service      8093
```

Endpoints principales:

```text
GET  /api/accounts/{cuentaId}

GET  /api/transactions/{id}
POST /api/transactions/{id}/publish

GET  /api/movements/accounts/{cuentaId}
```

## 12. Spring Cloud

La infraestructura utiliza:

```text
Config Server          8888
Eureka Server          8761
Authorization Server   9000
```

Los microservicios obtienen su configuración desde Spring Cloud Config y se registran en Eureka.

La configuración centralizada se encuentra en:

```text
cloud/config-repo/
```

## 13. OAuth 2.0

Los tres microservicios funcionan como OAuth 2.0 Resource Servers.

El Authorization Server utiliza el flujo:

```text
Client Credentials
```

Scopes disponibles:

```text
account.read
transaction.read
transaction.publish
movement.read
```

Para solicitar un token:

```text
POST http://localhost:9000/oauth2/token
```

Ejemplo:

```powershell
$credenciales = "$env:OAUTH2_CLIENT_ID`:$env:OAUTH2_CLIENT_SECRET"

$basic = [Convert]::ToBase64String(
    [Text.Encoding]::ASCII.GetBytes($credenciales)
)

$respuesta = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:9000/oauth2/token" `
    -Headers @{
        Authorization = "Basic $basic"
    } `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "client_credentials"
        scope = "account.read transaction.read transaction.publish movement.read"
    }

$token = $respuesta.access_token
```

Ejemplo de acceso a `account-service`:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8091/api/accounts/<CUENTA_ID>" `
    -Headers @{
        Authorization = "Bearer $token"
    }
```

La seguridad fue validada con:

```text
401 Unauthorized -> solicitud sin token
403 Forbidden    -> token sin el scope requerido
200 OK           -> scope autorizado
202 Accepted     -> publicación Kafka autorizada
```

## 14. Apache Kafka

Kafka se utiliza para comunicación asíncrona entre:

```text
transaction-service
        |
        v
bank.transactions
        |
        v
movement-service
```

Configuración utilizada:

```text
Tópico: bank.transactions
Particiones: 3
Consumer group: movement-service-group
```

`transaction-service` publica mediante:

```text
POST /api/transactions/{id}/publish
```

El endpoint requiere:

```text
transaction.publish
```

`movement-service` consume el evento mediante `@KafkaListener`.

El consumidor registra el evento recibido sin generar movimientos financieros adicionales ni modificar saldos.

## 15. Resilience4j

Los microservicios protegen el acceso a Oracle mediante Circuit Breaker.

Instancias configuradas:

```text
accountDatabase
transactionDatabase
movementDatabase
```

Configuración principal:

```text
Sliding window: 4 llamadas
Mínimo de llamadas: 2
Failure threshold: 50 %
OPEN: 10 segundos
HALF_OPEN: 1 llamada permitida
```

## 16. Docker Compose

La arquitectura distribuida puede levantarse desde:

```text
docker-compose.yaml
```

Primero validar:

```powershell
docker compose config
```

Construir imágenes:

```powershell
docker compose build
```

Levantar servicios:

```powershell
docker compose up -d
```

Comprobar:

```powershell
docker compose ps
```

La composición incluye:

```text
Apache Kafka
Config Server
Eureka Server
Authorization Server
account-service
transaction-service
movement-service
```

Oracle permanece como servicio externo.

## 17. Logs

Logs generales:

```powershell
docker compose logs --tail 100
```

Ejemplo para `transaction-service`:

```powershell
docker compose logs --tail 100 transaction-service
```

Ejemplo para `movement-service`:

```powershell
docker compose logs --tail 100 movement-service
```

## 18. Detener la infraestructura

```powershell
docker compose down
```

También se pueden eliminar contenedores huérfanos:

```powershell
docker compose down --remove-orphans
```

## 19. Evidencias

Las evidencias se encuentran organizadas en:

```text
evidencias/
├── semana_2/
├── semana_3/
├── semana_4/
├── semana_5/
├── semana_6/
├── semana_7/
├── semana_8/
└── semana_9/
```

Las evidencias finales del procesamiento con el dataset oficial se encuentran en:

```text
evidencias/semana_9/
```

Incluyen:

```text
auditoria_anual_oficial.txt
batch_dataset_oficial.txt
batch_restart_checkpoint.txt
```

Para mayor detalle técnico de la implementación consultar también:

```text
README.md
```
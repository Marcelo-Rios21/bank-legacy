# Bank Legacy

Proyecto desarrollado con **Spring Batch** para procesar información legacy del Banco XYZ almacenada en archivos CSV.

La aplicación actualmente ejecuta tres procesos batch independientes:

- Reporte diario de transacciones.
- Cálculo mensual de intereses.
- Procesamiento y auditoría de estados de cuenta anuales.

Los archivos son leídos mediante Spring Batch, sus registros son validados y transformados mediante `ItemProcessor` y los resultados son persistidos en **Oracle Database**.

## Tecnologías

- Java 21
- Spring Boot 4.1.0
- Spring Batch 6.0.4
- Spring JDBC
- Oracle Database
- Maven
- JUnit 5

## Fuente de datos

Los datos legacy utilizados provienen de:

https://github.com/KariVillagran/bank_legacy_data

La aplicación actualmente procesa los archivos ubicados en:

```text
data/semana_3/
├── transacciones.csv
├── intereses.csv
└── cuentas_anuales.csv
```

## Estructura del proyecto

```text
src/
├── main/
│   ├── java/com/bank/bank_legacy/
│   │   ├── config/
│   │   ├── exception/
│   │   ├── job/
│   │   ├── model/
│   │   ├── partition/
│   │   ├── policy/
│   │   ├── processor/
│   │   ├── reader/
│   │   ├── writer/
│   │   └── BankLegacyApplication.java
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/
    └── java/com/bank/bank_legacy/
        ├── BankLegacyApplicationTests.java
        ├── policy/
        │   └── BankDataSkipPolicyTest.java
        └── processor/
            ├── AnnualAccountProcessorTest.java
            ├── DailyTransactionProcessorTest.java
            └── MonthlyInterestProcessorTest.java
```
## Flujo de procesamiento

Los procesos batch utilizan principalmente el siguiente flujo:

```text
CSV
 ↓
FlatFileItemReader
 ↓
ItemProcessor
 ↓
JdbcBatchItemWriter
 ↓
Oracle Database
```

Los `ItemProcessor` contienen las reglas de validación y transformación de cada proceso.

Cuando un registro contiene datos inválidos, se lanza una excepción específica y Spring Batch lo omite mediante una política `skip`, permitiendo continuar con los registros restantes.

---

## Jobs

### 1. Transacciones diarias

El Job `dailyTransactionJob` procesa:

```text
data/semana_3/transacciones.csv
```

Los registros contienen:

```text
id,fecha,monto,tipo
```

El proceso valida:

- ID obligatorio, numérico y mayor que cero.
- Fecha obligatoria y válida.
- Monto obligatorio, numérico y mayor que cero.
- Tipo de transacción `debito` o `credito`.

La aplicación reconoce distintos formatos de fecha presentes en los datos legacy.

Los registros inválidos generan `InvalidTransactionException`.

Las transacciones válidas se almacenan en la tabla:

```text
DAILY_TRANSACTION
```

El Writer utiliza `MERGE`, permitiendo ejecutar nuevamente el Job sin duplicar registros por ID.

La ejecución actual con los datos de semana 3 obtiene:

```text
Registros leídos:      1000
Registros persistidos: 401
Registros inválidos:   599
Filas finales Oracle:  401
```

El Job también genera un resumen con la cantidad de registros recibidos, registros válidos, registros omitidos y posibles duplicados.

Resultado actual:

```text
===== RESUMEN TRANSACCIONES DIARIAS =====
Total recibidas: 1000
Validas persistidas: 401
Invalidas omitidas: 599
Posibles duplicados: 14 grupo(s), 28 registro(s)
==========================================
```

---

### 2. Intereses mensuales

El Job `monthlyInterestJob` procesa:

```text
data/semana_3/intereses.csv
```

Los registros contienen:

```text
cuenta_id,nombre,saldo,edad,tipo
```

El proceso valida:

- ID de cuenta obligatorio, numérico y mayor que cero.
- Nombre obligatorio.
- Saldo obligatorio y numérico.
- Saldo mayor o igual a cero.
- Edad dentro del rango aceptado.
- Tipo de cuenta `ahorro` o `prestamo`.

Los registros inválidos generan `InvalidInterestException`.

Para este ejercicio académico se utilizan las siguientes tasas mensuales:

| Tipo | Tasa |
|---|---:|
| Ahorro | 1 % |
| Préstamo | 2 % |

El interés se calcula mediante:

```text
interes = saldoInicial × tasa
```

El saldo final se calcula mediante:

```text
saldoFinal = saldoInicial + interes
```

Las tasas utilizadas corresponden a valores definidos para este proyecto académico y no representan tasas bancarias comerciales reales.

Los resultados se almacenan en:

```text
MONTHLY_INTEREST
```

El Writer utiliza `MERGE`, por lo que una nueva ejecución actualiza la cuenta existente en lugar de crear un registro duplicado.

La ejecución actual obtiene:

```text
Registros leídos:       1000
Registros persistidos:  263
Registros inválidos:    737
Cuentas finales Oracle: 50
```

Los 263 registros válidos pueden corresponder varias veces a una misma cuenta. Como el Writer utiliza `MERGE` por `CUENTA_ID`, el resultado final contiene 50 cuentas distintas.

---

### 3. Estados de cuenta anuales

El Job `annualAccountJob` procesa:

```text
data/semana_3/cuentas_anuales.csv
```

El proceso está compuesto por tres Steps:

```text
annualAccountCleanupStep
        ↓
annualAccountStep
        ↓
annualAccountAuditStep
```

#### annualAccountCleanupStep

Elimina la carga anual existente antes de procesar nuevamente el archivo.

Esto evita duplicar movimientos al volver a ejecutar el Job.

#### annualAccountStep

Procesa registros con la estructura:

```text
cuenta_id,fecha,transaccion,monto,descripcion
```

El proceso valida:

- ID de cuenta.
- Fecha.
- Tipo de transacción.
- Monto.
- Descripción.

Actualmente se aceptan los tipos de movimiento:

- `deposito`
- `retiro`
- `compra`
- `pago`

También se reconocen distintos formatos de fecha presentes en los archivos legacy.

En este proceso los montos negativos son válidos, ya que pueden representar retiros o compras.

Los registros inválidos generan `InvalidAnnualAccountException`.

Los movimientos válidos se almacenan en:

```text
ANNUAL_ACCOUNT_ENTRY
```

Cada movimiento posee un `MOVIMIENTO_ID` autogenerado, lo que permite almacenar múltiples movimientos asociados a una misma cuenta.

La ejecución actual obtiene:

```text
Registros leídos:      1000
Registros persistidos: 732
Registros inválidos:   268
Cuentas distintas:     20
Filas finales Oracle:  732
```

#### annualAccountAuditStep

Después de procesar los movimientos se genera un reporte de auditoría con:

- Periodo procesado.
- Registros leídos.
- Registros persistidos.
- Registros inválidos omitidos.
- Cantidad de cuentas distintas.
- Depósitos.
- Retiros.
- Compras.
- Pagos.
- Movimientos con monto cero.
- Movimientos con monto negativo.
- Balance neto de movimientos.

El reporte actual es:

```text
===== REPORTE DE AUDITORIA ANUAL =====
Periodo: 2024-01-02 a 2024-12-29

Registros leidos: 1000
Registros persistidos: 732
Registros invalidos omitidos: 268
Cuentas distintas: 20

Depositos: 262
Retiros: 205
Compras: 232
Pagos: 33

Movimientos con monto cero: 9
Movimientos con monto negativo: 199
Balance neto de movimientos: 1048600
======================================
```

El reporte también se almacena en:

```text
output/auditoria_anual.txt
```

---

## Base de datos

La aplicación utiliza **Oracle Database** mediante JDBC.

Las tablas de negocio actuales son:

- `DAILY_TRANSACTION`
- `MONTHLY_INTEREST`
- `ANNUAL_ACCOUNT_ENTRY`

Spring Batch utiliza además sus tablas internas de metadatos para registrar la ejecución de Jobs y Steps, entre ellas:

- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_STEP_EXECUTION`

El esquema de las tablas de negocio está definido en:

```text
src/main/resources/schema.sql
```

---

## Configuración de Oracle

Las credenciales de conexión no se almacenan directamente en el código.

`application.properties` utiliza variables de entorno:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

Antes de ejecutar la aplicación en PowerShell se definen:

```powershell
$env:DB_URL = "URL_DE_ORACLE"
$env:DB_USERNAME = "BANK_LEGACY"
$env:DB_PASSWORD = "PASSWORD"
```

---

## Ejecución

Como el proyecto contiene varios Jobs, cada proceso se selecciona mediante la variable:

```powershell
$env:SPRING_BATCH_JOB_NAME = "nombreDelJob"
```

Cada ejecución utiliza un `run.id` diferente.

### Transacciones diarias

```powershell
$env:SPRING_BATCH_JOB_NAME = "dailyTransactionJob"

$runId = Get-Date -Format "yyyyMMddHHmmss"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=run.id=$runId"
```

### Intereses mensuales

```powershell
$env:SPRING_BATCH_JOB_NAME = "monthlyInterestJob"

$runId = Get-Date -Format "yyyyMMddHHmmss"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=run.id=$runId"
```

### Estados de cuenta anuales

```powershell
$env:SPRING_BATCH_JOB_NAME = "annualAccountJob"

$runId = Get-Date -Format "yyyyMMddHHmmss"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=run.id=$runId"
```

Para eliminar la selección del Job:

```powershell
Remove-Item Env:SPRING_BATCH_JOB_NAME
```

---

## Manejo de errores y tolerancia a fallos

Los tres Jobs utilizan procesamiento tolerante a fallos mediante una política personalizada llamada `BankDataSkipPolicy`.

Cada Worker Step configura tolerancia a fallos mediante:

```java
.faultTolerant()
.retryPolicy(bankRetryPolicy)
.skipPolicy(new BankDataSkipPolicy(
        ExcepcionDelProceso.class,
        maxSkips))
```

El límite de omisiones se configura mediante:

```properties
batch.fault-tolerance.max-skips=1000
```

La política permite omitir solamente errores de datos conocidos mientras no se alcance el límite configurado. Una excepción no contemplada no es omitida y provoca el fallo del Step.

Cada proceso mantiene su propia excepción de validación:

- `InvalidTransactionException`
- `InvalidInterestException`
- `InvalidAnnualAccountException`

Los errores de validación se omiten porque volver a procesar un registro cuyo contenido es inválido no modifica los datos originales del CSV.

La política personalizada fue validada mediante pruebas que comprueban:

- Error conocido bajo el límite: puede ser omitido.
- Error no configurado: no puede ser omitido.
- Límite de omisiones alcanzado: el error deja de ser tolerado.

### Retry para fallos transitorios

Los errores temporales utilizan una política de `retry` independiente del `skip`.

La configuración actual es:

```properties
batch.fault-tolerance.max-retries=2
batch.fault-tolerance.retry-delay-ms=200
```

La política considera recuperables:

- `TransientBankException`
- `SQLTransientException`
- `TransientDataAccessException`

En la prueba controlada de `dailyTransactionJob` se obtuvo:

```text
[RETRY DEMO] Fallo transitorio simulado. El chunk debe reintentarse.
[RETRY DEMO] Reintento exitoso. El procesamiento continua.
```

El Job terminó `COMPLETED` y mantuvo:

```text
Total recibidas: 1000
Validas persistidas: 401
Invalidas omitidas: 599
```

Por lo tanto:

```text
Dato inválido     -> SKIP
Fallo transitorio -> RETRY
```

### Restart y checkpoint

También se validó la recuperación de una ejecución fallida.

Se provocó un fallo controlado en la transacción 600, correspondiente a `partition2`.

Primera ejecución:

```text
partition0 -> COMPLETED
partition1 -> COMPLETED
partition2 -> FAILED
partition3 -> COMPLETED

dailyTransactionJob -> FAILED
```

Luego se ejecutó nuevamente la misma JobInstance utilizando el mismo `run.id` y desactivando el fallo controlado.

Spring Batch:

- no volvió a ejecutar `dailyTransactionCleanupStep`;
- no volvió a ejecutar las particiones 0, 1 y 3;
- reanudó solamente `partition2`;
- continuó desde el último checkpoint confirmado.

En la reejecución se procesaron solamente:

```text
Registros leídos:      175
Registros persistidos: 64
Registros inválidos:   111
```

La segunda ejecución terminó `COMPLETED` y `DAILY_TRANSACTION` quedó con 401 filas.

Esto demuestra que una partición fallida puede recuperarse sin volver a procesar todo el batch.

## Escalamiento y procesamiento paralelo

En Semana 3 los tres procesos principales utilizan **partitioning**.

La configuración normal es:

```properties
batch.scaling.grid-size=4
batch.scaling.threads=4
batch.scaling.chunk-size=25
batch.input.total-items=1000
```

El procesamiento utiliza un `ThreadPoolTaskExecutor` compartido, cuyo número de hilos se obtiene desde `batch.scaling.threads`.

### Daily y Annual

`dailyTransactionJob` y `annualAccountJob` utilizan `CsvRangePartitioner`.

Con cuatro particiones y 1000 registros:

```text
partition0 -> registros 1-250
partition1 -> registros 251-500
partition2 -> registros 501-750
partition3 -> registros 751-1000
```

Cada partición ejecuta su propio Worker Step.

### Monthly

`monthlyInterestJob` utiliza `AccountRangePartitioner`.

Los registros se distribuyen por `CUENTA_ID` para mantener los registros de una misma cuenta dentro de una única partición.

Con cuatro particiones:

```text
partition0 -> cuentas 101-113
partition1 -> cuentas 114-126
partition2 -> cuentas 127-138
partition3 -> cuentas 139-150
```

Esta estrategia evita condiciones de carrera al actualizar una misma clave primaria.

### Comparación de configuraciones

Se compararon tres configuraciones utilizando `dailyTransactionJob`:

| Configuración | Particiones | Hilos | Chunk | Tiempo Step | Tiempo Job |
|---|---:|---:|---:|---:|---:|
| A | 2 | 2 | 50 | 3.118 s | 3.950 s |
| B | 4 | 4 | 25 | 3.157 s | 3.973 s |
| C | 8 | 4 | 25 | 3.861 s | 4.676 s |

Las tres ejecuciones conservaron:

```text
Total recibidas: 1000
Validas persistidas: 401
Invalidas omitidas: 599
Estado: COMPLETED
```

La configuración A obtuvo el menor tiempo en la medición final, aunque A y B presentaron tiempos muy similares. La diferencia entre ambas fue mínima para este volumen de datos.

La configuración C muestra que aumentar la cantidad de particiones no garantiza un mejor rendimiento, ya que también aumenta el costo de coordinación.

Se mantiene B como configuración normal por ofrecer mayor paralelismo con un tiempo prácticamente equivalente a A en estas pruebas:

```properties
batch.scaling.grid-size=4
batch.scaling.threads=4
batch.scaling.chunk-size=25
```

---

## Semana 4 - Backend for Frontend

Para la Semana 4 se implementó el patrón **Backend for Frontend (BFF)** con un backend independiente para cada tipo de cliente del Banco XYZ.

La estrategia elegida separa las APIs según las necesidades de cada frontend:

```text
Frontend Web    -> BFF Web    -> Oracle Database
Frontend Mobile -> BFF Mobile -> Oracle Database
Cajero ATM      -> BFF ATM    -> Oracle Database
```

Los tres BFF se encuentran dentro de `bff/` y corresponden a aplicaciones Spring Boot independientes:

```text
bff/
├── web/
├── mobile/
└── atm/
```

Cada aplicación posee su propio `pom.xml`, configuración, capa de acceso a datos, servicios, controladores y seguridad.

### BFF Web

El BFF Web está orientado a interfaces de escritorio que pueden consumir información más completa.

Endpoints principales:

```text
GET /api/web/accounts/{cuentaId}
GET /api/web/accounts/{cuentaId}/movements
```

El detalle de cuenta expone:

- identificador de cuenta;
- nombre;
- saldo inicial;
- edad;
- tipo de cuenta;
- interés;
- saldo final.

El historial de movimientos entrega la información completa disponible en `ANNUAL_ACCOUNT_ENTRY`.

### BFF Mobile

El BFF Mobile reduce el volumen de información transferida para adaptarse a dispositivos móviles.

Endpoints principales:

```text
GET /api/mobile/accounts/{cuentaId}/summary
GET /api/mobile/accounts/{cuentaId}/movements
```

El resumen de cuenta contiene solamente:

- identificador de cuenta;
- tipo;
- saldo.

Además, el historial se limita a los últimos **5 movimientos** y cada movimiento incluye únicamente:

- fecha;
- transacción;
- monto.

De esta forma, el frontend móvil recibe un payload menor que el frontend Web.

### BFF ATM

El BFF ATM está orientado exclusivamente a operaciones necesarias para un cajero automático.

Endpoints principales:

```text
GET  /api/atm/accounts/{cuentaId}/balance
POST /api/atm/accounts/{cuentaId}/withdrawals
```

La consulta de saldo retorna solamente:

- identificador de cuenta;
- saldo disponible.

El retiro valida que:

- el monto sea mayor que cero;
- la cuenta exista;
- exista saldo suficiente.

La operación de retiro utiliza una transacción de base de datos. La cuenta se consulta mediante `SELECT ... FOR UPDATE`, se actualiza `MONTHLY_INTEREST.SALDO_FINAL` y se registra un movimiento `RETIRO_ATM` en `ANNUAL_ACCOUNT_ENTRY`.

El movimiento se almacena con monto negativo para representar el débito.

Si alguna operación falla, la transacción completa se revierte.

### Seguridad por canal

Cada BFF utiliza Spring Security con autenticación HTTP Basic y sesiones `STATELESS`.

Los accesos están separados por canal:

| BFF | Rol | Ruta protegida |
|---|---|---|
| Web | `ROLE_WEB` | `/api/web/**` |
| Mobile | `ROLE_MOBILE` | `/api/mobile/**` |
| ATM | `ROLE_ATM` | `/api/atm/**` |

Las credenciales no se almacenan en el repositorio. Se obtienen mediante variables de entorno:

```text
BFF_WEB_USERNAME
BFF_WEB_PASSWORD

BFF_MOBILE_USERNAME
BFF_MOBILE_PASSWORD

BFF_ATM_USERNAME
BFF_ATM_PASSWORD
```

Los tests de seguridad verifican solicitudes sin credenciales, credenciales incorrectas y acceso autorizado con las credenciales correspondientes al canal.

### Diferencias entre los BFF

| Característica | Web | Mobile | ATM |
|---|---|---|---|
| Detalle de cuenta | Completo | Reducido | Solo saldo |
| Campos principales | 7 | 3 | 2 |
| Movimientos | Historial completo | Últimos 5 | Solo registra retiros |
| Operaciones críticas | No | No | Retiro |
| Seguridad | `ROLE_WEB` | `ROLE_MOBILE` | `ROLE_ATM` |

Esta separación permite adaptar cantidad de datos, operaciones y controles de seguridad a las necesidades específicas de cada frontend.

### Consideraciones del patrón BFF

El patrón BFF permite reducir lógica específica del canal en los clientes, disminuir información innecesaria y evolucionar las APIs de Web, Mobile y ATM de manera independiente.

No obstante, introduce más aplicaciones que mantener y puede generar duplicación si la lógica de negocio se implementa directamente en varios BFF. Por esta razón, resulta especialmente útil cuando los frontends poseen necesidades claramente diferentes.

En comparación con Web, un cliente Mobile suele beneficiarse de respuestas más pequeñas por sus restricciones de ancho de banda, latencia, tamaño de pantalla y recursos del dispositivo. El cliente Web puede recibir información más completa para interfaces con mayor cantidad de datos y funcionalidades.

La personalización por canal también mejora el rendimiento y la experiencia de usuario al evitar transferir información innecesaria y reducir el procesamiento requerido por cada frontend. En este proyecto esto se observa directamente en la diferencia entre el detalle completo de Web, el resumen limitado de Mobile y la respuesta mínima del ATM.

### Evidencias Semana 4

Las evidencias de ejecución, personalización por canal, estructura de los tres BFF y resultados de las pruebas se encuentran en:

[`evidencias/semana_4/`](evidencias/semana_4/README.md)

## Pruebas
El proyecto contiene pruebas para los tres `ItemProcessor`, la política personalizada de tolerancia a fallos y la carga del contexto de Spring.

Actualmente existen:

- 2 pruebas para `DailyTransactionProcessor`.
- 2 pruebas para `MonthlyInterestProcessor`.
- 2 pruebas para `AnnualAccountProcessor`.
- 3 pruebas para `BankDataSkipPolicy`.
- 1 prueba de carga del contexto con `BankLegacyApplicationTests`.

Para ejecutar solamente las pruebas de los Processors:

```powershell
.\mvnw.cmd test "-Dtest=*ProcessorTest"
```

Para ejecutar toda la suite:

```powershell
.\mvnw.cmd test
```

La suite principal del proyecto Batch finaliza actualmente con:

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```
---


### Validación global Semana 4

Además de los 10 tests existentes del procesamiento Batch, los tres BFF poseen pruebas de integración para acceso a datos, endpoints y seguridad.

Resultados finales:

| Módulo | Tests | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| Batch S1-S3 | 10 | 0 | 0 | 0 |
| BFF Web | 9 | 0 | 0 | 0 |
| BFF Mobile | 9 | 0 | 0 | 0 |
| BFF ATM | 10 | 0 | 0 | 0 |
| **Total** | **38** | **0** | **0** | **0** |

La validación global confirma que la incorporación de los BFF no rompe las funcionalidades Batch desarrolladas durante las semanas anteriores.

## Estado actual
Los tres Jobs principales se encuentran operativos:

| Job | Estado |
|---|---|
| `dailyTransactionJob` | `COMPLETED` |
| `monthlyInterestJob` | `COMPLETED` |
| `annualAccountJob` | `COMPLETED` |


### Estado de los BFF

| Backend | Puerto | Estado |
|---|---:|---|
| BFF Web | 8081 | Operativo |
| BFF Mobile | 8082 | Operativo |
| BFF ATM | 8083 | Operativo |

Los tres backends utilizan la misma base Oracle, pero exponen APIs, DTOs y reglas específicas para su respectivo frontend.

Resultados validados con los datos de Semana 3:

| Job | Leídos | Persistidos | Omitidos | Resultado final DB |
|---|---:|---:|---:|---:|
| Daily | 1000 | 401 | 599 | 401 transacciones |
| Monthly | 1000 | 263 | 737 | 50 cuentas |
| Annual | 1000 | 732 | 268 | 732 movimientos |

La aplicación actualmente lee archivos CSV legacy, valida y transforma sus registros, maneja datos inválidos mediante `skip`, reintenta fallos temporales mediante `retry`, procesa datos en paralelo mediante partitioning, recupera ejecuciones mediante restart/checkpoint, persiste los resultados en Oracle Database y genera los reportes correspondientes.

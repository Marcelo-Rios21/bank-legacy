# Bank Legacy

Proyecto desarrollado con **Spring Boot** para procesar información legacy del Banco XYZ mediante Spring Batch, Backend for Frontend y microservicios con Spring Cloud.

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
- Spring Security
- Spring Cloud Config
- Netflix Eureka
- Spring Cloud Circuit Breaker
- Resilience4j
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
Además del módulo Batch raíz, el repositorio contiene `bff/` para los backends Web, Mobile y ATM, `cloud/` para Config Server y Eureka, y `microservices/` para `account-service`, `transaction-service` y `movement-service`.

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

### Servicios distribuidos

La arquitectura distribuida se inicia en este orden:

```text
Config Server       -> 8888
Eureka Server       -> 8761
account-service     -> 8091
transaction-service -> 8092
movement-service    -> 8093
```

Los tres microservicios obtienen su configuración desde Config Server y se registran en Eureka al iniciar.

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

### Circuit Breaker en microservicios

`account-service`, `transaction-service` y `movement-service` utilizan Spring Cloud Circuit Breaker con Resilience4j para proteger el acceso a Oracle.

Cada servicio posee una instancia independiente (`accountDatabase`, `transactionDatabase` y `movementDatabase`). Ante fallos del acceso a datos, el servicio responde con `503 Service Unavailable` y el Circuit Breaker puede abrirse para evitar nuevos intentos mientras persiste el problema.

La configuración de estas instancias se mantiene centralizada en `cloud/config-repo/`.

## Escalamiento y procesamiento paralelo

Los tres procesos principales utilizan **partitioning**.

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

## Backend for Frontend (BFF)

El sistema implementa el patrón **Backend for Frontend (BFF)** mediante un backend independiente para cada tipo de cliente del Banco XYZ.

La estrategia separa contratos, datos expuestos, operaciones y controles de seguridad según las necesidades de cada frontend:

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

Cada aplicación posee su propio `pom.xml`, configuración, acceso a datos, servicios, controladores, DTOs y seguridad. Esta separación permite que cada canal evolucione de manera independiente sin obligar a los demás clientes a consumir contratos que no necesitan.

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

El historial se limita a los últimos **5 movimientos** y cada movimiento incluye únicamente:

- fecha;
- transacción;
- monto.

De esta forma, el cliente móvil evita recibir información que no necesita y reduce el tamaño de las respuestas respecto del BFF Web.

### BFF ATM

El BFF ATM está orientado exclusivamente a las operaciones necesarias para un cajero automático.

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

El movimiento se almacena con monto negativo para representar el débito. Si alguna operación falla, la transacción completa se revierte.

### Seguridad por canal

Cada BFF utiliza **Spring Security**, autenticación mediante **JWT Bearer** y sesiones `STATELESS`.

Cada canal posee un endpoint propio para obtener un token:

```text
POST /api/web/auth/token
POST /api/mobile/auth/token
POST /api/atm/auth/token
```

Las credenciales se validan en el BFF correspondiente. Cuando son correctas se genera un JWT firmado con **HS256**, con una vigencia de **900 segundos (15 minutos)**.

Los accesos están separados mediante roles:

| BFF | Rol requerido | Ruta protegida |
|---|---|---|
| Web | `ROLE_WEB` | `/api/web/**` |
| Mobile | `ROLE_MOBILE` | `/api/mobile/**` |
| ATM | `ROLE_ATM` | `/api/atm/**` |

Cada BFF utiliza además una clave de firma independiente:

```text
BFF_WEB_JWT_SECRET
BFF_MOBILE_JWT_SECRET
BFF_ATM_JWT_SECRET
```

Las credenciales utilizadas para solicitar los tokens tampoco se almacenan en el repositorio:

```text
BFF_WEB_USERNAME
BFF_WEB_PASSWORD

BFF_MOBILE_USERNAME
BFF_MOBILE_PASSWORD

BFF_ATM_USERNAME
BFF_ATM_PASSWORD
```

La separación de secretos impide que un token firmado por un canal sea aceptado por otro. Además, un token correctamente firmado pero sin el rol requerido recibe una respuesta `403 Forbidden`.

Las pruebas de seguridad validan los siguientes escenarios:

- solicitud sin token: `401 Unauthorized`;
- token inválido: `401 Unauthorized`;
- token válido con rol incorrecto: `403 Forbidden`;
- token válido con el rol del canal: acceso autorizado;
- token firmado con una clave de otro canal: `401 Unauthorized`.

Actualmente existen **13 pruebas específicas de seguridad** distribuidas entre Web, Mobile y ATM.

#### Seguridad de microservicios

`account-service`, `transaction-service` y `movement-service` utilizan Spring Security con JWT Bearer y sesiones `STATELESS`. Cada servicio posee credenciales, clave de firma y rol independientes:

- `account-service`: `ROLE_ACCOUNT`
- `transaction-service`: `ROLE_TRANSACTION`
- `movement-service`: `ROLE_MOVEMENT`

La autorización fue validada en los tres servicios: una solicitud sin token responde `401 Unauthorized`, un token válido con rol incorrecto responde `403 Forbidden` y un token válido con el rol correspondiente permite el acceso con `200 OK`.

### HTTPS

Los tres BFF pueden ejecutarse mediante HTTPS utilizando el perfil Spring `https`.

| BFF | Puerto HTTP | Puerto HTTPS |
|---|---:|---:|
| Web | 8081 | 8441 |
| Mobile | 8082 | 8442 |
| ATM | 8083 | 8443 |

El perfil HTTPS utiliza un `KeyStore` PKCS12 configurado mediante variables de entorno:

```text
BFF_TLS_KEYSTORE_PATH
BFF_TLS_KEYSTORE_PASSWORD
```

Para desarrollo local se utiliza un certificado autofirmado para `localhost`. El archivo del certificado local no se versiona en Git y su procedimiento de generación se encuentra documentado en `certs/README.md`.

En un ambiente productivo el certificado autofirmado debe reemplazarse por un certificado emitido por una autoridad certificadora confiable.

### Diferencias entre los BFF

| Característica | Web | Mobile | ATM |
|---|---|---|---|
| Detalle de cuenta | Completo | Reducido | Solo saldo |
| Campos principales | 7 | 3 | 2 |
| Movimientos | Historial completo | Últimos 5 | Registra retiros |
| Operación crítica | No | No | Retiro |
| Rol | `ROLE_WEB` | `ROLE_MOBILE` | `ROLE_ATM` |
| Autenticación | JWT Bearer | JWT Bearer | JWT Bearer |
| Clave JWT | Independiente | Independiente | Independiente |

Esta separación adapta tanto el volumen de información como las operaciones y los controles de seguridad a las necesidades específicas de cada frontend.

### Optimización de respuestas

La personalización de los contratos fue validada mediante mediciones locales sobre la cuenta `101`. Cada endpoint tuvo una solicitud inicial de calentamiento y posteriormente **5 mediciones**.

Para la consulta principal de cuenta se obtuvieron los siguientes resultados:

| Canal | Tamaño | Tiempo promedio |
|---|---:|---:|
| Web | 122 bytes | 40,19 ms |
| Mobile | 47 bytes | 39,66 ms |
| ATM | 39 bytes | 40,52 ms |

Respecto de Web, el resumen Mobile reduce el tamaño de la respuesta aproximadamente un **61,5 %**, mientras que la consulta de saldo ATM lo reduce aproximadamente un **68,0 %**.

La diferencia es aún mayor en el historial de movimientos:

| Canal | Tamaño | Movimientos | Tiempo promedio |
|---|---:|---:|---:|
| Web | 4049 bytes | 32 | 60,22 ms |
| Mobile | 302 bytes | 5 | 38,58 ms |

En este caso Mobile reduce aproximadamente un **92,5 %** el payload al entregar solo los últimos cinco movimientos y los campos requeridos por el cliente.

Los tiempos obtenidos corresponden a ejecuciones locales y pueden variar entre mediciones, por lo que no se consideran un benchmark de producción ni un SLA. La evidencia principal de optimización es la reducción del volumen de datos transferidos según las necesidades de cada canal.

### Consideraciones del patrón BFF

El patrón BFF permite reducir lógica específica del canal en los clientes, disminuir información innecesaria y evolucionar las APIs de Web, Mobile y ATM de manera independiente.

No obstante, introduce más aplicaciones que mantener y puede producir duplicación si la misma lógica de negocio comienza a implementarse en múltiples BFF. Por esta razón resulta especialmente útil cuando los frontends poseen necesidades claramente diferentes.

Un **API Gateway** y un BFF cumplen responsabilidades distintas y pueden coexistir. El Gateway puede centralizar aspectos transversales de entrada, mientras que los BFF mantienen contratos y comportamiento específicos para cada cliente.

Como evolución futura, si aumenta la lógica compartida de acceso al sistema legacy, podría incorporarse una capa o adaptador común de acceso a datos manteniendo separados los contratos y reglas particulares de Web, Mobile y ATM.

### Reflexiones de diseño

**¿Por qué es útil dividir un sistema en microservicios?**

Permite separar responsabilidades en servicios independientes, de modo que cada componente pueda evolucionar, desplegarse y mantenerse sin concentrar toda la lógica del sistema en una sola aplicación.

**¿Cómo puede un sistema mantenerse funcionando incluso cuando un servicio falla?**

Mediante mecanismos de tolerancia a fallos que eviten propagar el problema al resto del sistema. En este proyecto se utiliza Circuit Breaker para detectar fallos repetidos y evitar nuevos intentos mientras la dependencia continúa indisponible.

**¿Qué aporta Spring Cloud a la construcción de microservicios?**

Aporta componentes para resolver necesidades comunes de una arquitectura distribuida. En este proyecto se utiliza Spring Cloud Config para centralizar configuración, Eureka para descubrimiento de servicios y Spring Cloud Circuit Breaker para integrar tolerancia a fallos.

**¿Por qué es importante contar con mecanismos de seguridad en una arquitectura distribuida?**

Porque existen múltiples servicios y endpoints que deben controlar quién puede acceder a sus recursos. La autenticación mediante JWT y la autorización por roles permiten proteger cada API sin almacenar sesiones en el servidor.

**¿Cómo ayuda un Circuit Breaker a evitar que un fallo se propague dentro del sistema?**

Cuando detecta una cantidad suficiente de fallos, abre el circuito y evita seguir ejecutando temporalmente la operación que está fallando. Esto reduce llamadas innecesarias a una dependencia con problemas y permite responder de forma controlada mientras se recupera.

### Evidencias

Las evidencias se mantienen organizadas por entrega:

- [`evidencias/semana_4/`](evidencias/semana_4/README.md): implementación inicial de los tres BFF.
- [`evidencias/semana_5/`](evidencias/semana_5/README.md): HTTPS, JWT, autorización por canal, optimización de respuestas y validación global.
- [`evidencias/semana_6/`](evidencias/semana_6/README.md): Config Server, Eureka, Circuit Breaker y seguridad de los microservicios.

## Pruebas

El proyecto contiene pruebas para el procesamiento Batch y pruebas de integración para los tres BFF.

En el módulo Batch existen:

- 2 pruebas para `DailyTransactionProcessor`;
- 2 pruebas para `MonthlyInterestProcessor`;
- 2 pruebas para `AnnualAccountProcessor`;
- 3 pruebas para `BankDataSkipPolicy`;
- 1 prueba de carga del contexto con `BankLegacyApplicationTests`.

Para ejecutar solamente las pruebas de los Processors:

```powershell
.\mvnw.cmd test "-Dtest=*ProcessorTest"
```

Para ejecutar toda la suite Batch:

```powershell
.\mvnw.cmd test
```

La suite Batch contiene actualmente:

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### Validación global

La validación completa ejecuta las pruebas de Batch, BFF Web, BFF Mobile y BFF ATM.

Resultados actuales:

| Módulo | Tests | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| Batch | 10 | 0 | 0 | 0 |
| BFF Web | 10 | 0 | 0 | 0 |
| BFF Mobile | 11 | 0 | 0 | 0 |
| BFF ATM | 11 | 0 | 0 | 0 |
| **Total** | **42** | **0** | **0** | **0** |

La validación global confirma que la evolución de seguridad y configuración de los BFF no rompe las funcionalidades Batch ni los endpoints desarrollados anteriormente.

## Estado actual

Los tres Jobs principales se encuentran operativos:

| Job | Estado |
|---|---|
| `dailyTransactionJob` | `COMPLETED` |
| `monthlyInterestJob` | `COMPLETED` |
| `annualAccountJob` | `COMPLETED` |

### Estado de los BFF

| Backend | HTTP | HTTPS con perfil `https` | Estado |
|---|---:|---:|---|
| BFF Web | 8081 | 8441 | Operativo |
| BFF Mobile | 8082 | 8442 | Operativo |
| BFF ATM | 8083 | 8443 | Operativo |

Los tres backends utilizan la misma base Oracle, pero exponen APIs, DTOs, reglas y controles de seguridad específicos para su respectivo frontend.

La arquitectura distribuida utiliza además Spring Cloud Config en el puerto `8888` y Eureka Server en el puerto `8761`. Los microservicios consumen su configuración desde Config Server y se registran en Eureka.

`account-service` (`8091`), `transaction-service` (`8092`) y `movement-service` (`8093`) exponen APIs sobre los datos migrados de cuentas, transacciones y movimientos, respectivamente.

Resultados actuales del procesamiento Batch con los datos utilizados para validación:

| Job | Leídos | Persistidos | Omitidos | Resultado final DB |
|---|---:|---:|---:|---:|
| Daily | 1000 | 401 | 599 | 401 transacciones |
| Monthly | 1000 | 263 | 737 | 50 cuentas |
| Annual | 1000 | 732 | 268 | 732 movimientos |

La aplicación actualmente lee archivos CSV legacy, valida y transforma sus registros, maneja datos inválidos mediante `skip`, reintenta fallos temporales mediante `retry`, procesa datos en paralelo mediante partitioning, recupera ejecuciones mediante restart/checkpoint, persiste los resultados en Oracle Database y genera los reportes correspondientes.

En paralelo, los BFF Web, Mobile y ATM proporcionan interfaces específicas por cliente, autenticación y autorización mediante JWT, comunicación HTTPS configurable y respuestas optimizadas según las necesidades de cada canal.

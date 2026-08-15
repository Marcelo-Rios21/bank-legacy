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
data/semana_1/
├── transacciones.csv
├── intereses.csv
└── cuentas_anuales.csv
```

## Estructura del proyecto

```text
src/
├── main/
│   ├── java/com/bank/bank_legacy/
│   │   ├── exception/
│   │   ├── job/
│   │   ├── model/
│   │   ├── processor/
│   │   └── BankLegacyApplication.java
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/
    └── java/com/bank/bank_legacy/
        ├── BankLegacyApplicationTests.java
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
data/semana_1/transacciones.csv
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

La ejecución actual con los datos de semana 1 obtiene:

```text
Registros leídos:      10
Registros persistidos: 8
Registros inválidos:   2
```

El Job también genera un resumen con la cantidad de registros recibidos, registros válidos, registros omitidos y posibles duplicados.

Resultado actual:

```text
===== RESUMEN TRANSACCIONES DIARIAS =====
Total recibidas: 10
Validas persistidas: 8
Invalidas omitidas: 2
Posibles duplicados: 1 grupo(s), 2 registro(s)
==========================================
```

---

### 2. Intereses mensuales

El Job `monthlyInterestJob` procesa:

```text
data/semana_1/intereses.csv
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
Registros leídos:      8
Registros persistidos: 7
Registros inválidos:   1
```

---

### 3. Estados de cuenta anuales

El Job `annualAccountJob` procesa:

```text
data/semana_1/cuentas_anuales.csv
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
Registros leídos:      9
Registros persistidos: 9
Registros inválidos:   0
Cuentas distintas:     8
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
Periodo: 2024-01-01 a 2024-12-31

Registros leidos: 9
Registros persistidos: 9
Registros invalidos omitidos: 0
Cuentas distintas: 8

Depositos: 7
Retiros: 1
Compras: 1
Pagos: 0

Movimientos con monto cero: 1
Movimientos con monto negativo: 2
Balance neto de movimientos: 11400
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

## Manejo de errores

Los tres Jobs utilizan procesamiento tolerante a fallos mediante:

```java
.faultTolerant()
.skip(ExcepcionDelProceso.class)
.skipLimit(10)
```

Cada proceso utiliza su propia excepción:

- `InvalidTransactionException`
- `InvalidInterestException`
- `InvalidAnnualAccountException`

Los errores de validación utilizan `skip` porque volver a intentar procesar un registro cuyo contenido es inválido no modifica los datos originales del CSV.

---

## Pruebas

El proyecto contiene pruebas unitarias para los tres `ItemProcessor`.

Actualmente existen pruebas para:

- Procesamiento correcto de una transacción.
- Rechazo de un monto inválido.
- Cálculo correcto del interés mensual.
- Rechazo de un tipo de cuenta inválido.
- Aceptación de montos negativos en movimientos anuales.
- Rechazo de una descripción vacía.

Para ejecutar solamente las pruebas de los Processors:

```powershell
.\mvnw.cmd test "-Dtest=*ProcessorTest"
```

Actualmente las seis pruebas finalizan correctamente:

```text
Tests run: 6
Failures: 0
Errors: 0
```

La suite completa se ejecuta mediante:

```powershell
.\mvnw.cmd test
```

Actualmente la suite completa finaliza con:

```text
BUILD SUCCESS
```

---

## Estado actual

Los tres Jobs principales se encuentran operativos:

| Job | Estado |
|---|---|
| `dailyTransactionJob` | `COMPLETED` |
| `monthlyInterestJob` | `COMPLETED` |
| `annualAccountJob` | `COMPLETED` |

La aplicación actualmente lee los archivos CSV legacy, valida y transforma sus registros, maneja datos inválidos, persiste los resultados en Oracle Database y genera los reportes correspondientes.
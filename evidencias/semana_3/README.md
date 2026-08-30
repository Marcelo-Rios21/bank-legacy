# Evidencias Semana 3

## Procesamiento por Job

### Daily Transaction
- `01_particionamiento_daily.png`
- Entrada: `data/semana_3/transacciones.csv`
- Leídos: 1000
- Persistidos: 401
- Omitidos: 599
- Resultado Oracle: 401 filas

### Monthly Interest
- `02_particionamiento_monthly.png`
- Entrada: `data/semana_3/intereses.csv`
- Leídos: 1000
- Persistidos: 263
- Omitidos: 737
- Resultado Oracle: 50 cuentas

### Annual Account
- `03_particionamiento_annual.png`
- Entrada: `data/semana_3/cuentas_anuales.csv`
- Leídos: 1000
- Persistidos: 732
- Omitidos: 268
- Resultado Oracle: 732 movimientos
- Auditoría generada en `output/auditoria_anual.txt`

## Persistencia

- `04_persistencia_oracle.png`
- Daily: 401 filas
- Monthly: 50 cuentas
- Annual: 732 movimientos

## Retry

- `05_retry.png`
- Se simula un fallo transitorio.
- Spring Batch realiza retry.
- El reintento finaliza correctamente.
- Job final: `COMPLETED`.

## Restart y checkpoint

### Primera ejecución
- `06_restart_failed.png`
- Falla controlada en la transacción 600.
- `partition2` termina `FAILED`.
- Job termina `FAILED`.

### Reejecución
- `07_restart_completed.png`
- Se utiliza el mismo `run.id`.
- Las particiones previamente completadas no se reprocesan.
- Solo se reanuda `partition2`.
- Se procesan 175 registros pendientes.
- Job final: `COMPLETED`.

## Comparación de escalamiento

- `08_rendimiento_y_tests.png`

| Configuración | Particiones | Hilos | Chunk | Step | Job |
|---|---:|---:|---:|---:|---:|
| A | 2 | 2 | 50 | 3.118 s | 3.950 s |
| B | 4 | 4 | 25 | 3.157 s | 3.973 s |
| C | 8 | 4 | 25 | 3.861 s | 4.676 s |

La configuración A obtuvo el menor tiempo en la medición final, aunque A y B presentaron tiempos muy similares.

Suite final:

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```
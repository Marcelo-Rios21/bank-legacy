# Evidencias - Microservicios y seguridad en la nube

Esta carpeta contiene las evidencias de la configuración y validación de la arquitectura distribuida con Spring Cloud.

## Evidencias

| Archivo | Evidencia |
|---|---|
| `config_server.png` | Config Server entregando la configuración centralizada de `account-service`, incluyendo puerto, Eureka y Circuit Breaker. |
| `eureka_micro.png` | Eureka Server mostrando `ACCOUNT-SERVICE`, `TRANSACTION-SERVICE` y `MOVEMENT-SERVICE` registrados y en estado `UP`. |
| `circuit_breaker.png` | Prueba controlada de `account-service`: después de los primeros fallos hacia Oracle, el Circuit Breaker abre el circuito y las siguientes respuestas `503` se producen en pocos milisegundos. |
| `seguridad_micro.png` | Validación de autenticación y autorización en los tres microservicios: `401` sin token, `403` con rol incorrecto y `200` con token y rol correctos. |

## Seguridad de las evidencias

Las capturas no incluyen contraseñas, secretos JWT ni tokens completos.
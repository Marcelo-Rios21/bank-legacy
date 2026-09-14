# Evidencias - Backend for Frontend

Esta carpeta contiene las evidencias de la implementación y validación del estado actual de los Backend for Frontend Web, Mobile y ATM.

## Evidencias

| Archivo | Evidencia |
|---|---|
| `https_bff.png` | Los tres BFF ejecutándose mediante HTTPS en los puertos 8441, 8442 y 8443. |
| `jwt_web.png` | Generación de un JWT Bearer con vigencia de 900 segundos y acceso exitoso a un endpoint protegido. |
| `seguridad_canales.png` | Ejecución de 13 pruebas específicas de autenticación, autorización y aislamiento entre canales, sin fallos ni errores. |
| `optimizacion_respuestas.png` | Comparación del tamaño y tiempo de respuesta entre Web, Mobile y ATM para la cuenta 101. |
| `optimizacion_movimientos.png` | Comparación del historial de movimientos: Web entrega 32 movimientos y 4049 bytes, mientras Mobile entrega 5 movimientos y 302 bytes. |
| `test_globales.png` | Validación global del proyecto con 42 pruebas, 0 fallos, 0 errores y 0 pruebas omitidas. |

## Seguridad de las evidencias

Las capturas no incluyen contraseñas, secretos JWT, contraseña del KeyStore ni tokens completos.

El certificado utilizado para las pruebas HTTPS es un certificado autofirmado de desarrollo local y no se almacena en el repositorio.
# Evidencias de OAuth2, Docker Compose, Kafka y Resilience4j

Estas evidencias validan la ejecución integrada de los microservicios con seguridad OAuth 2.0, contenerización mediante Docker, orquestación con Docker Compose, mensajería asíncrona con Apache Kafka y tolerancia a fallos mediante Resilience4j.

## 01 - Docker y Docker Compose

Archivo: `01-docker-compose.txt`

Demuestra:

- imágenes Docker de los seis servicios Spring;
- imagen oficial de Apache Kafka 4.1.2;
- siete contenedores ejecutándose mediante Docker Compose;
- Kafka en estado `healthy`;
- tópico `bank.transactions` con tres particiones;
- validación correcta de `docker-compose.yaml`.

## 02 - OAuth2, Oracle y Kafka

Archivo: `02-oauth2-kafka.txt`

Demuestra:

- obtención de token OAuth 2.0 mediante Client Credentials;
- token Bearer con scopes;
- `401 Unauthorized` sin token;
- `403 Forbidden` con scope incorrecto;
- acceso HTTP 200 a los tres microservicios;
- conexión con Oracle ADB;
- publicación en Kafka mediante HTTP 202;
- consumo del evento por `movement-service`.

## 03 - Resilience4j

Archivo: `03-resilience4j.txt`

La prueba controlada de `account-service` produjo:

```text
Llamada 1 -> HTTP 503 - 3379 ms
Llamada 2 -> HTTP 503 - 2142 ms
Llamada 3 -> HTTP 503 - 8 ms
Llamada 4 -> HTTP 503 - 6 ms
```

Las primeras solicitudes intentan acceder a Oracle y fallan. Las siguientes son rechazadas casi inmediatamente después de abrirse el Circuit Breaker.

Después de restaurar la conexión:

```text
account-service -> HTTP 200
```

Esto valida tanto la apertura del Circuit Breaker como la recuperación del servicio una vez restablecida la dependencia.

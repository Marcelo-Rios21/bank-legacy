# Despliegue - Bank Legacy

## 1. Objetivo

Este documento describe el despliegue de la arquitectura distribuida de Bank Legacy.

Se distinguen dos escenarios:

1. ejecución local validada mediante Docker Compose;
2. arquitectura objetivo para despliegue y escalamiento horizontal en AWS.

La ejecución con Docker Compose fue comprobada durante el desarrollo.

La sección AWS representa una propuesta de despliegue cloud basada en la arquitectura implementada. No implica que exista actualmente una infraestructura AWS activa.

## 2. Componentes desplegables

La arquitectura distribuida contiene los siguientes componentes:

```text
Config Server
Eureka Server
Authorization Server

account-service
transaction-service
movement-service

Apache Kafka
Oracle Database
```

Oracle Database se mantiene como servicio externo.

Los componentes Spring disponen de Dockerfile en:

```text
cloud/config-server/
cloud/discovery-server/
cloud/authorization-server/

microservices/account-service/
microservices/transaction-service/
microservices/movement-service/
```

Kafka utiliza:

```text
apache/kafka:4.1.2
```

## 3. Arquitectura implementada

La ejecución integrada validada utiliza la siguiente estructura:

```text
                         +----------------------+
                         | Authorization Server |
                         |        :9000         |
                         +----------+-----------+
                                    |
                                  JWT
                                    |
             +----------------------+----------------------+
             |                      |                      |
             v                      v                      v
     +---------------+      +------------------+    +----------------+
     | account       |      | transaction      |    | movement       |
     | service :8091 |      | service :8092    |    | service :8093  |
     +-------+-------+      +---------+--------+    +--------+-------+
             |                        |                      |
             |                        |                      |
             |                        v                      |
             |                +---------------+              |
             |                | Apache Kafka  |--------------+
             |                | bank.         |
             |                | transactions  |
             |                +---------------+
             |
             +-----------------------+----------------------+
                                     |
                                     v
                              +-------------+
                              | Oracle      |
                              | Database    |
                              +-------------+

      +----------------+                +----------------+
      | Config Server  |                | Eureka Server  |
      |     :8888      |                |     :8761      |
      +----------------+                +----------------+
```

Spring Cloud Config centraliza la configuración de los microservicios.

Eureka permite el registro y descubrimiento de servicios.

El Authorization Server emite tokens OAuth 2.0 utilizados por los tres microservicios.

Kafka desacopla la publicación de transacciones de su consumo.

## 4. Docker Compose

La infraestructura integrada se encuentra definida en:

```text
docker-compose.yaml
```

Para validar su sintaxis:

```powershell
docker compose config
```

Para construir las imágenes:

```powershell
docker compose build
```

Para iniciar la arquitectura:

```powershell
docker compose up -d
```

Para comprobar el estado:

```powershell
docker compose ps
```

Para visualizar logs:

```powershell
docker compose logs --tail 100
```

Para detener los servicios:

```powershell
docker compose down
```

## 5. Servicios Docker

Docker Compose administra:

```text
kafka
config-server
discovery-server
authorization-server
account-service
transaction-service
movement-service
```

Los principales puertos utilizados son:

```text
Config Server          8888
Eureka Server          8761
Authorization Server   9000

account-service         8091
transaction-service     8092
movement-service        8093

Kafka                   9092
```

Dentro de la red Docker se utilizan nombres internos de servicio.

Ejemplos:

```text
config-server:8888
discovery-server:8761
authorization-server:9000
kafka
```

## 6. Configuración centralizada

Los tres microservicios consumen configuración desde Spring Cloud Config.

La configuración se encuentra en:

```text
cloud/config-repo/
```

Los servicios utilizan Config Server mediante la variable o configuración correspondiente a:

```text
CONFIG_SERVER_URL
```

En Docker se utiliza el nombre del servicio como dirección interna.

## 7. Service Discovery

Eureka Server se ejecuta en:

```text
8761
```

Los microservicios se registran como clientes Eureka.

La dirección interna utilizada en Docker corresponde a:

```text
http://discovery-server:8761/eureka/
```

El registro permite que la arquitectura mantenga información de las instancias disponibles.

## 8. Seguridad OAuth 2.0

El Authorization Server se ejecuta en:

```text
9000
```

La autenticación entre clientes y microservicios utiliza OAuth 2.0 con:

```text
Client Credentials
JWT
```

Scopes implementados:

```text
account.read
transaction.read
transaction.publish
movement.read
```

Los microservicios funcionan como Resource Servers.

Las pruebas realizadas comprobaron:

```text
401 Unauthorized
403 Forbidden
200 OK
202 Accepted
```

según la existencia del token y los scopes disponibles.

## 9. Kafka

Kafka permite comunicación asíncrona entre:

```text
transaction-service
        |
        v
bank.transactions
        |
        v
movement-service
```

Configuración validada:

```text
Tópico: bank.transactions
Particiones: 3
Grupo consumidor: movement-service-group
```

`transaction-service` funciona como productor.

`movement-service` funciona como consumidor.

La publicación se realiza mediante:

```text
POST /api/transactions/{id}/publish
```

y requiere:

```text
transaction.publish
```

La operación responde:

```text
HTTP 202 Accepted
```

cuando la publicación es aceptada correctamente.

## 10. Resilience4j

Los microservicios utilizan Spring Cloud Circuit Breaker con Resilience4j para proteger el acceso a Oracle.

Instancias configuradas:

```text
accountDatabase
transactionDatabase
movementDatabase
```

Configuración utilizada:

```text
Sliding window: 4 llamadas
Mínimo de llamadas: 2
Failure rate threshold: 50 %
Tiempo OPEN: 10 segundos
Llamadas HALF_OPEN: 1
```

El comportamiento esperado es:

```text
CLOSED
   |
   | fallos suficientes
   v
OPEN
   |
   | espera
   v
HALF_OPEN
   |
   | recuperación
   v
CLOSED
```

Esto evita continuar realizando llamadas repetidas a una dependencia que presenta fallos.

## 11. Datos sensibles

Los secretos no deben incluirse directamente en:

```text
Git
Dockerfile
README
logs
capturas de evidencia
```

Entre los valores sensibles se encuentran:

```text
credenciales Oracle
OAUTH2_CLIENT_ID
OAUTH2_CLIENT_SECRET
BFF_WEB_JWT_SECRET
BFF_MOBILE_JWT_SECRET
BFF_ATM_JWT_SECRET
```

Durante una ejecución local deben utilizarse variables de entorno.

En una plataforma cloud deben utilizarse mecanismos específicos para gestión de secretos.

## 12. Arquitectura objetivo en AWS

Para el despliegue cloud se propone utilizar contenedores independientes.

Arquitectura conceptual:

```text
                           Internet
                              |
                              v
                 +-------------------------+
                 | Application Load        |
                 | Balancer                |
                 +------------+------------+
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
       account-service  transaction-service  otros
         ECS/Fargate       ECS/Fargate
             |                |
             |                v
             |          +-------------+
             |          | Kafka / MSK |
             |          +------+------+
             |                 |
             |                 v
             |          movement-service
             |             ECS/Fargate
             |                 |
             +--------+--------+
                      |
                      v
                Oracle externo


Servicios internos:

Config Server
Eureka Server
Authorization Server
```

Servicios AWS considerados:

```text
Amazon ECR
Amazon ECS
AWS Fargate
Application Load Balancer
Application Auto Scaling
AWS Secrets Manager
Amazon CloudWatch
```

Amazon MSK puede utilizarse como alternativa administrada para Kafka.

## 13. Amazon ECR

Se propone almacenar una imagen por componente.

Repositorios:

```text
bank-legacy-config-server
bank-legacy-discovery-server
bank-legacy-authorization-server

bank-legacy-account-service
bank-legacy-transaction-service
bank-legacy-movement-service
```

Ejemplo de variables:

```powershell
$region = "<REGION_AWS>"
$accountId = "<AWS_ACCOUNT_ID>"
```

Inicio de sesión:

```powershell
aws ecr get-login-password `
    --region $region |
    docker login `
        --username AWS `
        --password-stdin "$accountId.dkr.ecr.$region.amazonaws.com"
```

Ejemplo para construir `account-service`:

```powershell
docker build `
    -t bank-legacy-account-service:latest `
    .\microservices\account-service
```

Etiquetar:

```powershell
docker tag `
    bank-legacy-account-service:latest `
    "$accountId.dkr.ecr.$region.amazonaws.com/bank-legacy-account-service:latest"
```

Publicar:

```powershell
docker push `
    "$accountId.dkr.ecr.$region.amazonaws.com/bank-legacy-account-service:latest"
```

El mismo procedimiento se puede aplicar a las demás imágenes.

## 14. Amazon ECS y Fargate

Se propone crear un cluster:

```text
bank-legacy-cluster
```

Cada componente debe utilizar una Task Definition independiente.

Servicios de negocio:

```text
account-service
transaction-service
movement-service
```

Servicios de infraestructura:

```text
config-server
discovery-server
authorization-server
```

Separar los servicios permite:

```text
desplegarlos independientemente
actualizarlos independientemente
escalarlos independientemente
aislar fallos
```

## 15. Escalamiento horizontal

Los tres microservicios de negocio pueden ejecutarse mediante varias tareas.

Configuración inicial propuesta:

```text
account-service
mínimo: 2
máximo: 4

transaction-service
mínimo: 2
máximo: 4

movement-service
mínimo: 2
máximo: 3
```

Para `movement-service`, el paralelismo efectivo del grupo Kafka depende de la cantidad de particiones.

Actualmente:

```text
bank.transactions = 3 particiones
```

Por lo tanto, un grupo consumidor puede utilizar hasta tres consumidores activos simultáneamente para esas particiones.

Para aumentar el paralelismo de consumo se debe evaluar también incrementar la cantidad de particiones del tópico.

## 16. Auto Scaling

Amazon ECS puede utilizar Application Auto Scaling.

Política conceptual:

```text
Métrica:
ECSServiceAverageCPUUtilization

Objetivo:
60 %

Mínimo:
2 tareas

Máximo:
4 tareas
```

Cada microservicio debe disponer de una política independiente.

Esto permite escalar únicamente el servicio sometido a mayor carga.

## 17. Balanceo de carga

Application Load Balancer puede distribuir las solicitudes entre las distintas tareas de un servicio ECS.

El balanceador debe exponer únicamente los servicios que requieran acceso externo.

Componentes como:

```text
Config Server
Eureka Server
Kafka
```

deben mantenerse dentro de la red privada de la arquitectura.

No se debe asumir la existencia de `/actuator/health` mientras Spring Boot Actuator no haya sido incorporado y configurado explícitamente en el proyecto.

## 18. Red AWS

La arquitectura objetivo debería utilizar una VPC distribuida en más de una zona de disponibilidad.

Estructura conceptual:

```text
Internet
   |
   v
ALB
   |
   v
Subredes privadas
   |
   +-- account-service
   +-- transaction-service
   +-- movement-service
   +-- Authorization Server
   +-- Config Server
   +-- Eureka
   +-- Kafka
```

Los Security Groups deben permitir únicamente el tráfico necesario entre componentes.

## 19. Gestión de secretos en AWS

Para producción se recomienda utilizar:

```text
AWS Secrets Manager
```

o:

```text
AWS Systems Manager Parameter Store
```

Las Task Definitions pueden recibir los secretos sin almacenarlos directamente en las imágenes.

Se deben proteger especialmente:

```text
credenciales Oracle
cliente OAuth 2.0
secretos de autenticación
```

## 20. Oracle Database

Oracle permanece como dependencia externa a los contenedores.

Los servicios deben disponer de:

```text
conectividad hacia Oracle
credenciales válidas
permisos de red
configuración JDBC correspondiente
```

Si Oracle restringe conexiones por origen, la infraestructura AWS deberá utilizar una salida de red autorizada por la configuración de Oracle.

## 21. Monitoreo

Las tareas ECS pueden enviar sus logs a Amazon CloudWatch Logs.

Separación recomendada:

```text
/aws/ecs/bank-legacy-account
/aws/ecs/bank-legacy-transaction
/aws/ecs/bank-legacy-movement
/aws/ecs/bank-legacy-auth
/aws/ecs/bank-legacy-config
/aws/ecs/bank-legacy-discovery
```

También se pueden observar métricas como:

```text
CPU
memoria
cantidad de tareas
errores del Load Balancer
reinicios
```

## 22. Orden propuesto de despliegue

Para respetar las dependencias de la arquitectura:

```text
1. Config Server
2. Eureka Server
3. Authorization Server
4. Kafka
5. account-service
6. transaction-service
7. movement-service
8. Load Balancer
9. Auto Scaling
10. Validaciones funcionales
```

Cada componente debe comprobarse antes de avanzar al siguiente.

## 23. Validación posterior

Una vez desplegada la arquitectura se deben comprobar:

```text
servicios en ejecución
registro en Eureka
lectura desde Config Server
conexión con Oracle
emisión de token OAuth 2.0
validación JWT
401 sin credenciales
403 sin permisos
200 con permisos correctos
publicación Kafka con HTTP 202
consumo por movement-service
funcionamiento de Resilience4j
```

## 24. Rollback

Las imágenes utilizadas en un despliegue real deberían utilizar versiones explícitas.

Ejemplo:

```text
bank-legacy-account-service:1.0.0
bank-legacy-account-service:1.0.1
```

Ante una versión defectuosa:

```text
1. seleccionar la Task Definition estable anterior;
2. actualizar el ECS Service;
3. esperar la sustitución de las tareas;
4. validar los endpoints;
5. revisar los logs.
```

No es recomendable depender exclusivamente del tag:

```text
latest
```

para un despliegue productivo.

## 25. Estado actual

Actualmente está validada la ejecución integrada mediante:

```text
Docker
Docker Compose
OAuth 2.0
Kafka
Spring Cloud Config
Eureka
Resilience4j
Oracle
```

La arquitectura AWS descrita en este documento representa el diseño propuesto para cumplir con el requerimiento de despliegue cloud y escalamiento horizontal.

No se declara un despliegue AWS real mientras no exista evidencia de recursos efectivamente creados y ejecutándose en AWS.

## 26. Evidencias requeridas si se despliega en AWS

Si se realiza posteriormente el despliegue real, deberían conservarse evidencias de:

```text
repositorios ECR
imágenes publicadas
cluster ECS
Task Definitions
ECS Services
tareas RUNNING
Application Load Balancer
Target Groups
políticas de Auto Scaling
Secrets Manager
CloudWatch Logs
pruebas contra la URL desplegada
```

Estas evidencias permitirían diferenciar claramente el diseño propuesto de una infraestructura cloud efectivamente implementada.
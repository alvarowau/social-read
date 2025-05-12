# Proyecto de Microservicios: Red Lectora (📚)

Este repositorio contiene la implementación de una arquitectura de microservicios para una plataforma de "Red Lectora". El objetivo es proporcionar una base modular y escalable para gestionar usuarios, autenticación, y futuras funcionalidades relacionadas con la lectura y la interacción social entre lectores.

## 🚀 Arquitectura del Sistema

El proyecto está construido sobre una arquitectura de microservicios utilizando Spring Boot y Spring Cloud, y se compone de los siguientes servicios principales:

* **Eureka Server (`eureka-server`)**: Servidor de descubrimiento de servicios. Todos los microservicios se registran aquí para que puedan encontrarse entre sí.
* **API Gateway (`api-gateway`)**: Punto de entrada único para todas las peticiones de los clientes externos. Se encarga del enrutamiento de peticiones a los microservicios adecuados, balanceo de carga y seguridad.
* **Auth Service (`auth-service`)**: Microservicio encargado de la autenticación y autorización de usuarios. Gestiona el registro de nuevos usuarios, el inicio de sesión y la generación de tokens JWT (o similar). Se comunica con Kafka para publicar eventos de usuario (ej. `user-created-topic`).
* **User Service (`user-service`)**: Microservicio (a desarrollar o en proceso) que gestionaría la información detallada de los usuarios. Consumiría eventos de Kafka generados por el Auth Service para mantener su propia base de datos de usuarios.
* **Servicios de Base de Datos/Mensajería**: Se utilizan servicios externos como PostgreSQL para la persistencia de datos y Apache Kafka para la comunicación asíncrona entre microservicios.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje**: Java 17+
* **Frameworks**: Spring Boot 3, Spring Cloud
* **Servicio de Descubrimiento**: Spring Cloud Netflix Eureka
* **API Gateway**: Spring Cloud Gateway
* **Seguridad**: Spring Security, JWT (si aplica)
* **Persistencia**: Spring Data JPA, PostgreSQL
* **Mensajería Asíncrona**: Spring Cloud Stream, Apache Kafka
* **Gestor de Dependencias**: Maven
* **Contenedores**: Docker / Docker Compose

---

## 📋 Prerrequisitos

Antes de empezar, asegúrate de tener instalado lo siguiente:

* **Java Development Kit (JDK)**: Versión 17 o superior.
* **Apache Maven**: Versión 3.8.x o superior.
* **Docker y Docker Compose**: Para levantar las bases de datos (PostgreSQL) y Kafka.
* **Git**: Para clonar el repositorio.
* **Un IDE**: (IntelliJ IDEA, VS Code con extensiones de Java, Eclipse)

---

## 🚀 Cómo Ejecutar el Proyecto

Este proyecto sigue una arquitectura de monorepo, donde cada microservicio es un proyecto Spring Boot independiente.

### 1. Clonar el Repositorio

```bash
git clone https://github.com/alvarowau/social-read
cd social-read
```

### 2. Configurar Servicios Externos (PostgreSQL y Kafka)

Asegúrate de que tienes un `docker-compose.yml` en la raíz de tu proyecto (o en una carpeta separada) que levante los servicios necesarios.

#### Ejemplo de docker-compose.yml (ajusta los puertos y nombres de las bases de datos según tu configuración):

```yaml
version: '3.8'

services:
  auth_db:
    image: postgres:13-alpine
    container_name: auth_db
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: auth_db_name
      POSTGRES_USER: auth_user
      POSTGRES_PASSWORD: auth_password
    volumes:
      - auth_data:/var/lib/postgresql/data

  kafka:
    image: confluentinc/cp-kafka:6.2.1
    container_name: kafka
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:6.2.1
    container_name: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

volumes:
  auth_data:
```

Levanta los servicios con Docker Compose:

```bash
docker-compose up -d
```

### 3. Compilar y Ejecutar los Microservicios

Deberás compilar y ejecutar cada microservicio de forma independiente, respetando el orden de arranque:

#### Eureka Server:

```bash
cd eureka-server
mvn spring-boot:run
# O construye el JAR y ejecútalo:
# mvn clean install
# java -jar target/eureka-server-0.0.1-SNAPSHOT.jar
```

Verifica que está levantado en http://localhost:8761.

#### API Gateway:

```bash
cd ../api-gateway
mvn spring-boot:run
```

#### Auth Service:

```bash
cd ../auth-service
mvn spring-boot:run
```

#### User Service (y otros si los tienes):

```bash
cd ../user-service
mvn spring-boot:run
```

---

## 🧪 Interacción con la API

Una vez que todos los servicios estén levantados y registrados en Eureka, puedes interactuar con la API a través del API Gateway.

### Ejemplo de Endpoints

**Registro de Usuario (Auth Service):**

`POST /api/auth/register`

```json
{
    "name": "Alvaro",
    "surname": "Wau",
    "nickname": "AlvaroWau",
    "email": "alvaro_wau@example.com",
    "password": "Password123!"
}
```

**Login de Usuario (Auth Service):**

`POST /api/auth/login`

```json
{
    "username": "nuevoUsuario",
    "password": "passwordSeguro"
}
```

(Esto debería devolver un token JWT si implementas JWT)

---

## 📁 Estructura del Proyecto

```
microservicios/
├── eureka-server/          # Proyecto Spring Boot para el Servidor Eureka
├── api-gateway/            # Proyecto Spring Boot para el API Gateway
├── auth-service/           # Proyecto Spring Boot para el Servicio de Autenticación
├── user-service/           # Proyecto Spring Boot para el Servicio de Usuarios
├── docker-compose.yml      # Archivo para levantar PostgreSQL y Kafka con Docker Compose
├── .gitignore              # Archivo de Git para ignorar ficheros/directorios no necesarios
└── README.md               # Este archivo
```

---

## 🤝 Contribuciones

Si deseas contribuir a este proyecto, no dudes en crear un *fork* del repositorio, realizar tus cambios en una rama nueva y enviar un *pull request*.

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT License 

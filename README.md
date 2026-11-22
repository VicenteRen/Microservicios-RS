
# Rincón Sur - Microservicios

Backend de la aplicación móvil **Rincón Sur**, desarrollado en Java y Spring Boot. Permite gestionar usuarios, observaciones y accesos mediante APIs REST, utilizando una arquitectura de microservicios.

## Integrantes

- Vicente Cespedes
- Vicente Ponce
- Emilia Gallardo

## Tecnologías utilizadas

- Java 21 y Spring Boot 
- Spring Cloud, Eureka Server y API Gateway
- MySQL y Flyway
- Maven
- JUnit 5, Mockito y JaCoCo 
- Swagger / OpenAPI
- Git y GitHub

## Arquitectura y funcionalidades

| Microservicio | Puerto | Funcionalidad |
|---|---|---|
| Eureka Server | 8761 | Registro y descubrimiento de servicios |
| API Gateway | 8080 | Comunicación entre Android y el backend |
| Usuarios | 8082 | Gestión de usuarios, autenticación y recuperación de contraseñas |
| Observaciones | 8081 | Registro, consulta y anulación de observaciones |
| Accesos | 8083 | Registro, consulta y actualización del estado de accesos |

## Principales endpoints

Las solicitudes de Android se centralizan mediante el API Gateway (`http://localhost:8080`).

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Listar usuarios |
| POST | `/api/usuarios` | Registrar usuario |
| POST | `/api/usuarios/login` | Autenticación |
| PUT | `/api/usuarios/{id}/contacto` | Actualizar contacto |
| PUT | `/api/usuarios/{id}/cambiar-contrasena` | Cambiar contraseña |
| POST | `/api/observaciones` | Registrar observación |
| GET | `/api/observaciones` | Listar observaciones |
| GET | `/api/observaciones/usuario/{usuarioId}` | Consultar por usuario |
| PUT | `/api/observaciones/{id}/anular` | Anular observación |
| GET | `/api/accesos` | Listar accesos |
| GET | `/api/accesos/activos` | Consultar accesos activos |
| POST | `/api/accesos` | Registrar acceso |
| PUT | `/api/accesos/{id}/estado` | Actualizar estado |

La documentación completa de los endpoints está disponible mediante Swagger UI en los puertos 8081, 8082 y 8083.

## Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/VicenteRen/Microservicios-RS.git
```

### 2. Configurar MySQL

Iniciar MySQL en el puerto `3306` y verificar las credenciales de conexión.

Las bases de datos utilizadas son:

- `rinconsur_usuarios`
- `rinconsur_observaciones`
- `rinconsur_accesos`

Se utiliza `createDatabaseIfNotExist=true` para permitir su creación automática y **Flyway** para gestionar las tablas mediante migraciones.

### 3. Ejecutar los microservicios

Se requiere JDK 21. Abrir una terminal independiente para cada módulo e iniciarlos en este orden:

1. `eureka-server`
2. `usuarios`
3. `observaciones`
4. `accesos`
5. `api-gateway`

Desde la carpeta de cada módulo ejecutar:

```bat
mvnw.cmd spring-boot:run
```

## Pruebas unitarias y cobertura

Se implementaron pruebas con **JUnit 5 y Mockito**, utilizando **JaCoCo** para medir la cobertura del código.

| Microservicio | Cobertura de instrucciones |
|---|---|
| Usuarios | 90 % |
| Observaciones | 89 % |
| Accesos | 97 % |

Los tres microservicios superan el **80 % mínimo de cobertura**.

Para ejecutar las pruebas y generar los informes:

```bat
mvnw.cmd clean verify
```

Los resultados se consultan en `target/site/jacoco/index.html` de cada microservicio.

## Aplicación móvil

El backend se integra con la aplicación Android Rincón Sur, desarrollada en Kotlin con Jetpack Compose y Retrofit.

**Repositorio Android:**  
https://github.com/VicenteRen/RinconSurApp

### Conexión con el emulador Android

La aplicación utiliza Retrofit para comunicarse con el API Gateway mediante `http://127.0.0.1:8080/`.

Para permitir la conexión desde el emulador Android al backend local, ejecutar:

```bat
adb reverse tcp:8080 tcp:8080
```

El API Gateway distribuye las solicitudes hacia Usuarios (8082), Observaciones (8081) y Accesos (8083).

## Control de versiones

El proyecto utiliza GitHub para registrar los avances mediante commits, ramas de trabajo y Pull Requests, permitiendo organizar el desarrollo colaborativo.

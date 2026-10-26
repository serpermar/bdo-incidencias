# 🛠️ Gestión de incidencias · Prueba técnica Java Middle

Pequeña aplicación web para registrar y consultar incidencias técnicas de un equipo de soporte.  
Incluye **API REST**, base de datos **H2 en memoria/PostgreSQL**, **frontend** en HTML/CSS/JavaScript sin frameworks, **seguridad**, **auditoría**, **paginación**, **filtros combinados** y **tests**.

## ⚙️ Stack Tecnológico

| Pieza | Elección |
|---|---|
| **Lenguaje** | Java 17 |
| **Framework** | Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Bean Validation, Spring Security, Actuator) |
| **Base de datos** | H2 en memoria (dev) / PostgreSQL (perfil postgres) |
| **Migraciones** | Flyway |
| **Build** | Maven (con Maven Wrapper incluido) |
| **Tests** | JUnit 5, Mockito, AssertJ, MockMvc y Spring Security Test |
| **Frontend** | HTML, CSS y JavaScript nativos (sin frameworks) |
| **Documentación API** | SpringDoc OpenAPI / Swagger UI |
| **Contenedores** | Docker / Docker Compose |

## 🚀 Arranque rápido

Requisitos: **JDK 17** instalado y `JAVA_HOME` apuntando a él. No hace falta tener Maven: el wrapper lo descarga.

### 🧪 Modo desarrollo (H2 + Flyway)
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### 🐘 Con PostgreSQL (Docker Compose)
```bash
docker compose up --build
```

La aplicación queda en <http://localhost:8080>.

### 🔐 Credenciales de acceso
Los endpoints `/api/**` requieren autenticación **HTTP Basic** (usuarios en memoria):

| Usuario | Contraseña | Rol |
|---|---|---|
| `soporte` | `soporte123` | `ROLE_SOPORTE` |
| `jefe` | `jefe123` | `ROLE_JEFE` |

Solo `SOPORTE` o `JEFE` pueden crear incidencias (`POST`) o cambiar su estado (`PATCH`). La lectura del listado y usuarios requiere estar autenticado. Los recursos estáticos, Swagger, H2 Console y `/actuator/health` son accesibles sin login.

| Recurso | URL |
|---|---|
| 🌐 **Frontend** | <http://localhost:8080/> |
| 🔗 **API de incidencias** | <http://localhost:8080/api/incidencias> |
| 👥 **API de usuarios** | <http://localhost:8080/api/usuarios> |
| 📖 **Swagger UI** | <http://localhost:8080/swagger-ui.html> |
| 📄 **OpenAPI JSON** | <http://localhost:8080/v3/api-docs> |
| ❤️ **Actuator Health** | <http://localhost:8080/actuator/health> |
| 🗄️ **Consola H2** | <http://localhost:8080/h2-console> (`jdbc:h2:mem:incidencias`, usuario `sa`, sin contraseña) |

### 🧪 Tests

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

Para compilar, ejecutar los tests y empaquetar en un solo comando: `.\mvnw.cmd verify`.  
El ejecutable resultante se lanza con:

```bash
java -jar target/incidencias-1.0.0.jar
```

## API REST

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| `POST` | `/api/incidencias` | Crea una incidencia en estado `ABIERTA` (requiere rol `SOPORTE`/`JEFE`) | `201`, `400`, `401`, `403` |
| `GET` | `/api/incidencias` | Lista incidencias con paginación, ordenación y filtros combinados (`q`, `estado`, `prioridad`, `usuarioId`, `page`, `size`, `sort`) | `200`, `400`, `401` |
| `PATCH` | `/api/incidencias/{id}/estado` | Cambia el estado con auditoría (requiere rol `SOPORTE`/`JEFE`). Aplica bloqueo optimista (`@Version`) | `200`, `400`, `401`, `403`, `404`, `409` |
| `GET` | `/api/usuarios` | Lista usuarios para el formulario de alta (extra) | `200`, `401` |

### Ejemplos

```bash
# Crear (el estado siempre es ABIERTA; autenticación requerida)
curl -i -u soporte:soporte123 -X POST http://localhost:8080/api/incidencias \
  -H "Content-Type: application/json" \
  -d '{"titulo":"No arranca la VPN","descripcion":"Error 619","prioridad":"ALTA","categoria":"REDES","fechaLimite":"2026-10-30","usuarioAsignadoId":1}'

# Listar con filtros combinados + paginación
curl -u soporte:soporte123 "http://localhost:8080/api/incidencias?q=VPN&estado=ABIERTA&prioridad=ALTA&page=0&size=10"

# Filtrar por estado (compatible con listado sin paginación)
curl -u soporte:soporte123 "http://localhost:8080/api/incidencias?estado=CERRADA"

# Cambiar estado (se guarda quién lo audita)
curl -i -u soporte:soporte123 -X PATCH http://localhost:8080/api/incidencias/3/estado \
  -H "Content-Type: application/json" \
  -d '{"estado":"EN_CURSO"}'
```

Respuesta de creación (`201`):

```json
{
  "id": 6,
  "titulo": "No arranca la VPN",
  "descripcion": "Error 619",
  "prioridad": "ALTA",
  "estado": "ABIERTA",
  "fechaCreacion": "2026-10-26T10:15:30.123",
  "usuarioAsignado": {
    "id": 1,
    "nombre": "Ana García",
    "email": "ana.garcia@example.com"
  },
  "categoria": "REDES",
  "fechaLimite": "2026-10-30",
  "auditadoPor": null,
  "fechaUltimaModificacionEstado": null
}
```

### Formato común de error

Todos los errores de la API devuelven la misma estructura:

```json
{
  "timestamp": "2026-10-26T10:20:00.456",
  "status": 400,
  "error": "Bad Request",
  "message": "Los datos enviados no son válidos",
  "path": "/api/incidencias",
  "detalles": [
    "titulo: El título es obligatorio",
    "prioridad: La prioridad es obligatoria"
  ]
}
```

- `detalles` solo lleva contenido en errores de validación; en el resto es `[]`.
- `message` es un texto apto para mostrar al usuario; los errores inesperados devuelven un mensaje genérico y se registran en el log (no se filtra información interna).

### Reglas y decisiones de la API

- El estado inicial no lo elige el cliente: la entidad nace `ABIERTA`.
- `PATCH` sobre una incidencia `CERRADA` con un estado distinto de `CERRADA` devuelve `409`.
  Volver a enviar `CERRADA` sobre una incidencia ya cerrada es idempotente y devuelve `200` (no es reabrir).
- Si dos usuarios intentan modificar la misma incidencia a la vez se aplica **bloqueo optimista** (`@Version`) y se devuelve `409` con mensaje claro.
- `400` cubre: campos obligatorios/formatos inválidos, título de más de 100 caracteres, enums desconocidos y filtros con valores no permitidos.
- `404` si la incidencia (o el usuario asignado) no existen; `409` para la regla de negocio o conflicto optimista.
- Al cambiar el estado se registra `auditadoPor` y `fechaUltimaModificacionEstado`.

## 🎨 Frontend

- **Diseño moderno** con modo claro/oscuro automático (`prefers-color-scheme`), tipografía Inter, accesibilidad básica (skip-link, `aria-live`) y `styles.css`.
- **Tabla interactiva** consumida con `fetch`, con prioridad, estado, fecha y usuario asignado.
- **Filtros combinados:** búsqueda por título/descripción (`q`), estado, prioridad y usuario asignado. Recarga solo la tabla sin recargar la página.
- **Paginación** soportada desde la API (`Page<IncidenciaResponse>`), manteniendo compatibilidad con el listado sin paginación.
- **Gestión de errores:** mensaje visible y claro si falla la API o la red, mostrando el `message`/`detalles` del JSON de error.
- **Formulario de alta** en un `<dialog>`: valida en cliente, envía el POST y muestra los errores de validación que devuelve el servidor.
- **Acciones rápidas** por fila para pasar de `ABIERTA` a `EN_CURSO` y de `EN_CURSO` a `CERRADA` (demuestran el `PATCH` y la regla del 409).
- **Seguridad en cliente:** las filas se construyen con APIs del DOM y `textContent` (no se inyecta HTML con datos de la API).

## 🗂️ Estructura del proyecto

```text
src/main/java/com/example/incidencias
├── config/           Configuración (Seguridad, Auditoría)
├── domain/           Entidades JPA y enums (Incidencia, Usuario, Estado, Prioridad, Categoria, ComentarioIncidencia)
├── repository/       Spring Data JPA + Specifications (IncidenciaRepository, UsuarioRepository)
├── service/          Lógica de negocio y excepciones de dominio
└── web/              Controladores REST, DTOs y manejador global de errores

src/main/resources
├── application.yml / application-dev.yml / application-postgres.yml
├── db/migration/     Migraciones Flyway (V1__init.sql)
└── static/           index.html, css/styles.css, js/app.js

src/test/java/...     Tests unitarios, de contrato, de integración y de arranque
Dockerfile, docker-compose.yml, Jenkinsfile, sonar-project.properties
```

## 🗄️ Base de datos

El esquema se gestiona con **Flyway** en `src/main/resources/db/migration/V1__init.sql`:

- **Tablas:** `usuarios`, `incidencias` y `comentarios_incidencia` con PK, FK, `UNIQUE` en el email y `CHECK` para prioridad, estado y categoría.
- **Campos añadidos:** `version` (`@Version`), `categoria`, `fecha_limite`, `auditado_por`, `fecha_ultima_modificacion_estado`.
- **Índices:** por `estado` y `usuario_id`.
- **Datos de ejemplo:** 3 usuarios, 5 incidencias (una `CERRADA`) y 2 comentarios.
- **Validación:** Hibernate valida el esquema (`ddl-auto: validate`).

## 🧪 Tests

`.\mvnw.cmd test` ejecuta **23 tests** en total:

| Test | Nº | Descripción |
|---|---|---|
| **`IncidenciaServiceTest`** | 10 | Regla de incidencias cerradas (no reabrir, tampoco a `EN_CURSO`), idempotencia de `CERRADA`, alta con estado `ABIERTA`, normalización del título, usuario inexistente, incidencia inexistente, transición válida y filtros del listado. |
| **`IncidenciaControllerTest`** | 10 | Contrato HTTP con `@WebMvcTest` + Spring Security mockeado: `201`, validaciones `400` (título vacío, título > 100, prioridad inválida), filtros, `404`, `409` y estructura del JSON de error. |
| **`IncidenciasApplicationTests`** | 1 | Arranca el contexto completo y comprueba que las migraciones cargan los datos de ejemplo. |
| **`IncidenciaIntegracionTest`** | 2 | Pruebas de integración con `@SpringBootTest` + `MockMvc` (flujo completo crear/listar/cambiar estado con auditoría y regla del `409`). |

## 💡 Decisiones técnicas

1. **Spring Boot 3 + Java 17 + Maven Wrapper.** Stack habitual, permite arrancar sin Maven instalado.
2. **Separación en capas y DTOs.** `controller → service → repository`. Se usan `record`s para evitar exponer entidades JPA y el problema con relaciones perezosas.
3. **Validación, negocio y errores centralizados.** Bean Validation en DTOs, reglas de negocio en servicio y `@RestControllerAdvice` con `ApiError` unificado.
4. **Formato de error propio.** Más claro para el frontend que `ProblemDetail`. Incluye manejo de `ObjectOptimisticLockingFailureException`.
5. **JPA optimizado.** `@EntityGraph`, `JpaSpecificationExecutor`, `Specification` para filtros combinados, `Pageable` para paginación y `open-in-view: false`.
6. **Enums como STRING + CHECK.** Legibilidad en SQL y defensa en profundidad.
7. **Flyway con migraciones versionadas.** Evolutivo, listo para PostgreSQL con perfiles.
8. **Bloqueo optimista (`@Version`).** Evita conflictos entre usuarios, traducido a `409`.
9. **Spring Security + roles.** HTTP Basic en memoria, `@EnableMethodSecurity`, restricción de `POST/PATCH` a `SOPORTE`/`JEFE` y auditoría de cambios.
10. **Modelo ampliado.** `Categoria`, `fechaLimite`, `comentarios` y trazabilidad para mostrar evolución del dominio.
11. **Fechas coherentes.** `LocalDateTime`/`LocalDate` según el caso.
12. **Frontend vanilla.** `fetch` + DOM + `Intl.DateTimeFormat`, `textContent`, responsive y modo claro/oscuro automático.
13. **Estrategia de tests completa.** Unitarios, contrato, integración y smoke test.
14. **Extras útiles.** `GET /api/usuarios`, acciones rápidas en UI, OpenAPI/Swagger, Actuator, Dockerfile y docker-compose.

## ⏳ Pendientes y mejoras con más tiempo

- **🔐 Autenticación real:** sustituir usuarios en memoria por base de datos + JWT/OAuth2.
- **🧬 Migraciones completas:** añadir más versiones de Flyway para evolución futura.
- **🛡️ Autorización más granular:** reglas por incidencia/propietario además de roles.
- **✉️ Notificaciones:** alertas por correo al cambiar el estado o al acercarse la fecha límite.
- **🚀 Pipeline completo:** integración real con Jenkins, SonarQube, despliegue en contenedor/orquestador (Kubernetes).
- **🤖 Tests E2E:** Playwright/Cypress para cubrir el flujo completo del frontend.
- **♿ Accesibilidad avanzada:** revisión con herramientas automatizadas (axe) y pruebas con lector de pantalla.
- **🌍 Internacionalización (i18n):** soporte multiidioma para el frontend.

## 🤖 Uso de IA

He utilizado un asistente de IA (OpenCode) como apoyo para generar el esqueleto del proyecto, parte del CSS y borradores de tests y documentación.  
Todo el código está revisado, compilado y ejecutado localmente; las decisiones de diseño están tomadas y justificadas en este README, y puedo explicar y modificar cualquier parte del proyecto en la sesión de revisión.
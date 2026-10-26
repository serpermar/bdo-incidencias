# Gestión de incidencias · Prueba técnica Java Middle

Pequeña aplicación web para registrar y consultar incidencias técnicas de un equipo de soporte.
Incluye API REST, base de datos H2 en memoria, frontend en HTML/CSS/JavaScript sin frameworks y tests.

## Stack

| Pieza | Elección |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Bean Validation) |
| Base de datos | H2 en memoria |
| Build | Maven (con Maven Wrapper incluido) |
| Tests | JUnit 5, Mockito, AssertJ y MockMvc |
| Frontend | HTML, CSS y JavaScript nativos (sin frameworks) |

## Arranque rápido

Requisitos: **JDK 17** instalado y `JAVA_HOME` apuntando a él. No hace falta tener Maven: el wrapper lo descarga.

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

La aplicación queda en <http://localhost:8080>.

| Recurso | URL |
|---|---|
| Frontend | <http://localhost:8080/> |
| API de incidencias | <http://localhost:8080/api/incidencias> |
| Consola H2 | <http://localhost:8080/h2-console> (`jdbc:h2:mem:incidencias`, usuario `sa`, sin contraseña) |

### Tests

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
| `POST` | `/api/incidencias` | Crea una incidencia en estado `ABIERTA` | `201`, `400` |
| `GET` | `/api/incidencias?estado=ABIERTA` | Lista incidencias; el filtro por estado es opcional | `200`, `400` |
| `PATCH` | `/api/incidencias/{id}/estado` | Cambia el estado | `200`, `400`, `404`, `409` |
| `GET` | `/api/usuarios` | Lista usuarios para el formulario de alta (extra) | `200` |

### Ejemplos

```bash
# Crear (el estado siempre es ABIERTA; no se acepta en la petición)
curl -i -X POST http://localhost:8080/api/incidencias \
  -H "Content-Type: application/json" \
  -d '{"titulo":"No arranca la VPN","descripcion":"Error 619","prioridad":"ALTA","usuarioAsignadoId":1}'

# Listar todas
curl http://localhost:8080/api/incidencias

# Filtrar por estado
curl "http://localhost:8080/api/incidencias?estado=CERRADA"

# Cambiar estado
curl -i -X PATCH http://localhost:8080/api/incidencias/3/estado \
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
  }
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
- `400` cubre: campos obligatorios/formatos inválidos, título de más de 100 caracteres, enums desconocidos y filtros con valores no permitidos.
- `404` si la incidencia (o el usuario asignado) no existen; `409` para la regla de negocio.

## Frontend

- Tabla de incidencias consumida con `fetch`, con prioridad, estado, fecha y usuario asignado.
- Desplegable de filtro por estado que recarga solo la tabla (sin recargar la página) llamando a `GET /api/incidencias?estado=...`.
- Mensaje de error visible y claro si falla la API o la red, mostrando el `message`/`detalles` del JSON de error.
- Formulario opcional de alta en un `<dialog>`: valida en cliente, envía el POST y muestra los errores de validación que devuelve el servidor.
- Acciones rápidas por fila para pasar de `ABIERTA` a `EN_CURSO` y de `EN_CURSO` a `CERRADA` (demuestran el `PATCH` y la regla del 409).
- Las filas se construyen con APIs del DOM y `textContent` (no se inyecta HTML con datos de la API).

## Estructura

```
src/main/java/com/example/incidencias
├── domain/           Entidades JPA y enums (Incidencia, Usuario, Estado, Prioridad)
├── repository/       Spring Data JPA (IncidenciaRepository, UsuarioRepository)
├── service/          Lógica de negocio y excepciones de dominio
└── web/              Controladores REST, DTOs y manejador global de errores
src/main/resources
├── application.yml
├── schema.sql        Script único: tablas, restricciones, índices y datos de ejemplo
└── static/           index.html, css/estilos.css, js/app.js
src/test/java/...     Tests de servicio, de contrato HTTP y de arranque
```

## Base de datos

`src/main/resources/schema.sql` es el único script SQL y la fuente de verdad del esquema:

- Tablas `usuarios` e `incidencias` con PK, FK, `UNIQUE` en el email y `CHECK` para prioridad y estado.
- Índices por `estado` y `usuario_id`, que son los filtros habituales.
- Datos de ejemplo: 3 usuarios y 5 incidencias (una de ellas `CERRADA`, para poder probar el 409).

Hibernate no genera el esquema (`ddl-auto: none`); así el SQL entregado es exactamente el que se ejecuta.

## Tests

`.\mvnw.cmd test` ejecuta 21 tests:

- **`IncidenciaServiceTest` (10)**: regla de incidencias cerradas (no reabrir, tampoco a `EN_CURSO`), idempotencia de `CERRADA`, alta con estado `ABIERTA`, normalización del título, usuario inexistente, incidencia inexistente, transición válida y filtros del listado.
- **`IncidenciaControllerTest` (10)**: contrato HTTP con `@WebMvcTest` y servicio mockeado: `201`, validaciones `400` (título vacío, título > 100, prioridad inválida), filtros, `404`, `409` y estructura del JSON de error.
- **`IncidenciasApplicationTests` (1)**: arranca el contexto completo y comprueba que `schema.sql` crea las tablas y carga los datos de ejemplo.

## Decisiones técnicas

1. **Spring Boot 3 + Java 17 + Maven Wrapper.** Es el stack más habitual para este tipo de aplicación y permite arrancar sin instalar Maven. Descarté Jakarta EE puro porque exigía más configuración (servidor, JPA, validación) para el mismo resultado y no aportaba nada al ejercicio.
2. **Separación en capas y DTOs.** `controller → service → repository`. Los controladores no exponen entidades JPA, sino `record`s de respuesta (`IncidenciaResponse`, `UsuarioResponse`); así el contrato HTTP no depende del modelo de persistencia y se evita el problema de las relaciones perezosas al serializar.
3. **Validación en el borde, negocio en el servicio, HTTP en un `@RestControllerAdvice`.** Los DTOs usan Bean Validation (`@NotBlank`, `@Size`, `@NotNull`); la regla de “una incidencia cerrada no se reabre” vive en el servicio porque es negocio, no validación de formato; el manejador global traduce excepciones a códigos y al JSON común.
4. **Formato de error propio (`ApiError`) en lugar de `ProblemDetail`.** `ProblemDetail` es el estándar de Spring 6, pero el enunciado pide un formato coherente y sencillo de consumir desde JavaScript. Un `record` propio con `message` y `detalles` deja el contrato explícito y fácil de testear. La alternativa (extender `ResponseEntityExceptionHandler`) habría reutilizado más código, pero mezclaba dos formatos.
5. **JPA + `@EntityGraph` y `open-in-view: false`.** El listado necesita el usuario asignado; el `EntityGraph` lo trae en la misma consulta y evita el N+1. Desactivar `open-in-view` obliga a resolver las relaciones dentro de la transacción del servicio, que es donde se mapea a DTO.
6. **Enums como `STRING` y `CHECK` en la base de datos.** Legibilidad en SQL y defensa en profundidad: aunque alguien inserte saltándose la aplicación, la base de datos rechaza valores fuera del dominio.
7. **Un único `schema.sql` con `ddl-auto: none`.** Cumple el requisito de un solo script y garantiza que el esquema revisado es el que se ejecuta. En un proyecto real usaría Flyway o Liquibase para versionar migraciones; aquí el H2 es en memoria y se recrea en cada arranque.
8. **Fechas con `LocalDateTime`.** La aplicación es de un único equipo y no hay husos horarios en juego; `Instant` sería mejor si hubiera varios clientes o persistencia a largo plazo. La fecha la pone el dominio (`new Incidencia(...)`), no el cliente.
9. **Frontend sin frameworks.** `fetch` + DOM + `Intl.DateTimeFormat`. Se construyen nodos y se usa `textContent` para no introducir XSS con títulos o descripciones. El filtro se resuelve en servidor para que la API demuestre el parámetro `estado`.
10. **Estrategia de tests.** Tests unitarios del servicio con Mockito (rápidos, sin contexto) para las reglas; tests de contrato del controlador con `@WebMvcTest` para códigos y JSON; un test de arranque para verificar el script SQL. No he añadido tests de repositorio: con H2 y consultas derivadas el valor sería bajo frente al coste de mantenerlos.
11. **Extras justificados.** `GET /api/usuarios` existe solo para poblar el desplegable del formulario opcional; las acciones de cambio de estado en la tabla permiten probar el `PATCH` sin salir de la interfaz.

## Pendientes y mejoras con más tiempo

- Paginación y ordenación configurable en el listado (hoy devuelve todo; con volumen real no escala).
- Autenticación/autorización (Spring Security) y auditoría de quién cambia cada estado.
- Migraciones versionadas con Flyway y perfil de configuración para una base de datos real (PostgreSQL).
- Bloqueo optimista (`@Version`) para evitar que dos usuarios pisen el mismo cambio de estado.
- Documentación OpenAPI/Swagger y pruebas de integración de la API con `@SpringBootTest` + `MockMvc` sobre H2.
- Alta de usuarios e incidencias con más campos (categoría, fecha límite, comentarios).
- Búsqueda por texto y filtro combinado por prioridad y usuario.
- Pipeline de Jenkins con análisis de SonarQube y despliegue en contenedor.
- Tests end-to-end del frontend y revisión de accesibilidad con lector de pantalla.

## Uso de IA

He utilizado un asistente de IA (OpenCode) como apoyo para generar el esqueleto del proyecto, parte del CSS y borradores de tests y documentación. Todo el código está revisado, compilado y ejecutado localmente; las decisiones de diseño están tomadas y justificadas en este README, y puedo explicar y modificar cualquier parte del proyecto en la sesión de revisión.

## Autor

Sergi Pérez

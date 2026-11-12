# prospin-backend

Backend de la tienda ProSpin Tenis de Mesa (Evaluación Parcial 3, DSY1104).

Stack: Java 17, Spring Boot 3.3.5, Spring Data JPA, Spring Security con JWT (jjwt 0.12.6),
springdoc-openapi (Swagger), H2 (desarrollo) y MySQL/RDS (producción).

## Ejecutar en local

```bash
mvn spring-boot:run
```

- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html
- Consola H2: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/prospin`, usuario `sa`, clave vacía)

Al arrancar por primera vez se crean usuarios de demostración y los 80 productos de `src/main/resources/data/productos.json`.

| Rol | Correo | Clave por defecto |
|---|---|---|
| ADMIN | admin@prospin.cl | Admin12345 |
| VENDEDOR | vendedor@prospin.cl | Vendedor123 |
| CLIENTE | cliente@prospin.cl | Cliente123 |

## Variables de entorno

| Variable | Para qué | Valor por defecto |
|---|---|---|
| `JWT_SECRET` | Firma de los tokens (mínimo 32 caracteres) | solo para desarrollo |
| `JWT_EXPIRACION_MINUTOS` | Duración del token | 60 |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend, separados por coma | localhost 3000, 5500, 8080 |
| `SEED_ADMIN_PASSWORD`, `SEED_VENDEDOR_PASSWORD`, `SEED_CLIENTE_PASSWORD` | Claves de los usuarios de demostración | ver tabla |
| `SPRING_PROFILES_ACTIVE` | `mysql` para usar RDS | (vacío = H2) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a RDS (perfil `mysql`) | — |

Nunca subas al repositorio valores reales de `JWT_SECRET` ni claves de bases de datos.

## Ejecutar con MySQL (AWS RDS)

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL=jdbc:mysql://HOST_RDS:3306/prospin
export DB_USER=admin
export DB_PASSWORD=********
export JWT_SECRET=una-clave-larga-de-al-menos-32-caracteres
mvn spring-boot:run
```

## Pruebas

```bash
mvn test
```

Incluye pruebas unitarias de la lógica de compra (`BoletaServiceTest`): cálculo del total con precios de la base de datos, descuento de stock y rechazo sin stock suficiente.

Para probar los endpoints completos, con el servidor corriendo:

```bash
bash scripts/probar_api.sh
```

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | /api/v1/auth/registro | Público | Registra un cliente (rol CLIENTE) |
| POST | /api/v1/auth/login | Público | Devuelve un JWT |
| POST | /api/v1/auth/refresh | Autenticado | Renueva el JWT |
| GET | /api/v1/productos | ADMIN, VENDEDOR, CLIENTE | Lista productos |
| GET | /api/v1/productos/{id} | ADMIN, VENDEDOR, CLIENTE | Detalle de producto |
| POST / PUT / DELETE | /api/v1/productos[/{id}] | ADMIN | Crear, actualizar, eliminar |
| GET | /api/v1/categorias | ADMIN, VENDEDOR, CLIENTE | Lista categorías |
| GET | /api/v1/categorias/{id} | ADMIN, VENDEDOR, CLIENTE | Detalle de categoría |
| POST / PUT / DELETE | /api/v1/categorias[/{id}] | ADMIN | Crear, actualizar, eliminar |
| POST | /api/v1/boletas | ADMIN, CLIENTE | Crea una compra; valida stock y descuenta |
| GET | /api/v1/boletas | ADMIN, VENDEDOR, CLIENTE | Todas (ADMIN/VENDEDOR) o las propias (CLIENTE) |
| GET | /api/v1/boletas/{id} | ADMIN, VENDEDOR, CLIENTE | Detalle; el cliente solo ve las suyas |
| DELETE | /api/v1/boletas/{id} | ADMIN | Anula la boleta y devuelve el stock |
| GET/POST/PUT/DELETE | /api/v1/usuarios[/{id}] | ADMIN | Gestión de usuarios |

## Reglas de negocio

- El precio de cada producto se toma de la base de datos; el cliente no puede enviar precios.
- Si el mismo producto aparece varias veces en una compra, las cantidades se suman.
- Máximo 99 unidades por producto en una compra.
- Si no hay stock suficiente, responde 409 y no descuenta nada.
- Al anular una boleta, el stock vuelve al producto.
- Un producto o categoría con registros asociados no se puede borrar (409).
- Un cliente que pide una boleta ajena recibe 404.

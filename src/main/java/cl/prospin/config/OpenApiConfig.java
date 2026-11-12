package cl.prospin.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// Agrega el botón "Authorize" en Swagger para pegar el token JWT
@OpenAPIDefinition(info = @Info(
        title = "ProSpin API",
        version = "v1",
        description = "API REST de la tienda ProSpin Tenis de Mesa. Autenticación con JWT y roles ADMIN, VENDEDOR y CLIENTE."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}

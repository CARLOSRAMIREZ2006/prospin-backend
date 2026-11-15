package cl.prospin.controller;

import cl.prospin.dto.AuthDto.LoginRequest;
import cl.prospin.dto.AuthDto.RegistroRequest;
import cl.prospin.dto.AuthDto.TokenResponse;
import cl.prospin.model.Usuario;
import cl.prospin.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y renovación de JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente nuevo (rol CLIENTE)")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Creado correctamente"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Usuario registro(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un JWT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")})
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Renueva el JWT a partir de uno válido")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")})
    public TokenResponse refresh(Authentication auth) {
        return authService.renovar(auth.getName());
    }
}

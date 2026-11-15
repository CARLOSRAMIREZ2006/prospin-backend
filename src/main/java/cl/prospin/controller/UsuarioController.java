package cl.prospin.controller;

import cl.prospin.dto.UsuarioRequest;
import cl.prospin.model.Usuario;
import cl.prospin.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Gestión de usuarios (solo ADMIN)")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos los usuarios")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso")})
    public List<Usuario> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un usuario por id")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    public Usuario obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario con cualquier rol")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Creado correctamente"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Usuario crear(@Valid @RequestBody UsuarioRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un usuario")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Usuario actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un usuario")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Eliminado correctamente"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

package cl.prospin.controller;

import cl.prospin.dto.BoletaDto.BoletaRequest;
import cl.prospin.model.Boleta;
import cl.prospin.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boletas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Boletas", description = "Compras. El cliente ve las suyas; ADMIN y VENDEDOR ven todas")
public class BoletaController {

    private final BoletaService service;

    public BoletaController(BoletaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una boleta. El precio se toma de la base de datos y se descuenta el stock")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Creado correctamente"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Boleta crear(@Valid @RequestBody BoletaRequest req, Authentication auth) {
        return service.crear(auth.getName(), req.items());
    }

    @GetMapping
    @Operation(summary = "Lista boletas (todas para ADMIN/VENDEDOR; propias para CLIENTE)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")})
    public List<Boleta> listar(Authentication auth) {
        return service.listar(auth.getName(), BoletaService.esStaff(auth));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una boleta")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    public Boleta obtener(@PathVariable Long id, Authentication auth) {
        return service.obtener(id, auth.getName(), BoletaService.esStaff(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Anula una boleta y devuelve el stock (solo ADMIN)")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Eliminado correctamente"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    public void anular(@PathVariable Long id) {
        service.anular(id);
    }
}

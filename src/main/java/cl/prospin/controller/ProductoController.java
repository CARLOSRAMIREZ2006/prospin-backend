package cl.prospin.controller;

import cl.prospin.dto.CatalogoDto.ProductoRequest;
import cl.prospin.model.Producto;
import cl.prospin.service.ProductoService;
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
@RequestMapping("/api/v1/productos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Productos", description = "Consulta para todos los roles; modificación solo ADMIN")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los productos")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")})
    public List<Producto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    public Producto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un producto")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Creado correctamente"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Producto crear(@Valid @RequestBody ProductoRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un producto")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Eliminado correctamente"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

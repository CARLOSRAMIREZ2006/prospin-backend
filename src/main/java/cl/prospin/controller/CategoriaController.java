package cl.prospin.controller;

import cl.prospin.dto.CatalogoDto.CategoriaRequest;
import cl.prospin.model.Categoria;
import cl.prospin.service.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categorías", description = "Consulta para todos los roles; modificación solo ADMIN")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista las categorías")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido")})
    public List<Categoria> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una categoría por id")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    public Categoria obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una categoría")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Creado correctamente"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Categoria crear(@Valid @RequestBody CategoriaRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una categoría")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación exitosa"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public Categoria actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una categoría (falla si tiene productos)")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Eliminado correctamente"), @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"), @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto: dato duplicado, stock insuficiente o registro en uso")})
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

package cl.prospin.controller;

import cl.prospin.dto.CatalogoDto.ProductoRequest;
import cl.prospin.model.Producto;
import cl.prospin.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
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
    public List<Producto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto")
    public Producto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un producto")
    public Producto crear(@Valid @RequestBody ProductoRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un producto")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

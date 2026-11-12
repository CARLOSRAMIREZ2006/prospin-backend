package cl.prospin.controller;

import cl.prospin.dto.BoletaDto.BoletaRequest;
import cl.prospin.model.Boleta;
import cl.prospin.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
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
    public Boleta crear(@Valid @RequestBody BoletaRequest req, Authentication auth) {
        return service.crear(auth.getName(), req.items());
    }

    @GetMapping
    @Operation(summary = "Lista boletas (todas para ADMIN/VENDEDOR; propias para CLIENTE)")
    public List<Boleta> listar(Authentication auth) {
        return service.listar(auth.getName(), BoletaService.esStaff(auth));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una boleta")
    public Boleta obtener(@PathVariable Long id, Authentication auth) {
        return service.obtener(id, auth.getName(), BoletaService.esStaff(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Anula una boleta y devuelve el stock (solo ADMIN)")
    public void anular(@PathVariable Long id) {
        service.anular(id);
    }
}

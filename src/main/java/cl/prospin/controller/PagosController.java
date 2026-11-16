package cl.prospin.controller;

import cl.prospin.dto.BoletaDto.BoletaRequest;
import cl.prospin.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "Pago con Webpay Plus (ambiente de integración)")
public class PagosController {

    private final PagoService pagos;

    public PagosController(PagoService pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/iniciar")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crea la boleta pendiente y la transacción en Webpay; devuelve la URL y el token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/vencido"),
            @ApiResponse(responseCode = "403", description = "El rol del usuario no tiene permiso"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente"),
            @ApiResponse(responseCode = "502", description = "No se pudo conectar con Webpay")
    })
    public Map<String, Object> iniciar(@Valid @RequestBody BoletaRequest req, Authentication auth) {
        return pagos.iniciar(auth.getName(), req.items(), dirPublica());
    }

    // Webpay devuelve al navegador aquí (GET o POST). Confirma el pago y redirige a la tienda.
    @RequestMapping(value = "/retorno", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Retorno desde Webpay: confirma el pago y redirige a la tienda")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirección a la tienda con el resultado"),
            @ApiResponse(responseCode = "400", description = "Retorno sin datos válidos")
    })
    public ResponseEntity<Void> retorno(
            @RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String ordenCompra) {
        String destino;
        try {
            if (tokenWs != null) {
                PagoService.Resultado r = pagos.confirmar(tokenWs);
                destino = (r.aprobada() ? "pago=ok" : "pago=rechazado") + "&boleta=" + r.boletaId();
            } else if (tbkToken != null && ordenCompra != null && ordenCompra.startsWith("B")) {
                long id = Long.parseLong(ordenCompra.substring(1));
                pagos.cancelar(id);
                destino = "pago=cancelado&boleta=" + id;
            } else {
                destino = "pago=error";
            }
        } catch (RuntimeException e) {
            destino = "pago=error";
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(dirPublica() + "/index.html?" + destino))
                .build();
    }

    // Dirección por la que el navegador llegó (IP o dominio actual), sin puerto si es el 80
    private String dirPublica() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}

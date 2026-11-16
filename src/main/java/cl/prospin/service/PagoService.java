package cl.prospin.service;

import cl.prospin.dto.BoletaDto.ItemBoleta;
import cl.prospin.model.Boleta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pago con Webpay Plus (API REST de Transbank, ambiente de integración).
 * El total lo calcula el servidor desde la base de datos; el cliente nunca envía precios.
 */
@Service
public class PagoService {

    private static final String RUTA = "/rswebpaytransaction/api/webpay/v1.2/transactions";

    public record Resultado(Long boletaId, boolean aprobada) {}

    private final BoletaService boletas;
    private final MailService mail;
    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    // Tiempos límite: si Webpay no responde, el pago falla con un error claro en vez de quedar colgado
    private final RestTemplate http = new RestTemplate(tiempos());

    private static SimpleClientHttpRequestFactory tiempos() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(20_000);
        return f;
    }
    private final String baseUrl;
    private final String codigoComercio;
    private final String apiSecret;

    public PagoService(BoletaService boletas, MailService mail,
                       @Value("${webpay.base-url}") String baseUrl,
                       @Value("${webpay.commerce-code}") String codigoComercio,
                       @Value("${webpay.api-secret}") String apiSecret) {
        this.boletas = boletas;
        this.mail = mail;
        this.baseUrl = baseUrl;
        this.codigoComercio = codigoComercio;
        this.apiSecret = apiSecret;
    }

    private HttpHeaders cabeceras() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Tbk-Api-Key-Id", codigoComercio);
        h.set("Tbk-Api-Key-Secret", apiSecret);
        // Webpay (Imperva) bloquea el identificador por defecto de Java; se envía uno de navegador
        h.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) ProSpin/1.0");
        h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return h;
    }

    // Crea la boleta pendiente y la transacción en Webpay; devuelve la URL y el token para redirigir al cliente
    // baseUrl es la dirección por la que llegó el cliente (p. ej. http://IP-actual), así no depende de una IP fija
    public Map<String, Object> iniciar(String email, List<ItemBoleta> items, String dirPublica) {
        Boleta boleta = boletas.crearPendiente(email, items);
        Map<String, Object> cuerpo = Map.of(
                "buy_order", "B" + boleta.getId(),
                "session_id", "S" + boleta.getId() + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                "amount", boleta.getTotal(),
                "return_url", dirPublica + "/api/v1/pagos/retorno");
        try {
            ResponseEntity<Map> r = http.exchange(this.baseUrl + RUTA, HttpMethod.POST,
                    new HttpEntity<>(cuerpo, cabeceras()), Map.class);
            Map<?, ?> datos = r.getBody();
            String token = (String) datos.get("token");
            String url = (String) datos.get("url");
            boletas.guardarToken(boleta.getId(), token);
            return Map.of("boletaId", boleta.getId(), "token", token, "url", url);
        } catch (RestClientException e) {
            log.warn("Webpay (crear transacción) falló: {}", e.getMessage());
            boletas.rechazar(boleta.getId());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con Webpay");
        }
    }

    // Confirma la transacción al volver de Webpay (commit)
    public Resultado confirmar(String token) {
        Boleta boleta = boletas.buscarPorToken(token);
        if ("PAGADA".equals(boleta.getEstado())) {
            return new Resultado(boleta.getId(), true); // ya confirmada (p. ej., al recargar la página)
        }
        try {
            ResponseEntity<Map> r = http.exchange(baseUrl + RUTA + "/" + token, HttpMethod.PUT,
                    new HttpEntity<>(Map.of(), cabeceras()), Map.class);
            Map<?, ?> datos = r.getBody();
            boolean aprobada = "AUTHORIZED".equals(datos.get("status"))
                    && Integer.valueOf(0).equals(datos.get("response_code"));
            if (aprobada) {
                boletas.marcarPagada(boleta.getId());
                mail.confirmacionCompra(boleta.getUsuario().getEmail(), boleta.getId(), boleta.getTotal());
            } else {
                boletas.rechazar(boleta.getId());
            }
            return new Resultado(boleta.getId(), aprobada);
        } catch (RestClientException e) {
            log.warn("Webpay (confirmar transacción) falló: {}", e.getMessage());
            boletas.rechazar(boleta.getId());
            return new Resultado(boleta.getId(), false);
        }
    }

    // El cliente anuló el pago en Webpay
    public void cancelar(Long boletaId) {
        boletas.rechazar(boletaId);
    }
}

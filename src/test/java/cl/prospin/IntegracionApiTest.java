package cl.prospin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración: levantan la aplicación completa con H2 en memoria
 * y recorren la API con MockMvc (filtros de seguridad incluidos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:prospin-test;DB_CLOSE_DELAY=-1", "app.mail.enabled=false"})
class IntegracionApiTest {

    private static final String ADMIN = "admin@prospin.cl";
    private static final String ADMIN_PASS = "Admin12345";
    private static final String VENDEDOR = "vendedor@prospin.cl";
    private static final String VENDEDOR_PASS = "Vendedor123";
    private static final String CLIENTE = "cliente@prospin.cl";
    private static final String CLIENTE_PASS = "Cliente123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    // ---------- utilidades ----------

    private String login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode leer(String ruta, String token) throws Exception {
        MvcResult r = mvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private long categoriaId(String tokenAdmin) throws Exception {
        return leer("/api/v1/categorias", tokenAdmin).get(0).get("id").asLong();
    }

    private long crearProducto(String tokenAdmin, int stock) throws Exception {
        Map<String, Object> cuerpo = Map.of(
                "codigo", "IT-" + UUID.randomUUID().toString().substring(0, 8),
                "nombre", "Producto de prueba",
                "descripcion", "Creado por la prueba de integración",
                "precio", 1000,
                "stock", stock,
                "imagen", "img/productos/TT-001.jpg",
                "categoriaId", categoriaId(tokenAdmin));
        MvcResult r = mvc.perform(post("/api/v1/productos")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private MvcResult comprar(String tokenCliente, long productoId, int cantidad) throws Exception {
        Map<String, Object> item = Map.of("productoId", productoId, "cantidad", cantidad);
        Map<String, Object> cuerpo = Map.of("items", List.of(item));
        return mvc.perform(post("/api/v1/boletas")
                .header("Authorization", "Bearer " + tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)))
                .andReturn();
    }

    // ---------- autenticación ----------

    @Test
    void loginCorrectoDevuelveTokenYRol() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", CLIENTE, "password", CLIENTE_PASS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", CLIENTE, "password", "clave-mala-123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void peticionSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/productos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer token.falso.invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void renovarTokenDevuelveUnoNuevo() throws Exception {
        String token = login(CLIENTE, CLIENTE_PASS);
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    // ---------- autorización por rol ----------

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        String cliente = login(CLIENTE, CLIENTE_PASS);
        mvc.perform(post("/api/v1/productos")
                .header("Authorization", "Bearer " + cliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"X-1\",\"nombre\":\"x\",\"precio\":1,\"stock\":1,\"categoriaId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendedorPuedeVerOrdenesPeroNoComprar() throws Exception {
        String vendedor = login(VENDEDOR, VENDEDOR_PASS);
        mvc.perform(get("/api/v1/boletas").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk());
        assertEquals(403, comprar(vendedor, 1L, 1).getResponse().getStatus());
    }

    @Test
    void clienteNoPuedeGestionarUsuarios() throws Exception {
        String cliente = login(CLIENTE, CLIENTE_PASS);
        mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeGestionarUsuarios() throws Exception {
        String admin = login(ADMIN, ADMIN_PASS);
        JsonNode usuarios = leer("/api/v1/usuarios", admin);
        assertTrue(usuarios.size() >= 3);
    }

    // ---------- lógica de compra ----------

    @Test
    void compraCalculaTotalYDescuentaStock() throws Exception {
        String cliente = login(CLIENTE, CLIENTE_PASS);
        String admin = login(ADMIN, ADMIN_PASS);
        long productoId = crearProducto(admin, 10);

        MvcResult r = comprar(cliente, productoId, 2);
        assertEquals(201, r.getResponse().getStatus());
        JsonNode boleta = json.readTree(r.getResponse().getContentAsString());
        assertEquals(2000, boleta.get("total").asInt());

        JsonNode producto = leer("/api/v1/productos/" + productoId, admin);
        assertEquals(8, producto.get("stock").asInt());
    }

    @Test
    void compraSinStockSuficienteDevuelve409YNoDescuenta() throws Exception {
        String cliente = login(CLIENTE, CLIENTE_PASS);
        String admin = login(ADMIN, ADMIN_PASS);
        long productoId = crearProducto(admin, 1);

        assertEquals(409, comprar(cliente, productoId, 2).getResponse().getStatus());
        assertEquals(1, leer("/api/v1/productos/" + productoId, admin).get("stock").asInt());
    }

    @Test
    void clienteSoloVeSusPropiasBoletas() throws Exception {
        String cliente = login(CLIENTE, CLIENTE_PASS);
        String admin = login(ADMIN, ADMIN_PASS);
        long productoId = crearProducto(admin, 5);
        long boletaId = json.readTree(comprar(cliente, productoId, 1).getResponse().getContentAsString()).get("id").asLong();

        // Otro cliente recién registrado
        String email = "prueba-" + UUID.randomUUID().toString().substring(0, 8) + "@prospin.cl";
        mvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "nombre", "Cliente prueba", "email", email, "password", "Clave12345"))))
                .andExpect(status().isCreated());
        String otro = login(email, "Clave12345");

        mvc.perform(get("/api/v1/boletas/" + boletaId).header("Authorization", "Bearer " + otro))
                .andExpect(status().isNotFound());
        assertEquals(0, leer("/api/v1/boletas", otro).size());
    }

    @Test
    void registroDeClienteAsignaRolCliente() throws Exception {
        String email = "nuevo-" + UUID.randomUUID().toString().substring(0, 8) + "@prospin.cl";
        mvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "nombre", "Nuevo", "email", email, "password", "Clave12345"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ---------- integridad de datos ----------

    @Test
    void noSePuedeEliminarCategoriaQueTieneProductos() throws Exception {
        String admin = login(ADMIN, ADMIN_PASS);
        long catId = categoriaId(admin); // la semilla asigna productos a todas las categorías
        mvc.perform(delete("/api/v1/categorias/" + catId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void adminAnulaBoletaYDevuelveStock() throws Exception {
        String cliente = login(CLIENTE, CLIENTE_PASS);
        String admin = login(ADMIN, ADMIN_PASS);
        long productoId = crearProducto(admin, 6);
        long boletaId = json.readTree(comprar(cliente, productoId, 4).getResponse().getContentAsString()).get("id").asLong();
        assertEquals(2, leer("/api/v1/productos/" + productoId, admin).get("stock").asInt());

        mvc.perform(delete("/api/v1/boletas/" + boletaId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        assertEquals(6, leer("/api/v1/productos/" + productoId, admin).get("stock").asInt());
    }
}

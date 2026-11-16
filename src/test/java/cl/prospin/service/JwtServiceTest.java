package cl.prospin.service;

import cl.prospin.model.Rol;
import cl.prospin.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("clave-de-prueba-de-al-menos-32-caracteres-xyz", 60);

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setEmail("admin@prospin.cl");
        u.setRol(Rol.ADMIN);
        return u;
    }

    @Test
    void generaUnTokenQueSeValidaConSujetoYRol() {
        Claims c = jwt.validar(jwt.generar(usuario()));
        assertEquals("admin@prospin.cl", c.getSubject());
        assertEquals("ADMIN", c.get("rol", String.class));
    }

    @Test
    void rechazaUnTokenConFirmaAlterada() {
        String token = jwt.generar(usuario());
        String[] partes = token.split("\\.");
        String firma = partes[2];
        int medio = firma.length() / 2;
        char nuevo = firma.charAt(medio) == 'A' ? 'B' : 'A';
        String firmaAlterada = firma.substring(0, medio) + nuevo + firma.substring(medio + 1);
        String alterado = partes[0] + "." + partes[1] + "." + firmaAlterada;
        assertThrows(JwtException.class, () -> jwt.validar(alterado));
    }

    @Test
    void rechazaUnaClaveDemasiadoCorta() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corta", 60));
    }

    @Test
    void expiracionEsDeSesentaMinutos() {
        assertEquals(3600, jwt.segundosExpiracion());
    }
}

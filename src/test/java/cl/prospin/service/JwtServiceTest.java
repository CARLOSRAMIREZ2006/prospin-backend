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
        char ultimo = token.charAt(token.length() - 1);
        String alterado = token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A');
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

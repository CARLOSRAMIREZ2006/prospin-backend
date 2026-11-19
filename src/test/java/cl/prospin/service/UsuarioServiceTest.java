package cl.prospin.service;

import cl.prospin.dto.UsuarioRequest;
import cl.prospin.model.Rol;
import cl.prospin.model.Usuario;
import cl.prospin.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private PasswordEncoder encoder;
    @InjectMocks private UsuarioService service;

    @Test
    void crearUsuarioEsClienteYClaveCodificada() {
        when(usuarios.existsByEmail("vend@prospin.cl")).thenReturn(false);
        when(encoder.encode("Vendedor123")).thenReturn("hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario u = service.crear(new UsuarioRequest("Vend", "Vend@prospin.cl", "Vendedor123"));

        assertEquals("vend@prospin.cl", u.getEmail());
        assertEquals(Rol.CLIENTE, u.getRol());
        assertEquals("hash", u.getPassword());
    }

    @Test
    void crearUsuarioConCorreoRepetidoDevuelve409() {
        when(usuarios.existsByEmail("vend@prospin.cl")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(new UsuarioRequest("Vend", "vend@prospin.cl", "Vendedor123")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarios, never()).save(any());
    }
}

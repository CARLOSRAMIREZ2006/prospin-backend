package cl.prospin.service;

import cl.prospin.dto.AuthDto.LoginRequest;
import cl.prospin.dto.AuthDto.RegistroRequest;
import cl.prospin.dto.AuthDto.TokenResponse;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwt;
    @Mock private MailService mail;
    @InjectMocks private AuthService service;

    private Usuario admin() {
        Usuario u = new Usuario();
        u.setEmail("admin@prospin.cl");
        u.setRol(Rol.ADMIN);
        u.setPassword("hash");
        return u;
    }

    @Test
    void registroNuevoAsignaRolClienteNormalizaCorreoYCodificaClave() {
        when(usuarios.existsByEmail("ana@prospin.cl")).thenReturn(false);
        when(encoder.encode("Clave12345")).thenReturn("hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario u = service.registrar(new RegistroRequest("  Ana  ", " ANA@prospin.cl ", "Clave12345"));

        assertEquals(Rol.CLIENTE, u.getRol());
        assertEquals("ana@prospin.cl", u.getEmail());
        assertEquals("Ana", u.getNombre());
        assertEquals("hash", u.getPassword());
    }

    @Test
    void registroConCorreoRepetidoDevuelve409SinGuardar() {
        when(usuarios.existsByEmail("ana@prospin.cl")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrar(new RegistroRequest("Ana", "ana@prospin.cl", "Clave12345")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarios, never()).save(any());
    }

    @Test
    void loginCorrectoDevuelveTokenConRol() {
        when(usuarios.findByEmail("admin@prospin.cl")).thenReturn(Optional.of(admin()));
        when(encoder.matches("Admin12345", "hash")).thenReturn(true);
        when(jwt.generar(any(Usuario.class))).thenReturn("tok");
        when(jwt.segundosExpiracion()).thenReturn(3600L);

        TokenResponse r = service.login(new LoginRequest("admin@prospin.cl", "Admin12345"));

        assertEquals("tok", r.token());
        assertEquals("Bearer", r.tipo());
        assertEquals("ADMIN", r.rol());
        assertEquals(3600L, r.expiraEnSegundos());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401SinEmitirToken() {
        when(usuarios.findByEmail("admin@prospin.cl")).thenReturn(Optional.of(admin()));
        when(encoder.matches("mala", "hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("admin@prospin.cl", "mala")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(jwt, never()).generar(any());
    }

    @Test
    void loginConCorreoInexistenteDevuelve401() {
        when(usuarios.findByEmail("nadie@prospin.cl")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("nadie@prospin.cl", "Clave12345")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}

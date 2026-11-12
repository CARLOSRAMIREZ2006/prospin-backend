package cl.prospin.service;

import cl.prospin.dto.AuthDto.LoginRequest;
import cl.prospin.dto.AuthDto.RegistroRequest;
import cl.prospin.dto.AuthDto.TokenResponse;
import cl.prospin.model.Rol;
import cl.prospin.model.Usuario;
import cl.prospin.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // Registro público: todo usuario nuevo es CLIENTE. Solo el ADMIN puede crear otros roles.
    @Transactional
    public Usuario registrar(RegistroRequest req) {
        String email = normalizarEmail(req.email());
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(req.password()));
        u.setRol(Rol.CLIENTE);
        return usuarios.save(u);
    }

    public TokenResponse login(LoginRequest req) {
        // Mismo mensaje si el correo no existe o la clave es incorrecta (no revelar cuál)
        Usuario u = usuarios.findByEmail(normalizarEmail(req.email()))
                .filter(x -> encoder.matches(req.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return respuestaToken(u);
    }

    // Renovación: se emite un token nuevo a partir de un token todavía válido
    public TokenResponse renovar(String email) {
        Usuario u = usuarios.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no válido"));
        return respuestaToken(u);
    }

    private TokenResponse respuestaToken(Usuario u) {
        return new TokenResponse(jwt.generar(u), "Bearer", jwt.segundosExpiracion(), u.getRol().name());
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}

package cl.prospin.service;

import cl.prospin.dto.UsuarioRequest;
import cl.prospin.model.Rol;
import cl.prospin.model.Usuario;
import cl.prospin.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public List<Usuario> listar() {
        return usuarios.findAll();
    }

    public Usuario obtener(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public Usuario crear(UsuarioRequest req) {
        String email = AuthService.normalizarEmail(req.email());
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        Usuario u = new Usuario();
        u.setRol(Rol.CLIENTE); // todo usuario creado por Swagger es cliente
        aplicar(u, req, email);
        return usuarios.save(u);
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioRequest req) {
        Usuario u = obtener(id);
        String email = AuthService.normalizarEmail(req.email());
        if (!email.equals(u.getEmail()) && usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        aplicar(u, req, email); // el rol se conserva
        return usuarios.save(u);
    }

    @Transactional
    public void eliminar(Long id) {
        usuarios.delete(obtener(id));
    }

    private void aplicar(Usuario u, UsuarioRequest req, String email) {
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(req.password()));
    }
}

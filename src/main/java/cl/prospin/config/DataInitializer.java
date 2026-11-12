package cl.prospin.config;

import cl.prospin.model.Categoria;
import cl.prospin.model.Producto;
import cl.prospin.model.Rol;
import cl.prospin.model.Usuario;
import cl.prospin.repository.CategoriaRepository;
import cl.prospin.repository.ProductoRepository;
import cl.prospin.repository.UsuarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga datos de demostración la primera vez que arranca la aplicación:
 * 3 usuarios (uno por rol) y el catálogo de 80 productos desde data/productos.json.
 * Solo inserta si las tablas están vacías, así no duplica datos al reiniciar.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PasswordEncoder encoder;
    private final ObjectMapper mapper;
    private final String passAdmin;
    private final String passVendedor;
    private final String passCliente;

    public DataInitializer(UsuarioRepository usuarios,
                           CategoriaRepository categorias,
                           ProductoRepository productos,
                           PasswordEncoder encoder,
                           ObjectMapper mapper,
                           @Value("${app.seed.admin-password}") String passAdmin,
                           @Value("${app.seed.vendedor-password}") String passVendedor,
                           @Value("${app.seed.cliente-password}") String passCliente) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.productos = productos;
        this.encoder = encoder;
        this.mapper = mapper;
        this.passAdmin = passAdmin;
        this.passVendedor = passVendedor;
        this.passCliente = passCliente;
    }

    @Override
    public void run(String... args) throws Exception {
        if (usuarios.count() == 0) {
            crearUsuario("Administrador", "admin@prospin.cl", passAdmin, Rol.ADMIN);
            crearUsuario("Vendedor", "vendedor@prospin.cl", passVendedor, Rol.VENDEDOR);
            crearUsuario("Cliente demo", "cliente@prospin.cl", passCliente, Rol.CLIENTE);
        }

        if (productos.count() == 0) {
            cargarCatalogo();
        }
    }

    private void crearUsuario(String nombre, String email, String password, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRol(rol);
        usuarios.save(u);
    }

    private void cargarCatalogo() throws Exception {
        List<ProductoSemilla> lista;
        try (InputStream in = new ClassPathResource("data/productos.json").getInputStream()) {
            lista = mapper.readValue(in, new TypeReference<List<ProductoSemilla>>() {});
        }

        Map<String, Categoria> porNombre = new HashMap<>();
        for (ProductoSemilla s : lista) {
            Categoria categoria = porNombre.computeIfAbsent(s.categoria(), nombre ->
                    categorias.findByNombre(nombre).orElseGet(() -> categorias.save(new Categoria(nombre))));

            Producto p = new Producto();
            p.setCodigo(s.codigo());
            p.setNombre(s.nombre());
            p.setDescripcion(s.descripcion());
            p.setPrecio(s.precio());
            p.setStock(s.stock());
            p.setImagen(s.imagen());
            p.setCategoria(categoria);
            productos.save(p);
        }
    }

    // Formato de cada elemento de data/productos.json
    public record ProductoSemilla(
            String codigo,
            String nombre,
            String categoria,
            Integer precio,
            Integer stock,
            String descripcion,
            String imagen) {}
}

package cl.prospin.service;

import cl.prospin.dto.BoletaDto.ItemBoleta;
import cl.prospin.model.Boleta;
import cl.prospin.model.DetalleBoleta;
import cl.prospin.model.Producto;
import cl.prospin.model.Usuario;
import cl.prospin.repository.BoletaRepository;
import cl.prospin.repository.ProductoRepository;
import cl.prospin.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de las compras (boletas).
 *  - El precio se toma siempre de la base de datos, nunca del cliente.
 *  - Se valida el stock antes de descontarlo.
 *  - Al anular una boleta se devuelve el stock.
 */
@Service
public class BoletaService {

    private static final int MAX_UNIDADES_POR_PRODUCTO = 99;

    private final BoletaRepository boletas;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;

    public BoletaService(BoletaRepository boletas, ProductoRepository productos, UsuarioRepository usuarios) {
        this.boletas = boletas;
        this.productos = productos;
        this.usuarios = usuarios;
    }

    @Transactional
    public Boleta crear(String emailCliente, List<ItemBoleta> items) {
        Usuario usuario = usuarios.findByEmail(emailCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no válido"));

        // Si el mismo producto aparece dos veces, se suman las cantidades
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemBoleta item : items) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        Boleta boleta = new Boleta();
        boleta.setUsuario(usuario);
        boleta.setFecha(LocalDateTime.now());

        int total = 0;
        for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {
            int cantidad = entrada.getValue();
            if (cantidad > MAX_UNIDADES_POR_PRODUCTO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Máximo " + MAX_UNIDADES_POR_PRODUCTO + " unidades por producto");
            }

            Producto producto = productos.findById(entrada.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado: " + entrada.getKey()));

            if (producto.getStock() < cantidad) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para " + producto.getNombre() + ". Disponible: " + producto.getStock());
            }

            producto.setStock(producto.getStock() - cantidad);
            productos.save(producto);

            int subtotal = producto.getPrecio() * cantidad;
            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);
            boleta.agregarDetalle(detalle);

            total += subtotal;
        }

        boleta.setTotal(total);
        return boletas.save(boleta);
    }

    // ADMIN y VENDEDOR ven todas las boletas; el CLIENTE solo las suyas
    public List<Boleta> listar(String emailSolicitante, boolean verTodas) {
        return verTodas
                ? boletas.findAll(Sort.by(Sort.Direction.DESC, "fecha"))
                : boletas.findByUsuarioEmailOrderByFechaDesc(emailSolicitante);
    }

    public Boleta obtener(Long id, String emailSolicitante, boolean verTodas) {
        Boleta boleta = boletas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleta no encontrada"));
        // Un cliente que pide una boleta ajena recibe 404 para no revelar que existe
        if (!verTodas && !boleta.getUsuario().getEmail().equals(emailSolicitante)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleta no encontrada");
        }
        return boleta;
    }

    @Transactional
    public void anular(Long id) {
        Boleta boleta = boletas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleta no encontrada"));
        for (DetalleBoleta detalle : boleta.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productos.save(producto);
        }
        boletas.delete(boleta);
    }

    public static boolean esStaff(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_VENDEDOR"));
    }
}

package cl.prospin.service;

import cl.prospin.dto.CatalogoDto.ProductoRequest;
import cl.prospin.model.Categoria;
import cl.prospin.model.Producto;
import cl.prospin.repository.CategoriaRepository;
import cl.prospin.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {

    static final String CATEGORIA_POR_DEFECTO = "General";
    static final String IMAGEN_POR_DEFECTO = "img/productos/sin-foto.svg";

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public ProductoService(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    public List<Producto> listar() {
        return productos.findAll();
    }

    public Producto obtener(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Transactional
    public Producto crear(ProductoRequest req) {
        if (productos.existsByCodigo(req.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese código");
        }
        Producto p = new Producto();
        p.setCategoria(categoriaPorDefecto());
        p.setImagen(IMAGEN_POR_DEFECTO);
        aplicar(p, req);
        return productos.save(p);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest req) {
        Producto p = obtener(id);
        if (!req.codigo().equals(p.getCodigo()) && productos.existsByCodigo(req.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese código");
        }
        aplicar(p, req); // la categoría y la imagen se conservan
        return productos.save(p);
    }

    @Transactional
    public void eliminar(Long id) {
        productos.delete(obtener(id));
    }

    private Categoria categoriaPorDefecto() {
        return categorias.findByNombre(CATEGORIA_POR_DEFECTO)
                .orElseGet(() -> categorias.save(new Categoria(CATEGORIA_POR_DEFECTO)));
    }

    private void aplicar(Producto p, ProductoRequest req) {
        p.setCodigo(req.codigo().trim());
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
    }
}

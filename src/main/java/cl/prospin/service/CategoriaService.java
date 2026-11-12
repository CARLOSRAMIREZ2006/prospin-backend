package cl.prospin.service;

import cl.prospin.dto.CatalogoDto.CategoriaRequest;
import cl.prospin.model.Categoria;
import cl.prospin.repository.CategoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;

    public CategoriaService(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    public List<Categoria> listar() {
        return categorias.findAll();
    }

    public Categoria obtener(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    @Transactional
    public Categoria crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categorias.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya existe");
        }
        return categorias.save(new Categoria(nombre));
    }

    @Transactional
    public Categoria actualizar(Long id, CategoriaRequest req) {
        Categoria c = obtener(id);
        String nombre = req.nombre().trim();
        if (!nombre.equals(c.getNombre()) && categorias.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya existe");
        }
        c.setNombre(nombre);
        return categorias.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        // Si tiene productos asociados, la base de datos rechaza el borrado (ver GlobalExceptionHandler)
        categorias.delete(obtener(id));
    }
}

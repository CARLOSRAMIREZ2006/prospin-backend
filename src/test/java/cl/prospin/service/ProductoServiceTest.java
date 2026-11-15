package cl.prospin.service;

import cl.prospin.dto.CatalogoDto.ProductoRequest;
import cl.prospin.model.Categoria;
import cl.prospin.model.Producto;
import cl.prospin.repository.CategoriaRepository;
import cl.prospin.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock private ProductoRepository productos;
    @Mock private CategoriaRepository categorias;
    @InjectMocks private ProductoService service;

    private ProductoRequest req(String codigo, long categoriaId) {
        return new ProductoRequest(codigo, " Goma test ", "desc", 1500, 10, "img/productos/x.jpg", categoriaId);
    }

    @Test
    void crearAplicaCamposYCategoriaDelRequest() {
        Categoria cat = new Categoria("Gomas");
        when(productos.existsByCodigo("TT-900")).thenReturn(false);
        when(categorias.findById(3L)).thenReturn(Optional.of(cat));
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto p = service.crear(req("TT-900", 3L));

        assertEquals("TT-900", p.getCodigo());
        assertEquals("Goma test", p.getNombre());
        assertEquals(1500, p.getPrecio());
        assertEquals(10, p.getStock());
        assertSame(cat, p.getCategoria());
    }

    @Test
    void crearConCodigoRepetidoDevuelve409SinGuardar() {
        when(productos.existsByCodigo("TT-001")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(req("TT-001", 1L)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(productos, never()).save(any());
    }

    @Test
    void crearConCategoriaInexistenteDevuelve400() {
        when(productos.existsByCodigo("TT-901")).thenReturn(false);
        when(categorias.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(req("TT-901", 99L)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(productos, never()).save(any());
    }

    @Test
    void obtenerProductoInexistenteDevuelve404() {
        when(productos.findById(5L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.obtener(5L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}

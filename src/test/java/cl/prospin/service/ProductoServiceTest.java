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

    private ProductoRequest req(String codigo) {
        return new ProductoRequest(codigo, " Goma test ", "desc", 1500, 10);
    }

    @Test
    void crearUsaCategoriaGeneralEImagenPendiente() {
        Categoria general = new Categoria("General");
        when(productos.existsByCodigo("TT-900")).thenReturn(false);
        when(categorias.findByNombre("General")).thenReturn(Optional.of(general));
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto p = service.crear(req("TT-900"));

        assertEquals("TT-900", p.getCodigo());
        assertEquals("Goma test", p.getNombre());
        assertEquals(1500, p.getPrecio());
        assertEquals(10, p.getStock());
        assertSame(general, p.getCategoria());
        assertEquals("img/productos/sin-foto.svg", p.getImagen());
    }

    @Test
    void crearCreaLaCategoriaGeneralSiNoExiste() {
        when(productos.existsByCodigo("TT-901")).thenReturn(false);
        when(categorias.findByNombre("General")).thenReturn(Optional.empty());
        when(categorias.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto p = service.crear(req("TT-901"));

        assertEquals("General", p.getCategoria().getNombre());
    }

    @Test
    void crearConCodigoRepetidoDevuelve409SinGuardar() {
        when(productos.existsByCodigo("TT-001")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(req("TT-001")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(productos, never()).save(any());
    }

    @Test
    void obtenerProductoInexistenteDevuelve404() {
        when(productos.findById(5L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.obtener(5L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}

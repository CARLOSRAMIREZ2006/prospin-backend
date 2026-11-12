package cl.prospin.service;

import cl.prospin.dto.BoletaDto.ItemBoleta;
import cl.prospin.model.Boleta;
import cl.prospin.model.Categoria;
import cl.prospin.model.Producto;
import cl.prospin.model.Usuario;
import cl.prospin.repository.BoletaRepository;
import cl.prospin.repository.ProductoRepository;
import cl.prospin.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock private BoletaRepository boletas;
    @Mock private ProductoRepository productos;
    @Mock private UsuarioRepository usuarios;
    @InjectMocks private BoletaService service;

    private Usuario cliente() {
        Usuario u = new Usuario();
        u.setEmail("cliente@prospin.cl");
        return u;
    }

    private Producto producto(Long id, int precio, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Goma test");
        p.setPrecio(precio);
        p.setStock(stock);
        p.setCategoria(new Categoria("Gomas"));
        return p;
    }

    @Test
    void calculaTotalConPreciosDeBaseDeDatosYDescuentaStock() {
        Producto p = producto(1L, 1500, 20);
        when(usuarios.findByEmail("cliente@prospin.cl")).thenReturn(Optional.of(cliente()));
        when(productos.findById(1L)).thenReturn(Optional.of(p));
        when(boletas.save(any(Boleta.class))).thenAnswer(i -> i.getArgument(0));

        Boleta boleta = service.crear("cliente@prospin.cl", List.of(new ItemBoleta(1L, 2)));

        assertEquals(3000, boleta.getTotal().intValue());
        assertEquals(18, p.getStock().intValue());
        assertEquals(1, boleta.getDetalles().size());
        assertEquals(1500, boleta.getDetalles().get(0).getPrecioUnitario().intValue());
    }

    @Test
    void rechazaCompraSinStockSuficienteYNoDescuentaNada() {
        Producto p = producto(1L, 1500, 1);
        when(usuarios.findByEmail("cliente@prospin.cl")).thenReturn(Optional.of(cliente()));
        when(productos.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(ResponseStatusException.class,
                () -> service.crear("cliente@prospin.cl", List.of(new ItemBoleta(1L, 2))));

        assertEquals(1, p.getStock().intValue());
        verify(boletas, never()).save(any());
    }
}

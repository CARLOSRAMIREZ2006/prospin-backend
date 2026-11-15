package cl.prospin.service;

import cl.prospin.dto.CatalogoDto.CategoriaRequest;
import cl.prospin.model.Categoria;
import cl.prospin.repository.CategoriaRepository;
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
class CategoriaServiceTest {

    @Mock private CategoriaRepository categorias;
    @InjectMocks private CategoriaService service;

    @Test
    void crearGuardaNombreSinEspaciosExtra() {
        when(categorias.existsByNombre("Pelotas")).thenReturn(false);
        when(categorias.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        Categoria c = service.crear(new CategoriaRequest("  Pelotas  "));

        assertEquals("Pelotas", c.getNombre());
    }

    @Test
    void crearConNombreExistenteDevuelve409() {
        when(categorias.existsByNombre("Gomas")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(new CategoriaRequest("Gomas")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categorias, never()).save(any());
    }

    @Test
    void eliminarCategoriaInexistenteDevuelve404() {
        when(categorias.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.eliminar(7L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(categorias, never()).delete(any());
    }
}

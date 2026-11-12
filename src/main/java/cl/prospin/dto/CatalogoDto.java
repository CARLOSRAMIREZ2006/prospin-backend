package cl.prospin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CatalogoDto {

    private CatalogoDto() {}

    public record CategoriaRequest(
            @NotBlank @Size(max = 80) String nombre) {}

    public record ProductoRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 150) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull @Min(0) Integer precio,
            @NotNull @Min(0) Integer stock,
            @Size(max = 200) String imagen,
            @NotNull Long categoriaId) {}
}

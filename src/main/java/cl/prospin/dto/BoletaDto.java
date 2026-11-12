package cl.prospin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class BoletaDto {

    private BoletaDto() {}

    public record BoletaRequest(
            @NotEmpty List<@Valid ItemBoleta> items) {}

    public record ItemBoleta(
            @NotNull Long productoId,
            @NotNull @Min(1) @Max(99) Integer cantidad) {}
}

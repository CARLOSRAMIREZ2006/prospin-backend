package cl.prospin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RegistroRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record TokenResponse(
            String token,
            String tipo,
            long expiraEnSegundos,
            String rol) {}
}

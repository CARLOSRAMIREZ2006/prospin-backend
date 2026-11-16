package cl.prospin.config;

import cl.prospin.security.JwtAuthFilter;
import cl.prospin.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            // API sin sesiones: cada petición lleva su JWT, por eso CSRF no aplica
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Necesario para mostrar la consola H2 (solo desarrollo)
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // Públicos
                .requestMatchers("/api/v1/auth/registro", "/api/v1/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**", "/error").permitAll()

                // Retorno desde Webpay (el navegador llega sin token) e inicio de pago
                .requestMatchers("/api/v1/pagos/retorno").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/pagos/iniciar").hasAnyRole("ADMIN", "CLIENTE")

                // Renovar token: requiere un token válido
                .requestMatchers("/api/v1/auth/refresh").authenticated()

                // Catálogo: todos los roles pueden ver; solo ADMIN modifica
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**")
                    .hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                .requestMatchers("/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMIN")

                // Boletas: CLIENTE y ADMIN compran; ADMIN y VENDEDOR ven órdenes; solo ADMIN anula
                .requestMatchers(HttpMethod.POST, "/api/v1/boletas").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/boletas/**").hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                .requestMatchers("/api/v1/boletas/**").hasRole("ADMIN")

                // Gestión de usuarios: solo ADMIN
                .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")

                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origenes);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}

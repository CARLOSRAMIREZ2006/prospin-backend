package cl.prospin.repository;

import cl.prospin.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    List<Boleta> findByUsuarioEmailOrderByFechaDesc(String email);

    Optional<Boleta> findByTokenWebpay(String token);
}

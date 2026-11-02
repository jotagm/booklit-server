package github.jotagm.clube_livro.adapter.out.persistence.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface FalhaDeLoginJpaRepository extends JpaRepository<FalhaDeLoginEntity, UUID> {

    long countByEmailAndIpAndOcorridaEmGreaterThanEqual(String email, String ip, Instant desde);

    long countByEmailAndOcorridaEmGreaterThanEqual(String email, Instant desde);

    long countByIpAndOcorridaEmGreaterThanEqual(String ip, Instant desde);

    @Query("select min(f.ocorridaEm) from FalhaDeLoginEntity f where f.email = :email and f.ip = :ip and f.ocorridaEm >= :desde")
    Instant maisAntigaPorEmailEIp(@Param("email") String email, @Param("ip") String ip, @Param("desde") Instant desde);

    @Query("select min(f.ocorridaEm) from FalhaDeLoginEntity f where f.email = :email and f.ocorridaEm >= :desde")
    Instant maisAntigaPorEmail(@Param("email") String email, @Param("desde") Instant desde);

    @Query("select min(f.ocorridaEm) from FalhaDeLoginEntity f where f.ip = :ip and f.ocorridaEm >= :desde")
    Instant maisAntigaPorIp(@Param("ip") String ip, @Param("desde") Instant desde);

    @Modifying
    @Query("delete from FalhaDeLoginEntity f where f.email = :email")
    int apagarPorEmail(@Param("email") String email);

    @Modifying
    @Query("delete from FalhaDeLoginEntity f where f.ocorridaEm < :limite")
    int apagarAnterioresA(@Param("limite") Instant limite);
}

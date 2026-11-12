package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TokenRevocado_Repository extends JpaRepository<TokenRevocado, Integer> {

    /** Verdadero si el `jti` está en la lista de revocados. */
    boolean existsByJti(String jti);

    /**
     * Purga oportunista: borra las filas cuya expiración ya pasó (el token
     * correspondiente ya no es válido ni sin revocar). La llama
     * {@code TokenRevocadoService.revocar} en cada logout; no hay scheduler.
     */
    void deleteByExpiraEnBefore(LocalDateTime fecha);
}

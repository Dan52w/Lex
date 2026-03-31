package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Historial_Cambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialCambio_Repository extends JpaRepository<Historial_Cambio, Integer> {
    List<Historial_Cambio> findByCasoId(Integer casoId);
}

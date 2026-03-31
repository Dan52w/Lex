package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Documento_Repository extends JpaRepository<Documento, Integer> {
    List<Documento> findByCasoId(Integer casoId);
}

package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoAgenda_Repository extends JpaRepository<Tipo_Agenda, Integer> {
    List<Tipo_Agenda> findAllByNombre(String nombre);
    Optional<Tipo_Agenda> findByNombre(String nombre);
}

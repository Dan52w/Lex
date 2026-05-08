package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCliente_Repository extends JpaRepository<Tipo_Cliente, Integer> {
    List<Tipo_Cliente> findAllByNombre(String nombre);
    Optional<Tipo_Cliente> findByNombre(String nombre);
}

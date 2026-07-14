package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Tipo_Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoEmpleado_Repository extends JpaRepository<Tipo_Empleado, Integer> {
    List<Tipo_Empleado> findAllByNombre(String nombre);
    Optional<Tipo_Empleado> findByNombre(String nombre);
}

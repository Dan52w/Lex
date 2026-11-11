package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.AuditoriaSeguridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaSeguridad_Repository extends JpaRepository<AuditoriaSeguridad, Integer> {
}

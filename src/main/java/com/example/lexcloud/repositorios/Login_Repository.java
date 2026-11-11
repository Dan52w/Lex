package com.example.lexcloud.repositorios;

import com.example.lexcloud.entidades.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Login_Repository extends JpaRepository<Login, Integer> {

    // H5: se borró findByCorreo (sin chequeo de activo, muerto — ningún
    // llamador en src/main/java). La autenticación usa la variante segura
    // findByCorreoAndPersonaActiva (UserDetailsServiceImpl).

    // 2. Login con Validación de Seguridad (Soft Delete)
    // Solo permite loguearse si el correo coincide Y la persona está 'activa'
    @Query("SELECT l FROM Login l WHERE l.correo = :correo AND l.persona.activo = true")
    Optional<Login> findByCorreoAndPersonaActiva(@Param("correo") String correo);

    // 3. Verificar si un correo ya existe
    boolean existsByCorreo(String correo);
}

package com.example.incidencias.repository;

import com.example.incidencias.domain.Incidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long>, JpaSpecificationExecutor<Incidencia> {

    @Override
    @EntityGraph(attributePaths = "usuarioAsignado")
    java.util.List<Incidencia> findAll();

    @Override
    @EntityGraph(attributePaths = "usuarioAsignado")
    Optional<Incidencia> findById(Long id);
}

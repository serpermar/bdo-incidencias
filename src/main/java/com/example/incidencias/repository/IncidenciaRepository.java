package com.example.incidencias.repository;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Incidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    // EntityGraph evita el problema N+1 al pintar el usuario asignado en el listado.
    @EntityGraph(attributePaths = "usuarioAsignado")
    List<Incidencia> findAllByOrderByFechaCreacionDescIdDesc();

    @EntityGraph(attributePaths = "usuarioAsignado")
    List<Incidencia> findByEstadoOrderByFechaCreacionDescIdDesc(Estado estado);
}

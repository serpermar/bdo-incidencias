package com.example.incidencias.repository;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Incidencia;
import com.example.incidencias.domain.Prioridad;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class IncidenciaSpecification {

    private IncidenciaSpecification() {
    }

    public static Specification<Incidencia> filtrar(String q, Estado estado, Prioridad prioridad, Long usuarioId) {
        return (root, query, cb) -> {
            if (Incidencia.class.equals(query.getResultType())) {
                root.fetch("usuarioAsignado", jakarta.persistence.criteria.JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Predicate titulo = cb.like(cb.lower(root.get("titulo")), like);
                Predicate descripcion = cb.like(cb.lower(root.get("descripcion")), like);
                predicates.add(cb.or(titulo, descripcion));
            }

            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            if (prioridad != null) {
                predicates.add(cb.equal(root.get("prioridad"), prioridad));
            }

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuarioAsignado").get("id"), usuarioId));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

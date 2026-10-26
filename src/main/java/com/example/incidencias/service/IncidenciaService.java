package com.example.incidencias.service;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Incidencia;
import com.example.incidencias.domain.Usuario;
import com.example.incidencias.repository.IncidenciaRepository;
import com.example.incidencias.repository.UsuarioRepository;
import com.example.incidencias.service.exception.IncidenciaNoEncontradaException;
import com.example.incidencias.service.exception.TransicionEstadoInvalidaException;
import com.example.incidencias.service.exception.UsuarioNoEncontradoException;
import com.example.incidencias.web.dto.CrearIncidenciaRequest;
import com.example.incidencias.web.dto.IncidenciaResponse;
import com.example.incidencias.web.dto.UsuarioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class IncidenciaService {

    private final IncidenciaRepository incidencias;
    private final UsuarioRepository usuarios;

    public IncidenciaService(IncidenciaRepository incidencias, UsuarioRepository usuarios) {
        this.incidencias = incidencias;
        this.usuarios = usuarios;
    }

    @Transactional
    public IncidenciaResponse crear(CrearIncidenciaRequest peticion) {
        Usuario asignado = resolverUsuario(peticion.usuarioAsignadoId());
        Incidencia incidencia = new Incidencia(
                peticion.titulo().trim(),
                normalizarDescripcion(peticion.descripcion()),
                peticion.prioridad(),
                asignado
        );
        return IncidenciaResponse.from(incidencias.save(incidencia));
    }

    public List<IncidenciaResponse> listar(Estado estado) {
        List<Incidencia> resultado = estado == null
                ? incidencias.findAllByOrderByFechaCreacionDescIdDesc()
                : incidencias.findByEstadoOrderByFechaCreacionDescIdDesc(estado);

        return resultado.stream()
                .map(IncidenciaResponse::from)
                .toList();
    }

    /**
     * Regla de negocio: una incidencia CERRADA no puede reabrirse.
     * Volver a enviar CERRADA sobre una incidencia ya cerrada es idempotente (no es reabrir).
     */
    @Transactional
    public IncidenciaResponse cambiarEstado(Long id, Estado nuevoEstado) {
        Incidencia incidencia = incidencias.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));

        boolean estaCerrada = incidencia.getEstado() == Estado.CERRADA;
        if (estaCerrada && nuevoEstado != Estado.CERRADA) {
            throw new TransicionEstadoInvalidaException(id);
        }

        incidencia.setEstado(nuevoEstado);
        return IncidenciaResponse.from(incidencia);
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarios.findAllByOrderByNombreAsc().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    private Usuario resolverUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));
    }

    private static String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String limpia = descripcion.trim();
        return limpia.isEmpty() ? null : limpia;
    }
}

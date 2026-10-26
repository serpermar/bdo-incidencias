package com.example.incidencias.web;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Prioridad;
import com.example.incidencias.service.IncidenciaService;
import com.example.incidencias.web.dto.CambiarEstadoRequest;
import com.example.incidencias.web.dto.CrearIncidenciaRequest;
import com.example.incidencias.web.dto.IncidenciaResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    private final IncidenciaService servicio;

    public IncidenciaController(IncidenciaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SOPORTE','JEFE')")
    public IncidenciaResponse crear(@Valid @RequestBody CrearIncidenciaRequest peticion) {
        return servicio.crear(peticion);
    }

    @GetMapping
    public Page<IncidenciaResponse> listar(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Estado estado,
                                          @RequestParam(required = false) Prioridad prioridad,
                                          @RequestParam(required = false) Long usuarioId,
                                          @PageableDefault(sort = {"fechaCreacion", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return servicio.listar(q, estado, prioridad, usuarioId, pageable);
    }

    @GetMapping(params = "!page")
    public List<IncidenciaResponse> listarSinPagina(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Estado estado,
                                                   @RequestParam(required = false) Prioridad prioridad,
                                                   @RequestParam(required = false) Long usuarioId) {
        return servicio.listar(q, estado, prioridad, usuarioId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE,
                        org.springframework.data.domain.Sort.by(Sort.Direction.DESC, "fechaCreacion", "id"))).getContent();
    }

    @PatchMapping("/{id}/estado")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SOPORTE','JEFE')")
    public IncidenciaResponse cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody CambiarEstadoRequest peticion) {
        return servicio.cambiarEstado(id, peticion.estado());
    }
}

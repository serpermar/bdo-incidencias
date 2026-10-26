package com.example.incidencias.web;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.service.IncidenciaService;
import com.example.incidencias.web.dto.CambiarEstadoRequest;
import com.example.incidencias.web.dto.CrearIncidenciaRequest;
import com.example.incidencias.web.dto.IncidenciaResponse;
import jakarta.validation.Valid;
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
    public IncidenciaResponse crear(@Valid @RequestBody CrearIncidenciaRequest peticion) {
        return servicio.crear(peticion);
    }

    @GetMapping
    public List<IncidenciaResponse> listar(@RequestParam(required = false) Estado estado) {
        return servicio.listar(estado);
    }

    @PatchMapping("/{id}/estado")
    public IncidenciaResponse cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody CambiarEstadoRequest peticion) {
        return servicio.cambiarEstado(id, peticion.estado());
    }
}

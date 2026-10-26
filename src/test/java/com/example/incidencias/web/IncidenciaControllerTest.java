package com.example.incidencias.web;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Prioridad;
import com.example.incidencias.service.IncidenciaService;
import com.example.incidencias.service.exception.IncidenciaNoEncontradaException;
import com.example.incidencias.service.exception.TransicionEstadoInvalidaException;
import com.example.incidencias.web.dto.IncidenciaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa web: contracto HTTP (códigos y JSON) sin levantar la base de datos.
 */
@WebMvcTest(IncidenciaController.class)
@Import(com.example.incidencias.config.SeguridadConfig.class)
@WithMockUser(username = "test", roles = "SOPORTE")
class IncidenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidenciaService servicio;

    @Test
    void post_valido_devuelve201ConLaIncidenciaCreada() throws Exception {
        when(servicio.crear(any())).thenReturn(new IncidenciaResponse(
                1L, "No arranca la VPN", "Error 619", Prioridad.ALTA, Estado.ABIERTA,
                LocalDateTime.of(2026, 10, 26, 10, 0), null, null, null, null, null));

        mockMvc.perform(post("/api/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"No arranca la VPN","descripcion":"Error 619","prioridad":"ALTA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("No arranca la VPN"))
                .andExpect(jsonPath("$.estado").value("ABIERTA"));
    }

    @Test
    void post_conTituloVacioYPrioridadNula_devuelve400ConFormatoComun() throws Exception {
        mockMvc.perform(post("/api/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Los datos enviados no son válidos"))
                .andExpect(jsonPath("$.path").value("/api/incidencias"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.detalles").isNotEmpty());

        verifyNoInteractions(servicio);
    }

    @Test
    void post_conTituloDeMasDe100Caracteres_devuelve400() throws Exception {
        String tituloLargo = "x".repeat(101);

        mockMvc.perform(post("/api/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"%s","prioridad":"MEDIA"}
                                """.formatted(tituloLargo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(
                        org.hamcrest.Matchers.containsString("100 caracteres")));
    }

    @Test
    void post_conPrioridadInvalida_devuelve400() throws Exception {
        mockMvc.perform(post("/api/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Título","prioridad":"URGENTE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void get_sinFiltro_devuelveListado() throws Exception {
        org.springframework.data.domain.PageImpl<IncidenciaResponse> pagina =
                new org.springframework.data.domain.PageImpl<>(List.of(
                        new IncidenciaResponse(1L, "Uno", null, Prioridad.BAJA, Estado.ABIERTA,
                                LocalDateTime.of(2026, 10, 26, 9, 0), null, null, null, null, null)));
        when(servicio.listar(any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/incidencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("ABIERTA"));
    }

    @Test
    void get_conFiltroDeEstado_loPasaAlServicio() throws Exception {
        org.springframework.data.domain.PageImpl<IncidenciaResponse> pagina =
                new org.springframework.data.domain.PageImpl<>(List.of());
        when(servicio.listar(any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);

        mockMvc.perform(get("/api/incidencias").param("estado", "CERRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(servicio).listar(org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(Estado.CERRADA),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void get_conFiltroInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/incidencias").param("estado", "PENDIENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("PENDIENTE")));
    }

    @Test
    void patch_sobreIncidenciaCerrada_devuelve409() throws Exception {
        when(servicio.cambiarEstado(5L, Estado.ABIERTA))
                .thenThrow(new TransicionEstadoInvalidaException(5L));

        mockMvc.perform(patch("/api/incidencias/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"ABIERTA"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("no puede reabrirse")));
    }

    @Test
    void patch_sobreIncidenciaInexistente_devuelve404() throws Exception {
        when(servicio.cambiarEstado(99L, Estado.EN_CURSO))
                .thenThrow(new IncidenciaNoEncontradaException(99L));

        mockMvc.perform(patch("/api/incidencias/99/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"EN_CURSO"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("99")));
    }

    @Test
    void patch_sinEstadoEnElCuerpo_devuelve400() throws Exception {
        mockMvc.perform(patch("/api/incidencias/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(
                        org.hamcrest.Matchers.containsString("estado")));
    }
}

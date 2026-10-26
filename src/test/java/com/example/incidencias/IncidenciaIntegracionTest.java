package com.example.incidencias;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Prioridad;
import com.example.incidencias.repository.IncidenciaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidenciaIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IncidenciaRepository incidencias;

    @Test
    @WithMockUser(username = "soporte", roles = "SOPORTE")
    void flujoCompleto_crearFiltrarYCambiarEstado() throws Exception {
        String cuerpo = """
                {"titulo":"Prueba integracion","descripcion":"Test completo","prioridad":"ALTA","categoria":"SOFTWARE"}
                """;

        MvcResult resultadoCrear = mockMvc.perform(post("/api/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.categoria").value("SOFTWARE"))
                .andReturn();

        JsonNode json = objectMapper.readTree(resultadoCrear.getResponse().getContentAsString());
        long id = json.get("id").asLong();

        mockMvc.perform(get("/api/incidencias").param("q", "integracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) id));

        mockMvc.perform(patch("/api/incidencias/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CURSO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        incidencias.findById(id).ifPresent(i -> {
            assertThat(i.getAuditadoPor()).isEqualTo("soporte");
            assertThat(i.getFechaUltimaModificacionEstado()).isNotNull();
        });
    }

    @Test
    @WithMockUser(username = "soporte", roles = "SOPORTE")
    void reglaIncidenciaCerradaDevuelve409() throws Exception {
        mockMvc.perform(patch("/api/incidencias/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ABIERTA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("no puede reabrirse")));
    }
}

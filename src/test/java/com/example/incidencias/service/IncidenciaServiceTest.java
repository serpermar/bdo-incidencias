package com.example.incidencias.service;

import com.example.incidencias.domain.Estado;
import com.example.incidencias.domain.Incidencia;
import com.example.incidencias.domain.Prioridad;
import com.example.incidencias.domain.Usuario;
import com.example.incidencias.repository.IncidenciaRepository;
import com.example.incidencias.repository.UsuarioRepository;
import com.example.incidencias.service.exception.IncidenciaNoEncontradaException;
import com.example.incidencias.service.exception.TransicionEstadoInvalidaException;
import com.example.incidencias.service.exception.UsuarioNoEncontradoException;
import com.example.incidencias.web.dto.CrearIncidenciaRequest;
import com.example.incidencias.web.dto.IncidenciaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidencias;

    @Mock
    private UsuarioRepository usuarios;

    @InjectMocks
    private IncidenciaService servicio;

    @Test
    void alCrear_laIncidenciaNaceAbiertaConFechaYSeNormalizaElTitulo() {
        CrearIncidenciaRequest peticion = new CrearIncidenciaRequest(
                "  No arranca la VPN  ", "Error 619 al conectar", Prioridad.ALTA, null);
        when(incidencias.save(any(Incidencia.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        IncidenciaResponse respuesta = servicio.crear(peticion);

        ArgumentCaptor<Incidencia> capturada = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidencias).save(capturada.capture());

        assertThat(capturada.getValue().getTitulo()).isEqualTo("No arranca la VPN");
        assertThat(capturada.getValue().getDescripcion()).isEqualTo("Error 619 al conectar");
        assertThat(capturada.getValue().getEstado()).isEqualTo(Estado.ABIERTA);
        assertThat(capturada.getValue().getFechaCreacion()).isNotNull();
        assertThat(respuesta.estado()).isEqualTo(Estado.ABIERTA);
        assertThat(respuesta.usuarioAsignado()).isNull();
    }

    @Test
    void alCrear_conUsuarioAsignado_loResuelveYLoDevuelve() {
        Usuario ana = new Usuario("Ana García", "ana@example.com");
        when(usuarios.findById(1L)).thenReturn(Optional.of(ana));
        when(incidencias.save(any(Incidencia.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        IncidenciaResponse respuesta = servicio.crear(
                new CrearIncidenciaRequest("Título", null, Prioridad.MEDIA, 1L));

        assertThat(respuesta.usuarioAsignado()).isNotNull();
        assertThat(respuesta.usuarioAsignado().nombre()).isEqualTo("Ana García");
    }

    @Test
    void alCrear_conUsuarioInexistente_lanzaUsuarioNoEncontrado() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(
                new CrearIncidenciaRequest("Título", null, Prioridad.BAJA, 99L)))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .hasMessageContaining("99");

        verify(incidencias, never()).save(any(Incidencia.class));
    }

    @Test
    void unaIncidenciaCerradaNoPuedeReabrirse() {
        Incidencia cerrada = incidencia(Estado.CERRADA);
        when(incidencias.findById(7L)).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> servicio.cambiarEstado(7L, Estado.ABIERTA))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("CERRADA")
                .hasMessageContaining("no puede reabrirse");

        assertThat(cerrada.getEstado()).isEqualTo(Estado.CERRADA);
    }

    @Test
    void unaIncidenciaCerradaNoPuedePasarAEnCurso() {
        Incidencia cerrada = incidencia(Estado.CERRADA);
        when(incidencias.findById(7L)).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> servicio.cambiarEstado(7L, Estado.EN_CURSO))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void volverAEnviarCerradaSobreUnaCerradaEsIdempotente() {
        Incidencia cerrada = incidencia(Estado.CERRADA);
        when(incidencias.findById(7L)).thenReturn(Optional.of(cerrada));

        IncidenciaResponse respuesta = servicio.cambiarEstado(7L, Estado.CERRADA);

        assertThat(respuesta.estado()).isEqualTo(Estado.CERRADA);
    }

    @Test
    void cambiarEstado_deUnaIncidenciaInexistente_lanzaNoEncontrada() {
        when(incidencias.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarEstado(123L, Estado.EN_CURSO))
                .isInstanceOf(IncidenciaNoEncontradaException.class)
                .hasMessageContaining("123");
    }

    @Test
    void cambiarEstado_deAbiertaAEnCurso_actualizaElEstado() {
        Incidencia abierta = incidencia(Estado.ABIERTA);
        when(incidencias.findById(1L)).thenReturn(Optional.of(abierta));

        IncidenciaResponse respuesta = servicio.cambiarEstado(1L, Estado.EN_CURSO);

        assertThat(respuesta.estado()).isEqualTo(Estado.EN_CURSO);
        assertThat(abierta.getEstado()).isEqualTo(Estado.EN_CURSO);
    }

    @Test
    void listar_sinFiltro_usaLaConsultaOrdenadaCompleta() {
        when(incidencias.findAllByOrderByFechaCreacionDescIdDesc())
                .thenReturn(List.of(incidencia(Estado.ABIERTA), incidencia(Estado.CERRADA)));

        List<IncidenciaResponse> resultado = servicio.listar(null);

        assertThat(resultado).hasSize(2);
        verify(incidencias, never()).findByEstadoOrderByFechaCreacionDescIdDesc(any());
    }

    @Test
    void listar_conFiltro_usaLaConsultaPorEstado() {
        when(incidencias.findByEstadoOrderByFechaCreacionDescIdDesc(Estado.CERRADA))
                .thenReturn(List.of(incidencia(Estado.CERRADA)));

        List<IncidenciaResponse> resultado = servicio.listar(Estado.CERRADA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estado()).isEqualTo(Estado.CERRADA);
        verify(incidencias, never()).findAllByOrderByFechaCreacionDescIdDesc();
    }

    private static Incidencia incidencia(Estado estado) {
        Incidencia incidencia = new Incidencia("Incidencia de prueba", "Descripción", Prioridad.MEDIA, null);
        incidencia.setEstado(estado);
        return incidencia;
    }
}

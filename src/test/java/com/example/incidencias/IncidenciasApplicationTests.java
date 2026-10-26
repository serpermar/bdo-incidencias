package com.example.incidencias;

import com.example.incidencias.repository.IncidenciaRepository;
import com.example.incidencias.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que la aplicación arranca y que schema.sql crea las tablas
 * y carga los datos de ejemplo.
 */
@SpringBootTest
class IncidenciasApplicationTests {

    @Autowired
    private IncidenciaRepository incidencias;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    void elContextoArrancaYLasMigracionesCarganLosDatosDeEjemplo() {
        assertThat(usuarios.count()).isEqualTo(3);
        assertThat(incidencias.count()).isGreaterThanOrEqualTo(5);
        assertThat(incidencias.findAll()).anyMatch(i -> i.getEstado().name().equals("CERRADA"));
    }
}

(() => {
    'use strict';

    const API_INCIDENCIAS = '/api/incidencias';
    const API_USUARIOS = '/api/usuarios';

    const ETIQUETAS_ESTADO = {
        ABIERTA: 'Abierta',
        EN_CURSO: 'En curso',
        CERRADA: 'Cerrada'
    };

    const ETIQUETAS_PRIORIDAD = {
        BAJA: 'Baja',
        MEDIA: 'Media',
        ALTA: 'Alta'
    };

    const formateadorFecha = new Intl.DateTimeFormat('es-ES', {
        dateStyle: 'short',
        timeStyle: 'short'
    });

    const el = {
        cuerpoTabla: document.getElementById('cuerpoTabla'),
        filtroEstado: document.getElementById('filtroEstado'),
        btnRecargar: document.getElementById('btnRecargar'),
        contador: document.getElementById('contador'),
        mensajeError: document.getElementById('mensajeError'),
        textoError: document.getElementById('textoError'),
        btnCerrarError: document.getElementById('btnCerrarError'),
        cargando: document.getElementById('cargando'),
        sinResultados: document.getElementById('sinResultados'),
        dialogo: document.getElementById('dialogoNueva'),
        btnNueva: document.getElementById('btnNueva'),
        btnCancelar: document.getElementById('btnCancelar'),
        formNueva: document.getElementById('formNueva'),
        errorFormulario: document.getElementById('errorFormulario'),
        btnGuardar: document.getElementById('btnGuardar'),
        titulo: document.getElementById('titulo'),
        descripcion: document.getElementById('descripcion'),
        prioridad: document.getElementById('prioridad'),
        usuarioAsignado: document.getElementById('usuarioAsignado')
    };

    function iniciar() {
        el.filtroEstado.addEventListener('change', cargarIncidencias);
        el.btnRecargar.addEventListener('click', cargarIncidencias);
        el.btnCerrarError.addEventListener('click', ocultarError);
        el.btnNueva.addEventListener('click', abrirFormulario);
        el.btnCancelar.addEventListener('click', () => el.dialogo.close());
        el.formNueva.addEventListener('submit', enviarFormulario);

        // Al cerrar con Escape también limpiamos el error del formulario.
        el.dialogo.addEventListener('close', limpiarErrorFormulario);

        cargarIncidencias();
        cargarUsuarios();
    }

    // ---------------------------------------------------------------------
    // Listado
    // ---------------------------------------------------------------------
    async function cargarIncidencias() {
        ocultarError();
        mostrarCargando(true);

        const estado = el.filtroEstado.value;
        const url = estado
            ? `${API_INCIDENCIAS}?estado=${encodeURIComponent(estado)}`
            : API_INCIDENCIAS;

        try {
            const respuesta = await fetch(url, {headers: {Accept: 'application/json'}});
            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const incidencias = await respuesta.json();
            pintarTabla(incidencias);
        } catch (error) {
            pintarTabla([]);
            mostrarError(`No se pudieron cargar las incidencias. ${mensajeDeFallo(error)}`);
            el.contador.textContent = '';
        } finally {
            mostrarCargando(false);
        }
    }

    function pintarTabla(incidencias) {
        el.cuerpoTabla.replaceChildren();

        incidencias.forEach((incidencia) => {
            el.cuerpoTabla.appendChild(crearFila(incidencia));
        });

        el.sinResultados.hidden = incidencias.length > 0;
        el.contador.textContent = incidencias.length === 1
            ? '1 incidencia'
            : `${incidencias.length} incidencias`;
    }

    function crearFila(incidencia) {
        const fila = document.createElement('tr');

        // Id
        const celdaId = document.createElement('td');
        celdaId.className = 'celda-id';
        celdaId.textContent = `#${incidencia.id}`;

        // Título + descripción
        const celdaTitulo = document.createElement('td');
        celdaTitulo.className = 'celda-titulo';
        celdaTitulo.textContent = incidencia.titulo;
        if (incidencia.descripcion) {
            const descripcion = document.createElement('span');
            descripcion.className = 'celda-descripcion';
            descripcion.textContent = incidencia.descripcion;
            celdaTitulo.appendChild(descripcion);
        }

        // Prioridad
        const celdaPrioridad = document.createElement('td');
        celdaPrioridad.appendChild(crearEtiqueta(
            ETIQUETAS_PRIORIDAD[incidencia.prioridad] || incidencia.prioridad,
            `etiqueta--${String(incidencia.prioridad).toLowerCase()}`
        ));

        // Estado
        const celdaEstado = document.createElement('td');
        celdaEstado.appendChild(crearEtiqueta(
            ETIQUETAS_ESTADO[incidencia.estado] || incidencia.estado,
            `etiqueta--${String(incidencia.estado).toLowerCase()}`
        ));

        // Fecha
        const celdaFecha = document.createElement('td');
        celdaFecha.textContent = formatearFecha(incidencia.fechaCreacion);

        // Usuario asignado
        const celdaUsuario = document.createElement('td');
        celdaUsuario.textContent = incidencia.usuarioAsignado
            ? incidencia.usuarioAsignado.nombre
            : 'Sin asignar';

        // Acciones
        const celdaAcciones = document.createElement('td');
        celdaAcciones.appendChild(crearAcciones(incidencia));

        fila.append(celdaId, celdaTitulo, celdaPrioridad, celdaEstado, celdaFecha, celdaUsuario, celdaAcciones);
        return fila;
    }

    function crearEtiqueta(texto, clase) {
        const etiqueta = document.createElement('span');
        etiqueta.className = `etiqueta ${clase}`;
        etiqueta.textContent = texto;
        return etiqueta;
    }

    function crearAcciones(incidencia) {
        const contenedor = document.createElement('div');

        if (incidencia.estado === 'ABIERTA') {
            contenedor.appendChild(botonCambioEstado(incidencia, 'EN_CURSO', 'Pasar a en curso'));
        } else if (incidencia.estado === 'EN_CURSO') {
            contenedor.appendChild(botonCambioEstado(incidencia, 'CERRADA', 'Cerrar'));
        } else {
            const candado = document.createElement('span');
            candado.className = 'celda-descripcion';
            candado.textContent = 'Cerrada';
            contenedor.appendChild(candado);
        }

        return contenedor;
    }

    function botonCambioEstado(incidencia, nuevoEstado, texto) {
        const boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'boton boton--enlace';
        boton.textContent = texto;
        boton.addEventListener('click', () => cambiarEstado(incidencia, nuevoEstado, boton));
        return boton;
    }

    async function cambiarEstado(incidencia, nuevoEstado, boton) {
        ocultarError();
        boton.disabled = true;

        try {
            const respuesta = await fetch(`${API_INCIDENCIAS}/${incidencia.id}/estado`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json'
                },
                body: JSON.stringify({estado: nuevoEstado})
            });

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            await cargarIncidencias();
        } catch (error) {
            mostrarError(`No se pudo cambiar el estado de la incidencia #${incidencia.id}. ${mensajeDeFallo(error)}`);
            boton.disabled = false;
        }
    }

    // ---------------------------------------------------------------------
    // Alta de incidencias (opcional)
    // ---------------------------------------------------------------------
    function abrirFormulario() {
        limpiarErrorFormulario();
        el.dialogo.showModal();
        el.titulo.focus();
    }

    async function enviarFormulario(evento) {
        evento.preventDefault();
        limpiarErrorFormulario();

        const titulo = el.titulo.value.trim();
        if (!titulo) {
            mostrarErrorFormulario('El título es obligatorio.');
            el.titulo.focus();
            return;
        }

        const cuerpo = {
            titulo,
            descripcion: el.descripcion.value.trim() || null,
            prioridad: el.prioridad.value
        };

        if (el.usuarioAsignado.value) {
            cuerpo.usuarioAsignadoId = Number(el.usuarioAsignado.value);
        }

        guardando(true);
        try {
            const respuesta = await fetch(API_INCIDENCIAS, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json'
                },
                body: JSON.stringify(cuerpo)
            });

            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            el.dialogo.close();
            el.formNueva.reset();
            el.prioridad.value = 'MEDIA';
            el.filtroEstado.value = '';
            await cargarIncidencias();
        } catch (error) {
            mostrarErrorFormulario(`No se pudo crear la incidencia. ${mensajeDeFallo(error)}`);
        } finally {
            guardando(false);
        }
    }

    async function cargarUsuarios() {
        try {
            const respuesta = await fetch(API_USUARIOS, {headers: {Accept: 'application/json'}});
            if (!respuesta.ok) {
                throw new Error(await leerError(respuesta));
            }

            const usuarios = await respuesta.json();
            usuarios.forEach((usuario) => {
                const opcion = document.createElement('option');
                opcion.value = usuario.id;
                opcion.textContent = `${usuario.nombre} (${usuario.email})`;
                el.usuarioAsignado.appendChild(opcion);
            });
        } catch (error) {
            // No es bloqueante: se puede crear una incidencia sin asignar.
            console.warn('No se pudieron cargar los usuarios:', mensajeDeFallo(error));
        }
    }

    // ---------------------------------------------------------------------
    // Utilidades de UI y errores
    // ---------------------------------------------------------------------
    function mostrarCargando(visible) {
        el.cargando.hidden = !visible;
    }

    function mostrarError(mensaje) {
        el.textoError.textContent = mensaje;
        el.mensajeError.hidden = false;
    }

    function ocultarError() {
        el.mensajeError.hidden = true;
        el.textoError.textContent = '';
    }

    function mostrarErrorFormulario(mensaje) {
        el.errorFormulario.textContent = mensaje;
        el.errorFormulario.hidden = false;
    }

    function limpiarErrorFormulario() {
        el.errorFormulario.hidden = true;
        el.errorFormulario.textContent = '';
    }

    function guardando(activo) {
        el.btnGuardar.disabled = activo;
        el.btnGuardar.textContent = activo ? 'Creando…' : 'Crear incidencia';
    }

    /**
     * Convierte una respuesta HTTP de error en un texto legible usando el
     * formato JSON común de la API ({message, detalles[]}).
     */
    async function leerError(respuesta) {
        try {
            const cuerpo = await respuesta.json();
            if (cuerpo && cuerpo.message) {
                if (Array.isArray(cuerpo.detalles) && cuerpo.detalles.length > 0) {
                    return `${cuerpo.message}: ${cuerpo.detalles.join(' · ')}`;
                }
                return cuerpo.message;
            }
        } catch (error) {
            // La respuesta no era JSON; usamos el código HTTP.
        }

        return `Error ${respuesta.status} ${respuesta.statusText || ''}`.trim();
    }

    function mensajeDeFallo(error) {
        if (error instanceof TypeError) {
            return 'No se pudo conectar con el servidor. Comprueba que la aplicación está en marcha.';
        }
        return error.message || 'Error inesperado.';
    }

    function formatearFecha(iso) {
        const fecha = new Date(iso);
        return Number.isNaN(fecha.getTime()) ? iso : formateadorFecha.format(fecha);
    }

    iniciar();
})();

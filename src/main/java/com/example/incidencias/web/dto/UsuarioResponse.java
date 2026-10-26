package com.example.incidencias.web.dto;

import com.example.incidencias.domain.Usuario;

public record UsuarioResponse(Long id, String nombre, String email) {

    public static UsuarioResponse from(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}

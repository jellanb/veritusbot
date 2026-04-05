package com.example.veritusbot.service;

import com.example.veritusbot.dto.UpdateUsuarioRequestDTO;
import com.example.veritusbot.dto.UsuarioLoginOptionDTO;
import com.example.veritusbot.exception.UsuarioNoEncontradoException;
import com.example.veritusbot.model.EstadoUsuario;
import com.example.veritusbot.model.RolUsuario;
import com.example.veritusbot.model.Usuario;
import com.example.veritusbot.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void listarUsuariosParaLoginShouldReturnLightweightDataAndActiveFlag() {
        Usuario ana = new Usuario("ana@example.com", "hash", "Ana Perez", RolUsuario.VIEWER);
        ana.setEstado(EstadoUsuario.ACTIVO);

        Usuario bruno = new Usuario("bruno@example.com", "hash", "Bruno Diaz", RolUsuario.OPERADOR);
        bruno.setEstado(EstadoUsuario.INACTIVO);

        when(usuarioRepository.findAllByOrderByNombreCompletoAsc())
                .thenReturn(List.of(ana, bruno));

        List<UsuarioLoginOptionDTO> resultado = usuarioService.listarUsuariosParaLogin();

        assertEquals(2, resultado.size());
        assertEquals("ana@example.com", resultado.get(0).getEmail());
        assertEquals("Ana Perez", resultado.get(0).getNombreCompleto());
        assertTrue(resultado.get(0).isActivo());
        assertEquals("bruno@example.com", resultado.get(1).getEmail());
        assertFalse(resultado.get(1).isActivo());

        verify(usuarioRepository, times(1)).findAllByOrderByNombreCompletoAsc();
    }

    @Test
    void editarUsuarioShouldUpdateNombreRolAndEstado() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("maria@example.com", "hash", "Maria Lopez", RolUsuario.VIEWER);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        UpdateUsuarioRequestDTO request = new UpdateUsuarioRequestDTO();
        request.setNombreCompleto("Maria Fernanda Lopez");
        request.setRol("OPERADOR");
        request.setEstado("BLOQUEADO");

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var actualizado = usuarioService.editarUsuario(id, request);

        assertEquals("Maria Fernanda Lopez", actualizado.getNombreCompleto());
        assertEquals("OPERADOR", actualizado.getRol());
        assertEquals("BLOQUEADO", actualizado.getEstado());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void desactivarUsuarioShouldSetEstadoInactivo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("luis@example.com", "hash", "Luis Perez", RolUsuario.OPERADOR);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = usuarioService.desactivarUsuario(id);

        assertEquals("INACTIVO", resultado.getEstado());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void eliminarUsuarioShouldDeleteWhenExists() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("ana@example.com", "hash", "Ana Perez", RolUsuario.ADMIN);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        doNothing().when(usuarioRepository).delete(usuario);

        usuarioService.eliminarUsuario(id);

        verify(usuarioRepository, times(1)).delete(usuario);
    }

    @Test
    void eliminarUsuarioShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.eliminarUsuario(id));
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }
}


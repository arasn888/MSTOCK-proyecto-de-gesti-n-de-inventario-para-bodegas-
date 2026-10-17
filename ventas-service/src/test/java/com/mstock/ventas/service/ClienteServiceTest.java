package com.mstock.ventas.service;

import com.mstock.ventas.dto.ClienteRequest;
import com.mstock.ventas.entity.Cliente;
import com.mstock.ventas.exception.RecursoDuplicadoException;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;
    @InjectMocks
    private ClienteService service;

    @Test
    @DisplayName("Crear: rechaza un documento ya registrado")
    void crearRechazaDocumentoDuplicado() {
        when(repository.existsByDocumento("12345678")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new ClienteRequest("12345678", "Ana", null, null)))
                .isInstanceOf(RecursoDuplicadoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Eliminar: es una baja lógica, el cliente queda inactivo")
    void eliminarEsBajaLogica() {
        Cliente cliente = Cliente.builder().id(1L).nombres("Ana").build();
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));

        service.eliminar(1L);

        assertThat(cliente.isActivo()).isFalse();
        verify(repository).saveAndFlush(cliente);
    }

    @Test
    @DisplayName("Para vender: un cliente inactivo se rechaza")
    void clienteInactivoNoSirveParaVender() {
        Cliente cliente = Cliente.builder().id(1L).nombres("Ana").activo(false).build();
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.buscarActivo(1L)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Obtener: un id inexistente da 404")
    void obtenerInexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}

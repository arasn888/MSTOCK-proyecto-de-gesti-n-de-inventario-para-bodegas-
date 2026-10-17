package com.mstock.ventas.service;

import com.mstock.ventas.dto.ClienteRequest;
import com.mstock.ventas.dto.ClienteResponse;
import com.mstock.ventas.entity.Cliente;
import com.mstock.ventas.exception.RecursoDuplicadoException;
import com.mstock.ventas.exception.RecursoNoEncontradoException;
import com.mstock.ventas.exception.ReglaNegocioException;
import com.mstock.ventas.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String nombre) {
        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepository.findByActivoTrueOrderByNombresAsc()
                : clienteRepository.findByActivoTrueAndNombresContainingIgnoreCaseOrderByNombresAsc(nombre.trim());
        return clientes.stream().map(ClienteResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return ClienteResponse.desde(buscar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String documento = limpiar(request.documento());
        if (documento != null && clienteRepository.existsByDocumento(documento)) {
            throw new RecursoDuplicadoException("Ya existe un cliente con el documento " + documento);
        }
        Cliente cliente = Cliente.builder()
                .documento(documento)
                .nombres(request.nombres().trim())
                .telefono(limpiar(request.telefono()))
                .email(limpiar(request.email()))
                .build();
        return ClienteResponse.desde(clienteRepository.saveAndFlush(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        String documento = limpiar(request.documento());
        if (documento != null && clienteRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new RecursoDuplicadoException("Ya existe otro cliente con el documento " + documento);
        }
        cliente.setDocumento(documento);
        cliente.setNombres(request.nombres().trim());
        cliente.setTelefono(limpiar(request.telefono()));
        cliente.setEmail(limpiar(request.email()));
        return ClienteResponse.desde(clienteRepository.saveAndFlush(cliente));
    }

    /** Baja lógica: las ventas pasadas siguen apuntando al cliente. */
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        if (cliente.isActivo()) {
            cliente.setActivo(false);
            clienteRepository.saveAndFlush(cliente);
        }
    }

    /** Cliente para usar en una venta: debe existir y estar activo. */
    @Transactional(readOnly = true)
    public Cliente buscarActivo(Long id) {
        Cliente cliente = buscar(id);
        if (!cliente.isActivo()) {
            throw new ReglaNegocioException("El cliente indicado está inactivo");
        }
        return cliente;
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con id " + id));
    }

    private String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}

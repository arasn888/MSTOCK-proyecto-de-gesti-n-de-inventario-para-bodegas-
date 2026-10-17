package com.mstock.catalogo.service;

import com.mstock.catalogo.dto.ProveedorRequest;
import com.mstock.catalogo.dto.ProveedorResponse;
import com.mstock.catalogo.entity.Proveedor;
import com.mstock.catalogo.exception.OperacionNoPermitidaException;
import com.mstock.catalogo.exception.RecursoDuplicadoException;
import com.mstock.catalogo.exception.RecursoNoEncontradoException;
import com.mstock.catalogo.repository.ProductoRepository;
import com.mstock.catalogo.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll().stream()
                .map(ProveedorResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return ProveedorResponse.desde(buscar(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        String ruc = request.ruc().trim();
        if (proveedorRepository.existsByRuc(ruc)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con el RUC '" + ruc + "'");
        }
        Proveedor guardado = proveedorRepository.save(Proveedor.builder()
                .ruc(ruc)
                .razonSocial(request.razonSocial().trim())
                .telefono(limpiar(request.telefono()))
                .email(limpiar(request.email()))
                .direccion(limpiar(request.direccion()))
                .activo(request.activo() == null || request.activo())
                .build());
        return ProveedorResponse.desde(guardado);
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscar(id);
        String ruc = request.ruc().trim();

        if (!proveedor.getRuc().equals(ruc) && proveedorRepository.existsByRuc(ruc)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con el RUC '" + ruc + "'");
        }

        proveedor.setRuc(ruc);
        proveedor.setRazonSocial(request.razonSocial().trim());
        proveedor.setTelefono(limpiar(request.telefono()));
        proveedor.setEmail(limpiar(request.email()));
        proveedor.setDireccion(limpiar(request.direccion()));
        if (request.activo() != null) {
            proveedor.setActivo(request.activo());
        }
        return ProveedorResponse.desde(proveedor);
    }

    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = buscar(id);
        long productos = productoRepository.countByProveedorId(id);
        if (productos > 0) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar el proveedor: tiene " + productos + " producto(s) asociado(s)");
        }
        proveedorRepository.delete(proveedor);
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el proveedor con id " + id));
    }

    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}

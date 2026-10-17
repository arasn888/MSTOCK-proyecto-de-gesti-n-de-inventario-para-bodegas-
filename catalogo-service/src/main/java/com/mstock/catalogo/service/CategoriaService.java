package com.mstock.catalogo.service;

import com.mstock.catalogo.dto.CategoriaRequest;
import com.mstock.catalogo.dto.CategoriaResponse;
import com.mstock.catalogo.entity.Categoria;
import com.mstock.catalogo.exception.OperacionNoPermitidaException;
import com.mstock.catalogo.exception.RecursoDuplicadoException;
import com.mstock.catalogo.exception.RecursoNoEncontradoException;
import com.mstock.catalogo.repository.CategoriaRepository;
import com.mstock.catalogo.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.desde(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new RecursoDuplicadoException(
                    "Ya existe una categoría con el nombre '" + nombre + "'");
        }
        Categoria guardada = categoriaRepository.save(Categoria.builder()
                .nombre(nombre)
                .descripcion(limpiar(request.descripcion()))
                .activo(request.activo() == null || request.activo())
                .build());
        return CategoriaResponse.desde(guardada);
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().trim();

        boolean cambioNombre = !categoria.getNombre().equalsIgnoreCase(nombre);
        if (cambioNombre && categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new RecursoDuplicadoException(
                    "Ya existe una categoría con el nombre '" + nombre + "'");
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(limpiar(request.descripcion()));
        if (request.activo() != null) {
            categoria.setActivo(request.activo());
        }
        return CategoriaResponse.desde(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        long productos = productoRepository.countByCategoriaId(id);
        if (productos > 0) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la categoría: tiene " + productos + " producto(s) asociado(s)");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoría con id " + id));
    }

    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}

package com.mstock.catalogo.service;

import com.mstock.catalogo.dto.ProductoRequest;
import com.mstock.catalogo.dto.ProductoResponse;
import com.mstock.catalogo.entity.Categoria;
import com.mstock.catalogo.entity.Producto;
import com.mstock.catalogo.entity.Proveedor;
import com.mstock.catalogo.exception.RecursoDuplicadoException;
import com.mstock.catalogo.exception.RecursoNoEncontradoException;
import com.mstock.catalogo.exception.ReglaNegocioException;
import com.mstock.catalogo.repository.CategoriaRepository;
import com.mstock.catalogo.repository.ProductoRepository;
import com.mstock.catalogo.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String nombre, Long categoriaId) {
        boolean hayNombre = nombre != null && !nombre.isBlank();
        List<Producto> productos;
        if (hayNombre && categoriaId != null) {
            productos = productoRepository.buscarActivosPorTextoYCategoria(nombre.trim(), categoriaId);
        } else if (hayNombre) {
            productos = productoRepository.buscarActivosPorTexto(nombre.trim());
        } else if (categoriaId != null) {
            productos = productoRepository.findByActivoTrueAndCategoriaId(categoriaId);
        } else {
            productos = productoRepository.findByActivoTrue();
        }
        return productos.stream().map(ProductoResponse::desde).toList();
    }

    /** Producto activo por su código exacto (lo usa el lector de código de barras del punto de venta). */
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorCodigo(String codigo) {
        String buscado = normalizarCodigo(codigo);
        return productoRepository.findByCodigoAndActivoTrue(buscado)
                .map(ProductoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un producto activo con el código '" + buscado + "'"));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (productoRepository.existsByCodigo(codigo)) {
            throw new RecursoDuplicadoException("Ya existe un producto con el código '" + codigo + "'");
        }
        validarPrecios(request.precioCompra(), request.precioVenta());
        Categoria categoria = categoriaActiva(request.categoriaId());
        Proveedor proveedor = proveedorActivo(request.proveedorId());

        Producto guardado = productoRepository.save(Producto.builder()
                .codigo(codigo)
                .nombre(request.nombre().trim())
                .descripcion(limpiar(request.descripcion()))
                .precioCompra(request.precioCompra())
                .precioVenta(request.precioVenta())
                .unidadMedida(normalizarUnidad(request.unidadMedida()))
                .categoria(categoria)
                .proveedor(proveedor)
                .activo(request.activo() == null || request.activo())
                .build());
        return ProductoResponse.desde(guardado);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        String codigo = normalizarCodigo(request.codigo());

        if (!producto.getCodigo().equalsIgnoreCase(codigo) && productoRepository.existsByCodigo(codigo)) {
            throw new RecursoDuplicadoException("Ya existe un producto con el código '" + codigo + "'");
        }
        validarPrecios(request.precioCompra(), request.precioVenta());
        Categoria categoria = categoriaActiva(request.categoriaId());
        Proveedor proveedor = proveedorActivo(request.proveedorId());

        producto.setCodigo(codigo);
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(limpiar(request.descripcion()));
        producto.setPrecioCompra(request.precioCompra());
        producto.setPrecioVenta(request.precioVenta());
        producto.setUnidadMedida(normalizarUnidad(request.unidadMedida()));
        producto.setCategoria(categoria);
        producto.setProveedor(proveedor);
        if (request.activo() != null) {
            producto.setActivo(request.activo());
        }
        return ProductoResponse.desde(productoRepository.saveAndFlush(producto));
    }

    /**
     * Baja lógica: el producto queda inactivo y deja de aparecer en el listado,
     * pero se conserva en la base de datos porque otros servicios (inventario,
     * ventas) guardarán su id y deben poder consultar el historial.
     */
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);
        if (producto.isActivo()) {
            producto.setActivo(false);
            productoRepository.saveAndFlush(producto);
        }
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + id));
    }

    private void validarPrecios(BigDecimal compra, BigDecimal venta) {
        if (venta.compareTo(compra) < 0) {
            throw new ReglaNegocioException(
                    "El precio de venta no puede ser menor al precio de compra");
        }
    }

    private Categoria categoriaActiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La categoría indicada no existe"));
        if (!categoria.isActivo()) {
            throw new ReglaNegocioException("La categoría indicada está inactiva");
        }
        return categoria;
    }

    private Proveedor proveedorActivo(Long id) {
        if (id == null) {
            return null;
        }
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El proveedor indicado no existe"));
        if (!proveedor.isActivo()) {
            throw new ReglaNegocioException("El proveedor indicado está inactivo");
        }
        return proveedor;
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private String normalizarUnidad(String unidad) {
        return (unidad == null || unidad.isBlank()) ? "UNIDAD" : unidad.trim().toUpperCase();
    }

    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
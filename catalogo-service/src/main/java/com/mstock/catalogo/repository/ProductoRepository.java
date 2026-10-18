package com.mstock.catalogo.repository;

import com.mstock.catalogo.entity.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    List<Producto> findByCategoriaId(Long categoriaId);

    long countByCategoriaId(Long categoriaId);

    long countByProveedorId(Long proveedorId);

    @Override
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    Optional<Producto> findById(Long id);

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByActivoTrue();

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByActivoTrueAndCategoriaId(Long categoriaId);

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByActivoTrueAndNombreContainingIgnoreCase(String nombre);

    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    List<Producto> findByActivoTrueAndCategoriaIdAndNombreContainingIgnoreCase(Long categoriaId, String nombre);

    /** Código de barras o interno exacto, solo de un producto activo (lector de código de barras del punto de venta). */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    Optional<Producto> findByCodigoAndActivoTrue(String codigo);

    /** Búsqueda de productos activos por parte del nombre O del código, sin distinguir mayúsculas. */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    @Query("select p from Producto p where p.activo = true "
            + "and (lower(p.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(p.codigo) like lower(concat('%', :texto, '%'))) "
            + "order by p.nombre")
    List<Producto> buscarActivosPorTexto(@Param("texto") String texto);

    /** Igual que buscarActivosPorTexto, pero limitada a una categoría. */
    @EntityGraph(attributePaths = {"categoria", "proveedor"})
    @Query("select p from Producto p where p.activo = true and p.categoria.id = :categoriaId "
            + "and (lower(p.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(p.codigo) like lower(concat('%', :texto, '%'))) "
            + "order by p.nombre")
    List<Producto> buscarActivosPorTextoYCategoria(@Param("texto") String texto,
                                                   @Param("categoriaId") Long categoriaId);
}

package com.mstock.notificaciones.repository;

import com.mstock.notificaciones.entity.Notificacion;
import com.mstock.notificaciones.entity.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findAllByOrderByFechaDescIdDesc();

    List<Notificacion> findByLeidaFalseOrderByFechaDescIdDesc();

    long countByLeidaFalse();

    boolean existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion tipo, Long referenciaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.leida = false")
    int marcarTodasLeidas();
}

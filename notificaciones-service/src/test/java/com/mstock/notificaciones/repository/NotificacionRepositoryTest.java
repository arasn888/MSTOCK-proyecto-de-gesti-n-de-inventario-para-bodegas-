package com.mstock.notificaciones.repository;

import com.mstock.notificaciones.entity.Notificacion;
import com.mstock.notificaciones.entity.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:notificaciontest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class NotificacionRepositoryTest {

    @Autowired
    private NotificacionRepository repository;

    private Notificacion notificacion(TipoNotificacion tipo, Long referenciaId,
                                      boolean leida, LocalDateTime fecha) {
        return Notificacion.builder()
                .tipo(tipo)
                .titulo("Titulo de prueba")
                .mensaje("Mensaje de prueba")
                .referenciaId(referenciaId)
                .leida(leida)
                .fecha(fecha)
                .build();
    }

    @Test
    @DisplayName("Insertar: guarda la notificación como no leída y con fecha")
    void insertar() {
        Notificacion guardada = repository.save(
                notificacion(TipoNotificacion.STOCK_BAJO, 10L, false, null));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.isLeida()).isFalse();
        assertThat(guardada.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("Listar: devuelve las notificaciones de la más reciente a la más antigua")
    void listarOrdenado() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 8, 0);
        repository.save(notificacion(TipoNotificacion.STOCK_BAJO, 1L, false, base));
        repository.save(notificacion(TipoNotificacion.VENTA_REGISTRADA, 2L, false, base.plusMinutes(5)));
        repository.save(notificacion(TipoNotificacion.STOCK_BAJO, 3L, false, base.plusMinutes(10)));

        assertThat(repository.findAllByOrderByFechaDescIdDesc())
                .extracting(Notificacion::getReferenciaId)
                .containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("Solo no leídas: excluye las ya leídas y las cuenta")
    void soloNoLeidas() {
        repository.save(notificacion(TipoNotificacion.STOCK_BAJO, 1L, false, null));
        repository.save(notificacion(TipoNotificacion.STOCK_BAJO, 2L, true, null));
        repository.save(notificacion(TipoNotificacion.VENTA_REGISTRADA, 3L, false, null));

        assertThat(repository.findByLeidaFalseOrderByFechaDescIdDesc()).hasSize(2);
        assertThat(repository.countByLeidaFalse()).isEqualTo(2);
    }

    @Test
    @DisplayName("Marcar todas como leídas: actualiza solo las pendientes")
    void marcarTodasLeidas() {
        repository.save(notificacion(TipoNotificacion.STOCK_BAJO, 1L, false, null));
        repository.save(notificacion(TipoNotificacion.STOCK_BAJO, 2L, true, null));
        repository.save(notificacion(TipoNotificacion.VENTA_REGISTRADA, 3L, false, null));

        int actualizadas = repository.marcarTodasLeidas();

        assertThat(actualizadas).isEqualTo(2);
        assertThat(repository.countByLeidaFalse()).isZero();
    }

    @Test
    @DisplayName("Eliminar: borra la notificación")
    void eliminar() {
        Notificacion guardada = repository.save(
                notificacion(TipoNotificacion.STOCK_BAJO, 1L, false, null));

        repository.deleteById(guardada.getId());
        repository.flush();

        assertThat(repository.findById(guardada.getId())).isEmpty();
    }

    @Test
    @DisplayName("Alerta pendiente: detecta si ya hay una sin leer del mismo producto y tipo")
    void alertaPendiente() {
        Notificacion guardada = repository.save(
                notificacion(TipoNotificacion.STOCK_BAJO, 10L, false, null));

        assertThat(repository.existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion.STOCK_BAJO, 10L)).isTrue();
        assertThat(repository.existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion.STOCK_BAJO, 99L)).isFalse();
        assertThat(repository.existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion.VENTA_REGISTRADA, 10L)).isFalse();

        guardada.setLeida(true);
        repository.saveAndFlush(guardada);

        assertThat(repository.existsByTipoAndReferenciaIdAndLeidaFalse(TipoNotificacion.STOCK_BAJO, 10L)).isFalse();
    }
}

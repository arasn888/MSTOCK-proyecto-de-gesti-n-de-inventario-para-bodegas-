package com.mstock.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Protege contra errores de escritura en application.yml: cada ruta apunta al servicio correcto. */
class RutasTest {

    private static final String BASE = "spring.cloud.gateway.server.webflux.routes";

    private final Properties yml = cargar();

    private static Properties cargar() {
        YamlPropertiesFactoryBean fabrica = new YamlPropertiesFactoryBean();
        fabrica.setResources(new ClassPathResource("application.yml"));
        return fabrica.getObject();
    }

    private String destinoDe(String id) {
        for (int i = 0; yml.containsKey(BASE + "[" + i + "].id"); i++) {
            if (id.equals(yml.getProperty(BASE + "[" + i + "].id"))) {
                return yml.getProperty(BASE + "[" + i + "].uri") + " | "
                        + yml.getProperty(BASE + "[" + i + "].predicates[0]") + " | "
                        + yml.getProperty(BASE + "[" + i + "].filters[0].args.fallbackUri");
            }
        }
        return null;
    }

    @Test
    @DisplayName("Hay una ruta por servicio y cada una apunta a su servicio en Eureka")
    void rutasPorServicio() {
        assertThat(destinoDe("auth")).startsWith("lb://auth-service | Path=/api/auth/**");
        assertThat(destinoDe("catalogo")).startsWith("lb://catalogo-service | Path=/api/productos/**");
        assertThat(destinoDe("inventario")).startsWith("lb://inventario-service | Path=/api/inventario/**");
        assertThat(destinoDe("ventas")).startsWith("lb://ventas-service | Path=/api/ventas/**");
        assertThat(destinoDe("compras")).startsWith("lb://compras-service | Path=/api/compras/**");
        assertThat(destinoDe("notificaciones"))
                .startsWith("lb://notificaciones-service | Path=/api/notificaciones/**");
    }

    @Test
    @DisplayName("Cada ruta tiene su respaldo con el nombre de su propio servicio")
    void respaldoPorRuta() {
        assertThat(destinoDe("auth")).endsWith("forward:/fallback/auth");
        assertThat(destinoDe("catalogo")).endsWith("forward:/fallback/catalogo");
        assertThat(destinoDe("inventario")).endsWith("forward:/fallback/inventario");
        assertThat(destinoDe("ventas")).endsWith("forward:/fallback/ventas");
        assertThat(destinoDe("compras")).endsWith("forward:/fallback/compras");
        assertThat(destinoDe("notificaciones")).endsWith("forward:/fallback/notificaciones");
    }

    @Test
    @DisplayName("Los prefijos de las rutas cubren todos los controladores existentes")
    void cubreTodosLosControladores() {
        String todas = String.join(" ", yml.stringPropertyNames().stream()
                .filter(k -> k.contains("predicates")).map(yml::getProperty).toList());

        for (String prefijo : new String[]{"/api/auth", "/api/usuarios", "/api/productos", "/api/categorias",
                "/api/proveedores", "/api/inventario", "/api/ventas", "/api/clientes", "/api/notificaciones", "/api/compras"}) {
            assertThat(todas).as("ruta para " + prefijo).contains(prefijo + "/**");
        }
    }
}

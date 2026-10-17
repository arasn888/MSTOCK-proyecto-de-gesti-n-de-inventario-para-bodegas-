package com.mstock.ventas.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/** Aquí y no en la clase principal, para que las pruebas @DataJpaTest no creen clientes Feign. */
@Configuration
@EnableFeignClients(basePackages = "com.mstock.ventas.client")
public class FeignConfig {
}

package com.mstock.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ComprasServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComprasServiceApplication.class, args);
    }
}

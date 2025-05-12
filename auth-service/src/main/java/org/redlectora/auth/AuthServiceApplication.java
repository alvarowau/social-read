package org.redlectora.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // Importa esta anotación
import org.springframework.cloud.openfeign.EnableFeignClients; // Importa esta anotación

@SpringBootApplication // Anotación principal de Spring Boot para la aplicación
@EnableDiscoveryClient // Habilita la registración y el descubrimiento de servicios en Eureka Server
@EnableFeignClients    // Habilita los clientes Feign para la comunicación entre microservicios
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
package org.redlectora.user; // Asegúrate de que este paquete sea correcto para tu proyecto

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // ¡IMPORTANTE! Importa esta anotación

@SpringBootApplication // Anotación principal de Spring Boot para la aplicación
@EnableDiscoveryClient // Habilita la registración y el descubrimiento de servicios en Eureka Server
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
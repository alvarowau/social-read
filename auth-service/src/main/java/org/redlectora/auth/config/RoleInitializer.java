package org.redlectora.auth.config;

import org.redlectora.auth.model.ERole;
import org.redlectora.auth.model.Role;
import org.redlectora.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Arrays;

@Configuration
public class RoleInitializer {

    //@Order(2) // Opcional: Define el orden de ejecución si tienes múltiples CommandLineRunners
    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            // Lista de roles a inicializar
            Arrays.stream(ERole.values()).forEach(roleEnum -> {
                if (roleRepository.findByName(roleEnum).isEmpty()) {
                    // Si el rol no existe, lo crea y lo guarda
                    roleRepository.save(new Role(roleEnum));
                    System.out.println("Rol '" + roleEnum.name() + "' creado y guardado en la base de datos.");
                } else {
                    System.out.println("Rol '" + roleEnum.name() + "' ya existe en la base de datos.");
                }
            });
        };

    }
}
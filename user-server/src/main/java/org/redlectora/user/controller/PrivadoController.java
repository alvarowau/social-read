package org.redlectora.user.controller;

import org.redlectora.user.model.UserProfile;
import org.redlectora.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
@RestController // Indica que esta clase es un controlador REST
@RequestMapping("/logueado")
public class PrivadoController {

    private final UserService userService; // Inyección del UserService
    private static final Logger logger = LoggerFactory.getLogger(PrivadoController.class);
    // Constructor para la inyección de dependencias
    public PrivadoController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity<List<UserProfile>> getAllLogueado(
            // <-- Opcional: Inyecta todas las cabeceras para ver la informacion del usuario
            @RequestHeader Map<String, String> headers
    ){
        // <-- Añade logging para ver que llega al endpoint protegido
        logger.info("--> USER-SERVICE: Recibida solicitud en /api/users/logueado");
        logger.debug("--> USER-SERVICE: Cabeceras recibidas en /logueado: {}", headers); // Usa debug

        // <-- Logea las cabeceras que esperamos que el Gateway añada
        // Nota: las cabeceras suelen ser convertidas a minusculas
        String userId = headers.get("x-user-id");
        String userRoles = headers.get("x-user-roles"); // Si añadiste roles en el filtro

        logger.info("--> USER-SERVICE: Informacion de usuario recibida via cabeceras: X-User-Id={}, X-User-Roles={}", userId, userRoles);

        // Aqui ya podrias usar userId para obtener datos especificos del usuario
        // return ResponseEntity.ok(userService.getUserById(userId)); // Ejemplo

        return ResponseEntity.ok(userService.getAll()); // Por ahora, sigue devolviendo todo
    }
}

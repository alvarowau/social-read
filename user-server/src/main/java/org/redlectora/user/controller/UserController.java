package org.redlectora.user.controller;

import org.redlectora.user.model.UserProfile;
import org.redlectora.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger; // <-- Importa Logger
import org.slf4j.LoggerFactory;

/**
 * Controlador REST para las operaciones relacionadas con los perfiles de usuario.
 */
@RestController // Indica que esta clase es un controlador REST
@RequestMapping("/api/users") // Define el prefijo de la URL para todos los endpoints en este controlador
public class UserController {

    private final UserService userService; // Inyección del UserService
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    // Constructor para la inyección de dependencias
    public UserController(UserService userService) {
        this.userService = userService;
    }



    @GetMapping("/logueado")
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
    /**
     * Endpoint para verificar si un nickname ya existe.
     * Este endpoint es llamado SÍNCRONAMENTE por el auth-service (via FeignClient).
     * URL: GET /api/users/exists/nickname/{nickname}
     *
     * @param nickname El nickname a verificar.
     * @return ResponseEntity con un cuerpo booleano (true si existe, false si no) y un HttpStatus.OK.
     */
    @GetMapping("/exists/nickname/{nickname}")
    public ResponseEntity<Boolean> existsByNickname(@PathVariable String nickname) {
        // Delega la lógica de verificación al UserService.
        boolean exists = userService.existsByNickname(nickname);
        // Retorna ResponseEntity.ok() con el valor booleano.
        return ResponseEntity.ok(exists);
    }

    /**
     * (OPCIONAL) Endpoint para obtener un perfil de usuario por su nickname.
     * Este endpoint no es directamente utilizado por el auth-service para el registro,
     * pero es útil para pruebas o para futuras funcionalidades de frontend o otros servicios.
     * URL: GET /api/users/profile/nickname/{nickname}
     *
     * @param nickname El nickname del perfil a buscar.
     * @return ResponseEntity con el UserProfile si se encuentra (HttpStatus.OK) o HttpStatus.NOT_FOUND si no.
     */
    @GetMapping("/profile/nickname/{nickname}")
    public ResponseEntity<UserProfile> getUserProfileByNickname(@PathVariable String nickname) {
        return userService.getProfileByNickname(nickname)
                .map(ResponseEntity::ok) // Si se encuentra el perfil, retorna 200 OK con el perfil
                .orElse(ResponseEntity.notFound().build()); // Si no se encuentra, retorna 404 Not Found
    }

    // En el futuro, se podrían añadir más endpoints aquí, como:
    // - PUT /api/users/{id} para actualizar un perfil
    // - GET /api/users/{id} para obtener un perfil por ID
    // - etc.
}
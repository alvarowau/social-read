package org.redlectora.auth.controller;

import org.redlectora.auth.dto.RegisterRequest; // Asegúrate de que RegisterRequest está en este paquete o en el correcto
import org.redlectora.auth.service.AuthService; // Importa tu nuevo AuthService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.redlectora.auth.exception.BadRequestException; // Importa tu BadRequestException

@RestController
@RequestMapping("/auth")
public class AuthServiceController {

    private final AuthService authService; // Ahora inyectamos AuthService

    @Autowired
    public AuthServiceController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint para verificar la existencia de un nickname.
     * Delega la lógica al AuthService.
     *
     * @param nickname El nickname a verificar, proporcionado en la URL.
     * @return Una respuesta HTTP que indica si el nickname existe o no.
     */
    @GetMapping("/check-nickname-existence/{nickname}")
    public ResponseEntity<String> checkNicknameExistence(@PathVariable("nickname") String nickname) {
        try {
            boolean exists = authService.checkNicknameExistence(nickname);
            if (exists) {
                return ResponseEntity.ok("El nickname '" + nickname + "' YA EXISTE en el User Service.");
            } else {
                return ResponseEntity.ok("El nickname '" + nickname + "' NO EXISTE en el User Service. ¡Puedes usarlo!");
            }
        } catch (RuntimeException e) {
            // Captura las excepciones del servicio y devuelve un error 500 o 400 si es BadRequest
            if (e instanceof BadRequestException) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al verificar nickname: " + e.getMessage());
        }
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     * Delega la lógica al AuthService.
     *
     * @param request Datos de la solicitud de registro.
     * @return ResponseEntity con el resultado de la operación.
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest request) {
        try {
            authService.registerUser(request); // El servicio se encarga de la lógica y excepciones
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuario registrado con éxito. Se ha enviado una solicitud para crear el perfil.");
        } catch (BadRequestException e) {
            // Captura la excepción de negocio (ej. email/nickname ya en uso)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error en el registro: " + e.getMessage());
        }
    }
}
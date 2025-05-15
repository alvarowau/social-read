package org.redlectora.auth.controller;

import org.redlectora.auth.dto.AuthenticationResponse;
import org.redlectora.auth.dto.LoginRequest;
import org.redlectora.auth.dto.RegisterRequest;
import org.redlectora.auth.exception.BadRequestException;
import org.redlectora.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthServiceController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;


    @Autowired
    public AuthServiceController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Endpoint para verificar la existencia de un nickname.
     * Delega la lógica al AuthService.
     *
     * @param nickname El nickname a verificar, proporcionado en la URL.
     * @return Una respuesta HTTP que indica si el nickname existe o no.
     */
//    @GetMapping("/check-nickname-existence/{nickname}")
//    public ResponseEntity<String> checkNicknameExistence(@PathVariable("nickname") String nickname) {
//        try {
//            boolean exists = authService.checkNicknameExistence(nickname);
//            if (exists) {
//                return ResponseEntity.ok("El nickname '" + nickname + "' YA EXISTE en el User Service.");
//            } else {
//                return ResponseEntity.ok("El nickname '" + nickname + "' NO EXISTE en el User Service. ¡Puedes usarlo!");
//            }
//        } catch (RuntimeException e) {
//            // Captura las excepciones del servicio y devuelve un error 500 o 400 si es BadRequest
//            if (e instanceof BadRequestException) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//            }
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al verificar nickname: " + e.getMessage());
//        }
//    }

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
            authService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuario registrado con éxito. Se ha enviado una solicitud para crear el perfil.");
        } catch (BadRequestException e) {
            // Captura la excepción de negocio (ej. email/nickname ya en uso)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error en el registro: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> handleLogin(@RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken intento = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword()
        );
        try {
            Authentication authentication = authenticationManager.authenticate(intento);
            AuthenticationResponse response = authService.login(authentication); // Llama al servicio

            // Retorna la respuesta exitosa con el token desde dentro del try
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // Loguea el error (buena práctica)
            System.err.println("Intento de autenticación fallido para usuario " + request.getEmail() + ": " + e.getMessage());

            // Retorna una respuesta 401 Unauthorized en caso de fallo
            // Puedes devolver un cuerpo nulo, un mensaje simple, o un DTO de error personalizado.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // O un mensaje de error: .body("Credenciales inválidas")

        }
        // Elimina el return que estaba fuera del try-catch,
        // ya que ambos casos (éxito y fallo) ahora retornan desde dentro de sus bloques.
        // return ResponseEntity.ok(response); // <--- ¡ELIMINA ESTA LÍNEA!
    }
}
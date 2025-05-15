package org.redlectora.auth.feign.client;

import org.redlectora.auth.feign.dto.UserProfileCreateRequest; // Importa este DTO, aunque no lo usaremos para llamadas directas de Feign aquí.
import org.springframework.cloud.openfeign.FeignClient; // Importa la anotación @FeignClient
import org.springframework.http.ResponseEntity; // Para manejar las respuestas HTTP
import org.springframework.web.bind.annotation.GetMapping; // Para mapear métodos GET
import org.springframework.web.bind.annotation.PathVariable; // Para extraer variables de la URL
import org.springframework.web.bind.annotation.PostMapping; // Para mapear métodos POST (lo dejamos por si acaso, pero no se usará para el perfil)
import org.springframework.web.bind.annotation.RequestBody; // Para enviar el cuerpo de la petición (lo dejamos por si acaso)

/**
 * Interfaz FeignClient para comunicarse con el Microservicio de Usuarios (user-service).
 * Spring Cloud OpenFeign usará esta interfaz para crear un cliente HTTP proxy.
 */
@FeignClient(name = "user-service")
// El atributo 'name' debe coincidir EXACTAMENTE con el 'spring.application.name'
// configurado en el application.properties del microservicio de USUARIOS (user-service).
// Feign usará este nombre para buscar y resolver la ubicación del user-service
// a través de Eureka Server. No necesitamos especificar IP o puerto aquí.
public interface UserServiceClient {

    /**
     * Llama al endpoint del user-service para verificar si un nickname ya existe.
     * Este es el único endpoint que el auth-service llamará de forma síncrona.
     *
     * @param nickname El nickname a verificar.
     * @return ResponseEntity<Boolean> que contiene 'true' si el nickname existe, 'false' en caso contrario.
     * El user-service debería devolver un HttpStatus.OK junto con el booleano.
     */
    @GetMapping("/api/users/exists/nickname/{nickname}") // Mapea esta llamada al endpoint GET en user-service
    ResponseEntity<Boolean> existsByNickname(@PathVariable("nickname") String nickname); // Captura el nickname de la URL

    /**
     * (Opcional) Define un método para crear un perfil de usuario.
     * NOTA IMPORTANTE: En nuestra arquitectura Event-Driven, el auth-service NO
     * llamará directamente a este endpoint del user-service para crear el perfil.
     * La creación del perfil se hará de forma asíncrona por el user-service
     * al consumir un evento de Kafka.
     * Lo incluimos aquí solo como un ejemplo de cómo se vería un método Feign para
     * un POST si decidiéramos cambiar a un patrón síncrono para la creación de perfiles
     * o si en el futuro el Auth Service necesitara otro tipo de comunicación síncrona
     * con el User Service que involucre este DTO.
     */
    @PostMapping("/api/users/profile")
    ResponseEntity<Void> createProfile(@RequestBody UserProfileCreateRequest request);
}
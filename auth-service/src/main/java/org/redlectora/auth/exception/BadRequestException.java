package org.redlectora.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción personalizada para indicar una solicitud incorrecta (Bad Request).
 * Cuando esta excepción es lanzada desde un controlador de Spring, la anotación @ResponseStatus
 * asegura que la respuesta HTTP tenga un código de estado 400 (BAD_REQUEST).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // Esta anotación es de Spring Web y le indica a Spring que cuando esta excepción sea lanzada,
// la respuesta HTTP debe tener el código de estado 400 (Bad Request).
public class BadRequestException extends RuntimeException {

    // Constructor que acepta un mensaje de error
    public BadRequestException(String message) {
        super(message); // Llama al constructor de la clase padre (RuntimeException) con el mensaje.
    }

    // Puedes añadir otro constructor si quieres incluir una causa subyacente (Throwable cause)
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
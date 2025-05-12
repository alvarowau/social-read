package org.redlectora.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera getters, setters, equals, hashCode y toString
@Builder // Proporciona un patrón de construcción de objetos (ej. RegisterRequest.builder().email("...").build())
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class RegisterRequest {

    @NotBlank(message = "Name cannot be blank") // Valida que el campo no sea nulo ni esté vacío
    private String name;

    @NotBlank(message = "Surname cannot be blank")
    private String surname;

    @NotBlank(message = "Nickname cannot be blank")
    @Size(min = 3, max = 30, message = "Nickname must be between 3 and 30 characters") // Valida la longitud
    private String nickname;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format") // Valida que sea un formato de email válido
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
    private String password;
}
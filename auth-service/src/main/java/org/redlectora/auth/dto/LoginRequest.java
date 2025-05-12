package org.redlectora.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email cannot be blank") // Ahora el login es por email
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}
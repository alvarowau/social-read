package org.redlectora.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String token; // El token JWT
    private String email; // Opcional: Devolver el email del usuario autenticado
    // private Set<String> roles; // Podríamos añadir los roles aquí si lo necesitas en el frontend
}
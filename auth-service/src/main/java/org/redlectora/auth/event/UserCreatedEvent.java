package org.redlectora.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private Long authUserId; // El ID que le asignó el auth-service a las credenciales del usuario
    private String name;
    private String surname;
    private String nickname;
    private String email;
    // Podrías añadir más campos si fueran relevantes para el evento y el consumo por otros servicios
    // private String confirmationToken; // Si el token de confirmación se generara aquí y lo necesitara otro servicio
}
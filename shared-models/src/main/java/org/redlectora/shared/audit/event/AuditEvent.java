package org.redlectora.shared.audit.event;

// Importaciones de Lombok (asegúrate de que tu IDE las resuelva con la dependencia en pom.xml)
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Importación para la fecha y hora
import java.time.LocalDateTime;
// Importación para Map si decides usarlo en details (aunque en el payload lo guardaremos como String JSON)
// import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    // No necesita @Id ni anotaciones JPA aquí.
    // Esto es solo el CONTRATO de datos para Kafka.

    private LocalDateTime timestamp; // Cuándo ocurrió el evento
    private String serviceName; // Qué servicio generó el evento (ej. "auth-service")
    private String userId; // Quién realizó la acción (ID del usuario, si aplica; puede ser null)
    private String actionType; // Tipo de acción (ej. "USER_EMAIL_CHANGED", "LOGIN_FAILED")
    private String details; // Detalles específicos del evento, guardaremos como String (ej. JSON)


}
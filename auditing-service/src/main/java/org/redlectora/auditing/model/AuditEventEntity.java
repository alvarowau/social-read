package org.redlectora.auditing.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import org.hibernate.annotations.Type;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "audit_events", indexes = { // <--- Añadimos la anotación @Table con la propiedad indexes
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"), // Índice por timestamp (muy común)
        @Index(name = "idx_audit_service_name", columnList = "serviceName"), // Índice por nombre de servicio
        @Index(name = "idx_audit_user_id", columnList = "userId"), // Índice por ID de usuario (si aplica)
        @Index(name = "idx_audit_action_type", columnList = "actionType") // Índice por tipo de acción
        // Opcional: Índice compuesto si buscas mucho por combinación de campos
        // @Index(name = "idx_audit_user_action_timestamp", columnList = "userId,actionType,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String serviceName;
    private String userId;
    private String actionType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String details;

    // Si no usas Lombok, tus getters/setters/constructores van aquí
}
package org.redlectora.auth.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redlectora.shared.audit.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component; // O @Service

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class AuditPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AuditPublisher.class);

    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String serviceName;

    private static final String AUDIT_BINDING_NAME = "auditProducer-out-0";

    public AuditPublisher(StreamBridge streamBridge, ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica un evento de auditora a Kafka.
     * Este es el mtodo principal para llamar desde otros servicios o controladores.
     *
     * @param userId     ID del usuario relacionado con la accin (puede ser null si la accin no est ligada a un usuario especfico, ej. arranque de servicio)
     * @param actionType Tipo de la accin (una cadena constante que describe lo que pas, ej. "USER_REGISTERED", "EMAIL_CHANGED", "LOGIN_FAILED", "PASSWORD_RESET_REQUESTED")
     * @param details    Un objeto (comnmente un Map<String, Object>) con detalles adicionales especficos de este evento. Ser serializado a JSON. Puede ser null si no hay detalles.
     */
    public void publishEvent(String userId, String actionType, Object details) {
        publish(userId, actionType, details); // Llama al mtodo interno de publicacin
    }

    /**
     * Mtodo interno que construye el objeto AuditEvent y lo enva a Kafka.
     *
     * @param userId     ID del usuario.
     * @param actionType Tipo de accin.
     * @param details    Detalles del evento.
     */
    private void publish(String userId, String actionType, Object details) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                logger.error("AUDIT-PUBLISHER: Failed to serialize audit event details for action {}", actionType, e);
                detailsJson = "{ \"error\": \"Failed to serialize details\" }"; // O dejarlo null, o handlear de otra forma
            }
        }

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setTimestamp(LocalDateTime.now());
        auditEvent.setServiceName(this.serviceName);
        auditEvent.setUserId(userId);
        auditEvent.setActionType(actionType);
        auditEvent.setDetails(detailsJson);

        try {
            // Puedes aadir ms cabeceras Kafka si es necesario (ej. correlation ID)
            // Message<AuditEvent> message = MessageBuilder.withPayload(auditEvent).setHeader(...).build();
            // boolean success = streamBridge.send(AUDIT_BINDING_NAME, message);

            boolean success = streamBridge.send(AUDIT_BINDING_NAME, auditEvent); // Envo simple del payload

            if (success) {
                logger.info("AUDIT-PUBLISHER: Audit event '{}' published successfully for user {}", actionType, userId != null ? userId : "N/A");
            } else {
                logger.error("AUDIT-PUBLISHER: Failed to publish audit event '{}' for user {}", actionType, userId != null ? userId : "N/A");
                // Considera estrategias de reintento o logging avanzado si el envo falla
            }
        } catch (Exception e) {
            // Excepciones durante el envo (ej. Kafka no disponible)
            logger.error("AUDIT-PUBLISHER: Exception occurred while publishing audit event '{}' for user {}", actionType, userId != null ? userId : "N/A", e);
        }
    }

    // Opcional: Puedes mantener mtodos wrapper para tipos de eventos comunes si simplifican la llamada
    // Pero asegúrate de que llamen al método principal publishEvent(userId, actionType, details)

    /**
     * Mtodo wrapper para publicar un evento de registro de usuario.
     *
     * @param userId            ID del usuario registrado.
     * @param registeredEmail   Email usado para el registro.
     * @param registeredNickname Nickname usado para el registro.
     * @param registrationIp    Direccin IP desde la que se realiz el registro (puede ser null).
     */
    public void publishUserRegistrationEvent(String userId, String registeredEmail, String registeredNickname, String registrationIp) {
        Map<String, Object> details = Map.of(
                "registeredEmail", registeredEmail,
                "registeredNickname", registeredNickname,
                "registrationIp", registrationIp != null ? registrationIp : "N/A" // Manejar IP nula
        );
        publishEvent(userId, "USER_REGISTERED", details); // Llama al mtodo principal
    }

    /**
     * Mtodo wrapper para publicar un evento de cambio de email de usuario.
     *
     * @param userId    ID del usuario cuyo email se cambi.
     * @param oldEmail  Email antiguo.
     * @param newEmail  Nuevo email.
     */
    public void publishUserEmailChangedEvent(String userId, String oldEmail, String newEmail) {
        Map<String, Object> details = Map.of(
                "oldEmail", oldEmail,
                "newEmail", newEmail
        );
        publishEvent(userId, "USER_EMAIL_CHANGED", details); // Llama al mtodo principal
    }


}
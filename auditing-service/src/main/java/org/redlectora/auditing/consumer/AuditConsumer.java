package org.redlectora.auditing.consumer;

// Importa la clase AuditEvent desde tu módulo shared-models
import org.redlectora.shared.audit.event.AuditEvent; // <-- IMPORTANTE: Importa desde shared-models

import org.redlectora.auditing.model.AuditEventEntity;
import org.redlectora.auditing.repository.AuditEventRepository;

// Importaciones de Spring y Java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Importación para Functional Kafka Consumer
import java.util.function.Consumer;
import java.time.LocalDateTime; // Para obtener la fecha/hora actual si el timestamp no viene en el evento

// Importaciones para inspeccionar el tipo del campo details (opcional pero til)
// import java.lang.Object; // Ya implcito
// import java.lang.String; // Ya implcito


@Configuration // O @Component, ambas funcionan para que Spring detecte este bean
public class AuditConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AuditConsumer.class);

    // Inyecta el repositorio JPA para guardar en la base de datos
    private final AuditEventRepository auditEventRepository;

    public AuditConsumer(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    // Define el Bean Consumer que Spring Cloud Stream enlazará al input Kafka
    // El nombre del bean (auditEventsListener) debe coincidir con la configuración del binder si la usas.
    // Si usas solo spring.kafka consumer properties, el topic se define ahí y el bean se asocia por tipo.
    @Bean
    public Consumer<AuditEvent> auditEventsListener() {
        // Este Consumer recibirá mensajes del tópico configurado para AuditEvent
        return auditEventPayload -> { // El parámetro auditEventPayload es el objeto AuditEvent deserializado

            logger.info("AUDIT-SERVICE CONSUMER: Recibido evento de auditoría de tipo '{}' desde servicio '{}' para usuario '{}'.",
                    auditEventPayload.getActionType(),
                    auditEventPayload.getServiceName(),
                    auditEventPayload.getUserId() != null ? auditEventPayload.getUserId() : "N/A"); // Maneja userId nulo

            // --- Aadir logging de debug para ver los campos recibidos ---
            logger.debug("AUDIT-SERVICE CONSUMER: --- Contenido del AuditEvent recibido ---");
            logger.debug("AUDIT-SERVICE CONSUMER: Timestamp: {}", auditEventPayload.getTimestamp());
            logger.debug("AUDIT-SERVICE CONSUMER: ServiceName: {}", auditEventPayload.getServiceName());
            logger.debug("AUDIT-SERVICE CONSUMER: UserId: {}", auditEventPayload.getUserId());
            logger.debug("AUDIT-SERVICE CONSUMER: ActionType: {}", auditEventPayload.getActionType());
            logger.debug("AUDIT-SERVICE CONSUMER: Details: {}", auditEventPayload.getDetails());
            // Opcional: intentar loguear el tipo del campo details (puede ser String, null, u otro si la deserializacin tuvo problemas)
            // Object detailsObject = auditEventPayload.getDetails();
            // logger.debug("AUDIT-SERVICE CONSUMER: Tipo de campo Details: {}", detailsObject != null ? detailsObject.getClass().getName() : "null");
            logger.debug("AUDIT-SERVICE CONSUMER: ---------------------------------------");
            // --------------------------------------------------------------


            // Convertir el payload de Kafka (AuditEvent) a la entidad JPA (AuditEventEntity)
            AuditEventEntity auditEventEntity = new AuditEventEntity();
            // No establecemos el ID, la base de datos lo genera automáticamente
            auditEventEntity.setTimestamp(auditEventPayload.getTimestamp());
            auditEventEntity.setServiceName(auditEventPayload.getServiceName());
            auditEventEntity.setUserId(auditEventPayload.getUserId());
            auditEventEntity.setActionType(auditEventPayload.getActionType());
            auditEventEntity.setDetails(auditEventPayload.getDetails()); // Copiamos el string JSON de detalles


            try {
                // Guardar la entidad en la base de datos
                auditEventRepository.save(auditEventEntity);
                logger.info("AUDIT-SERVICE CONSUMER: Evento de auditoría guardado exitosamente en la base de datos.");

            } catch (Exception e) {
                // Este catch capturara el error de DB
                logger.error("AUDIT-SERVICE CONSUMER: ERROR al guardar el evento de auditoría en la base de datos: {}", e.getMessage(), e);
                // La excepcin completa tambin se loguea por defecto si usas e.getMessage(), e en el log.error
                // logger.error("AUDIT-SERVICE CONSUMER: Detalles completos del error:", e); // Forma alternativa de loguear el stack trace completo
            }
        };
    }


}
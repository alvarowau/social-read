package org.redlectora.user.service;

import org.redlectora.user.event.UserCreatedEvent;       // Importa la clase del evento de Kafka
import org.redlectora.user.model.UserProfile;            // Importa la entidad UserProfile
import org.redlectora.user.repository.UserProfileRepository; // Importa el repositorio de UserProfile
import org.springframework.beans.factory.annotation.Autowired; // Para inyección de dependencias
import org.springframework.stereotype.Service;               // Marca esta clase como un componente de servicio de Spring
import org.springframework.transaction.annotation.Transactional; // Para gestión de transacciones de base de datos

import java.util.Optional; // Para manejar resultados que pueden estar ausentes

@Service // Indica a Spring que esta clase es un "servicio" y la gestionará
public class UserService {

    private final UserProfileRepository userProfileRepository; // Inyección del repositorio de perfiles

    // Constructor para la inyección de dependencias
    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Verifica si un nickname ya existe en la base de datos de perfiles.
     * Este método será llamado SÍNCRONAMENTE por el auth-service a través de FeignClient.
     *
     * @param nickname El nickname a verificar.
     * @return true si el nickname ya existe, false en caso contrario.
     */
    public boolean existsByNickname(String nickname) {
        // Usamos el método existsByNickname que definimos en UserProfileRepository.
        return userProfileRepository.existsByNickname(nickname);
    }

    /**
     * Crea un nuevo perfil de usuario en la base de datos.
     * Este método será invocado ASÍNCRONAMENTE cuando el servicio reciba un UserCreatedEvent de Kafka.
     * Es transaccional para asegurar que la operación se complete atómicamente.
     *
     * @param event El UserCreatedEvent recibido de Kafka.
     */
    @Transactional // Asegura que el método se ejecute dentro de una transacción de base de datos.
    // Si una excepción RuntimeException es lanzada, la transacción se hará rollback.
    public void createUserProfile(UserCreatedEvent event) {
        // Primero, realizar comprobaciones de unicidad para robustez.
        // Aunque auth-service ya lo valida, es una buena práctica para la idempotencia
        // y para manejar posibles reintentos o mensajes duplicados de Kafka.

        // Comprobar si el authUserId ya tiene un perfil asociado (podría ocurrir si el evento se reenvía)
        if (userProfileRepository.findByAuthUserId(event.getAuthUserId()).isPresent()) {
            System.out.println("Profile for authUserId " + event.getAuthUserId() + " already exists. Skipping creation.");
            return; // No hacemos nada si ya existe, ya que es un evento duplicado/reintento.
        }

        // Comprobar si el nickname o email ya existen (aunque auth-service ya valida el nickname síncronamente,
        // y el email debería ser único en ambos servicios).
        if (userProfileRepository.existsByNickname(event.getNickname())) {
            System.err.println("Error: Nickname '" + event.getNickname() + "' already exists in user-service. User with authUserId " + event.getAuthUserId() + " could not create profile.");
            // Aquí podrías lanzar una excepción si deseas que el listener marque el mensaje como fallido
            // o enviar un evento de "perfil no creado" para el seguimiento.
            throw new RuntimeException("Nickname " + event.getNickname() + " already exists.");
        }

        if (userProfileRepository.existsByEmail(event.getEmail())) {
            System.err.println("Error: Email '" + event.getEmail() + "' already exists in user-service. User with authUserId " + event.getAuthUserId() + " could not create profile.");
            throw new RuntimeException("Email " + event.getEmail() + " already exists.");
        }


        // Construir la entidad UserProfile a partir de los datos del evento
        UserProfile userProfile = UserProfile.builder()
                .authUserId(event.getAuthUserId()) // El ID del usuario del auth-service
                .name(event.getName())
                .surname(event.getSurname())
                .nickname(event.getNickname())
                .email(event.getEmail())
                .build();

        // Guardar el perfil en la base de datos del user-service
        UserProfile savedProfile = userProfileRepository.save(userProfile);
        System.out.println("User profile created successfully for authUserId: " + savedProfile.getAuthUserId() + ", Nickname: " + savedProfile.getNickname());
    }

    /**
     * (Opcional) Método para obtener un perfil de usuario por nickname, útil para el frontend o otros servicios.
     * @param nickname
     * @return
     */
    public Optional<UserProfile> getProfileByNickname(String nickname) {
        return userProfileRepository.findByNickname(nickname);
    }
}
package org.redlectora.user.repository;

import org.redlectora.user.model.UserProfile; // Importa la entidad UserProfile
import org.springframework.data.jpa.repository.JpaRepository; // Importa JpaRepository
import org.springframework.stereotype.Repository; // Marca la interfaz como un componente de repositorio de Spring

import java.util.Optional;

@Repository // Indica a Spring que esta interfaz es un "repositorio" de datos
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    // JpaRepository<T, ID> Proporciona métodos CRUD estándar (save, findById, findAll, delete, etc.)
    // T es la entidad (UserProfile)
    // ID es el tipo de la clave primaria de la entidad (Long)

    /**
     * Busca un perfil de usuario por su nickname.
     * Spring Data JPA generará la implementación de esta consulta automáticamente
     * basándose en el nombre del método.
     *
     * @param nickname El nickname a buscar.
     * @return Un Optional que contendrá el UserProfile si se encuentra, o estará vacío.
     */
    Optional<UserProfile> findByNickname(String nickname);

    /**
     * Verifica si un nickname ya existe en la base de datos.
     * Muy útil para la validación de unicidad antes de la creación.
     *
     * @param nickname El nickname a verificar.
     * @return true si el nickname ya existe, false en caso contrario.
     */
    boolean existsByNickname(String nickname);

    /**
     * Verifica si un email ya existe en la base de datos.
     *
     * @param email El email a verificar.
     * @return true si el email ya existe, false en caso contrario.
     */
    boolean existsByEmail(String email);

    /**
     * Busca un perfil de usuario por el ID que le asignó el Auth Service.
     * Esto es útil para vincular el perfil con las credenciales de autenticación.
     *
     * @param authUserId El ID de usuario del Auth Service.
     * @return Un Optional que contendrá el UserProfile si se encuentra, o estará vacío.
     */
    Optional<UserProfile> findByAuthUserId(Long authUserId);
}
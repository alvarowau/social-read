package org.redlectora.auth.repository;

import org.redlectora.auth.model.ERole;
import org.redlectora.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name); // Para buscar un rol por su nombre (ej. ROLE_USER)
}
package org.redlectora.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // 'user' es una palabra reservada en algunas BDs, mejor 'users'
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // Usamos email como el identificador único para login

    @Column(nullable = false)
    private String password; // La contraseña cifrada (hashed)

    @Column(columnDefinition = "boolean default false") // Por defecto, la cuenta estará DESACTIVADA
    private Boolean enabled; // Si la cuenta está activa (para confirmación por email)

    @Column(columnDefinition = "integer default 0")
    private Integer failedLoginAttempts; // Número de intentos de login fallidos

    private LocalDateTime accountLockedTime; // Momento en que la cuenta fue bloqueada (si aplica)

    @Column(columnDefinition = "boolean default false")
    private Boolean accountLocked; // Si la cuenta está bloqueada

    @ManyToMany(fetch = FetchType.EAGER) // Carga los roles inmediatamente
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Constructor de conveniencia para la creación de usuarios básicos (para registro)
    public User(String email, String password, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.enabled = false; // Por defecto, desactivada para confirmación por email
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.roles = roles;
    }
}
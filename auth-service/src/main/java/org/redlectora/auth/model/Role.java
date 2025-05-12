package org.redlectora.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // Almacena el nombre del enum (ej. "ROLE_USER")
    @Column(unique = true, nullable = false)
    private ERole name;

    public Role(ERole name) {
        this.name = name;
    }
}
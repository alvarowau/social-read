package org.redlectora.user.model;

import jakarta.persistence.*; // Importaciones de JPA para mapeo de entidades
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera getters, setters, equals, hashCode y toString
@Builder // Proporciona un patrón de construcción de objetos
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
@Entity // Indica que esta clase es una entidad JPA y se mapea a una tabla de base de datos
@Table(name = "user_profiles") // Especifica el nombre de la tabla en la base de datos
public class UserProfile {

    @Id // Marca el campo como la clave primaria de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Indica que el ID será generado por la base de datos (autoincremental)
    private Long id;

    @Column(nullable = false, unique = true) // La columna no puede ser nula y debe ser única
    private Long authUserId; // ¡IMPORTANTE! Este es el ID del usuario tal como lo gestiona el Auth Service.
    // Sirve como un identificador único para vincular el perfil al usuario autenticado.

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @Column(nullable = false, unique = true, length = 30) // El nickname debe ser único en el sistema
    private String nickname;

    @Column(nullable = false, unique = true, length = 100) // El email también debe ser único
    private String email;

    // Puedes añadir otros campos de perfil aquí si son necesarios para tu aplicación
    // @Column(length = 255)
    // private String bio;
    // private String profilePictureUrl;
}
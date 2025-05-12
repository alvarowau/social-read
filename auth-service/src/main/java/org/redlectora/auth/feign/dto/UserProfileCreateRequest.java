package org.redlectora.auth.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileCreateRequest {
    private String name;
    private String surname;
    private String nickname;
    private String email;
    private Long authUserId; // Para vincular el ID generado por auth-service con el perfil en user-service
}
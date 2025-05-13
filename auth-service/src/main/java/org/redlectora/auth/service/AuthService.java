package org.redlectora.auth.service;

import org.redlectora.auth.config.jwt.JwtService;
import org.redlectora.auth.dto.AuthenticationResponse;
import org.redlectora.auth.dto.RegisterRequest;
import org.redlectora.auth.event.UserCreatedEvent;
import org.redlectora.auth.exception.BadRequestException;
import org.redlectora.auth.feign.client.UserServiceClient;
import org.redlectora.auth.model.ERole;
import org.redlectora.auth.model.Role;
import org.redlectora.auth.model.User;
import org.redlectora.auth.repository.RoleRepository;
import org.redlectora.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;
    private final StreamBridge streamBridge;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserServiceClient userServiceClient, StreamBridge streamBridge, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.streamBridge = streamBridge;
        this.jwtService = jwtService;
    }

    public boolean checkNicknameExistence(String nickname) {
        System.out.println("DEBUG (AuthService): Checking nickname existence for: " + nickname);
        try {
            ResponseEntity<Boolean> response = userServiceClient.existsByNickname(nickname);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                System.out.println("DEBUG (AuthService): Nickname existence check result: " + response.getBody());
                return response.getBody();
            } else {
                System.err.println("ERROR (AuthService): Unexpected error when checking nickname. Status: " + response.getStatusCode());
                throw new RuntimeException("Error inesperado al verificar nickname en User Service. Estado: " + response.getStatusCode());
            }
        } catch (feign.FeignException.NotFound e) {
            System.err.println("ERROR (AuthService): Feign Error: User service endpoint for nickname check not found. Is user-service running and exposing the correct endpoint? " + e.getMessage());
            throw new RuntimeException("Error de comunicación con el servicio de usuarios: Endpoint de nickname no encontrado. " + e.getMessage(), e);
        } catch (feign.FeignException e) {
            System.err.println("ERROR (AuthService): Feign Error: Failed to check nickname availability with user-service. Status: " + e.status() + ", Message: " + e.getMessage());
            throw new RuntimeException("Fallo al verificar la disponibilidad del nickname con el servicio de usuarios. Código: " + e.status(), e);
        } catch (Exception e) {
            System.err.println("ERROR (AuthService): Unexpected error during nickname check: " + e.getMessage());
            throw new RuntimeException("Ocurrió un error inesperado al verificar el nickname: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        System.out.println("DEBUG (AuthService): Entering registerUser method for email: " + request.getEmail() + ", nickname: " + request.getNickname());

        // 1. Validar la unicidad del email (en la base de datos del auth-service)
        if (userRepository.existsByEmail(request.getEmail())) {
            System.out.println("DEBUG (AuthService): Email already in use: " + request.getEmail());
            throw new BadRequestException("Error: Email is already in use!");
        }
        System.out.println("DEBUG (AuthService): Email is unique: " + request.getEmail());

        // 2. Validar la unicidad del nickname (llamando al user-service de forma SÍNCRONA via FeignClient)
        boolean nicknameExists = checkNicknameExistence(request.getNickname());
        if (nicknameExists) {
            System.out.println("DEBUG (AuthService): Nickname already taken: " + request.getNickname());
            throw new BadRequestException("Error: Nickname is already taken!");
        }
        System.out.println("DEBUG (AuthService): Nickname is unique: " + request.getNickname());

        // 3. Crear el usuario en la base de datos del auth-service
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> {
            System.err.println("ERROR (AuthService): Default user role 'ROLE_USER' not found in DB.");
            return new RuntimeException("Error: Default user role not found.");
        });
        roles.add(userRole);
        user.setRoles(roles);

        System.out.println("DEBUG (AuthService): Attempting to save user to auth-service DB...");
        User savedUser = userRepository.save(user); // <-- Posible punto de fallo
        System.out.println("DEBUG (AuthService): User saved to auth-service DB. Auth ID: " + savedUser.getId());

        // 4. Publicar el evento UserCreatedEvent a Kafka (comunicación ASÍNCRONA)
        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder().authUserId(savedUser.getId()).name(request.getName()) // Asegúrate de que RegisterRequest tiene getName()
                .surname(request.getSurname()) // Asegúrate de que RegisterRequest tiene getSurname()
                .nickname(request.getNickname()).email(request.getEmail()).build();

        try {
            System.out.println("DEBUG (AuthService): Attempting to send UserCreatedEvent to Kafka for Auth ID: " + savedUser.getId());
            streamBridge.send("userProducer-out-0", userCreatedEvent);
            System.out.println("DEBUG (AuthService): UserCreatedEvent sent to Kafka successfully for user: " + request.getEmail() + " (Auth ID: " + savedUser.getId() + ")");
        } catch (Exception e) {
            System.err.println("ERROR (AuthService): Failed to send UserCreatedEvent to Kafka for user " + request.getEmail() + ": " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace completo para errores de Kafka
            // ¡Importante! Si Kafka falla aquí, deberías decidir si quieres que el registro del usuario falle también.
            // Por ahora, relanzamos la excepción para que la transacción se revierta.
            throw new RuntimeException("Fallo al enviar el evento de creación de usuario a Kafka.", e);
        }
        System.out.println("DEBUG (AuthService): Exiting registerUser method successfully.");
    }


    public AuthenticationResponse login(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return new AuthenticationResponse(token, userDetails.getUsername());
    }
}
package org.redlectora.auth.service;

// Importa tu clase AuditPublisher
import org.redlectora.auth.audit.AuditPublisher;

// Importa clases de configuracin y seguridad
import org.redlectora.auth.config.jwt.JwtService;


import org.redlectora.shared.user.event.UserCreatedEvent;
import org.redlectora.auth.dto.AuthenticationResponse;
import org.redlectora.auth.dto.RegisterRequest;
import org.redlectora.auth.exception.BadRequestException;
import org.redlectora.auth.feign.client.UserServiceClient;
import org.redlectora.auth.model.ERole;
import org.redlectora.auth.model.Role;
import org.redlectora.auth.model.User;
import org.redlectora.auth.repository.RoleRepository;
import org.redlectora.auth.repository.UserRepository;

import org.springframework.cloud.stream.function.StreamBridge; // Para enviar mensajes a Kafka
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class AuthService {

    // --- Inicializar el Logger ---
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // --- Constantes para tipos de eventos de auditora ---
    // Estos strings identifican la accin en el log de auditora.
    public static final String ACTION_TYPE_USER_REGISTER_ATTEMPT = "USER_REGISTER_ATTEMPT";
    public static final String ACTION_TYPE_USER_REGISTERED = "USER_REGISTERED"; // xito: Usuario guardado en Auth DB
    public static final String ACTION_TYPE_USER_REGISTER_FAILED = "USER_REGISTER_FAILED"; // Fallo en cualquier paso del registro
    public static final String ACTION_TYPE_NICKNAME_CHECK = "NICKNAME_AVAILABILITY_CHECK"; // Audita la llamada Feign al User Service
    public static final String ACTION_TYPE_KAFKA_EVENT_PUBLISH = "KAFKA_EVENT_PUBLISH"; // Audita el resultado de enviar eventos funcionales (ej. UserCreatedEvent)
    public static final String ACTION_TYPE_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    // Nota: La auditora de LOGIN_FAILED (intentos fallidos de contrasea) generalmente se hace con un Listener de Spring Security (AuthenticationFailureEvent)
    // Este mtodo 'login' slo se llama si la autenticacin ya fue exitosa.
    // ----------------------------------------------------


    // --- Inyeccin de dependencias ---
    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher; // Nuestro publicador de auditora
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient; // Cliente Feign para User Service
    private final StreamBridge streamBridge; // Para publicar eventos funcionales a Kafka
    private final JwtService jwtService; // Para generar JWTs


    // Constructor para inyectar dependencias
    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       UserServiceClient userServiceClient, StreamBridge streamBridge, JwtService jwtService,
                       AuditPublisher auditPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.streamBridge = streamBridge;
        this.jwtService = jwtService;
        this.auditPublisher = auditPublisher;
    }


    // --- Mtodo checkNicknameExistence CORREGIDO Y CON AUDITORA ---
    // Este mtodo encapsula la llamada Feign al User Service y audita su resultado.
    // Lanza una RuntimeException si hay fallo en la verificacin que debe detener el registro.
    private boolean checkNicknameExistence(String nickname, String email) { // Mantenemos email para el contexto de auditora en la auditora
        logger.debug("AUTH-SERVICE: Checking nickname existence for: {}", nickname);

        // userId para este evento es null porque ocurre durante el registro (usuario an no logueado)
        String userIdForAudit = null;

        Map<String, Object> details = new HashMap<>(); // Detalles para el evento de auditora NICKNAME_AVAILABILITY_CHECK
        details.put("nicknameToCheck", nickname);
        details.put("registrationEmail", email); // Contexto del registro

        Boolean nicknameExists = null; // Resultado booleano de la llamada Feign si es exitosa
        String checkOutcomeType = "Unknown Error"; // Tipo general del resultado (por defecto error antes del try)
        String checkOutcomeMessage = null; // Mensaje adicional del resultado

        try {
            // Realizar la llamada al User Service usando el cliente Feign
            ResponseEntity<Boolean> response = userServiceClient.existsByNickname(nickname);

            // Capturar el estado HTTP de la respuesta
            details.put("userServiceResponseStatus", response.getStatusCode().value());

            // Verificar si la respuesta fue exitosa (estado OK y cuerpo no nulo)
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.debug("AUTH-SERVICE: Nickname existence check received OK status.");
                nicknameExists = response.getBody(); // Obtener el resultado booleano (true/false)
                details.put("nicknameExists", nicknameExists); // Añadir el resultado booleano a los detalles del evento de auditora
                checkOutcomeType = "Success"; // Indicar que la llamada fue OK y el resultado es vlido
                checkOutcomeMessage = nicknameExists ? "Nickname already exists." : "Nickname is available.";
                logger.debug("AUTH-SERVICE: Nickname existence check result: {}", checkOutcomeMessage);

            } else {
                // Si la respuesta no es OK, loggear un warning y registrar en detalles
                logger.warn("AUTH-SERVICE: Nickname existence check received non-OK status: {}", response.getStatusCode());
                checkOutcomeType = "Unexpected Status";
                checkOutcomeMessage = "Received HTTP status: " + response.getStatusCode().value();
                details.put("checkOutcomeDetails", checkOutcomeMessage); // Registrar el estado inesperado

                // Si el estado no es OK, lanzamos excepcin para que el registro falle, ya que no pudimos verificar correctamente.
                throw new RuntimeException("Unexpected response status from User Service during nickname check: " + response.getStatusCode());
            }

        } catch (feign.FeignException e) {
            // Capturar excepciones especficas de Feign (ej. 404 Not Found, 500 Internal Server Error del otro servicio)
            logger.error("AUTH-SERVICE: Feign Error during nickname check for nickname {}: Status {}, Message: {}", nickname, e.status(), e.getMessage(), e);
            checkOutcomeType = "Feign Error";
            checkOutcomeMessage = "Feign Status " + e.status() + ": " + e.getMessage();
            details.put("feignStatus", e.status());
            details.put("errorMessage", e.getMessage());
            // Relanzar como RuntimeException para que el mtodo registerUser la capture y maneje el fallo del registro.
            throw new RuntimeException("Feign error during nickname check.", e);

        } catch (Exception e) {
            // Capturar cualquier otra excepcin inesperada (ej. Conexin rehusada antes de FeignException, problemas de red generales, timeouts)
            logger.error("AUTH-SERVICE: Unexpected Exception during nickname check for nickname {}: {}", nickname, e.getMessage(), e);
            checkOutcomeType = "Unexpected Exception";
            checkOutcomeMessage = e.getMessage();
            details.put("errorMessage", checkOutcomeMessage);
            // Relanzar como RuntimeException para que el mtodo registerUser la capture y maneje el fallo del registro.
            throw new RuntimeException("Unexpected error during nickname check.", e);

        } finally {
            // --- Publicar el evento de auditora SIEMPRE, despus de intentar la llamada ---
            // El bloque finally se ejecuta tanto si el cdigo en el 'try' termina normalmente como si se lanza una excepcin.
            // Esto asegura que SIEMPRE se publica un evento para cada intento de verificacin de nickname.
            details.put("checkOutcomeType", checkOutcomeType); // Aadir el tipo final de resultado del intento de verificacin
            if (checkOutcomeMessage != null) {
                details.put("checkOutcomeMessage", checkOutcomeMessage); // Aadir el mensaje final del resultado
            }
            // Si nicknameExists es nulo (por excepcin o estado inesperado en el try), no se aade a los detalles, lo cual est bien.
            // userIdForAudit es null en este mtodo, ya que el usuario an no est registrado/autenticado.
            auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_NICKNAME_CHECK, details);
        }

        // Si llegamos a este punto, significa que el 'try' se complet sin lanzar una excepcin.
        // En este caso, el resultado 'nicknameExists' tiene un valor booleano vlido.
        // Si se lanz una excepcin en el 'try' o en los 'catch', el mtodo habra terminado antes (relanzando la excepcin).
        return nicknameExists != null ? nicknameExists : false; // Devuelve el resultado booleano slo si la llamada fue exitosa.

    }


    // --- Mtodo registerUser CON AUDITORA COMPLETA ---
    @Transactional // Asegura transaccionalidad: si algo falla despus de guardar en DB, hace rollback
    public void registerUser(RegisterRequest request) {
        logger.debug("AUTH-SERVICE: Entering registerUser method for email: {}, nickname: {}", request.getEmail(), request.getNickname());

        String userIdForAudit = null; // ID del usuario. Inicialmente null, se obtiene despus de guardar en DB (si se guarda).

        // Mapa para detalles del evento de registro (para eventos ATTMPT y FAILED).
        // Estos detalles se usan si el proceso falla antes de obtener el userId real.
        Map<String, Object> auditDetailsOnFailure = new HashMap<>();
        auditDetailsOnFailure.put("requestEmail", request.getEmail());
        auditDetailsOnFailure.put("requestNickname", request.getNickname());
        // Puedes aadir la IP de la solicitud aqu si la obtienes del controlador (HttpServletRequest request)
        // auditDetailsOnFailure.put("registrationIp", getRequestIp()); // Si implementas getRequestIp()


        // --- Auditora: Intento de registro ---
        // Publicamos un evento al principio para registrar cada intento de registro.
        // userIdForAudit es null aqu.
        auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_USER_REGISTER_ATTEMPT, auditDetailsOnFailure);
        logger.info("AUTH-SERVICE: Audited registration attempt for email: {}", request.getEmail());


        try {
            // 1. Validar la unicidad del email (en la base de datos del auth-service)
            logger.debug("AUTH-SERVICE: Checking email uniqueness for: {}", request.getEmail());
            if (userRepository.existsByEmail(request.getEmail())) {
                logger.debug("AUTH-SERVICE: Email already in use: {}", request.getEmail());
                // Si falla, lanzamos BadRequestException, que se capta en el bloque catch (BadRequestException) abajo.
                throw new BadRequestException("Error: Email is already in use!");
            }
            logger.debug("AUTH-SERVICE: Email is unique: {}", request.getEmail());


            // 2. Validar la unicidad del nickname (llamando al user-service SÍNCRONA via FeignClient)
            logger.debug("AUTH-SERVICE: Checking nickname availability via User Service.");
            // checkNicknameExistence() audita internamente su resultado o fallo usando ACTION_TYPE_NICKNAME_CHECK.
            // Si checkNicknameExistence() lanza una excepcin (por fallo Feign o estado inesperado),
            // esa excepcin ser capturada por el bloque catch (RuntimeException) aqu.
            boolean nicknameExists = checkNicknameExistence(request.getNickname(), request.getEmail());

            if (nicknameExists) {
                logger.debug("AUTH-SERVICE: Nickname already taken: {}", request.getNickname());
                // Si el nickname existe, lanzamos BadRequestException, que se capta en el bloque catch (BadRequestException) abajo.
                throw new BadRequestException("Error: Nickname is already taken!");
            }
            logger.debug("AUTH-SERVICE: Nickname is unique: {}", request.getNickname());


            // 3. Crear el usuario en la base de datos del auth-service
            logger.debug("AUTH-SERVICE: Preparing to save user to auth-service DB...");
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEnabled(true); // Usuario habilitado por defecto al registrar
            user.setFailedLoginAttempts(0); // Reiniciar intentos fallidos
            user.setAccountLocked(false); // Cuenta no bloqueada inicialmente

            Set<Role> roles = new HashSet<>();
            Optional<Role> userRoleOptional = roleRepository.findByName(ERole.ROLE_USER);
            Role userRole = userRoleOptional.orElseThrow(() -> {
                // Si el rol ROLE_USER no se encuentra en la base de datos de Auth Service, esto es un error grave de configuracin.
                logger.error("AUTH-SERVICE: Default user role '{}' not found in DB.", ERole.ROLE_USER);
                // Relanzamos como RuntimeException para que el catch (RuntimeException) lo capture y la transaccin falle.
                return new RuntimeException("Error: Default user role not found in Auth Service DB.");
            });
            roles.add(userRole);
            user.setRoles(roles);

            // Guardar el usuario en la base de datos. Si JPA/DB lanza una excepcin, @Transactional har rollback.
            User savedUser = userRepository.save(user);
            userIdForAudit = savedUser.getId().toString(); // Obtener el ID DESPUÉS de guardar exitosamente en DB


            // --- Auditora: Usuario guardado exitosamente en DB de Auth ---
            // Este es el evento principal de xito del registro completo.
            // Publicamos USER_REGISTERED aqu, despus de guardar en DB y obtener el ID.
            Map<String, Object> registeredUserDetails = new HashMap<>(); // Detalles especficos para este evento de xito
            registeredUserDetails.put("authUserId", userIdForAudit);
            registeredUserDetails.put("registeredEmail", savedUser.getEmail());
            registeredUserDetails.put("registeredNickname", request.getNickname());
            // Opcional: Aadir otros detalles si quieres (ej. roles asignados)
            // registeredUserDetails.put("roles", savedUser.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList())); // Necesita Collectors.toList() y Stream
            // Asegurate de que org.redlectora.auth.model.Role tiene un mtodo getName() que devuelve algo como ERole

            auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_USER_REGISTERED, registeredUserDetails);
            logger.info("AUTH-SERVICE: Audited successful user registration to DB for Auth ID: {}", userIdForAudit);


            // 4. Publicar el evento UserCreatedEvent a Kafka (comunicacin ASNCRONA)
            // Este evento es para que otros servicios (como User Service) acten (ej. crear perfil en su DB).
            // Usamos la clase UserCreatedEvent del mdulo compartido si la moviste all.
            UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                    .authUserId(savedUser.getId())
                    // Ajusta los getters segn tus modelos User y RegisterRequest
                    .name(request.getName()) // Asumiendo que RegisterRequest tiene getName()
                    .surname(request.getSurname()) // Asumiendo que RegisterRequest tiene getSurname()
                    .nickname(request.getNickname())
                    .email(request.getEmail()) // Asumiendo que RegisterRequest tiene getEmail()
                    .build();

            try {
                logger.debug("AUTH-SERVICE: Attempting to send UserCreatedEvent to Kafka for Auth ID: {}", savedUser.getId());
                // "userCreatedProducer-out-0" debe coincidir con el nombre del binding de salida para este evento en application.yml
                // streamBridge.send devuelve true si el envo fue aceptado por el binder, no garantiza que lleg a Kafka.
                boolean kafkaSuccess = streamBridge.send("userCreatedProducer-out-0", userCreatedEvent);

                // --- Auditora: Resultado de publicacin de UserCreatedEvent ---
                // Auditamos el resultado del intento de publicacin del evento funcional UserCreatedEvent.
                Map<String, Object> kafkaPublishDetails = new HashMap<>();
                kafkaPublishDetails.put("eventType", "UserCreatedEvent");
                kafkaPublishDetails.put("authUserId", userIdForAudit);
                kafkaPublishDetails.put("publishSuccess", kafkaSuccess);
                kafkaPublishDetails.put("message", kafkaSuccess ? "Event published successfully by StreamBridge." : "StreamBridge failed to send.");

                auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_KAFKA_EVENT_PUBLISH, kafkaPublishDetails); // Auditar resultado de publicacin Kafka

                if (!kafkaSuccess) {
                    // Si streamBridge.send devuelve false, decidimos que el registro falle, ya que el evento funcional es crucial.
                    // Relanzamos como RuntimeException para que el catch (RuntimeException) la capture y la transaccin de DB se revierta.
                    throw new RuntimeException("Failed to send UserCreatedEvent to Kafka (StreamBridge returned false).");
                }
                logger.info("AUTH-SERVICE: UserCreatedEvent sent to Kafka successfully for user: {} (Auth ID: {})", request.getEmail(), savedUser.getId());


            } catch (Exception kafkaException) {
                // Este catch captura excepciones DURANTE el send() (ej. Kafka no disponible, serializacin falla)
                logger.error("AUTH-SERVICE: Exception during UserCreatedEvent Kafka publish for Auth ID {}: {}", savedUser.getId(), kafkaException.getMessage(), kafkaException);

                // --- Auditora: Fallo de publicacin de UserCreatedEvent por Excepcin ---
                // Auditamos el fallo de publicacin por excepcin.
                Map<String, Object> kafkaPublishDetails = new HashMap<>();
                kafkaPublishDetails.put("eventType", "UserCreatedEvent");
                kafkaPublishDetails.put("authUserId", userIdForAudit);
                kafkaPublishDetails.put("publishSuccess", false); // Asegurar que el estado es false
                kafkaPublishDetails.put("message", "Exception during Kafka publish: " + kafkaException.getMessage());
                // Opcional: Incluir stacktrace parcial o completo en detalles puede ser til para depurar
                // kafkaPublishDetails.put("exceptionStacktrace", getStackTrace(kafkaException)); // Si implementas getStackTrace()

                auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_KAFKA_EVENT_PUBLISH, kafkaPublishDetails); // Auditar fallo de publicacin Kafka por excepcin

                // Relanzamos como RuntimeException para que el catch (RuntimeException) la capture y la transaccin de DB se revierta.
                throw new RuntimeException("Exception occurred while publishing UserCreatedEvent to Kafka.", kafkaException);
            }

            // Si llegamos a este punto, significa que todo el proceso de registro, guardar en DB y publicar evento funcional fue exitoso.
            logger.debug("AUTH-SERVICE: Exiting registerUser method successfully for Auth ID: {}", userIdForAudit);


        } catch (BadRequestException bre) {
            // Captura excepciones de validacin (Email ya usado, Nickname ya usado)
            logger.warn("AUTH-SERVICE: Registration failed due to BadRequestException: {}", bre.getMessage());
            // userIdForAudit an es null aqu porque el usuario no se guard en DB en caso de estas validaciones.
            auditDetailsOnFailure.put("failureReasonType", "BadRequestException"); // Tipo de fallo
            auditDetailsOnFailure.put("message", bre.getMessage()); // Mensaje especfico (ej. "Email is already in use!" o "Nickname is already taken!")
            // Publicar evento de fallo de registro. userIdForAudit es null.
            auditPublisher.publishEvent(null, ACTION_TYPE_USER_REGISTER_FAILED, auditDetailsOnFailure);
            throw bre; // Relanzar la excepcin original para el controlador.
        } catch (RuntimeException re) {
            // Captura RuntimeExceptions lanzadas en el try (problemas de Feign relanzados, rol no encontrado, fallo de Kafka relanzado).
            logger.error("AUTH-SERVICE: Registration failed due to RuntimeException: {}", re.getMessage(), re);
            auditDetailsOnFailure.put("failureReasonType", "RuntimeException"); // Tipo de fallo
            auditDetailsOnFailure.put("message", re.getMessage()); // Mensaje de la excepcin
            // userIdForAudit puede ser null si el fallo ocurri antes de guardar/obtener ID, o el ID si ocurri despus.
            // El valor de userIdForAudit es el ltimo que tuvo antes de la excepcin.
            auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_USER_REGISTER_FAILED, auditDetailsOnFailure); // Auditar fallo de registro
            throw re; // Relanzar la excepcin original para el controlador.
        } catch (Exception generalException) {
            // Captura cualquier otra excepcin inesperada (errores de DB no manejados por JPA especficos, etc.)
            logger.error("AUTH-SERVICE: Registration failed due to unexpected exception: {}", generalException.getMessage(), generalException);
            auditDetailsOnFailure.put("failureReasonType", "Unexpected Exception"); // Tipo de fallo general
            auditDetailsOnFailure.put("message", generalException.getMessage()); // Mensaje de la excepcin
            // userIdForAudit puede ser null o el ID.
            auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_USER_REGISTER_FAILED, auditDetailsOnFailure); // Auditar fallo de registro
            // Relanzamos como una nueva RuntimeException con un mensaje genrico y la excepcin original como causa.
            throw new RuntimeException("An unexpected error occurred during registration.", generalException);
        }

        // El evento de xito principal (ACTION_TYPE_USER_REGISTERED) ya se publica dentro del try despus de guardar en DB.
        // Si no se lanz ninguna excepcin, el mtodo simplemente terminar aqu (void).
    }


    // --- Mtodo login CON AUDITORA DE XITO ---
    public AuthenticationResponse login(Authentication authentication) {
        // Este mtodo se llama DESPUS de que Spring Security ha autenticado exitosamente al usuario.
        // La autenticacin fallida (contrasea incorrecta, usuario no encontrado) es manejada por Spring Security
        // ANTES de llegar a este mtodo. Para auditar fallos de login, necesitas configurar un
        // AuthenticationFailureHandler o usar un Listener para AuthenticationFailureEvent de Spring Security.

        // El principal de Spring Security (authentication.getPrincipal()) es el UserDetails que devuelves de tu implementacin de UserDetailsService.
        // Si tu UserDetailsService devuelve tu entidad User, puedes castear directamente.
        // Si devuelve el UserDetails por defecto de Spring Security, necesitas buscar al usuario en DB por el username (email).
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // En tu caso, el username de UserDetails es el email.

        // Obtn el usuario de la base de datos para obtener su ID real (si UserDetails no lo tiene o para asegurarte)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    // Este escenario NO debera ocurrir despus de una autenticacin exitosa.
                    // Sugiere un estado inconsistente (usuario autenticado en Spring Security pero no en la DB).
                    logger.error("AUTH-SERVICE: Authenticated user email not found in DB AFTER successful authentication: {}", email);
                    // Podras auditar un evento crtico aqu si quieres, o simplemente lanzar una excepcin.
                    // auditPublisher.publishEvent(null, "CRITICAL_AUTH_ERROR", Map.of("message", "Authenticated user missing from DB", "email", email));
                    return new RuntimeException("Authenticated user not found in database after successful authentication.");
                });
        String userIdForAudit = user.getId().toString(); // Obtener el ID del usuario de la DB para la auditora

        // --- Auditora: Login Exitoso ---
        // Publicamos el evento de login exitoso.
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("authenticatedEmail", email);
        // Opcional: Si pudieras obtener la IP de la solicitud aqu (ej. inyectando HttpServletRequest en el controlador y pasndola), aadela
        // auditDetails.put("loginIp", getRequestIp()); // Necesitas implementar getRequestIp()

        // Publicar el evento de auditora con el ID del usuario.
        auditPublisher.publishEvent(userIdForAudit, ACTION_TYPE_LOGIN_SUCCESS, auditDetails);
        logger.info("AUTH-SERVICE: Audited successful login for user {}", email);

        // Generar el token JWT y devolver la respuesta de autenticacin
        String token = jwtService.generateToken(userDetails);
        return new AuthenticationResponse(token, email); // Devolvemos el token y el email/username
    }


    // --- Mtodos de ejemplo (comentados) ---

    // Mtodo de ejemplo para obtener la IP de la solicitud si necesitas auditarla
    // Debera ser llamado desde un controlador o un servicio con acceso al RequestAttributes.
    // Importaciones necesarias: org.springframework.web.context.request.RequestContextHolder, org.springframework.web.context.request.ServletRequestAttributes, jakarta.servlet.http.HttpServletRequest
     /*
     private String getRequestIp() {
        try {
            ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = sra.getRequest();
            String clientIp = request.getHeader("X-Forwarded-For"); // til si usas proxy inverso como Gateway
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getRemoteAddr(); // IP directa del cliente
            }
            return clientIp;
        } catch (Exception e) {
            // No se pudo obtener la IP (ej. no en contexto de solicitud web)
            logger.warn("AUTH-SERVICE: Could not get request IP address", e);
            return "Unknown/N/A";
        }
     }
     */

    // Mtodo de ejemplo para obtener el stacktrace como string para detalles de auditora (til en errores)
    // Importaciones necesarias: java.io.PrintWriter, java.io.StringWriter
      /*
       private String getStackTrace(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
       }
      */

}
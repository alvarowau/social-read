package org.redlectora.user.confg;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // Asegúrate de usar OncePerRequestFilter

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component // Asegúrate de que Spring detecte este filtro como un bean
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter { // OncePerRequestFilter garantiza que se ejecuta una vez por solicitud

    // Nombres de las cabeceras que esperamos del Gateway
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Leer las cabeceras del request
        String userId = request.getHeader(USER_ID_HEADER);
        String userRolesHeader = request.getHeader(USER_ROLES_HEADER);

        // Puedes añadir logging aquí para depurar si los headers llegan
        // logger.debug("Checking headers: {}: {}, {}: {}", USER_ID_HEADER, userId, USER_ROLES_HEADER, userRolesHeader);

        // 2. Si las cabeceras de usuario están presentes y no son nulas/vacías
        if (userId != null && !userId.trim().isEmpty()) {

            // 3. Crear la lista de roles/autoridades
            List<GrantedAuthority> authorities = null;
            if (userRolesHeader != null && !userRolesHeader.trim().isEmpty()) {
                // Esperamos que los roles vengan separados por comas (ej. "ROLE_USER,ROLE_ADMIN")
                authorities = Arrays.stream(userRolesHeader.split(","))
                        .map(role -> role.trim()) // Eliminar espacios en blanco
                        .filter(role -> !role.isEmpty()) // Ignorar roles vacíos resultantes
                        .map(SimpleGrantedAuthority::new) // Convertir cada rol a SimpleGrantedAuthority
                        .collect(Collectors.toList());
            }

            // 4. Crear el objeto de autenticación
            // Usamos UsernamePasswordAuthenticationToken, es una implementación básica que sirve aquí.
            // El 'principal' es el ID del usuario, las 'credentials' son null (ya autenticado), las 'authorities' son los roles.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // 5. Establecer el objeto de autenticación en el SecurityContextHolder
            // Esto es lo que le dice a Spring Security que la solicitud está autenticada
            // y quién es el usuario y qué roles tiene.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // logger.debug("User authenticated from headers: {}", userId);

        } else {
            // Si las cabeceras no están presentes, significa que la solicitud no fue autenticada por el Gateway.
            // Spring Security continuará la cadena y aplicará sus reglas de autorización
            // (ej. denegará acceso a anyRequest().authenticated()).
            // logger.debug("No user headers found. Request will proceed unauthenticated.");
        }

        // Continuar la cadena de filtros
        // Si se estableció la autenticación, los siguientes filtros de Spring Security y tu controlador
        // verán la solicitud como autenticada.
        filterChain.doFilter(request, response);
    }
}
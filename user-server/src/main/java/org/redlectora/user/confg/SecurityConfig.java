package org.redlectora.user.confg;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Importación correcta

@Configuration
@EnableWebSecurity // Habilita la configuración de seguridad web
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;

    // Inyecta tu filtro personalizado
    public SecurityConfig(GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter) {
        this.gatewayHeaderAuthenticationFilter = gatewayHeaderAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF (Cross-Site Request Forgery) ya que usaremos autenticación basada en tokens sin sesiones
                .csrf(AbstractHttpConfigurer::disable)

                // Deshabilitar la autenticación básica por defecto de Spring Security
                .httpBasic(AbstractHttpConfigurer::disable)

                // Deshabilitar la autenticación basada en formularios por defecto
                .formLogin(AbstractHttpConfigurer::disable)

                // Configurar la política de creación de sesiones a STATELESS (sin estado)
                // Ya que el Gateway maneja el estado de la sesión (si es que se usa alguna, aunque aquí no)
                // y cada solicitud es autenticada por el token/headers.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configurar las reglas de autorización para las peticiones
                .authorizeHttpRequests(authorize -> authorize
                        // Define aquí las rutas que quieres permitir sin autenticación
                        // ¡OJO! Estas rutas son dentro del USER-SERVICE, después de que el Gateway ha ruteado.
                        // Si usaste StripPrefix en el Gateway, estas rutas deben coincidir con lo que llega después del StripPrefix.
                        // Si NO usaste StripPrefix (nuestra última corrección), estas rutas deben ser las completas esperadas por el USER-SERVICE.
                        // Por ejemplo, si USER-SERVICE espera "/api/users/publico"
                        .requestMatchers("/api/users/publico").permitAll() // Permite el acceso a la ruta pública sin autenticación
                        // .requestMatchers("/otros/endpoints/publicos").permitAll() // Otras rutas públicas si las hay

                        // Define aquí las rutas que requieren autenticación
                        // Cualquier otra solicitud (no "permitAll") requerirá autenticación
                        .anyRequest().authenticated() // Todas las demás rutas requieren que la solicitud esté autenticada
                )

                // Añadir nuestro filtro personalizado antes del filtro de autenticación estándar de Spring Security
                // Este filtro leerá los headers y establecerá el contexto de seguridad
                .addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // *** IMPORTANTE: No necesitas definir AuthenticationManager ni AuthenticationProvider
    // en este escenario típico de microservicio detrás de Gateway que usa headers.
    // Nuestro filtro personalizado GatewayHeaderAuthenticationFilter se encarga de crear el
    // objeto Authentication y ponerlo en el SecurityContextHolder, que es lo que Spring Security
    // necesita para que `authenticated()` funcione y para la autorización basada en roles (@PreAuthorize). ***
}
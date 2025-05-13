package org.redlectora.gateway.config; // O el paquete de configuración que uses

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // Habilita la seguridad web reactiva
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // Deshabilita la protección CSRF si no la necesitas (común en APIs sin cookies de sesión)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Configura las reglas de autorización para las solicitudes
                .authorizeExchange(exchanges -> exchanges
                        // Permite el acceso sin autenticación a tu ruta pública
                        .pathMatchers("/api/users/publico").permitAll() // <-- Configura tu ruta pública aquí
                        .pathMatchers("/api/auth/register").permitAll()
                        .pathMatchers("/api/auth/login").permitAll()
                        .pathMatchers("/api/auth/**").permitAll() // <-- Si tu ruta de auth también es pública
                        .pathMatchers("/eureka/**").permitAll() // <-- Permite Eureka si es necesario
                        .pathMatchers("/actuator/**").permitAll() // <-- Permite Actuator si es necesario
                        // Todas las demás solicitudes requieren autenticación (serán manejadas por tu AuthenticationFilter después)
                        .anyExchange().authenticated() // Todas las demás requieren que el usuario esté autenticado de alguna manera (tu filtro se encargará de esto)
                )
                // Configura el manejo de excepciones para acceso denegado (opcional pero útil)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((exchange, exception) -> {
                            // Lógica para respuestas 401 no autenticado
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, exception) -> {
                            // Lógica para respuestas 403 acceso denegado (si usas roles/scopes después)
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                );
        // Si no vas a usar seguridad basada en sesión (típico en microservicios con JWT)
        // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        return http.build();
    }

    // No necesitas un PasswordEncoder si solo validas JWT.
    // Si usas autenticación básica o basada en formularios con passwords, lo necesitarías aquí.
    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new BCryptPasswordEncoder();
    // }

    // Tampoco necesitas UserDetailsService/ReactiveUserDetailsService si solo usas JWT para validar identidad.

}
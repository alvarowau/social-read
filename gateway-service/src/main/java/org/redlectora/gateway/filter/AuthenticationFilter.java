package org.redlectora.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro para autenticar solicitudes en el API Gateway.
 * Verifica el token JWT y permite o deniega el acceso según corresponda.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    // No usamos el Logger de SLF4J por ahora
    // private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Value("${application.security.jwt.secret}")
    private String secretKey;

    public AuthenticationFilter() {
        super(Config.class);
        System.out.println("[AUTH FILTER INFO] AuthenticationFilter initialized."); // Usamos System.out.println
    }

    @Override
    public GatewayFilter apply(Config config) {
        // Este es el método principal del filtro, se ejecuta por cada solicitud que pasa por él
        return (exchange, chain) -> {
            String requestPath = exchange.getRequest().getURI().getPath();
            System.out.println("[AUTH FILTER DEBUG] START processing request for path: " + requestPath); // Usamos System.out.println

            // Obtener la lista de rutas públicas configuradas para este filtro
            List<String> publicPaths = config.getPublicPaths();
            // Imprimir la lista de rutas públicas configuradas
            System.out.println("[AUTH FILTER DEBUG] Configured public paths: " + (publicPaths != null ? publicPaths : "null")); // Usamos System.out.println

            // Verificar si la ruta actual es una ruta pública
            if (isPublicPath(publicPaths, requestPath)) {
                System.out.println("[AUTH FILTER DEBUG] Path " + requestPath + " IS public, skipping authentication"); // Usamos System.out.println
                return chain.filter(exchange); // Continuar la cadena de filtros sin autenticación
            } else {
                System.out.println("[AUTH FILTER DEBUG] Path " + requestPath + " is NOT public, proceeding with authentication"); // Usamos System.out.println
            }

            // Si la ruta no es pública, intentar obtener y validar el token JWT
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            System.out.println("[AUTH FILTER DEBUG] Authorization header: " + (authorizationHeader != null ? authorizationHeader.substring(0, Math.min(authorizationHeader.length(), 20)) + (authorizationHeader.length() > 20 ? "..." : "") : "null")); // Usamos System.out.println

            // Verificar si el header Authorization está presente y tiene el formato correcto
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                System.out.println("[AUTH FILTER WARN] Authorization header missing or invalid for path: " + requestPath); // Usamos System.out.println
                return onError(exchange, "Authorization header is missing or invalid", HttpStatus.UNAUTHORIZED); // Devolver 401
            }

            // Extraer el token de la cadena "Bearer "
            String token = authorizationHeader.substring(7);
            System.out.println("[AUTH FILTER DEBUG] Extracted token (first 20 chars): " + (token.length() > 20 ? token.substring(0, 20) + "..." : token)); // Usamos System.out.println

            if (token.isEmpty()) {
                System.out.println("[AUTH FILTER WARN] JWT token is empty for path: " + requestPath); // Usamos System.out.println
                return onError(exchange, "JWT token is empty", HttpStatus.UNAUTHORIZED); // Devolver 401
            }

            try {
                // Configurar el parser JWT con la clave secreta
                Jws<Claims> claimsJws = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))) // Especificar charset UTF-8
                        .build()
                        .parseClaimsJws(token);

                // Obtener los claims (cuerpo) del token
                Claims claims = claimsJws.getBody();
                final String userId = claims.getSubject(); // El 'subject' (sub) usualmente contiene el ID del usuario

                System.out.println("[AUTH FILTER DEBUG] Token validated for user ID: " + userId); // Usamos System.out.println

                // Intentar obtener roles (manejando diferentes posibles formatos)
                List<String> userRolesList;
                try {
                    // Intenta directamente como lista
                    userRolesList = claims.get("roles", List.class);
                    System.out.println("[AUTH FILTER DEBUG] Roles found as List: " + userRolesList); // Usamos System.out.println
                } catch (Exception e) {
                    // Si falla, intenta como un objeto genérico (puede ser una cadena, etc.)
                    Object rolesObject = claims.get("roles");
                    System.out.println("[AUTH FILTER DEBUG] Roles not found as List, found as Object: " + rolesObject); // Usamos System.out.println
                    userRolesList = rolesObject != null ? List.of(rolesObject.toString()) : null; // Convierte a lista de un solo elemento si no es null
                    System.out.println("[AUTH FILTER DEBUG] Roles converted to list: " + userRolesList); // Usamos System.out.println
                }

                final List<String> userRoles = userRolesList;
                System.out.println("[AUTH FILTER DEBUG] Final user roles: " + userRoles); // Usamos System.out.println

                // Mutar (modificar) la solicitud para añadir headers con info del usuario autenticado
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(originalRequest -> {
                            originalRequest.header("X-User-Id", userId != null ? userId : "anonymous"); // Añadir header con ID de usuario
                            System.out.println("[AUTH FILTER DEBUG] Added header X-User-Id: " + (userId != null ? userId : "anonymous")); // Usamos System.out.println

                            if (userRoles != null && !userRoles.isEmpty()) {
                                // Unir los roles en una cadena separada por comas para el header
                                String rolesHeaderValue = userRoles.stream()
                                        .filter(role -> role != null && !role.trim().isEmpty()) // Limpiar roles vacíos o nulos
                                        .collect(Collectors.joining(","));
                                if (!rolesHeaderValue.isEmpty()) {
                                    originalRequest.header("X-User-Roles", rolesHeaderValue); // Añadir header con roles
                                    System.out.println("[AUTH FILTER DEBUG] Added header X-User-Roles: " + rolesHeaderValue); // Usamos System.out.println
                                } else {
                                    System.out.println("[AUTH FILTER DEBUG] User has roles list, but it contains only null/empty entries."); // Usamos System.out.println
                                }
                            } else {
                                System.out.println("[AUTH FILTER DEBUG] No roles found or roles list is null/empty."); // Usamos System.out.println
                            }
                        })
                        .build();
                System.out.println("[AUTH FILTER DEBUG] Request mutated with user info headers."); // Usamos System.out.println

                // Continuar la cadena de filtros con la solicitud modificada
                System.out.println("[AUTH FILTER DEBUG] Proceeding with filter chain for path: " + requestPath); // Usamos System.out.println
                return chain.filter(mutatedExchange);

            } catch (ExpiredJwtException e) {
                System.out.println("[AUTH FILTER WARN] JWT token expired for path: " + requestPath + ". Error: " + e.getMessage()); // Usamos System.out.println
                return onError(exchange, "JWT token expired", HttpStatus.UNAUTHORIZED); // Token expirado -> 401
            } catch (SignatureException e) {
                System.out.println("[AUTH FILTER WARN] Invalid JWT signature for path: " + requestPath + ". Error: " + e.getMessage()); // Usamos System.out.println
                return onError(exchange, "Invalid JWT signature", HttpStatus.UNAUTHORIZED); // Firma inválida -> 401
            } catch (MalformedJwtException e) {
                System.out.println("[AUTH FILTER WARN] Malformed JWT token for path: " + requestPath + ". Error: " + e.getMessage()); // Usamos System.out.println
                return onError(exchange, "Malformed JWT token", HttpStatus.UNAUTHORIZED); // Token mal formado -> 401
            } catch (UnsupportedJwtException e) {
                System.out.println("[AUTH FILTER WARN] Unsupported JWT token for path: " + requestPath + ". Error: " + e.getMessage()); // Usamos System.out.println
                return onError(exchange, "Unsupported JWT token", HttpStatus.UNAUTHORIZED); // Token no soportado -> 401
            } catch (IllegalArgumentException e) {
                System.out.println("[AUTH FILTER ERROR] JWT validation failed due to invalid argument for path: " + requestPath + ". Error: " + e.getMessage()); // Usamos System.out.println
                e.printStackTrace(); // Imprimir stack trace a System.err
                return onError(exchange, "JWT validation failed: Invalid argument", HttpStatus.INTERNAL_SERVER_ERROR); // Argumento inválido -> 500
            } catch (Exception e) {
                System.out.println("[AUTH FILTER ERROR] Unexpected error during JWT validation for path: " + requestPath + ". Error: " + e.getMessage()); // Usamos System.out.println
                e.printStackTrace(); // Imprimir stack trace a System.err
                return onError(exchange, "JWT validation failed: Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR); // Cualquier otro error -> 500
            }
        };
    }

    // Método para verificar si una ruta es pública
    private boolean isPublicPath(List<String> publicPaths, String path) {
        // Imprimir si la lista de rutas públicas no se cargó o está vacía
        if (publicPaths == null || publicPaths.isEmpty()) {
            System.out.println("[AUTH FILTER DEBUG] AuthenticationFilter.isPublicPath: publicPaths config is null or empty. Path " + path + " is NOT public by default.");
            return false;
        }
        // Iterar sobre las rutas públicas configuradas
        boolean isPublic = publicPaths.stream().anyMatch(publicPath -> {
            // Imprimir cada comparación individual
            // Asegúrate de que publicPath no sea null en la lista
            boolean matches = (publicPath != null) && path.startsWith(publicPath);
            System.out.println("[AUTH FILTER DEBUG] AuthenticationFilter.isPublicPath: Checking if request path '" + path + "' starts with configured public path '" + publicPath + "': " + matches);
            return matches; // Retorna true si encuentra alguna coincidencia
        });
        // Imprimir el resultado final de la verificación
        System.out.println("[AUTH FILTER DEBUG] AuthenticationFilter.isPublicPath: Final result for path '" + path + "': " + isPublic);
        return isPublic; // Retorna true si alguna ruta pública coincidió, false en caso contrario
    }

    // Método para manejar errores y devolver una respuesta
    private Mono<Void> onError(ServerWebExchange exchange, String errMsg, HttpStatus status) {
        System.out.println("[AUTH FILTER ERROR] AuthenticationFilter.onError: Returning status " + status + " with message '" + errMsg + "' for path: " + exchange.getRequest().getURI().getPath()); // Usamos System.out.println
        exchange.getResponse().setStatusCode(status); // Establecer el código de estado HTTP
        // Opcional: podrías añadir un cuerpo a la respuesta para dar más detalles al cliente
        // DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errMsg.getBytes(StandardCharsets.UTF_8));
        // return exchange.getResponse().writeWith(Mono.just(buffer));
        return exchange.getResponse().setComplete(); // Completar la respuesta (sin cuerpo en este caso simple)
    }

    // Clase de configuración para el filtro
    public static class Config {
        private List<String> publicPaths; // Lista de rutas consideradas públicas

        public List<String> getPublicPaths() {
            return publicPaths;
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths;
        }
    }
}
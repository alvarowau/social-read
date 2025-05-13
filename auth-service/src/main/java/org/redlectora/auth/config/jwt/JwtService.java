package org.redlectora.auth.config.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service // Anotación para que Spring la gestione como un Bean de servicio
public class JwtService {

    @Value("${application.security.jwt.secret}")
    private String secret; // ¡Ahora no tiene asignación directa!

    // Elimina la asignación directa y añade la anotación @Value
    @Value("${application.security.jwt.expiration}")
    private long expiration;

    // Opcional: si solo inyectas campos con @Value, el constructor por defecto es suficiente.
    // Si inyectaras otros Beans (@Autowired), lo harías por constructor.
    /*
    // Ejemplo si inyectaras algo más:
    private final SomeOtherBean someOtherBean;

    public JwtService(SomeOtherBean someOtherBean) {
        this.someOtherBean = someOtherBean;
    }
    */

    // Método para generar el token. Recibe los detalles del usuario.
    public String generateToken(UserDetails userDetails) {
        // *** AQUÍ ES DONDE PONDREMOS LA LÓGICA REAL DE GENERACIÓN DEL TOKEN EN EL SIGUIENTE PASO ***
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        List<String> roles = userDetails.getAuthorities().stream().map(
                GrantedAuthority::getAuthority
        ).toList();
        claims.put("roles", roles);


        // Esto es solo para que compile y veas que puedes acceder a 'secret' y 'expiration'
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // Más adelante, podríamos añadir métodos para validar tokens, etc.

}
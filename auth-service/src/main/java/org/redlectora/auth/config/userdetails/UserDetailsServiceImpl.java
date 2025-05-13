package org.redlectora.auth.config.userdetails;

import org.redlectora.auth.model.Role;
import org.redlectora.auth.model.User;
import org.redlectora.auth.repository.RoleRepository;
import org.redlectora.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException { // Cambié el nombre del parámetro a 'email' para mayor claridad

        // 1. Buscar al usuario por email
        Optional<User> optionalUser = userRepository.findByEmail(email);

        // Lanzar excepción si no se encuentra
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
        }

        // 2. Obtener el objeto User cuando sí existe
        User user = optionalUser.get();

        // 3. Obtener los roles del usuario y convertirlos a GrantedAuthority
        Set<Role> roles = user.getRoles(); // Obtiene el Set de entidades Role del User
        List<GrantedAuthority> authorities = new ArrayList<>(); // O una lista/conjunto mutable

        for (Role role : roles) {
            // Convertir el Enum ERole a String (ej. "ROLE_USER") y crear un SimpleGrantedAuthority
            authorities.add(new SimpleGrantedAuthority(role.getName().name()));
        }

        // 4. Construir y devolver el objeto UserDetails de Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),       // El "username" para Spring Security (tu email)
                user.getPassword(),    // La contraseña HASHEADA tal cual de la base de datos
                user.getEnabled(),     // Si la cuenta está habilitada (del campo 'enabled' de tu User)
                true,                  // ¿La cuenta no ha expirado? (True por ahora, podrías añadir lógica)
                !isCredentialsExpired(user), // ¿Las credenciales (password) no han expirado? (Implementa lógica si aplica)
                !user.getAccountLocked(), // ¿La cuenta no está bloqueada? (del campo 'accountLocked' de tu User)
                authorities            // La colección de GrantedAuthority (tus roles)
        );
    }

    // Podrías añadir un método auxiliar si tienes lógica para expiración de credenciales
    private boolean isCredentialsExpired(User user) {
        // Implementa tu lógica si tienes una fecha de última actualización de contraseña, etc.
        // Por defecto, retornamos false para indicar que no han expirado.
        return false;
    }
}

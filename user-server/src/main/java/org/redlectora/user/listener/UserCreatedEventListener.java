package org.redlectora.user.listener;

import org.redlectora.shared.user.event.UserCreatedEvent;
import org.redlectora.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class UserCreatedEventListener {

    private final UserService userService;

    public UserCreatedEventListener(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public Consumer<UserCreatedEvent> userCreatedEventConsumer() {
        return event -> {
            // ¡¡¡CAMBIO AQUÍ: Usamos getNickname() en lugar de username()!!!
            System.out.println("UserCreatedEvent received for user: " + event.getNickname());

            // Y aquí, llamamos al método createUserProfile con el evento completo,
            // ya que tu UserService espera el objeto UserCreatedEvent completo.
            userService.createUserProfile(event);
        };
    }
}
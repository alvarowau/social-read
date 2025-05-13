package org.redlectora.user.controller;


import lombok.RequiredArgsConstructor;
import org.redlectora.user.model.UserProfile;
import org.redlectora.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController // Indica que esta clase es un controlador REST
@RequestMapping("/publico")
public class PublicoController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(PublicoController.class);
    public PublicoController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping()
    public ResponseEntity<List<UserProfile>> getAllPublico(
            // <-- Opcional: Inyecta todas las cabeceras para verlas en el log
            @RequestHeader Map<String, String> headers
    ){
        // <-- Añade logging para ver que llega al endpoint publico
        logger.info("--> USER-SERVICE: Recibida solicitud en /api/users/publico");
        logger.debug("--> USER-SERVICE: Cabeceras recibidas en /publico: {}", headers); // Usa debug para no llenar el log

        return ResponseEntity.ok(userService.getAll());
    }
}

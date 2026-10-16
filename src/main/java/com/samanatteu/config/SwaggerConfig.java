package com.samanatteu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// @Configuration = « classe de réglages » : Spring la lit au démarrage.
// Ici elle ne protège rien, elle décrit juste l'API pour la doc.
@Configuration
public class SwaggerConfig {

    // @Bean = « fabrique-moi cet objet et garde-le » : springdoc cherche
    // automatiquement un bean OpenAPI et s'en sert pour la page doc.
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                // Le titre affiché en haut de /swagger-ui.html.
                .info(new Info()
                        .title("SamaNatteu API")
                        .version("1.0")
                        .description("Tontines, cotisations, tirages, prêts — voir README."))
                // Cadenas global : dit à la doc « toutes les routes veulent un token ».
                // (La vraie protection reste SecurityConfig ; ici on ne fait que l'afficher.)
                .addSecurityItem((new SecurityRequirement().addList("bearer-jwt")))
                .components(new Components()
                        // Le bouton « Authorize » : la doc saura envoyer
                        // Authorization: Bearer <votre JWT>.
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

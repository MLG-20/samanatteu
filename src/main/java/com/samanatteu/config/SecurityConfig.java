package com.samanatteu.config;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.samanatteu.security.JwtAccessDeniedHandler;
import com.samanatteu.security.JwtAuthFilter;
import com.samanatteu.security.JwtAuthentificationEntryPoint;

@Configuration
public class SecurityConfig {
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // JwtAuthFilter est injecté directement en paramètre de cette méthode @Bean
        // (pas besoin de champ ni de constructeur dans cette classe @Configuration :
        // Spring reconnaît le type du paramètre et fournit le bean correspondant).
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                        JwtAuthentificationEntryPoint entryPoint, JwtAccessDeniedHandler accessDeniedHandler)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(entryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))

                                // Règles d'accès évaluées dans l'ordre : la première qui correspond à la
                                // requête (méthode HTTP + chemin) s'applique. permitAll() = accessible sans
                                // token (login, création de compte). hasRole("ADMIN") = il faut un token
                                // valide ET l'autorité ROLE_ADMIN (construite dans JwtAuthFilter).
                                // anyRequest().authenticated() = filet de sécurité : toute route non listée
                                // ci-dessus exige au moins un token valide (peu importe le rôle).
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/utilisateur").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/utilisateur").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/utilisateur/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/utilisateur/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                // Insère notre filtre JWT AVANT le filtre standard d'authentification
                                // par mot de passe, pour qu'il soit exécuté sur chaque requête et
                                // puisse authentifier via le token s'il est présent.
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}

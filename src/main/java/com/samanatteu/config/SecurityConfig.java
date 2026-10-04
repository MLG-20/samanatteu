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

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                        JwtAuthentificationEntryPoint entryPoint, JwtAccessDeniedHandler accessDeniedHandler)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(entryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))

                                // Évaluées dans l'ordre : la première règle qui correspond s'applique.
                                // anyRequest().authenticated() est le filet : toute route non listée
                                // exige au moins un token valide.
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/utilisateur").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/utilisateur").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/utilisateur/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/utilisateur/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/tontine").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.POST, "/tontine/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/tontine").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.PUT, "/tontine/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.DELETE, "/tontine/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.POST, "/participation").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/participation").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.PUT, "/participation/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.DELETE, "/participation/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.POST, "/cycle/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/cycle").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.DELETE, "/cycle/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.POST, "/cotisation/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/cotisation/**").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.DELETE, "/cotisation/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.POST, "/tirage/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/tirage").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.POST, "/pret/**").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/pret", "/echeancePret").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.GET, "/transaction").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.GET, "/invitation/*").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/invitation/*/rejoindre").hasRole("MEMBRE")
                                                .requestMatchers(HttpMethod.GET, "/invitation").hasRole("GESTIONNAIRE")
                                                .requestMatchers(HttpMethod.GET, "/notification").hasAnyRole("GESTIONNAIRE", "MEMBRE")
                                                .requestMatchers(HttpMethod.GET, "/importMembre").hasRole("GESTIONNAIRE")

                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}

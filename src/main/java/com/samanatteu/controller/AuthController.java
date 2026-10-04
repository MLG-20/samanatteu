package com.samanatteu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.auth.ChangementMotDePasseDTO;
import com.samanatteu.dto.auth.LoginDTO;
import com.samanatteu.dto.auth.MotDePasseOublieDTO;
import com.samanatteu.dto.auth.RefreshRequestDTO;
import com.samanatteu.dto.auth.ReinitialisationMotDePasseDTO;
import com.samanatteu.dto.auth.TokenDTO;
import com.samanatteu.service.auth.AuthService;
import com.samanatteu.service.auth.MotDePasseOublieService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/auth")
@RestController
public class AuthController {
    private final AuthService authService;
    private final MotDePasseOublieService motDePasseOublieService;

    public AuthController(AuthService authService, MotDePasseOublieService motDePasseOublieService) {
        this.authService = authService;
        this.motDePasseOublieService = motDePasseOublieService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authService.login(loginDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenDTO> refresh(@Valid @RequestBody RefreshRequestDTO refreshRequestDTO) {
        return ResponseEntity.ok(authService.refresh(refreshRequestDTO));
    }

    // Pas d'id dans l'adresse : le compte est celui du token. 204 = fait, rien à renvoyer.
    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangementMotDePasseDTO dto) {
        authService.changerMotDePasse(dto);
        return ResponseEntity.noContent().build();
    }

    // Toujours 204, que le numéro existe ou non : la réponse ne révèle rien.
    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<Void> demanderCode(@Valid @RequestBody MotDePasseOublieDTO dto) {
        motDePasseOublieService.demanderCode(dto);
        return ResponseEntity.noContent().build();
    }

    // Publique : la preuve d'identité est le code reçu par SMS, pas un token.
    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<Void> reinitialiser(@Valid @RequestBody ReinitialisationMotDePasseDTO dto) {
        motDePasseOublieService.reinitialiser(dto);
        return ResponseEntity.noContent().build();
    }

}

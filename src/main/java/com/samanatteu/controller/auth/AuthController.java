package com.samanatteu.controller.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.auth.ChangementMotDePasseDTO;
import com.samanatteu.dto.auth.LoginDTO;
import com.samanatteu.dto.auth.MotDePasseOublieDTO;
import com.samanatteu.dto.auth.RefreshRequestDTO;
import com.samanatteu.dto.auth.ReinitialisationMotDePasseDTO;
import com.samanatteu.dto.auth.RenvoiVerificationDTO;
import com.samanatteu.dto.auth.TokenDTO;
import com.samanatteu.dto.auth.VerificationEmailDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.service.auth.AuthService;
import com.samanatteu.service.auth.MotDePasseOublieService;
import com.samanatteu.service.auth.VerificationEmailService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/auth")
@RestController
public class AuthController {
    private final AuthService authService;
    private final MotDePasseOublieService motDePasseOublieService;
    private final VerificationEmailService verificationEmailService;

    public AuthController(AuthService authService, MotDePasseOublieService motDePasseOublieService,
            VerificationEmailService verificationEmailService) {
        this.authService = authService;
        this.motDePasseOublieService = motDePasseOublieService;
        this.verificationEmailService = verificationEmailService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authService.login(loginDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenDTO> refresh(@Valid @RequestBody RefreshRequestDTO refreshRequestDTO) {
        return ResponseEntity.ok(authService.refresh(refreshRequestDTO));
    }

    // Qui je suis : l'utilisateur porté par le token. Le front en a besoin pour
    // afficher le bon tableau de bord et le bon menu.
    // La route est protégée : /auth/login et /auth/refresh sont les seules
    // en permitAll, tout le reste tombe sur anyRequest().authenticated().
    @GetMapping("/me")
    public UtilisateurDTO moi() {
        return authService.moi();
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

    // Publique : la preuve est le jeton du lien reçu par email. 204 : email
    // vérifié et essai démarré ; le front envoie ensuite vers la connexion.
    @PostMapping("/verifier-email")
    public ResponseEntity<Void> verifierEmail(@Valid @RequestBody VerificationEmailDTO dto) {
        verificationEmailService.verifier(dto.getJeton());
        return ResponseEntity.noContent().build();
    }

    // Toujours 204, que le compte existe ou non : la réponse ne révèle rien.
    @PostMapping("/renvoyer-verification")
    public ResponseEntity<Void> renvoyerVerification(@Valid @RequestBody RenvoiVerificationDTO dto) {
        verificationEmailService.renvoyer(dto);
        return ResponseEntity.noContent().build();
    }

    // Pas de corps : le token dit déjà qui se déconnecte.
    @PostMapping("/deconnexion")
    public ResponseEntity<Void> deconnecter() {
        authService.deconnecter();
        return ResponseEntity.noContent().build();
    }

}

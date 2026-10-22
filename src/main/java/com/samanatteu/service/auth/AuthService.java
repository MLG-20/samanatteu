package com.samanatteu.service.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.auth.ChangementMotDePasseDTO;
import com.samanatteu.dto.auth.LoginDTO;
import com.samanatteu.dto.auth.RefreshRequestDTO;
import com.samanatteu.dto.auth.TokenDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.auth.AncienMotDePasseIncorrectException;
import com.samanatteu.exception.auth.IdentifiantsInvalidesException;
import com.samanatteu.exception.auth.RefreshTokenInvalideException;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.utilisateur.TelephoneService;

@Service
public class AuthService {
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TelephoneService telephoneService;

    public AuthService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
            UtilisateurConnecte utilisateurConnecte, TelephoneService telephoneService) {
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.telephoneService = telephoneService;
    }

    // LoginDTO garantit (@AssertTrue) qu'un email OU un téléphone est fourni.
    // Compte inconnu et mauvais mot de passe lèvent la même exception : un attaquant ne
    // doit pas pouvoir savoir si un compte existe.
    public TokenDTO login(LoginDTO loginDTO) {
        Utilisateur utilisateur;
        if (loginDTO.getEmail() != null && !loginDTO.getEmail().isBlank()) {
            utilisateur = utilisateurRepository.findByEmail(loginDTO.getEmail())
                    .orElseThrow(() -> new IdentifiantsInvalidesException());
        } else {
            // Le numéro saisi est normalisé en E.164 avant la recherche : le compte
            // est enregistré en E.164, c'est donc le seul format qui le trouve.
            // Un numéro mal écrit donne 400 : un format invalide ne révèle rien
            // sur l'existence d'un compte (seul le 401 le ferait).
            String telephone = telephoneService.normaliser(loginDTO.getTelephone());
            utilisateur = utilisateurRepository.findByTelephone(telephone)
                    .orElseThrow(() -> new IdentifiantsInvalidesException());
        }
        boolean motDePasseValide = passwordEncoder.matches(loginDTO.getMotDePasse(), utilisateur.getMotDePasse());
        if (!motDePasseValide) {
            throw new IdentifiantsInvalidesException();
        }
        String accessToken = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken(utilisateur);
        return new TokenDTO(accessToken, refreshToken);
    }

    public TokenDTO refresh(RefreshRequestDTO refreshRequestDTO) {
        String telephone;
        String type;
        Integer version;
        try {
            telephone = jwtUtil.extractTelephone(refreshRequestDTO.getRefreshToken());
            type = jwtUtil.extractType(refreshRequestDTO.getRefreshToken());
            version = jwtUtil.extractVersion(refreshRequestDTO.getRefreshToken());
        } catch (io.jsonwebtoken.JwtException e) {
            throw new RefreshTokenInvalideException();
        }

        if (!"refresh".equals(type)) {
            throw new RefreshTokenInvalideException();
        }

        Utilisateur utilisateur = utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new RefreshTokenInvalideException());

        // Token fabriqué avant une déconnexion ou un changement de mot de passe :
        // son numéro n'est plus celui du compte. null testé en premier (ancien token).
        if (version == null || version != utilisateur.getVersionSessions()) {
            throw new RefreshTokenInvalideException();
        }

        String accessToken = jwtUtil.generateToken(utilisateur);
        String newRefreshToken = jwtUtil.generateRefreshToken(utilisateur);
        return new TokenDTO(accessToken, newRefreshToken);
    }

    // Qui je suis : l'utilisateur porté par le token (c'est son téléphone).
    // Le front en a besoin pour afficher le bon tableau de bord et le bon menu.
    // On renvoie le DTO de sortie (jamais l'entité) : le mot de passe n'existe
    // pas dans UtilisateurDTO, il ne peut donc pas sortir.
    public UtilisateurDTO moi() {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(utilisateurConnecte.telephone())
                .orElseThrow(() -> new AccesRefuseException());
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setEmail(utilisateur.getEmail());
        dto.setRole(utilisateur.getRole());
        dto.setActif(utilisateur.getActif());
        return dto;
    }

    // Le compte modifié est celui du token, jamais un identifiant envoyé par le
    // client : personne ne peut changer le mot de passe d'un autre.
    public void changerMotDePasse(ChangementMotDePasseDTO dto) {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(utilisateurConnecte.telephone())
                .orElseThrow(() -> new AccesRefuseException());

        // matches et non equals : la base ne contient que le hachage BCrypt.
        if (!passwordEncoder.matches(dto.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new AncienMotDePasseIncorrectException();
        }

        // Jamais de mot de passe en clair en base : on enregistre son hachage.
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        // Ferme aussi les sessions ouvertes : celui qui change de mot de passe parce
        // qu'il se croit piraté met le pirate dehors.
        utilisateur.setVersionSessions(utilisateur.getVersionSessions() + 1);

        utilisateurRepository.save(utilisateur);
    }

    // Un JWT ne peut pas être annulé un par un : on change le numéro de version du
    // compte, ce qui périme tous ses refresh tokens (tous ses appareils à la fois).
    public void deconnecter() {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(utilisateurConnecte.telephone())
                .orElseThrow(() -> new AccesRefuseException());

        utilisateur.setVersionSessions(utilisateur.getVersionSessions() + 1);
        utilisateurRepository.save(utilisateur);
    }

}

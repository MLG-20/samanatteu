package com.samanatteu.service.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.auth.LoginDTO;
import com.samanatteu.dto.auth.RefreshRequestDTO;
import com.samanatteu.dto.auth.TokenDTO;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.exception.IdentifiantsInvalidesException;
import com.samanatteu.exception.RefreshTokenInvalideException;
import com.samanatteu.repository.UtilisateurRepository;
import com.samanatteu.security.JwtUtil;

@Service
public class AuthService {
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Reçoit un LoginDTO déjà validé (voir @AssertTrue sur LoginDTO : email OU
    // téléphone garanti non-vide) et renvoie un token JWT si les identifiants
    // sont corrects, sinon lève IdentifiantsInvalidesException (-> 401 via
    // GlobalExceptionHandler).
    public TokenDTO login(LoginDTO loginDTO) {
        Utilisateur utilisateur;
        // On cherche l'utilisateur par email si fourni, sinon par téléphone.
        // orElseThrow : si findByEmail/findByTelephone renvoie un Optional vide
        // (aucun utilisateur trouvé), on lève l'exception immédiatement.
        if (loginDTO.getEmail() != null && !loginDTO.getEmail().isBlank()) {
            utilisateur = utilisateurRepository.findByEmail(loginDTO.getEmail())
                    .orElseThrow(() -> new IdentifiantsInvalidesException());
        } else {
            utilisateur = utilisateurRepository.findByTelephone(loginDTO.getTelephone())
                    .orElseThrow(() -> new IdentifiantsInvalidesException());
        }
        // Le mot de passe en base est haché (BCrypt) : matches() compare le mot de
        // passe en clair envoyé par le client avec le hash stocké, sans jamais
        // déchiffrer ce dernier (le hachage n'est pas réversible).
        boolean motDePasseValide = passwordEncoder.matches(loginDTO.getMotDePasse(), utilisateur.getMotDePasse());
        if (!motDePasseValide) {
            throw new IdentifiantsInvalidesException();
        }
        // Identifiants corrects : on génère un token JWT signé qui encode l'identité
        // (email) et le rôle de l'utilisateur, que le client réutilisera dans le
        // header "Authorization: Bearer <token>" pour ses prochaines requêtes.
        String accessToken = jwtUtil.generateToken(utilisateur);
        String refreshToken = jwtUtil.generateRefreshToken(utilisateur);
        return new TokenDTO(accessToken, refreshToken);
    }

    public TokenDTO refresh(RefreshRequestDTO refreshRequestDTO) {
        String telephone;
        String type;
        try {
            telephone = jwtUtil.extractTelephone(refreshRequestDTO.getRefreshToken());
            type = jwtUtil.extractType(refreshRequestDTO.getRefreshToken());
        } catch (io.jsonwebtoken.JwtException e) {
            throw new RefreshTokenInvalideException();
        }

        if (!"refresh".equals(type)) {
            throw new RefreshTokenInvalideException();
        }

        Utilisateur utilisateur = utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new RefreshTokenInvalideException());

        String accessToken = jwtUtil.generateToken(utilisateur);
        String newRefreshToken = jwtUtil.generateRefreshToken(utilisateur);
        return new TokenDTO(accessToken, newRefreshToken);
    }

}

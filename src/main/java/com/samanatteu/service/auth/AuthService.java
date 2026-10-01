package com.samanatteu.service.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.auth.LoginDTO;
import com.samanatteu.dto.auth.RefreshRequestDTO;
import com.samanatteu.dto.auth.TokenDTO;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.exception.auth.IdentifiantsInvalidesException;
import com.samanatteu.exception.auth.RefreshTokenInvalideException;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
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

    // LoginDTO garantit (@AssertTrue) qu'un email OU un téléphone est fourni.
    // Compte inconnu et mauvais mot de passe lèvent la même exception : un attaquant ne
    // doit pas pouvoir savoir si un compte existe.
    public TokenDTO login(LoginDTO loginDTO) {
        Utilisateur utilisateur;
        if (loginDTO.getEmail() != null && !loginDTO.getEmail().isBlank()) {
            utilisateur = utilisateurRepository.findByEmail(loginDTO.getEmail())
                    .orElseThrow(() -> new IdentifiantsInvalidesException());
        } else {
            utilisateur = utilisateurRepository.findByTelephone(loginDTO.getTelephone())
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

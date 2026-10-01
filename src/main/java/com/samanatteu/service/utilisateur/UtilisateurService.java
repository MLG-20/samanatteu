package com.samanatteu.service.utilisateur;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.ModificationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.utilisateur.EmailDejaUtiliseException;
import com.samanatteu.exception.utilisateur.TelephoneDejaUtiliseException;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class UtilisateurService {

    private final PasswordEncoder passwordEncoder;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
            UtilisateurConnecte utilisateurConnecte) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    public List<UtilisateurDTO> listUtilisateurs() {
        return utilisateurRepository.findAll()
                .stream()
                .map(this::convertiUtilisateurDTO)
                .toList();
    }

    public UtilisateurDTO createUtilisateur(CreationUtilisateurDTO dto) {
        // Seul un ADMIN peut créer un autre ADMIN (la route d'inscription est publique).
        if (dto.getRole() == RoleUtilisateur.ADMIN && !utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
            throw new AccesRefuseException();
        }
        // L'email est optionnel : son unicité n'est vérifiée que s'il est fourni
        // (sinon existsByEmail(null) bloquerait tous les comptes sans email après le premier).
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new EmailDejaUtiliseException(dto.getEmail());
        }
        if (utilisateurRepository.existsByTelephone(dto.getTelephone())) {
            throw new TelephoneDejaUtiliseException(dto.getTelephone());
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setRole(dto.getRole());

        Utilisateur enregistre = utilisateurRepository.save(utilisateur);
        return convertiUtilisateurDTO(enregistre);
    }

    public Optional<UtilisateurDTO> updateUtilisateur(Long id, ModificationUtilisateurDTO modifications) {
        return utilisateurRepository.findById(id).map(utilisateur -> {
            // Règle "propre profil" : seul le propriétaire du compte ou un ADMIN peut le modifier.
            if (!utilisateurConnecte.telephone().equals(utilisateur.getTelephone()) &&
                    !utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
                throw new AccesRefuseException();
            }

            // En base, "pas d'email" s'écrit toujours null, jamais "" : sinon deux comptes
            // sans email entreraient en conflit sur la contrainte d'unicité.
            String nouvelEmail = (modifications.getEmail() == null || modifications.getEmail().isBlank())
                    ? null
                    : modifications.getEmail();
            if (nouvelEmail != null
                    && !nouvelEmail.equals(utilisateur.getEmail())
                    && utilisateurRepository.existsByEmail(nouvelEmail)) {
                throw new EmailDejaUtiliseException(nouvelEmail);
            }

            utilisateur.setNom(modifications.getNom());
            utilisateur.setPrenom(modifications.getPrenom());
            utilisateur.setEmail(nouvelEmail);
            return convertiUtilisateurDTO(utilisateurRepository.save(utilisateur));
        });
    }

    public boolean deleteUtilisateur(Long id) {
        if (utilisateurRepository.existsById(id)) {
            utilisateurRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private UtilisateurDTO convertiUtilisateurDTO(Utilisateur utilisateur) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setEmail(utilisateur.getEmail());
        dto.setRole(utilisateur.getRole());
        dto.setActif(utilisateur.getActif());
        dto.setCreatedAt(utilisateur.getCreatedAt());
        dto.setUpdatedAt(utilisateur.getUpdatedAt());
        return dto;
    }
}

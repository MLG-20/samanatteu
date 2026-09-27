package com.samanatteu.service.utilisateur;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.ModificationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.EmailDejaUtiliseException;
import com.samanatteu.exception.TelephoneDejaUtiliseException;
import com.samanatteu.repository.UtilisateurRepository;
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

    // Lister
    public List<UtilisateurDTO> listUtilisateurs() {
        return utilisateurRepository.findAll() // 1. List<Utilisateur> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiUtilisateurDTO) // 3. applique la conversion à CHAQUE Utilisateur -> UtilisateurDTO
                                                   // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<UtilisateurDTO> à partir du flux
    }

    // créer
    public UtilisateurDTO createUtilisateur(CreationUtilisateurDTO dto) {

        if (dto.getRole() == RoleUtilisateur.ADMIN && !utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
            throw new AccesRefuseException();
        }
        // AVANT de sauvegarder : on vérifie si un autre utilisateur a déjà cet email en
        // base.
        // existsByEmail renvoie juste un boolean (true/false), pas l'utilisateur
        // lui-même.
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && utilisateurRepository.existsByEmail(dto.getEmail())) {
            // On lève l'exception : ça arrête immédiatement la méthode ici,
            // les lignes save()/return en dessous ne sont jamais exécutées.
            // C'est le GlobalExceptionHandler qui va l'attraper et renvoyer le 409 au
            // client.
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

        // Si on arrive ici, c'est que l'email est libre : on peut sauvegarder en
        // sécurité.
        Utilisateur enregistre = utilisateurRepository.save(utilisateur);
        return convertiUtilisateurDTO(enregistre);
    }

    // update
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

    // Delete
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

package com.samanatteu.service.utilisateur;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.EmailDejaUtiliseException;
import com.samanatteu.exception.TelephoneDejaUtiliseException;
import com.samanatteu.repository.UtilisateurRepository;

@Service
public class UtilisateurService {

    private final PasswordEncoder passwordEncoder;
    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
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

        if (dto.getRole() == RoleUtilisateur.ADMIN) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean estAdmin = auth.getAuthorities().stream()
                    .anyMatch(autorite -> autorite.getAuthority().equals("ROLE_ADMIN"));
            if (!estAdmin) {
                throw new AccesRefuseException();
            }
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
    public Optional<UtilisateurDTO> updateUtilisateur(Long id, Utilisateur utilisateurModifier) {
        // findById(id) renvoie un Optional<Utilisateur> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return utilisateurRepository.findById(id).map(utilisateurExsitant -> {

            // Authentication = l'objet déposé par JwtAuthFilter dans SecurityContextHolder
            // pour la requête en cours ; il contient l'identité (téléphone) ET les rôles
            // (autorités) de la personne qui a envoyé le token JWT.
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // getName() = le principal du token, ici le téléphone (1er argument du
            // UsernamePasswordAuthenticationToken construit dans JwtAuthFilter) —
            // identifiant
            // choisi car toujours présent (l'email, lui, est optionnel depuis la v1.1).
            String telephoneConnect = auth.getName();
            // getAuthorities() = la liste des rôles (ex: "ROLE_ADMIN", "ROLE_MEMBRE").
            // anyMatch(...) : true si AU MOINS UNE autorité de la liste vaut "ROLE_ADMIN".
            boolean estAdmin = auth.getAuthorities().stream()
                    .anyMatch(autorite -> autorite.getAuthority().equals("ROLE_ADMIN"));
            // Règle d'autorisation "propre profil" : on refuse SEULEMENT si ce n'est ni
            // le propriétaire du compte (téléphone différent) NI un admin (qui peut tout
            // modifier). Un ADMIN ou le propriétaire lui-même passent sans exception.
            if (!telephoneConnect.equals(utilisateurExsitant.getTelephone()) && !estAdmin) {
                throw new AccesRefuseException();
            }
            // utilisateurExsitant = l'entité déjà en base (trouvée par findById).
            // utilisateurModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            utilisateurExsitant.setNom(utilisateurModifier.getNom());
            utilisateurExsitant.setPrenom(utilisateurModifier.getPrenom());
            utilisateurExsitant.setTelephone(utilisateurModifier.getTelephone());
            utilisateurExsitant.setEmail(utilisateurModifier.getEmail());
            utilisateurExsitant.setRole(utilisateurModifier.getRole());
            utilisateurExsitant.setActif(utilisateurModifier.getActif());
            utilisateurExsitant.setCreatedAt(utilisateurModifier.getCreatedAt());
            utilisateurExsitant.setUpdatedAt(utilisateurModifier.getUpdatedAt());
            utilisateurExsitant.setTontineGerees(utilisateurModifier.getTontineGerees());

            // save() persiste les changements en base ET renvoie l'entité Utilisateur à
            // jour (avec motDePasse).
            Utilisateur enregistre = utilisateurRepository.save(utilisateurExsitant);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<UtilisateurDTO>.
            return convertiUtilisateurDTO(enregistre);
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

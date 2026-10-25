package com.samanatteu.service.site;

import java.util.List;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.site.EnvoiMessageContactDTO;
import com.samanatteu.dto.site.MessageContactDTO;
import com.samanatteu.entity.site.MessageContact;
import com.samanatteu.exception.site.MessageContactIntrouvableException;
import com.samanatteu.exception.site.MoyenDeReponseManquantException;
import com.samanatteu.repository.site.MessageContactRepository;

// Les messages du formulaire de contact.
//
// Envoyer = tout le monde (un visiteur sans compte) ; lire, marquer lu,
// supprimer = l'ADMIN (règles dans SecurityConfig).
@Service
public class MessageContactService {

    private final MessageContactRepository messageContactRepository;

    public MessageContactService(MessageContactRepository messageContactRepository) {
        this.messageContactRepository = messageContactRepository;
    }

    public void envoyer(EnvoiMessageContactDTO dto) {
        // Le pot de miel est rempli : c'est un robot. On ne sauvegarde rien,
        // mais on ne lève pas d'erreur : le robot croit avoir réussi et ne
        // cherche pas à contourner le piège.
        if (dto.getSiteWeb() != null && !dto.getSiteWeb().isBlank()) {
            return;
        }

        String telephone = nettoyer(dto.getTelephone());
        String email = nettoyer(dto.getEmail());
        // Même règle que le CHECK de la base, vérifiée AVANT : un message
        // d'erreur clair (400) plutôt qu'une violation de contrainte (500).
        if (telephone == null && email == null) {
            throw new MoyenDeReponseManquantException();
        }

        MessageContact message = new MessageContact();
        message.setNom(dto.getNom().trim());
        message.setTelephone(telephone);
        message.setEmail(email);
        message.setSujet(dto.getSujet().trim());
        message.setMessage(dto.getMessage().trim());
        message.setLu(false);
        messageContactRepository.save(message);
    }

    public List<MessageContactDTO> lister() {
        return messageContactRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertir)
                .toList();
    }

    // Lu ou non lu : l'ADMIN peut revenir en arrière (« à traiter plus tard »).
    public MessageContactDTO marquerLu(Long id, boolean lu) {
        MessageContact message = messageContactRepository.findById(id)
                .orElseThrow(() -> new MessageContactIntrouvableException(id));
        message.setLu(lu);
        return convertir(messageContactRepository.save(message));
    }

    public void supprimer(Long id) {
        if (!messageContactRepository.existsById(id)) {
            throw new MessageContactIntrouvableException(id);
        }
        messageContactRepository.deleteById(id);
    }

    // "  " ou "" → null : un champ laissé vide n'est pas un moyen de réponse.
    private String nettoyer(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    private MessageContactDTO convertir(MessageContact message) {
        MessageContactDTO dto = new MessageContactDTO();
        dto.setId(message.getId());
        dto.setNom(message.getNom());
        dto.setTelephone(message.getTelephone());
        dto.setEmail(message.getEmail());
        dto.setSujet(message.getSujet());
        dto.setMessage(message.getMessage());
        dto.setLu(message.getLu());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}

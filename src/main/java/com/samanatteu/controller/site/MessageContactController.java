package com.samanatteu.controller.site;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.site.EnvoiMessageContactDTO;
import com.samanatteu.dto.site.MessageContactDTO;
import com.samanatteu.service.site.MessageContactService;

import jakarta.validation.Valid;

// Le formulaire de contact.
//
// POST : PUBLIC (le visiteur n'a pas forcément de compte). Tout le reste :
// l'ADMIN, depuis son back-office (SecurityConfig).
@RequestMapping("/messageContact")
@RestController
public class MessageContactController {

    private final MessageContactService messageContactService;

    public MessageContactController(MessageContactService messageContactService) {
        this.messageContactService = messageContactService;
    }

    // 204 : rien à renvoyer au visiteur (il ne relira jamais son message).
    @PostMapping
    public ResponseEntity<Void> envoyer(@Valid @RequestBody EnvoiMessageContactDTO dto) {
        messageContactService.envoyer(dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<MessageContactDTO> lister() {
        return messageContactService.lister();
    }

    // PUT /messageContact/5/lu?valeur=true (ou false pour le remettre « non lu »).
    @PutMapping("/{id}/lu")
    public MessageContactDTO marquerLu(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean valeur) {
        return messageContactService.marquerLu(id, valeur);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        messageContactService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}

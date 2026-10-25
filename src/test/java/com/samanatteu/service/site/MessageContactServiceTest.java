package com.samanatteu.service.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samanatteu.dto.site.EnvoiMessageContactDTO;
import com.samanatteu.entity.site.MessageContact;
import com.samanatteu.exception.site.MessageContactIntrouvableException;
import com.samanatteu.exception.site.MoyenDeReponseManquantException;
import com.samanatteu.repository.site.MessageContactRepository;

@ExtendWith(MockitoExtension.class)
class MessageContactServiceTest {

    @Mock
    private MessageContactRepository messageContactRepository;

    @InjectMocks
    private MessageContactService messageContactService;

    private EnvoiMessageContactDTO demande() {
        EnvoiMessageContactDTO dto = new EnvoiMessageContactDTO();
        dto.setNom("Awa Diop");
        dto.setSujet("Formule PRO");
        dto.setMessage("Bonjour, je voudrais des informations.");
        return dto;
    }

    // Un message arrive toujours NON LU, nettoyé, avec un email vide → null.
    @Test
    void envoyer_sauvegardeUnMessageNonLu() {
        EnvoiMessageContactDTO dto = demande();
        dto.setTelephone(" 77 123 45 67 ");
        dto.setEmail("");

        messageContactService.envoyer(dto);

        ArgumentCaptor<MessageContact> capture = ArgumentCaptor.forClass(MessageContact.class);
        verify(messageContactRepository).save(capture.capture());
        MessageContact sauve = capture.getValue();
        assertFalse(sauve.getLu());
        assertEquals("77 123 45 67", sauve.getTelephone());
        assertNull(sauve.getEmail());
    }

    // Ni téléphone ni email : on ne pourrait pas répondre → 400, rien sauvegardé.
    @Test
    void envoyer_sansTelephoneNiEmail_refuse() {
        EnvoiMessageContactDTO dto = demande();
        dto.setTelephone("  ");

        assertThrows(MoyenDeReponseManquantException.class, () -> messageContactService.envoyer(dto));
        verify(messageContactRepository, never()).save(any());
    }

    // Le pot de miel rempli = un robot : aucune erreur, mais rien sauvegardé.
    @Test
    void envoyer_avecLePotDeMielRempli_ignoreLeMessageSansErreur() {
        EnvoiMessageContactDTO dto = demande();
        dto.setEmail("robot@spam.com");
        dto.setSiteWeb("https://spam.com");

        messageContactService.envoyer(dto);

        verify(messageContactRepository, never()).save(any());
    }

    @Test
    void marquerLu_changeLEtat() {
        MessageContact message = new MessageContact();
        message.setId(5L);
        message.setLu(false);
        when(messageContactRepository.findById(5L)).thenReturn(Optional.of(message));
        when(messageContactRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(messageContactService.marquerLu(5L, true).getLu());
    }

    @Test
    void supprimer_unMessageInexistant_donne404() {
        when(messageContactRepository.existsById(9L)).thenReturn(false);

        assertThrows(MessageContactIntrouvableException.class, () -> messageContactService.supprimer(9L));
        verify(messageContactRepository, never()).deleteById(any());
    }
}

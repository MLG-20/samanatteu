package com.samanatteu.service.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samanatteu.dto.site.ParametreSiteDTO;
import com.samanatteu.entity.site.ParametreSite;
import com.samanatteu.repository.site.ParametreSiteRepository;

@ExtendWith(MockitoExtension.class)
class ParametreSiteServiceTest {

    @Mock
    private ParametreSiteRepository parametreSiteRepository;

    @InjectMocks
    private ParametreSiteService parametreSiteService;

    // Base vidée à la main : le site public reste debout, sans paramètres.
    @Test
    void lire_sansLigneEnBase_renvoieDesParametresVides() {
        when(parametreSiteRepository.findById(ParametreSite.ID_UNIQUE)).thenReturn(Optional.empty());

        ParametreSiteDTO resultat = parametreSiteService.lire();

        assertNull(resultat.getFacebook());
        assertNull(resultat.getContactEmail());
    }

    // Un champ vidé par l'ADMIN efface la valeur (le réseau disparaît du footer),
    // et les espaces autour d'une valeur sont retirés.
    @Test
    void modifier_videLesChampsBlancsEtNettoieLesAutres() {
        ParametreSite enBase = new ParametreSite();
        enBase.setId(ParametreSite.ID_UNIQUE);
        enBase.setFacebook("https://facebook.com/ancien");
        when(parametreSiteRepository.findById(ParametreSite.ID_UNIQUE)).thenReturn(Optional.of(enBase));
        when(parametreSiteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ParametreSiteDTO demande = new ParametreSiteDTO();
        demande.setFacebook("   ");
        demande.setInstagram("  https://instagram.com/samanatteu  ");

        ParametreSiteDTO resultat = parametreSiteService.modifier(demande);

        assertNull(resultat.getFacebook());
        assertEquals("https://instagram.com/samanatteu", resultat.getInstagram());
    }

    // Ligne absente : la modification la recrée, toujours avec l'id 1.
    @Test
    void modifier_sansLigneEnBase_creeLaLigneUnique() {
        when(parametreSiteRepository.findById(ParametreSite.ID_UNIQUE)).thenReturn(Optional.empty());
        when(parametreSiteRepository.save(any())).thenAnswer(invocation -> {
            ParametreSite sauvee = invocation.getArgument(0);
            assertEquals(ParametreSite.ID_UNIQUE, sauvee.getId());
            return sauvee;
        });

        ParametreSiteDTO demande = new ParametreSiteDTO();
        demande.setContactEmail("contact@samanatteu.sn");

        assertEquals("contact@samanatteu.sn", parametreSiteService.modifier(demande).getContactEmail());
    }
}

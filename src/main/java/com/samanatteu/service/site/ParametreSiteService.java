package com.samanatteu.service.site;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.site.ParametreSiteDTO;
import com.samanatteu.entity.site.ParametreSite;
import com.samanatteu.repository.site.ParametreSiteRepository;

// Les paramètres du site public : réseaux sociaux, contact, « À propos ».
//
// Une seule ligne en base (id = 1). Lire = tout le monde (le footer et les
// pages publiques en ont besoin) ; modifier = l'ADMIN (règle dans SecurityConfig).
@Service
public class ParametreSiteService {

    private final ParametreSiteRepository parametreSiteRepository;

    public ParametreSiteService(ParametreSiteRepository parametreSiteRepository) {
        this.parametreSiteRepository = parametreSiteRepository;
    }

    // La ligne est créée par la migration V11. Si elle manquait quand même (base
    // vidée à la main), on renvoie des paramètres vides plutôt qu'une erreur :
    // le site public reste debout, simplement sans réseaux ni coordonnées.
    public ParametreSiteDTO lire() {
        return parametreSiteRepository.findById(ParametreSite.ID_UNIQUE)
                .map(this::convertir)
                .orElseGet(ParametreSiteDTO::new);
    }

    // L'ADMIN envoie TOUT le formulaire : chaque champ remplace l'ancien. Un
    // champ vidé efface donc la valeur (le réseau disparaît du footer).
    public ParametreSiteDTO modifier(ParametreSiteDTO dto) {
        ParametreSite parametres = parametreSiteRepository.findById(ParametreSite.ID_UNIQUE)
                .orElseGet(() -> {
                    ParametreSite nouveaux = new ParametreSite();
                    nouveaux.setId(ParametreSite.ID_UNIQUE);
                    return nouveaux;
                });

        parametres.setFacebook(nettoyer(dto.getFacebook()));
        parametres.setInstagram(nettoyer(dto.getInstagram()));
        parametres.setTiktok(nettoyer(dto.getTiktok()));
        parametres.setWhatsapp(nettoyer(dto.getWhatsapp()));
        parametres.setLinkedin(nettoyer(dto.getLinkedin()));
        parametres.setYoutube(nettoyer(dto.getYoutube()));
        parametres.setX(nettoyer(dto.getX()));

        parametres.setContactTelephone(nettoyer(dto.getContactTelephone()));
        parametres.setContactWhatsapp(nettoyer(dto.getContactWhatsapp()));
        parametres.setContactEmail(nettoyer(dto.getContactEmail()));
        parametres.setContactAdresse(nettoyer(dto.getContactAdresse()));
        parametres.setContactHoraires(nettoyer(dto.getContactHoraires()));

        parametres.setAproposAccroche(nettoyer(dto.getAproposAccroche()));
        parametres.setAproposHistoire(nettoyer(dto.getAproposHistoire()));
        parametres.setAproposMission(nettoyer(dto.getAproposMission()));
        parametres.setAproposValeurs(nettoyer(dto.getAproposValeurs()));

        return convertir(parametreSiteRepository.save(parametres));
    }

    // "  " ou "" → null : en base, « rien à afficher » s'écrit d'une seule façon.
    private String nettoyer(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    private ParametreSiteDTO convertir(ParametreSite parametres) {
        ParametreSiteDTO dto = new ParametreSiteDTO();
        dto.setFacebook(parametres.getFacebook());
        dto.setInstagram(parametres.getInstagram());
        dto.setTiktok(parametres.getTiktok());
        dto.setWhatsapp(parametres.getWhatsapp());
        dto.setLinkedin(parametres.getLinkedin());
        dto.setYoutube(parametres.getYoutube());
        dto.setX(parametres.getX());

        dto.setContactTelephone(parametres.getContactTelephone());
        dto.setContactWhatsapp(parametres.getContactWhatsapp());
        dto.setContactEmail(parametres.getContactEmail());
        dto.setContactAdresse(parametres.getContactAdresse());
        dto.setContactHoraires(parametres.getContactHoraires());

        dto.setAproposAccroche(parametres.getAproposAccroche());
        dto.setAproposHistoire(parametres.getAproposHistoire());
        dto.setAproposMission(parametres.getAproposMission());
        dto.setAproposValeurs(parametres.getAproposValeurs());
        return dto;
    }
}

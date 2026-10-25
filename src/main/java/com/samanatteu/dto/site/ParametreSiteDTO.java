package com.samanatteu.dto.site;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Les paramètres du site public, lus par tous (GET /site) et modifiés par
// l'ADMIN (PUT /site).
//
// Tous les champs sont FACULTATIFS : vide = rien à afficher. Mais un champ
// rempli doit être propre : une URL de réseau commence par https://, sinon
// le footer afficherait un lien cassé (ou dangereux, comme javascript:...).
@Getter
@Setter
public class ParametreSiteDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public ParametreSiteDTO() {
        super();
    }

    // Une URL https, ou rien. ^$ accepte la chaîne vide (champ effacé).
    private static final String URL_HTTPS = "^$|^https://\\S+$";
    private static final String MESSAGE_URL = "Le lien doit commencer par https://";

    // ---------- Les réseaux sociaux ----------

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String facebook;

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String instagram;

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String tiktok;

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String whatsapp;

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String linkedin;

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String youtube;

    @Pattern(regexp = URL_HTTPS, message = MESSAGE_URL)
    @Size(max = 255)
    private String x;

    // ---------- La page Contact ----------

    // Affiché tel quel (« +221 77 123 45 67 ») : chiffres, espaces et + seulement.
    @Pattern(regexp = "^$|^\\+?[0-9 ]{6,20}$", message = "Le numéro ne doit contenir que des chiffres, des espaces et le +.")
    private String contactTelephone;

    @Pattern(regexp = "^$|^\\+?[0-9 ]{6,20}$", message = "Le numéro ne doit contenir que des chiffres, des espaces et le +.")
    private String contactWhatsapp;

    @Email(message = "L'email n'est pas valide.")
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 255)
    private String contactAdresse;

    @Size(max = 255)
    private String contactHoraires;

    // ---------- La page À propos ----------

    @Size(max = 255)
    private String aproposAccroche;

    @Size(max = 5000)
    private String aproposHistoire;

    @Size(max = 5000)
    private String aproposMission;

    @Size(max = 5000)
    private String aproposValeurs;
}

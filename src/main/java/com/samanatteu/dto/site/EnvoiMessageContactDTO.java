package com.samanatteu.dto.site;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Ce que le visiteur envoie depuis le formulaire de la page Contact.
//
// Pas d'id, pas de "lu" : c'est le serveur qui les décide (même principe que
// CreationTontineDTO, où le client ne choisit pas le gestionnaire).
@Getter
@Setter
public class EnvoiMessageContactDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public EnvoiMessageContactDTO() {
        super();
    }

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String nom;

    // Téléphone OU email : au moins l'un des deux (vérifié dans le service,
    // une annotation ne porte que sur un champ à la fois).
    @Pattern(regexp = "^$|^\\+?[0-9 ]{6,20}$", message = "Le numéro ne doit contenir que des chiffres, des espaces et le +.")
    private String telephone;

    @Email(message = "L'email n'est pas valide.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Le sujet est obligatoire.")
    @Size(max = 150, message = "Le sujet ne doit pas dépasser 150 caractères.")
    private String sujet;

    @NotBlank(message = "Le message est obligatoire.")
    @Size(max = 3000, message = "Le message ne doit pas dépasser 3000 caractères.")
    private String message;

    // Le « pot de miel » anti-robot : un champ CACHÉ dans le formulaire. Un
    // humain ne le voit pas et le laisse vide ; un robot qui remplit tout le
    // remplit. Rempli = message ignoré (sans le dire au robot).
    private String siteWeb;
}

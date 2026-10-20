package com.samanatteu.dto.abonnement;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// La création ou la modification d'une formule, faite par l'ADMIN.
//
// Les quotas sont des entiers : 0 = "sans plafond". Le prix est en FCFA.
@Getter
@Setter
public class FormuleDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public FormuleDTO() {
        super();
    }


    // Absent à la création, présent à la modification.
    private Long id;

    @NotBlank(message = "Le nom de la formule est obligatoire.")
    private String nom;

    // 0 = sans plafond.
    @NotNull(message = "Le quota de tontines est obligatoire.")
    @Min(value = 0, message = "Le quota ne peut pas être négatif.")
    private Integer nbTontinesMax;

    @NotNull(message = "Le quota de membres est obligatoire.")
    @Min(value = 0, message = "Le quota ne peut pas être négatif.")
    private Integer nbMembresMax;

    @NotNull(message = "Le quota de SMS est obligatoire.")
    @Min(value = 0, message = "Le quota ne peut pas être négatif.")
    private Integer smsParMois;

    // Prix mensuel en FCFA. 0 = "sur devis".
    @NotNull(message = "Le prix est obligatoire.")
    @Min(value = 0, message = "Le prix ne peut pas être négatif.")
    private BigDecimal prixMensuel;

    // Une formule retirée de la vente reste en base, mais on ne peut plus la choisir.
    private Boolean active;
}

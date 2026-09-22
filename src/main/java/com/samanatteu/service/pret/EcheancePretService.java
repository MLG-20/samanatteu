package com.samanatteu.service.pret;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.EcheancePretDTO;
import com.samanatteu.entity.EcheancePret;
import com.samanatteu.exception.MontantPayeSuperieurAuDuException;
import com.samanatteu.exception.NumeroEcheanceDejaExistantException;
import com.samanatteu.repository.EcheancePretRepository;

@Service 
public class EcheancePretService {
     private final EcheancePretRepository echeancePretRepository;

    public EcheancePretService(EcheancePretRepository echeancePretRepository) {
        this.echeancePretRepository = echeancePretRepository;
    }

    // Lister
    public List<EcheancePretDTO> listEcheancePret() {
        return echeancePretRepository.findAll() // 1. List<EcheancePret> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiEcheancePretDTO) // 3. applique la conversion à CHAQUE EcheancePret -> EcheancePretDTO
                                                  // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<EcheancePretDTO> à partir du flux
    }

    // créer
    public EcheancePretDTO createEcheancePret(EcheancePret echeancePret) {
        if (echeancePretRepository.existsByNumeroEcheanceAndPretId(echeancePret.getNumeroEcheance(), echeancePret.getPret().getId())) {
            throw new NumeroEcheanceDejaExistantException(echeancePret.getNumeroEcheance(), echeancePret.getPret().getId());
        }
        if (echeancePret.getMontantPaye().compareTo(echeancePret.getMontantDu()) > 0) {
            throw new MontantPayeSuperieurAuDuException(echeancePret.getMontantPaye(), echeancePret.getMontantDu());
        }
        EcheancePret enregistre = echeancePretRepository.save(echeancePret);
        return convertiEcheancePretDTO(enregistre);
    }

    // update
    public Optional<EcheancePretDTO> updateEcheancePret(Long id, EcheancePret echeancePretModifier) {
        // findById(id) renvoie un Optional<EcheancePret> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return echeancePretRepository.findById(id).map(echeancePretExsitante -> {
            // echeancePretExsitante = l'entité déjà en base (trouvée par findById).
            // echeancePretModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            echeancePretExsitante.setPret(echeancePretModifier.getPret());
            echeancePretExsitante.setNumeroEcheance(echeancePretModifier.getNumeroEcheance());
            echeancePretExsitante.setMontantDu(echeancePretModifier.getMontantDu());
            echeancePretExsitante.setMontantPaye(echeancePretModifier.getMontantPaye());
            echeancePretExsitante.setDateEcheance(echeancePretModifier.getDateEcheance());
            echeancePretExsitante.setDatePaiement(echeancePretModifier.getDatePaiement());
            echeancePretExsitante.setStatut(echeancePretModifier.getStatut());

            // save() persiste les changements en base ET renvoie l'entité EcheancePret à jour
            // (avec motDePasse).
            EcheancePret enregistre = echeancePretRepository.save(echeancePretExsitante);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<EcheancePretDTO>.
            return convertiEcheancePretDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteEcheancePret(Long id) {
        if (echeancePretRepository.existsById(id)) {
            echeancePretRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private EcheancePretDTO convertiEcheancePretDTO(EcheancePret echeancePret) {
        EcheancePretDTO dto = new EcheancePretDTO();
        dto.setId(echeancePret.getId());
        dto.setPretId(echeancePret.getPret().getId());
        dto.setNumeroEcheance(echeancePret.getNumeroEcheance());
        dto.setMontantDu(echeancePret.getMontantDu());
        dto.setMontantPaye(echeancePret.getMontantPaye());
        dto.setDateEcheance(echeancePret.getDateEcheance());
        dto.setDatePaiement(echeancePret.getDatePaiement());
        dto.setStatut(echeancePret.getStatut());

        return dto;
    }
}

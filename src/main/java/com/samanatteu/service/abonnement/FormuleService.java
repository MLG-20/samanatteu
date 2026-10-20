package com.samanatteu.service.abonnement;

import java.util.List;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.abonnement.FormuleDTO;
import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.exception.abonnement.FormuleIntrouvableException;
import com.samanatteu.repository.abonnement.FormuleRepository;

// Les formules d'abonnement.
//
// Ce sont des DONNÉES : l'ADMIN les modifie (quotas, prix) depuis son
// dashboard, sans redéployer l'application. C'est tout l'intérêt de les avoir
// sorties du code.
@Service
public class FormuleService {

    private final FormuleRepository formuleRepository;

    public FormuleService(FormuleRepository formuleRepository) {
        this.formuleRepository = formuleRepository;
    }

    // Toutes les formules, y compris celles retirées de la vente : l'ADMIN doit
    // pouvoir voir l'historique (les abonnements existants les référencent).
    public List<FormuleDTO> lister() {
        return formuleRepository.findAll().stream()
                .map(this::convertir)
                .toList();
    }

    // Les formules proposées à la vente.
    public List<FormuleDTO> listerProposables() {
        return formuleRepository.findByActiveTrueOrderByPrixMensuelAsc().stream()
                .map(this::convertir)
                .toList();
    }

    public FormuleDTO creer(FormuleDTO dto) {
        Formule formule = new Formule();
        appliquer(formule, dto);
        return convertir(formuleRepository.save(formule));
    }

    // Modifier une formule existante. Un abonnement qui la référence garde
    // toujours la même formule : seuls ses QUOTAS changent (c'est le but).
    public FormuleDTO modifier(Long id, FormuleDTO dto) {
        Formule formule = formuleRepository.findById(id)
                .orElseThrow(() -> new FormuleIntrouvableException(id));
        appliquer(formule, dto);
        return convertir(formuleRepository.save(formule));
    }

    private void appliquer(Formule formule, FormuleDTO dto) {
        formule.setNom(dto.getNom());
        formule.setNbTontinesMax(dto.getNbTontinesMax());
        formule.setNbMembresMax(dto.getNbMembresMax());
        formule.setSmsParMois(dto.getSmsParMois());
        formule.setPrixMensuel(dto.getPrixMensuel());
        // Absent dans le JSON = on garde l'état courant (une formule retirée
        // de la vente ne se réactive pas par accident).
        if (dto.getActive() != null) {
            formule.setActive(dto.getActive());
        }
    }

    private FormuleDTO convertir(Formule formule) {
        FormuleDTO dto = new FormuleDTO();
        dto.setId(formule.getId());
        dto.setNom(formule.getNom());
        dto.setNbTontinesMax(formule.getNbTontinesMax());
        dto.setNbMembresMax(formule.getNbMembresMax());
        dto.setSmsParMois(formule.getSmsParMois());
        dto.setPrixMensuel(formule.getPrixMensuel());
        dto.setActive(formule.getActive());
        return dto;
    }
}

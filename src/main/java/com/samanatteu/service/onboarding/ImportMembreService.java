package com.samanatteu.service.onboarding;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.onboarding.ImportMembreDTO;
import com.samanatteu.entity.ImportMembre;
import com.samanatteu.exception.ImportIncoherentException;
import com.samanatteu.repository.ImportMembreRepository;

@Service 
public class ImportMembreService {
     private final ImportMembreRepository importMembreRepository;

    public ImportMembreService(ImportMembreRepository importMembreRepository) {
        this.importMembreRepository = importMembreRepository;
    }

    // Lister
    public List<ImportMembreDTO> listImportMembre() {
        return importMembreRepository.findAll() // 1. List<ImportMembre> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiImportMembreDTO) // 3. applique la conversion à CHAQUE ImportMembre -> ImportMembreDTO
                                                  // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<ImportMembreDTO> à partir du flux
    }

    // créer
    public ImportMembreDTO createImportMembre(ImportMembre importMembre) {
        if (importMembre.getNbImportes() + importMembre.getNbErreurs() != importMembre.getNbMembresTotal()){
            throw new ImportIncoherentException(importMembre.getNbMembresTotal(),importMembre.getNbImportes(), importMembre.getNbErreurs());
        }
        ImportMembre enregistre = importMembreRepository.save(importMembre);
        return convertiImportMembreDTO(enregistre);
    }

    // update
    public Optional<ImportMembreDTO> updateImportMembre(Long id, ImportMembre cycleModifier) {
        // findById(id) renvoie un Optional<ImportMembre> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return importMembreRepository.findById(id).map(cycleExsitant -> {
            // cycleExsitant = l'entité déjà en base (trouvée par findById).
            // cycleModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            cycleExsitant.setTontine(cycleModifier.getTontine());
            cycleExsitant.setFichierNom(cycleModifier.getFichierNom());
            cycleExsitant.setNbMembresTotal(cycleModifier.getNbMembresTotal());
            cycleExsitant.setNbImportes(cycleModifier.getNbImportes());
            cycleExsitant.setNbErreurs(cycleModifier.getNbErreurs());
            cycleExsitant.setErreursDetail(cycleModifier.getErreursDetail());
            cycleExsitant.setStatut(cycleModifier.getStatut());
            cycleExsitant.setCreatedAt(cycleModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité ImportMembre à jour
            // (avec motDePasse).
            ImportMembre enregistre = importMembreRepository.save(cycleExsitant);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<ImportMembreDTO>.
            return convertiImportMembreDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteImportMembre(Long id) {
        if (importMembreRepository.existsById(id)) {
            importMembreRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private ImportMembreDTO convertiImportMembreDTO(ImportMembre importMembre) {
        ImportMembreDTO dto = new ImportMembreDTO();
        dto.setId(importMembre.getId());
        dto.setTontineId(importMembre.getTontine().getId());
        dto.setFichierNom(importMembre.getFichierNom());
        dto.setNbMembresTotal(importMembre.getNbMembresTotal());
        dto.setNbImportes(importMembre.getNbImportes());
        dto.setNbErreurs(importMembre.getNbErreurs());
        dto.setErreursDetail(importMembre.getErreursDetail());
        dto.setStatut(importMembre.getStatut());
        dto.setCreatedAt(importMembre.getCreatedAt());

        return dto;
    }
}

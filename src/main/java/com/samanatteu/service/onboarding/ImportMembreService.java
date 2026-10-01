package com.samanatteu.service.onboarding;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.onboarding.ImportMembreDTO;
import com.samanatteu.entity.onboarding.ImportMembre;
import com.samanatteu.exception.onboarding.ImportIncoherentException;
import com.samanatteu.repository.onboarding.ImportMembreRepository;

@Service 
public class ImportMembreService {
     private final ImportMembreRepository importMembreRepository;

    public ImportMembreService(ImportMembreRepository importMembreRepository) {
        this.importMembreRepository = importMembreRepository;
    }

    public List<ImportMembreDTO> listImportMembre() {
        return importMembreRepository.findAll()
                .stream()
                .map(this::convertiImportMembreDTO)
                .toList();
    }

    public ImportMembreDTO createImportMembre(ImportMembre importMembre) {
        if (importMembre.getNbImportes() + importMembre.getNbErreurs() != importMembre.getNbMembresTotal()){
            throw new ImportIncoherentException(importMembre.getNbMembresTotal(),importMembre.getNbImportes(), importMembre.getNbErreurs());
        }
        ImportMembre enregistre = importMembreRepository.save(importMembre);
        return convertiImportMembreDTO(enregistre);
    }

    public Optional<ImportMembreDTO> updateImportMembre(Long id, ImportMembre cycleModifier) {
        return importMembreRepository.findById(id).map(cycleExsitant -> {
            cycleExsitant.setTontine(cycleModifier.getTontine());
            cycleExsitant.setFichierNom(cycleModifier.getFichierNom());
            cycleExsitant.setNbMembresTotal(cycleModifier.getNbMembresTotal());
            cycleExsitant.setNbImportes(cycleModifier.getNbImportes());
            cycleExsitant.setNbErreurs(cycleModifier.getNbErreurs());
            cycleExsitant.setErreursDetail(cycleModifier.getErreursDetail());
            cycleExsitant.setStatut(cycleModifier.getStatut());
            cycleExsitant.setCreatedAt(cycleModifier.getCreatedAt());

            ImportMembre enregistre = importMembreRepository.save(cycleExsitant);
            return convertiImportMembreDTO(enregistre);
        });
    }

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

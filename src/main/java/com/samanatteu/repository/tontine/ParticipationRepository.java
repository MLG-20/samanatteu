package com.samanatteu.repository.tontine;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.enums.tontine.StatutParticipation;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByMembreIdAndTontineId(Long membreId, Long tontineId);

    // La participation au plus grand ordre d'inscription ; Optional vide si aucun participant.
    Optional<Participation> findFirstByTontineIdOrderByOrdreInscriptionDesc(Long tontineId);

    List<Participation> findByTontineGestionnaireTelephone(String telephone);

    List<Participation> findByMembreTelephone(String telephone);

    // Les participations d'une tontine ayant ce statut (ex. ACTIF à l'ouverture
    // d'un cycle, pour savoir qui doit cotiser).
    List<Participation> findByTontineIdAndStatut(Long tontineId, StatutParticipation statut);

    // La participation d'un membre dans une tontine (vide s'il n'y est pas).
    // Sert au prêt : vérifier qu'il est bien membre de cette tontine.
    Optional<Participation> findByTontineIdAndMembreId(Long tontineId, Long membreId);

    // COUNT(*) côté base : on récupère un nombre, pas la liste des participants.
    long countByTontineIdAndStatut(Long tontineId, StatutParticipation statut);

    // Pour l'ADMIN : le nombre de membres de chaque gestionnaire, sans jamais charger
    // les membres eux-mêmes (ni nom, ni téléphone, ni email : frontière SaaS).
    // count(distinct …) : un membre inscrit dans deux tontines du même gestionnaire
    // compte une fois. Une seule requête groupée pour tous les gestionnaires (pas une
    // par ligne). Un gestionnaire sans aucun membre n'a pas de ligne : 0 par défaut.
    @Query("""
            select new com.samanatteu.repository.tontine.NbMembresParGestionnaire(
                p.tontine.gestionnaire.id, count(distinct p.membre.id))
            from Participation p
            where p.statut <> :exclu
            group by p.tontine.gestionnaire.id
            """)
    List<NbMembresParGestionnaire> compterMembresParGestionnaire(@Param("exclu") StatutParticipation exclu);

    // Pour la fiche d'un gestionnaire (ADMIN) : le nombre de membres de chacune de
    // ses tontines, en une requête. Une tontine sans membre n'a pas de ligne.
    @Query("""
            select new com.samanatteu.repository.tontine.NbMembresParTontine(
                p.tontine.id, count(p))
            from Participation p
            where p.tontine.gestionnaire.id = :gestionnaireId and p.statut <> :exclu
            group by p.tontine.id
            """)
    List<NbMembresParTontine> compterMembresParTontine(@Param("gestionnaireId") Long gestionnaireId,
            @Param("exclu") StatutParticipation exclu);

    // Le total de la fiche : personnes distinctes sur toutes les tontines d'UN
    // gestionnaire (on ne peut pas additionner les lignes par tontine : un membre
    // inscrit dans deux tontines serait compté deux fois).
    @Query("""
            select count(distinct p.membre.id)
            from Participation p
            where p.tontine.gestionnaire.id = :gestionnaireId and p.statut <> :exclu
            """)
    long compterMembresDistincts(@Param("gestionnaireId") Long gestionnaireId,
            @Param("exclu") StatutParticipation exclu);

}

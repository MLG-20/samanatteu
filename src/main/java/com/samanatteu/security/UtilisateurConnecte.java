package com.samanatteu.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.samanatteu.entity.Tontine;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;

@Component
public class UtilisateurConnecte {

    public String telephone() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public boolean aLeRole(RoleUtilisateur role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(autorite -> autorite.getAuthority().equals("ROLE_" + role.name()));
    }

    public void verifierGestionnaire(Tontine tontine) {
        if (!tontine.estGereePar(telephone())) {
            throw new AccesRefuseException();
        }
    }
}

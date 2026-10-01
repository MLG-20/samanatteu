package com.samanatteu.repository.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.onboarding.Invitation;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    
}

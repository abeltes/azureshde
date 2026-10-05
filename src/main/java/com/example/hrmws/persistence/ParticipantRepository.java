package com.example.hrmws.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<ParticipantEntity, ParticipantId> {
}

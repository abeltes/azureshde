package com.example.hrmws.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<IdentityEntity, String> {
}

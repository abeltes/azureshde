package com.example.hrmws.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "IDENTITIES")
public class IdentityEntity {

    @Id
    @Column(name = "AFM", nullable = false)
    private String afm;

    @Column(name = "ISSUEDATE")
    private LocalDateTime issueDate;

    @Column(name = "IDENTITYID")
    private String identityId;

    public IdentityEntity() {
    }

    public IdentityEntity(String afm, LocalDateTime issueDate, String identityId) {
        this.afm = afm;
        this.issueDate = issueDate;
        this.identityId = identityId;
    }
}

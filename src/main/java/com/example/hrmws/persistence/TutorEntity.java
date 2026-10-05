package com.example.hrmws.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "TUTORS")
@IdClass(TutorId.class)
public class TutorEntity {

    @Id
    @Column(name = "SEMCODE", nullable = false)
    private String semCode;

    @Id
    @Column(name = "AFM", nullable = false)
    private String afm;

    @Column(name = "CREATEDDATE", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "DELETED")
    private Boolean deleted;

    @Column(name = "MODIFIEDDATE")
    private LocalDateTime modifiedDate;

    public TutorEntity() {
    }

    public TutorEntity(String semCode, String afm, LocalDateTime createdDate, Boolean deleted, LocalDateTime modifiedDate) {
        this.semCode = semCode;
        this.afm = afm;
        this.createdDate = createdDate;
        this.deleted = deleted;
        this.modifiedDate = modifiedDate;
    }

    public String getSemCode() {
        return semCode;
    }

    public String getAfm() {
        return afm;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}

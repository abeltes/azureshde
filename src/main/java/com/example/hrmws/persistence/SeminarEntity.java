package com.example.hrmws.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SEMINARS")
public class SeminarEntity {

    @Id
    @Column(name = "SEMCODE", nullable = false)
    private String semCode;

    @Column(name = "SEMDESCR")
    private String semDescr;

    @Column(name = "BEGINDATE")
    private LocalDateTime beginDate;

    @Column(name = "ENDDATE")
    private LocalDateTime endDate;

    @Column(name = "CREATEDDATE", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "DELETED")
    private Boolean deleted;

    @Column(name = "MODIFIEDDATE")
    private LocalDateTime modifiedDate;

    public SeminarEntity() {
    }

    public SeminarEntity(String semCode, String semDescr, LocalDateTime beginDate, LocalDateTime endDate,
            LocalDateTime createdDate, Boolean deleted, LocalDateTime modifiedDate) {
        this.semCode = semCode;
        this.semDescr = semDescr;
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.createdDate = createdDate;
        this.deleted = deleted;
        this.modifiedDate = modifiedDate;
    }
}

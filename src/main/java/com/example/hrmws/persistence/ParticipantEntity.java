package com.example.hrmws.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "PARTICIPANTS")
@IdClass(ParticipantId.class)
public class ParticipantEntity {

    @Id
    @Column(name = "SEMCODE", nullable = false)
    private String semCode;

    @Id
    @Column(name = "AFM", nullable = false)
    private String afm;

    @Column(name = "ATTENDANCE")
    private Boolean attendance;

    @Column(name = "UNITDURINGSEMINAR")
    private String unitDuringSeminar;

    @Column(name = "DELETED")
    private Boolean deleted;

    @Column(name = "CREATEDDATE")
    private LocalDateTime createdDate;

    @Column(name = "MODIFIEDDATE")
    private LocalDateTime modifiedDate;

    public ParticipantEntity() {
    }

    public ParticipantEntity(String semCode, String afm, Boolean attendance, String unitDuringSeminar,
            Boolean deleted, LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.semCode = semCode;
        this.afm = afm;
        this.attendance = attendance;
        this.unitDuringSeminar = unitDuringSeminar;
        this.deleted = deleted;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }
}

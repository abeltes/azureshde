package com.example.hrmws.persistence;

import java.io.Serializable;
import java.util.Objects;

public class ParticipantId implements Serializable {

    private String semCode;
    private String afm;

    public ParticipantId() {
    }

    public ParticipantId(String semCode, String afm) {
        this.semCode = semCode;
        this.afm = afm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParticipantId that)) {
            return false;
        }
        return Objects.equals(semCode, that.semCode)
                && Objects.equals(afm, that.afm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semCode, afm);
    }
}

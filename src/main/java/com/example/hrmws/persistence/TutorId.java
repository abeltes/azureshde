package com.example.hrmws.persistence;

import java.io.Serializable;
import java.util.Objects;

public class TutorId implements Serializable {

    private String semCode;
    private String afm;

    public TutorId() {
    }

    public TutorId(String semCode, String afm) {
        this.semCode = semCode;
        this.afm = afm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TutorId tutorId)) {
            return false;
        }
        return Objects.equals(semCode, tutorId.semCode)
                && Objects.equals(afm, tutorId.afm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semCode, afm);
    }
}

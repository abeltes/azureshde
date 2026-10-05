package com.example.hrmws.api.dto;

import java.time.LocalDateTime;

public record Participant(
        String semCode,
        String afm,
        boolean attendance,
        String unitDuringSeminar,
        boolean deleted,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate
) {
}

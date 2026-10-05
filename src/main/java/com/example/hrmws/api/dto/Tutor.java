package com.example.hrmws.api.dto;

import java.time.LocalDateTime;

public record Tutor(
        String semCode,
        String afm,
        boolean deleted,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate
) {
}

package com.example.hrmws.api.dto;

import java.time.LocalDateTime;

public record Seminar(
        String semCode,
        String semDescr,
        LocalDateTime beginDate,
        LocalDateTime endDate,
        boolean deleted,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate
) {
}

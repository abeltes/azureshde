package com.example.hrmws.api.dto;

import java.time.LocalDateTime;

public record Identity(
        String afm,
        LocalDateTime issueDate,
        String identityId
) {
}

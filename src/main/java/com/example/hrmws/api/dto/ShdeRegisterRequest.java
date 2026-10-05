package com.example.hrmws.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShdeRegisterRequest(
        String afm,
        @JsonProperty("Title") String title,
        @JsonProperty("TypeId") Integer typeId,
        @JsonProperty("KindId") Integer kindId,
        @JsonProperty("MainFileName") String mainFileName,
        @JsonProperty("MainBase64File") String mainBase64File) {
}
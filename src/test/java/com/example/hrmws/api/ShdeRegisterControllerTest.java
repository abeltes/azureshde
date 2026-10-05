package com.example.hrmws.api;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.example.hrmws.api.dto.ShdeRegisterRequest;
import com.example.hrmws.service.ShdeRegisterService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShdeRegisterControllerTest {

    @Mock
    private ShdeRegisterService service;

    @Test
    void register_shouldReturnUpstreamJsonUnmodified() {
        ShdeRegisterController controller = new ShdeRegisterController(service);
        String json = "{\"registered\":true}";
        when(service.register(42L))
                .thenReturn(ResponseEntity.ok().body(json));

        ResponseEntity<?> result = controller.register(42L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(json, result.getBody());
    }

    @Test
    void register_shouldReturnRequestedErrorJsonForUpstreamHttpError() {
        ShdeRegisterController controller = new ShdeRegisterController(service);
        when(service.register(42L))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid registration"));

        ResponseEntity<?> result = controller.register(42L);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals(Map.of("error", 400, "errormessage", "400 Invalid registration"), result.getBody());
    }

    @Test
    void postRegister_shouldMapJsonFieldsAndForwardRequest() throws Exception {
        ShdeRegisterController controller = new ShdeRegisterController(service);
        ShdeRegisterRequest request = new ObjectMapper().readValue("""
                {
                  "afm": "012345",
                  "Title": "Title",
                  "TypeId": 1,
                  "KindId": 2,
                  "MainFileName": "mainfilename",
                  "MainBase64File": "encoded-content"
                }
                """, ShdeRegisterRequest.class);
        String responseBody = "{\"registered\":true}";
        when(service.register("012345", "Title", 1, 2, "mainfilename", "encoded-content"))
                .thenReturn(ResponseEntity.ok(responseBody));

        ResponseEntity<?> result = controller.register(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(responseBody, result.getBody());
        verify(service).register("012345", "Title", 1, 2, "mainfilename", "encoded-content");
    }
}
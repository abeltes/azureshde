package com.example.hrmws.api;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.example.hrmws.service.ShdeGetDocService;

@ExtendWith(MockitoExtension.class)
class ShdeGetDocControllerTest {

    @Mock
    private ShdeGetDocService service;

    @Test
    void getDocument_shouldReturnUpstreamJsonUnmodified() {
        ShdeGetDocController controller = new ShdeGetDocController(service);
        String json = "{\"document\":{\"id\":\"123\"}}";
        when(service.getDocument("123456789", "123"))
                .thenReturn(ResponseEntity.ok().body(json));

        ResponseEntity<?> result = controller.getDocument("123456789", "123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(json, result.getBody());
    }

    @Test
    void getDocument_shouldReturnRequestedErrorJsonForUpstreamHttpError() {
        ShdeGetDocController controller = new ShdeGetDocController(service);
        when(service.getDocument("123456789", "123"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Document not found"));

        ResponseEntity<?> result = controller.getDocument("123456789", "123");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals(Map.of("error", 404, "errormessage", "404 Document not found"), result.getBody());
    }
}
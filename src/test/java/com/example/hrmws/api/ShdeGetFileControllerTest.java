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

import com.example.hrmws.service.ShdeGetFileService;

@ExtendWith(MockitoExtension.class)
class ShdeGetFileControllerTest {

    @Mock
    private ShdeGetFileService service;

    @Test
    void getFile_shouldReturnUpstreamResponseUnmodified() {
        ShdeGetFileController controller = new ShdeGetFileController(service);
        String json = "{\"file\":\"content\"}";
        when(service.getFile("123456789", "doc 1", "file/1"))
                .thenReturn(ResponseEntity.ok().body(json));

        ResponseEntity<?> result = controller.getFile("123456789", "doc 1", "file/1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(json, result.getBody());
    }

    @Test
    void getFile_shouldReturnRequestedErrorJsonForUpstreamHttpError() {
        ShdeGetFileController controller = new ShdeGetFileController(service);
        when(service.getFile("123456789", "doc1", "file1"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "File not found"));

        ResponseEntity<?> result = controller.getFile("123456789", "doc1", "file1");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals(Map.of("error", 404, "errormessage", "404 File not found"), result.getBody());
    }
}
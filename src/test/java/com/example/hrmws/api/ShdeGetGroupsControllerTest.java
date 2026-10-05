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

import com.example.hrmws.service.ShdeGetGroupsService;

@ExtendWith(MockitoExtension.class)
class ShdeGetGroupsControllerTest {

    @Mock
    private ShdeGetGroupsService service;

    @Test
    void getGroups_shouldReturnUpstreamJsonUnmodified() {
        ShdeGetGroupsController controller = new ShdeGetGroupsController(service);
        String json = "[{\"role\":\"manager\"}]";
        when(service.getGroups("123456789"))
                .thenReturn(ResponseEntity.ok().body(json));

        ResponseEntity<?> result = controller.getGroups("123456789");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(json, result.getBody());
    }

    @Test
    void getGroups_shouldReturnRequestedErrorJsonForUpstreamHttpError() {
        ShdeGetGroupsController controller = new ShdeGetGroupsController(service);
        when(service.getGroups("123456789"))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Groups not found"));

        ResponseEntity<?> result = controller.getGroups("123456789");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals(Map.of("error", 404, "errormessage", "404 Groups not found"), result.getBody());
    }
}
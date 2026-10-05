package com.example.hrmws.service;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ShdeGetFileServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ShdeAuthenticationService authenticationService;

    @Test
    void getFile_shouldCallExpectedUrlWithAuthenticationHeaders() {
        ShdeGetFileService service = new ShdeGetFileService(
                restTemplate,
                authenticationService,
                "https://shde-test.aade.gr",
                "/services/v2/document");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("authentication-token");
        headers.set("X-User-Token", "user-token");
        when(authenticationService.headersFor("123456789")).thenReturn(headers);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"file\":\"content\"}"));

        ResponseEntity<String> result = service.getFile("123456789", "doc 1", "file/1");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(
                (Class<HttpEntity<Void>>) (Class<?>) HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));

        assertEquals("https://shde-test.aade.gr/services/v2/document/doc%201/file/file%2F1",
                uriCaptor.getValue().toString());
        assertEquals("Bearer authentication-token",
                entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("user-token", entityCaptor.getValue().getHeaders().getFirst("X-User-Token"));
        assertTrue(result.getBody().contains("\"file\""));
    }
}
package com.example.hrmws.api;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.hrmws.api.dto.Identity;
import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.IdentityUpsertService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class IdentitiesControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FotaTokenService fotaTokenService;

    @Mock
    private IdentityUpsertService identityUpsertService;

    private IdentitiesController controller;

    @Test
    void getIdentities_shouldCallFotaApiWithCookieHeaderAndDateParams() {
        controller = new IdentitiesController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/Identities",
                fotaTokenService,
                identityUpsertService);
        when(fotaTokenService.getAccessToken()).thenReturn("test-token");

        Identity identity = new Identity(
                "002912266",
                LocalDateTime.parse("2021-01-22T00:00:00"),
                "Α14375");
        ParameterizedTypeReference<List<Identity>> responseType = new ParameterizedTypeReference<>() {};
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(responseType)))
                .thenReturn(ResponseEntity.<List<Identity>>ok(List.of(identity)));

        ResponseEntity<?> result = controller.identities("2026-09-09", "2026-09-01");

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass((Class<HttpEntity<Void>>) (Class<?>) HttpEntity.class);

        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(responseType));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(List.of(identity), result.getBody());
        assertEquals("http://hvitestdate2.aade.int/Identities?toDate=2026-09-09&fromDate=2026-09-01",
                urlCaptor.getValue().toString());
        assertEquals("accessToken=test-token", entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.COOKIE));
        verify(fotaTokenService).getAccessToken();
        verify(identityUpsertService).upsertIdentities(List.of(identity));
    }

    @Test
    void getIdentities_shouldReturnErrorJsonWhenUpstreamReturnsHttpError() {
        controller = new IdentitiesController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/Identities",
                fotaTokenService,
                identityUpsertService);
        when(fotaTokenService.getAccessToken()).thenReturn("test-token");

        ParameterizedTypeReference<List<Identity>> responseType = new ParameterizedTypeReference<>() {};
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(responseType)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request from upstream"));

        ResponseEntity<?> result = controller.identities("2026-09-09", "2026-09-01");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> errorBody = assertInstanceOf(Map.class, result.getBody());
        assertEquals("400 Bad request from upstream", errorBody.get("error"));
        verify(fotaTokenService).getAccessToken();
        verify(identityUpsertService, never()).upsertIdentities(any());
    }

    @Test
    void getIdentities_shouldReturnUnauthorizedWhenTokenLookupFails() {
        controller = new IdentitiesController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/Identities",
                fotaTokenService,
                identityUpsertService);
        when(fotaTokenService.getAccessToken())
                .thenThrow(new IllegalStateException("No FOTA token found for id=1"));

        ResponseEntity<?> result = controller.identities("2026-09-09", "2026-09-01");

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<?, ?> errorBody = assertInstanceOf(Map.class, result.getBody());
        assertEquals("No FOTA token found for id=1", errorBody.get("error"));
        verify(fotaTokenService).getAccessToken();
        verify(identityUpsertService, never()).upsertIdentities(any());
    }
}

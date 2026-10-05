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

import com.example.hrmws.api.dto.Seminar;
import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.SeminarUpsertService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SeminarsControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FotaTokenService fotaTokenService;

    @Mock
    private SeminarUpsertService seminarUpsertService;

    private SeminarsController controller;

    @Test
    void getSeminars_shouldCallFotaApiWithCookieHeaderAndDateParams() {
        controller = new SeminarsController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/HrmsWebApi/Fota/Seminars",
                fotaTokenService,
                seminarUpsertService);
        when(fotaTokenService.getAccessToken()).thenReturn("test-token");

        Seminar seminar = new Seminar(
                "10.073.Φ.132.19",
                "Seminar title",
                LocalDateTime.parse("2026-09-01T09:00:00"),
                LocalDateTime.parse("2026-09-01T12:00:00"),
                false,
                LocalDateTime.parse("2026-06-23T12:58:15.583211"),
                null);
        ParameterizedTypeReference<List<Seminar>> responseType = new ParameterizedTypeReference<>() {};
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(responseType)))
                .thenReturn(ResponseEntity.<List<Seminar>>ok(List.of(seminar)));

        ResponseEntity<?> result = controller.seminars("2026-09-09", "2026-09-01");

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass((Class<HttpEntity<Void>>) (Class<?>) HttpEntity.class);

        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(responseType));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(List.of(seminar), result.getBody());
        assertEquals("http://hvitestdate2.aade.int/HrmsWebApi/Fota/Seminars?toDate=2026-09-09&fromDate=2026-09-01",
                urlCaptor.getValue().toString());
        assertEquals("accessToken=test-token", entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.COOKIE));
        verify(fotaTokenService).getAccessToken();
        verify(seminarUpsertService).upsertSeminars(List.of(seminar));
    }

    @Test
    void getSeminars_shouldReturnErrorJsonWhenUpstreamReturnsHttpError() {
        controller = new SeminarsController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/HrmsWebApi/Fota/Seminars",
                fotaTokenService,
                seminarUpsertService);
        when(fotaTokenService.getAccessToken()).thenReturn("test-token");

        ParameterizedTypeReference<List<Seminar>> responseType = new ParameterizedTypeReference<>() {};
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(responseType)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request from upstream"));

        ResponseEntity<?> result = controller.seminars("2026-09-09", "2026-09-01");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> errorBody = assertInstanceOf(Map.class, result.getBody());
        assertEquals("400 Bad request from upstream", errorBody.get("error"));
        verify(fotaTokenService).getAccessToken();
        verify(seminarUpsertService, never()).upsertSeminars(any());
    }

    @Test
    void getSeminars_shouldReturnUnauthorizedWhenTokenLookupFails() {
        controller = new SeminarsController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/HrmsWebApi/Fota/Seminars",
                fotaTokenService,
                seminarUpsertService);
        when(fotaTokenService.getAccessToken())
                .thenThrow(new IllegalStateException("No FOTA token found for id=1"));

        ResponseEntity<?> result = controller.seminars("2026-09-09", "2026-09-01");

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<?, ?> errorBody = assertInstanceOf(Map.class, result.getBody());
        assertEquals("No FOTA token found for id=1", errorBody.get("error"));
        verify(fotaTokenService).getAccessToken();
        verify(seminarUpsertService, never()).upsertSeminars(any());
    }
}

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

import com.example.hrmws.api.dto.Tutor;
import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.TutorUpsertService;

@ExtendWith(MockitoExtension.class)
class TutorsControllerTest {

    @Mock
    private RestTemplate restTemplate;

        @Mock
        private FotaTokenService fotaTokenService;

        @Mock
        private TutorUpsertService tutorUpsertService;

    private TutorsController controller;

    @Test
    void getTutors_shouldCallFotaApiWithCookieHeaderAndDateParams() {
        controller = new TutorsController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/HrmsWebApi/Fota/Tutors",
                fotaTokenService,
                tutorUpsertService);
        when(fotaTokenService.getAccessToken()).thenReturn("test-token");

        Tutor tutor = new Tutor("10.073.Φ.132.19", "123434637", false, LocalDateTime.parse("2026-06-23T12:58:15.583211"), null);
        ParameterizedTypeReference<List<Tutor>> responseType = new ParameterizedTypeReference<>() {};
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(responseType)))
                .thenReturn(ResponseEntity.<List<Tutor>>ok(List.of(tutor)));

        ResponseEntity<?> result = controller.tutors("2026-09-09", "2026-09-01");

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass((Class<HttpEntity<Void>>) (Class<?>) HttpEntity.class);

        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(responseType));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(List.of(tutor), result.getBody());
        assertEquals("http://hvitestdate2.aade.int/HrmsWebApi/Fota/Tutors?toDate=2026-09-09&fromDate=2026-09-01",
                urlCaptor.getValue().toString());
        assertEquals("accessToken=test-token", entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.COOKIE));
        verify(fotaTokenService).getAccessToken();
        verify(tutorUpsertService).upsertTutors(List.of(tutor));
    }

    @Test
    void getTutors_shouldReturnErrorJsonWhenUpstreamReturnsHttpError() {
        controller = new TutorsController(
                restTemplate,
                "http://hvitestdate2.aade.int",
                "/HrmsWebApi/Fota/Tutors",
                fotaTokenService,
                tutorUpsertService);
        when(fotaTokenService.getAccessToken()).thenReturn("test-token");

        ParameterizedTypeReference<List<Tutor>> responseType = new ParameterizedTypeReference<>() {};
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(responseType)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request from upstream"));

        ResponseEntity<?> result = controller.tutors("2026-09-09", "2026-09-01");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> errorBody = assertInstanceOf(Map.class, result.getBody());
        assertEquals("400 Bad request from upstream", errorBody.get("error"));
        verify(fotaTokenService).getAccessToken();
        verify(tutorUpsertService, never()).upsertTutors(any());
    }

        @Test
        void getTutors_shouldReturnUnauthorizedWhenTokenLookupFails() {
                controller = new TutorsController(
                                restTemplate,
                                "http://hvitestdate2.aade.int",
                                "/HrmsWebApi/Fota/Tutors",
                                fotaTokenService,
                                tutorUpsertService);
                when(fotaTokenService.getAccessToken())
                                .thenThrow(new IllegalStateException("No FOTA token found for id=1"));

                ResponseEntity<?> result = controller.tutors("2026-09-09", "2026-09-01");

                assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
                Map<?, ?> errorBody = assertInstanceOf(Map.class, result.getBody());
                assertEquals("No FOTA token found for id=1", errorBody.get("error"));
                verify(fotaTokenService).getAccessToken();
                verify(tutorUpsertService, never()).upsertTutors(any());
        }
}

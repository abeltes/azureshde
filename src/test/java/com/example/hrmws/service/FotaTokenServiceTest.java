package com.example.hrmws.service;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FotaTokenServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RestTemplate restTemplate;

    private FotaTokenService service() {
        return new FotaTokenService(
                jdbcTemplate,
                restTemplate,
                1,
                "http://hvitestdate2.aade.int",
                "/HrmsWebApi/Auth/GenerateAccessToken",
                "{\"username\":\"fotaProj\",\"password\":\"secret\"}");
    }

    @Test
    void getAccessToken_shouldReturnStoredTokenWhenExpiryIsMoreThanOneHourAway() {
        FotaTokenService service = service();
        when(jdbcTemplate.queryForObject(
                eq("select token, expdt from tokens where id = ?"),
                ArgumentMatchers.<RowMapper<FotaTokenService.StoredToken>>any(),
                eq(1L)))
                .thenReturn(new FotaTokenService.StoredToken("stored-token", Instant.now().plusSeconds(7200)));

        String token = service.getAccessToken();

        assertEquals("stored-token", token);
        verify(restTemplate, org.mockito.Mockito.never())
                .postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getAccessToken_shouldRefreshTokenWhenExpiryIsLessThanOneHourAway() {
        FotaTokenService service = service();
        when(jdbcTemplate.queryForObject(
                eq("select token, expdt from tokens where id = ?"),
                ArgumentMatchers.<RowMapper<FotaTokenService.StoredToken>>any(),
                eq(1L)))
                .thenReturn(new FotaTokenService.StoredToken("old-token", Instant.now().plusSeconds(1800)));

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE,
                "accessToken=refreshed-token; expires=Tue, 22 Sep 2026 13:11:04 GMT; path=/; secure");
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", responseHeaders, HttpStatus.OK));
        when(jdbcTemplate.update(
                eq("update tokens set token = ?, expdt = ? where id = ?"),
                eq("refreshed-token"),
                any(Timestamp.class),
                eq(1L)))
                .thenReturn(1);

        String token = service.getAccessToken();

        assertEquals("refreshed-token", token);
        verify(jdbcTemplate).update(
                eq("update tokens set token = ?, expdt = ? where id = ?"),
                eq("refreshed-token"),
                any(Timestamp.class),
                eq(1L));
    }

    @Test
    void refreshAccessToken_shouldPostCredentialsStoreTokenAndReturnCookieValues() {
        FotaTokenService service = service();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE,
                "accessToken=test-token; expires=Tue, 22 Sep 2026 13:11:04 GMT; path=/; secure; samesite=none; httponly");
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("", responseHeaders, HttpStatus.OK));
        when(jdbcTemplate.update(
                eq("update tokens set token = ?, expdt = ? where id = ?"),
                eq("test-token"),
                any(Timestamp.class),
                eq(1L)))
                .thenReturn(1);

        FotaTokenService.TokenRefreshResult result = service.refreshAccessToken();

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> requestCaptor = ArgumentCaptor.forClass((Class<HttpEntity<String>>) (Class<?>) HttpEntity.class);
        ArgumentCaptor<Timestamp> timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);

        verify(restTemplate).postForEntity(uriCaptor.capture(), requestCaptor.capture(), eq(String.class));
        verify(jdbcTemplate).update(
                eq("update tokens set token = ?, expdt = ? where id = ?"),
                eq("test-token"),
                timestampCaptor.capture(),
                eq(1L));

        assertEquals("http://hvitestdate2.aade.int/HrmsWebApi/Auth/GenerateAccessToken", uriCaptor.getValue().toString());
        assertEquals(MediaType.APPLICATION_JSON, requestCaptor.getValue().getHeaders().getContentType());
        assertEquals("{\"username\":\"fotaProj\",\"password\":\"secret\"}", requestCaptor.getValue().getBody());
        assertEquals("test-token", result.token());
        assertEquals("Tue, 22 Sep 2026 13:11:04 GMT", result.expdate());
        assertEquals(Instant.parse("2026-09-22T13:11:04Z"), timestampCaptor.getValue().toInstant());
    }
}

package com.example.hrmws.service;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FotaTokenService {

    public record TokenRefreshResult(String token, String expdate) {}
    record StoredToken(String token, Instant expiresAt) {}

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final long tokenId;
    private final String baseUrl;
    private final String tokenEndpoint;
    private final String credentials;

    public FotaTokenService(
            JdbcTemplate jdbcTemplate,
            RestTemplate restTemplate,
            @Value("${hrmws.fota_tokenid}") long tokenId,
            @Value("${hrmws.fota_baseurl}") String baseUrl,
            @Value("${hrmws.fota_token_endpoint}") String tokenEndpoint,
            @Value("${hrmws.fota_credentials}") String credentials) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
        this.tokenId = tokenId;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint).trim();
        this.credentials = Objects.requireNonNull(credentials);
    }

    public String getAccessToken() {
        String sql = "select token, expdt from tokens where id = ?";
        StoredToken storedToken = jdbcTemplate.queryForObject(sql, (resultSet, rowNum) -> {
            Timestamp expiresAt = resultSet.getTimestamp("expdt");
            return new StoredToken(
                    resultSet.getString("token"),
                    expiresAt == null ? null : expiresAt.toInstant());
        }, tokenId);
        if (storedToken == null) {
            throw new IllegalStateException("No FOTA token found for id=" + tokenId);
        }

        if (storedToken.expiresAt() == null
                || storedToken.expiresAt().isBefore(Instant.now().plus(Duration.ofHours(1)))) {
            return refreshAccessToken().token();
        }

        String token = storedToken.token();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No FOTA token found for id=" + tokenId);
        }
        return token;
    }

    public TokenRefreshResult refreshAccessToken() {
        URI uri = UriComponentsBuilder.fromUriString(Objects.requireNonNull(baseUrl))
            .path(Objects.requireNonNull(tokenEndpoint))
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(credentials, headers);

        System.out.printf("Calling FOTA token endpoint: POST %s%n", uri);
        ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
        TokenRefreshResult result = tokenFromSetCookie(response.getHeaders().get(HttpHeaders.SET_COOKIE));
        updateStoredToken(result);
        return result;
    }

    private void updateStoredToken(TokenRefreshResult result) {
        Instant expiresAt = ZonedDateTime.parse(result.expdate(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        int updated = jdbcTemplate.update(
                "update tokens set token = ?, expdt = ? where id = ?",
                result.token(),
                Timestamp.from(expiresAt),
                tokenId);
        if (updated == 0) {
            throw new RestClientException("No token row found for id=" + tokenId);
        }
    }

    private TokenRefreshResult tokenFromSetCookie(List<String> setCookieHeaders) {
        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            throw new RestClientException("FOTA token response did not include a Set-Cookie header");
        }

        for (String setCookieHeader : setCookieHeaders) {
            TokenRefreshResult result = tokenFromSetCookie(setCookieHeader);
            if (result != null) {
                return result;
            }
        }

        throw new RestClientException("FOTA token response did not include an accessToken cookie");
    }

    private TokenRefreshResult tokenFromSetCookie(String setCookieHeader) {
        if (setCookieHeader == null || setCookieHeader.isBlank()) {
            return null;
        }

        String[] parts = setCookieHeader.split(";");
        String token = null;
        String expires = null;

        for (String part : parts) {
            String attribute = part.trim();
            String lowerAttribute = attribute.toLowerCase(Locale.ROOT);
            if (lowerAttribute.startsWith("accesstoken=")) {
                token = attribute.substring("accessToken=".length());
            } else if (lowerAttribute.startsWith("expires=")) {
                expires = attribute.substring("expires=".length());
            }
        }

        if (token == null) {
            return null;
        }
        if (token.isBlank()) {
            throw new RestClientException("FOTA token response included a blank accessToken cookie");
        }
        if (expires == null || expires.isBlank()) {
            throw new RestClientException("FOTA accessToken cookie did not include an expires attribute");
        }

        try {
            ZonedDateTime.parse(expires, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new RestClientException("FOTA accessToken cookie included an invalid expires attribute", ex);
        }

        return new TokenRefreshResult(token, expires);
    }
}

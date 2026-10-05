package com.example.hrmws.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ShdeAuthenticationService {

    private static final long TOKEN_REFRESH_WINDOW_SECONDS = 10 * 60;

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final long tokenId;
    private final long pkiKeyId;
    private final String baseUrl;
    private final String tokenEndpoint;
    private final String credentials;

    public ShdeAuthenticationService(
            JdbcTemplate jdbcTemplate,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${hrmws.shde_tokenid}") long tokenId,
            @Value("${hrmws.shde_pkikey}") long pkiKeyId,
            @Value("${hrmws.shde_baseurl}") String baseUrl,
            @Value("${hrmws.shde_token_endpoint}") String tokenEndpoint,
            @Value("${hrmws.shde_credentials}") String credentials) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tokenId = tokenId;
        this.pkiKeyId = pkiKeyId;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint).trim();
        this.credentials = Objects.requireNonNull(credentials);
    }

    public HttpHeaders headersFor(String afm) {
        String authenticationToken = getAuthenticationToken();
        JsonNode tokenClaims = parseJwtPayload(authenticationToken);
        String userInfoToken = createUserInfoToken(afm, tokenClaims);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authenticationToken);
        headers.set("X-User-Token", userInfoToken);
        return headers;
    }

    private String getAuthenticationToken() {
        StoredToken storedToken = jdbcTemplate.queryForObject(
                "select token, expdt from tokens where id = ?",
                (resultSet, rowNum) -> new StoredToken(
                        resultSet.getString("token"),
                        resultSet.getTimestamp("expdt") == null
                                ? null
                                : resultSet.getTimestamp("expdt").toInstant()),
                tokenId);
        if (storedToken == null || storedToken.token() == null || storedToken.token().isBlank()) {
            return refreshAuthenticationToken();
        }
        if (storedToken.expiresAt() == null
                || storedToken.expiresAt().isBefore(Instant.now().plusSeconds(TOKEN_REFRESH_WINDOW_SECONDS))) {
            return refreshAuthenticationToken();
        }
        return storedToken.token();
    }

    private String refreshAuthenticationToken() {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(tokenEndpoint)
                .build()
                .encode()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        System.out.printf("Calling SHDE token endpoint: POST %s%n", uri);
        ResponseEntity<String> response = restTemplate.postForEntity(
                uri, new HttpEntity<>(credentials, headers), String.class);
        try {
            JsonNode tokenResponse = objectMapper.readTree(response.getBody());
            String token = requiredText(tokenResponse, "AuthenticationToken");
            long expiresIn = requiredLong(tokenResponse, "ExpiresIn");
            int updated = jdbcTemplate.update(
                    "update tokens set token = ?, expdt = ? where id = ?",
                    token,
                    Timestamp.from(Instant.now().plusSeconds(expiresIn)),
                    tokenId);
            if (updated == 0) {
                throw new IllegalStateException("No SHDE token row found for id=" + tokenId);
            }
            return token;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Invalid SHDE token response", ex);
        }
    }

    private JsonNode parseJwtPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalStateException("Invalid SHDE authentication token");
        }
        try {
            return objectMapper.readTree(new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid SHDE authentication token", ex);
        }
    }

    private String createUserInfoToken(String afm, JsonNode tokenClaims) {
        try {
            long issuedAt = requiredLong(tokenClaims, "iat");
            long expiresAt = requiredLong(tokenClaims, "exp");
            String key = jdbcTemplate.queryForObject(
                    "select key from pkikeys where id = ?", String.class, pkiKeyId);
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("No SHDE private key found for id=" + pkiKeyId);
            }

            String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String payload = base64Url(objectMapper.createObjectNode()
                    .put("sub", afm)
                    .put("iat", issuedAt)
                    .put("exp", expiresAt)
                    .toString());
            String unsignedToken = header + "." + payload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey(key));
            signature.update(unsignedToken.getBytes(StandardCharsets.US_ASCII));
            return unsignedToken + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signature.sign());
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create SHDE user token", ex);
        }
    }

    private PrivateKey readPrivateKey(String pem) throws Exception {
        String encoded = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("SHDE token response is missing " + field);
        }
        return value.asText();
    }

    private long requiredLong(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new IllegalStateException("SHDE token is missing " + field);
        }
        return value.asLong();
    }

    private record StoredToken(String token, Instant expiresAt) {}
}
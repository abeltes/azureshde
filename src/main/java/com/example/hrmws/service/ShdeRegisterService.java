package com.example.hrmws.service;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.sql.PreparedStatement;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ShdeRegisterService {

    private final RestTemplate restTemplate;
    private final ShdeAuthenticationService authenticationService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String registerEndpoint;

    public ShdeRegisterService(
            RestTemplate restTemplate,
            ShdeAuthenticationService authenticationService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${hrmws.shde_baseurl}") String baseUrl,
            @Value("${hrmws.shde_register_endpoint}") String registerEndpoint) {
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.registerEndpoint = Objects.requireNonNull(registerEndpoint).trim();
    }

    public ResponseEntity<String> register(long inoutId) {
        ShdeInout inout = jdbcTemplate.queryForObject(
                "select afm, json_in from shdeinout where id = ?",
                (resultSet, rowNum) -> new ShdeInout(
                        resultSet.getString("afm"),
                        readClob(resultSet.getCharacterStream("json_in"))),
                inoutId);
        if (inout == null) {
            throw new IllegalStateException("No shdeinout row found for id=" + inoutId);
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(registerEndpoint)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = authenticationService.headersFor(inout.afm());
        headers.setContentType(MediaType.APPLICATION_JSON);
        System.out.printf("Calling SHDE register endpoint: POST %s%n", uri);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    uri,
                    new HttpEntity<>(inout.jsonIn(), headers),
                    String.class);
            String output = response.getBody();
            updateJsonOut(inoutId, output);
            MediaType contentType = response.getHeaders().getContentType();
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(contentType == null ? MediaType.APPLICATION_JSON : contentType)
                    .body(output);
        } catch (HttpStatusCodeException ex) {
            String output = errorJson(ex.getStatusCode().value(), ex.getResponseBodyAsString());
            updateJsonOut(inoutId, output);
            throw ex;
        } catch (RestClientException | IllegalStateException ex) {
            updateJsonOut(inoutId, errorJson(HttpStatus.BAD_GATEWAY.value(), ex.getMessage()));
            throw ex;
        }
    }

        public ResponseEntity<String> register(
            String afm,
            String title,
            Integer typeId,
            Integer kindId,
            String mainFileName,
            String mainBase64File) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
            .path(registerEndpoint)
            .build()
            .encode()
            .toUri();

        HttpHeaders headers = authenticationService.headersFor(afm);
        headers.setContentType(MediaType.APPLICATION_JSON);
        System.out.printf("Calling SHDE register endpoint: POST %s%n", uri);
        ResponseEntity<String> response = restTemplate.postForEntity(
            uri,
            new HttpEntity<>(registrationPayload(title, typeId, kindId, mainFileName, mainBase64File), headers),
            String.class);
        MediaType contentType = response.getHeaders().getContentType();
        return ResponseEntity.status(response.getStatusCode())
            .contentType(contentType == null ? MediaType.APPLICATION_JSON : contentType)
            .body(response.getBody());
        }

        private String registrationPayload(
            String title,
            Integer typeId,
            Integer kindId,
            String mainFileName,
            String mainBase64File) {
            ObjectNode document = objectMapper.createObjectNode();
            document.put("Title", title);
            document.set("Type", objectMapper.createObjectNode().put("Id", typeId));
            document.set("Kind", objectMapper.createObjectNode().put("Id", kindId));
            document.set("MainFile", objectMapper.createObjectNode()
                .put("FileName", mainFileName)
                .put("Base64File", mainBase64File));

            return objectMapper.createObjectNode()
                .set("Document", document)
                .toString();
        }

    private void updateJsonOut(long inoutId, String output) {
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "update shdeinout set json_out = ? where id = ?");
            statement.setCharacterStream(1, new StringReader(output == null ? "null" : output));
            statement.setLong(2, inoutId);
            return statement;
        });
    }

    private String errorJson(int status, String message) {
        try {
            if (message != null && !message.isBlank() && objectMapper.readTree(message) != null) {
                return message;
            }
            return objectMapper.createObjectNode()
                    .put("error", status)
                    .put("errormessage", message == null || message.isBlank() ? "Unexpected error" : message)
                    .toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create SHDE register error JSON", ex);
        }
    }

    private String readClob(Reader reader) {
        if (reader == null) {
            return null;
        }
        try (reader) {
            StringBuilder value = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                value.append(buffer, 0, read);
            }
            return value.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read shdeinout.json_in", ex);
        }
    }

    private record ShdeInout(String afm, String jsonIn) {}
}
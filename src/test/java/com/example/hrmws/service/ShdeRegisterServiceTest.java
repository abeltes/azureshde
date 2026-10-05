package com.example.hrmws.service;

import java.io.StringReader;
import java.net.URI;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ShdeRegisterServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ShdeAuthenticationService authenticationService;

        @Mock
        private JdbcTemplate jdbcTemplate;

    @Test
    void register_shouldPostTemplateWithAuthenticationHeaders() {
        ShdeRegisterService service = new ShdeRegisterService(
                restTemplate,
                authenticationService,
                jdbcTemplate,
                new ObjectMapper(),
                "https://shde-test.aade.gr",
                "/services/v2/document/register");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("authentication-token");
        headers.set("X-User-Token", "user-token");
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq(42L)))
                .thenAnswer(invocation -> {
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getString("afm")).thenReturn("123456789");
                    when(resultSet.getCharacterStream("json_in"))
                            .thenReturn(new StringReader("{\"Document\":{}}"));
                    @SuppressWarnings("unchecked")
                    RowMapper<?> rowMapper = invocation.getArgument(1);
                    return rowMapper.mapRow(resultSet, 0);
                });
        when(authenticationService.headersFor("123456789")).thenReturn(headers);
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"registered\":true}"));

        ResponseEntity<String> result = service.register(42L);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(
                (Class<HttpEntity<String>>) (Class<?>) HttpEntity.class);
        verify(restTemplate).postForEntity(uriCaptor.capture(), entityCaptor.capture(), eq(String.class));

        assertEquals("https://shde-test.aade.gr/services/v2/document/register", uriCaptor.getValue().toString());
        assertEquals(MediaType.APPLICATION_JSON, entityCaptor.getValue().getHeaders().getContentType());
        assertEquals("Bearer authentication-token",
                entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("user-token", entityCaptor.getValue().getHeaders().getFirst("X-User-Token"));
        assertTrue(entityCaptor.getValue().getBody().contains("\"Document\""));
        assertEquals("{\"registered\":true}", result.getBody());
                verify(jdbcTemplate).update(any(org.springframework.jdbc.core.PreparedStatementCreator.class));
    }

    @Test
    void register_shouldBuildPostPayloadAndUseSubmittedAfmWithoutDatabase() throws Exception {
        ShdeRegisterService service = new ShdeRegisterService(
                restTemplate,
                authenticationService,
                jdbcTemplate,
                new ObjectMapper(),
                "https://shde-test.aade.gr",
                "/services/v2/document/register");
        HttpHeaders headers = new HttpHeaders();
        when(authenticationService.headersFor("012345")).thenReturn(headers);
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"registered\":true}"));

        ResponseEntity<String> result = service.register(
                "012345", "Title", 1, 2, "mainfilename", "encoded-content");

        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(
                (Class<HttpEntity<String>>) (Class<?>) HttpEntity.class);
        verify(authenticationService).headersFor("012345");
        verify(restTemplate).postForEntity(any(URI.class), entityCaptor.capture(), eq(String.class));
        verifyNoInteractions(jdbcTemplate);
        assertEquals("{\"registered\":true}", result.getBody());
        assertEquals(
                new ObjectMapper().readTree("""
                        {
                          "Document": {
                            "Title": "Title",
                            "Type": {"Id": 1},
                            "Kind": {"Id": 2},
                            "MainFile": {
                              "FileName": "mainfilename",
                              "Base64File": "encoded-content"
                            }
                          }
                        }
                        """),
                new ObjectMapper().readTree(entityCaptor.getValue().getBody()));
    }
}
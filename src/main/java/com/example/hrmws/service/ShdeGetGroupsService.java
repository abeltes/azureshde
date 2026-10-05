package com.example.hrmws.service;

import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ShdeGetGroupsService {

    private final RestTemplate restTemplate;
    private final ShdeAuthenticationService authenticationService;
    private final String baseUrl;
    private final String groupsEndpoint;

    public ShdeGetGroupsService(
            RestTemplate restTemplate,
            ShdeAuthenticationService authenticationService,
            @Value("${hrmws.shde_baseurl}") String baseUrl,
            @Value("${hrmws.shde_groups_endpoint}") String groupsEndpoint) {
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.groupsEndpoint = Objects.requireNonNull(groupsEndpoint).trim();
    }

    public ResponseEntity<String> getGroups(String afm) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(groupsEndpoint)
                .queryParam("afm", afm)
                .build()
                .encode()
                .toUri();
        System.out.printf("Calling SHDE groups endpoint: GET %s%n", uri);
        ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(authenticationService.headersFor(afm)),
                String.class);
        MediaType contentType = response.getHeaders().getContentType();
        return ResponseEntity.status(response.getStatusCode())
                .contentType(contentType == null ? MediaType.APPLICATION_JSON : contentType)
                .body(response.getBody());
    }
}
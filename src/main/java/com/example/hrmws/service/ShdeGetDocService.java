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
public class ShdeGetDocService {

    private final RestTemplate restTemplate;
    private final ShdeAuthenticationService authenticationService;
    private final String baseUrl;
    private final String docsEndpoint;
    public ShdeGetDocService(
            RestTemplate restTemplate,
            ShdeAuthenticationService authenticationService,
            @Value("${hrmws.shde_baseurl}") String baseUrl,
            @Value("${hrmws.shde_docs_endpoint}") String docsEndpoint) {
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.docsEndpoint = Objects.requireNonNull(docsEndpoint).trim();
    }

    public ResponseEntity<String> getDocument(String afm, String docid) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(docsEndpoint)
                .pathSegment(docid)
                .build()
                .encode()
                .toUri();

            System.out.printf("Calling SHDE document endpoint: GET %s%n", uri);
        ResponseEntity<String> response = restTemplate.exchange(
            uri, HttpMethod.GET, new HttpEntity<>(authenticationService.headersFor(afm)), String.class);
        MediaType contentType = response.getHeaders().getContentType();
        return ResponseEntity.status(response.getStatusCode())
                .contentType(contentType == null ? MediaType.APPLICATION_JSON : contentType)
                .body(response.getBody());
    }

}
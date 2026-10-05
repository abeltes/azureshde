package com.example.hrmws.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.hrmws.api.dto.Participant;
import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.ParticipantUpsertService;

@RestController
@RequestMapping("${hrmws.participantsapi}")
public class ParticipantsController {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String participantsEndpoint;
    private final FotaTokenService fotaTokenService;
    private final ParticipantUpsertService participantUpsertService;

    public ParticipantsController(
            RestTemplate restTemplate,
            @Value("${hrmws.fota_baseurl}") String baseUrl,
            @Value("${hrmws.fota_participants_endpoint}") String participantsEndpoint,
            FotaTokenService fotaTokenService,
            ParticipantUpsertService participantUpsertService) {
        this.restTemplate = restTemplate;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.participantsEndpoint = Objects.requireNonNull(participantsEndpoint).trim();
        this.fotaTokenService = Objects.requireNonNull(fotaTokenService);
        this.participantUpsertService = Objects.requireNonNull(participantUpsertService);
    }

    public ResponseEntity<?> participants(String toDate, String fromDate) {
        return participants(toDate, fromDate, null);
    }

    @GetMapping
    public ResponseEntity<?> participants(
            @RequestParam("toDate") String toDate,
            @RequestParam("fromDate") String fromDate,
            @RequestParam(value = "noout", required = false) Integer noout) {
        URI uri = UriComponentsBuilder.fromUriString(Objects.requireNonNull(baseUrl))
                .path(Objects.requireNonNull(participantsEndpoint))
                .queryParam("toDate", "{toDate}")
                .queryParam("fromDate", "{fromDate}")
                .buildAndExpand(toDate, fromDate)
                .encode()
                .toUri();

        try {
            String accessToken = fotaTokenService.getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.COOKIE, "accessToken=" + accessToken);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            System.out.printf("Calling FOTA participants endpoint: %s %s%n", HttpMethod.GET, uri);
            ResponseEntity<List<Participant>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Participant>>() {}
            );

            List<Participant> participants = response.getBody() == null ? List.of() : response.getBody();
            int insertedCount = participantUpsertService.upsertParticipants(participants);
            if (Integer.valueOf(1).equals(noout)) {
                return ResponseEntity.ok(Map.of(
                        "sourceRecords", participants.size(),
                        "insertedRecords", insertedCount));
            }
            return ResponseEntity.ok(participants);
        } catch (HttpStatusCodeException e) {
            String message = e.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = e.getMessage();
            }
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", message));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (RestClientException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

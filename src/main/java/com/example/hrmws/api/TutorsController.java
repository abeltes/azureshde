package com.example.hrmws.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.net.URI;

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

import com.example.hrmws.api.dto.Tutor;
import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.TutorUpsertService;

@RestController
@RequestMapping("${hrmws.tutorsapi}")
public class TutorsController {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String tutorsEndpoint;
    private final FotaTokenService fotaTokenService;
    private final TutorUpsertService tutorUpsertService;

    public TutorsController(
            RestTemplate restTemplate,
            @Value("${hrmws.fota_baseurl}") String baseUrl,
            @Value("${hrmws.fota_tutors_endpoint}") String tutorsEndpoint,
            FotaTokenService fotaTokenService,
            TutorUpsertService tutorUpsertService) {
        this.restTemplate = restTemplate;
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.tutorsEndpoint = Objects.requireNonNull(tutorsEndpoint).trim();
        this.fotaTokenService = Objects.requireNonNull(fotaTokenService);
        this.tutorUpsertService = Objects.requireNonNull(tutorUpsertService);
    }

    public ResponseEntity<?> tutors(String toDate, String fromDate) {
        return tutors(toDate, fromDate, null);
    }

    @GetMapping
    public ResponseEntity<?> tutors(
            @RequestParam("toDate") String toDate,
            @RequestParam("fromDate") String fromDate,
            @RequestParam(value = "noout", required = false) Integer noout) {
        URI uri = UriComponentsBuilder.fromUriString(Objects.requireNonNull(baseUrl))
                .path(Objects.requireNonNull(tutorsEndpoint))
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
            System.out.printf("Calling FOTA tutors endpoint: %s %s%n", HttpMethod.GET, uri);
            ResponseEntity<List<Tutor>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Tutor>>() {}
            );

            List<Tutor> tutors = response.getBody() == null ? List.of() : response.getBody();
            int insertedCount = tutorUpsertService.upsertTutors(tutors);
            if (Integer.valueOf(1).equals(noout)) {
                return ResponseEntity.ok(Map.of(
                        "sourceRecords", tutors.size(),
                        "insertedRecords", insertedCount));
            }
            return ResponseEntity.ok(tutors);
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

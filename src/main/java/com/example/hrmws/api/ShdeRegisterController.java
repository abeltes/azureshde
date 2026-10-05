package com.example.hrmws.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.example.hrmws.api.dto.ShdeRegisterRequest;
import com.example.hrmws.service.ShdeRegisterService;

@RestController
@RequestMapping("${hrmws.shderegisterapi}")
public class ShdeRegisterController {

    private final ShdeRegisterService service;

    public ShdeRegisterController(ShdeRegisterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> register(@RequestParam("inoutId") long inoutId) {
        try {
            return service.register(inoutId);
        } catch (HttpStatusCodeException ex) {
            String message = ex.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = ex.getMessage();
            }
            return error(ex.getStatusCode().value(), message);
        } catch (RestClientException | IllegalStateException ex) {
            return error(HttpStatus.BAD_GATEWAY.value(), ex.getMessage());
        } catch (Exception ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> register(@RequestBody ShdeRegisterRequest request) {
        try {
            return service.register(
                    request.afm(),
                    request.title(),
                    request.typeId(),
                    request.kindId(),
                    request.mainFileName(),
                    request.mainBase64File());
        } catch (HttpStatusCodeException ex) {
            String message = ex.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = ex.getMessage();
            }
            return error(ex.getStatusCode().value(), message);
        } catch (RestClientException | IllegalStateException ex) {
            return error(HttpStatus.BAD_GATEWAY.value(), ex.getMessage());
        } catch (Exception ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", status);
        body.put("errormessage", message == null || message.isBlank() ? "Unexpected error" : message);
        return ResponseEntity.status(status).body(body);
    }
}
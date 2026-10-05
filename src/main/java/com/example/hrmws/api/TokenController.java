package com.example.hrmws.api;

import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.FotaTokenService.TokenRefreshResult;

@RestController
@RequestMapping("${hrmws.newtokenapi}")
public class TokenController {

    private final FotaTokenService fotaTokenService;

    public TokenController(FotaTokenService fotaTokenService) {
        this.fotaTokenService = Objects.requireNonNull(fotaTokenService);
    }

    @GetMapping
    public TokenRefreshResult getToken() {
        return fotaTokenService.refreshAccessToken();
    }
}

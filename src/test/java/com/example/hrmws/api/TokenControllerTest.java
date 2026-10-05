package com.example.hrmws.api;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;

import com.example.hrmws.service.FotaTokenService;
import com.example.hrmws.service.FotaTokenService.TokenRefreshResult;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TokenControllerTest {

    @Mock
    private FotaTokenService fotaTokenService;

        private MockMvc mockMvc() {
                return MockMvcBuilders
                .standaloneSetup(new TokenController(fotaTokenService))
                                .addPlaceholderValue("hrmws.newtokenapi", "/api/v1/newtoken")
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void getToken_shouldReturnTokenJson() throws Exception {
        when(fotaTokenService.refreshAccessToken())
                .thenReturn(new TokenRefreshResult("test-token", "Tue, 22 Sep 2026 13:11:04 GMT"));

        mockMvc().perform(get("/api/v1/newtoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.expdate").value("Tue, 22 Sep 2026 13:11:04 GMT"));
    }

    @Test
    void getToken_shouldReturnStandardErrorJsonWhenUpstreamReturnsHttpError() throws Exception {
        when(fotaTokenService.refreshAccessToken())
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad request from upstream",
                        new HttpHeaders(),
                        "bad token request".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));

        mockMvc().perform(get("/api/v1/newtoken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Upstream service error"))
                .andExpect(jsonPath("$.message").value(containsString("bad token request")))
                .andExpect(jsonPath("$.path").value("/api/v1/newtoken"));
    }
}

package com.ymall.backend.integration.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HttpSecurityBoundaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsTraceConnectAndWebDavMethods() throws Exception {
        mockMvc.perform(request(HttpMethod.TRACE, "/api/products"))
            .andExpect(status().is4xxClientError());
        mockMvc.perform(request(HttpMethod.valueOf("CONNECT"), "/api/products"))
            .andExpect(status().is4xxClientError());
        mockMvc.perform(request(HttpMethod.valueOf("PROPFIND"), "/api/products"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void rejectsCredentialedRequestsFromDisallowedOrigin() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/members/tokens/refresh")
                .header(HttpHeaders.ORIGIN, "https://attacker.example"))
            .andExpect(status().isForbidden());
        mockMvc.perform(request(HttpMethod.POST, "/api/members/logout")
                .header(HttpHeaders.ORIGIN, "https://attacker.example"))
            .andExpect(status().isForbidden());
    }

    @Test
    void allowedOriginReachesRefreshTokenValidation() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/members/tokens/refresh")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
            .andExpect(status().isUnauthorized());
    }
}

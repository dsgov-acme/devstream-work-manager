package io.nuvalence.workmanager.service.usermanagementapi;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import io.nuvalence.workmanager.service.usermanagementapi.models.ApplicationRoles;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Client to interface with User Management API.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserManagementClient implements UserManagementService {

    @Value("${userManagement.baseUrl}")
    private String baseUrl;

    private final RestTemplate httpClient;

    @Override
    public Optional<User> getUser(UUID userId) {
        final String url = String.format("%s/api/v1/users/%s", baseUrl, userId.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> payload = new HttpEntity<>(headers);
        ResponseEntity<User> response = null;
        try {
            response = httpClient.exchange(url, HttpMethod.GET, payload, User.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to get user from user management service", e);
            if (NOT_FOUND == e.getStatusCode()) {
                return Optional.empty();
            }
            throw e;
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ApiException("Failed to upload roles: " + response.getStatusCode());
        }
        return Optional.ofNullable(response.getBody());
    }

    @Override
    public void publishRoles(ApplicationRoles roles) throws IOException {
        String rolesRequest = new ObjectMapper().writeValueAsString(roles);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> payload = new HttpEntity<String>(rolesRequest, headers);
        final String url = String.format("%s/api/v1/application/roles", baseUrl);

        ResponseEntity<?> response =
                httpClient.exchange(url, HttpMethod.PUT, payload, Object.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ApiException("Failed to upload roles: " + response.getStatusCode());
        }
    }
}

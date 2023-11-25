package io.nuvalence.workmanager.service.documentmanagementapi;

import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import io.nuvalence.workmanager.service.documentmanagementapi.models.DocumentProcessingRequest;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Client to interface with User Management API.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentManagementClient {

    @Value("${documentManagement.baseUrl}")
    private String baseUrl;

    private final RestTemplate httpClient;

    private String initiateDocumentProcessingApiExceptionMessage =
            "Failed to initiate document processing for documentId %s, error code: %s";

    /**
     * Creates a new request the document management endpoint to initiate a document processing.
     *
     * @param documentId ID of the document to process
     * @param documentProcessorsIds IDs of the processors to initiate in document management
     *
     * @throws ApiException if the request fails
     */
    public void initiateDocumentProcessing(String documentId, List<String> documentProcessorsIds) {
        List<DocumentProcessingRequest> documentProcessingRequests =
                documentProcessorsIds.stream()
                        .map(id -> DocumentProcessingRequest.builder().processorId(id).build())
                        .collect(Collectors.toList());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<List<DocumentProcessingRequest>> payload =
                new HttpEntity<>(documentProcessingRequests, headers);
        final String url = String.format("%s/api/v1/documents/%s/process", baseUrl, documentId);

        try {
            ResponseEntity<?> response =
                    httpClient.exchange(url, HttpMethod.POST, payload, Object.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ApiException(
                        String.format(
                                initiateDocumentProcessingApiExceptionMessage,
                                documentId,
                                response.getStatusCode()));
            }
        } catch (HttpClientErrorException e) {
            log.warn("HttpClientErrorException: ", e);
            throw new ApiException(
                    String.format(
                            initiateDocumentProcessingApiExceptionMessage,
                            documentId,
                            e.getRawStatusCode()));
        }
    }
}

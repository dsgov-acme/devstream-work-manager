package io.nuvalence.workmanager.service.documentmanagementapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.nuvalence.workmanager.service.config.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "DOCUMENT_MANAGEMENT_BASE_URL=http://dsgov-document-management/dm")
class DocumentManagementClientTest {

    @Mock private RestTemplate restTemplate;

    private DocumentManagementClient documentManagementClient;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        documentManagementClient = new DocumentManagementClient(restTemplate);
    }

    @Test
    void testInitiateDocumentProcessing_WithSuccessfulResponse() {
        // Arrange
        String documentId = "123";
        List<String> documentProcessorsIds = Collections.singletonList("processor1");
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        documentManagementClient.initiateDocumentProcessing(documentId, documentProcessorsIds);

        // Assert
        verify(restTemplate, times(1))
                .exchange(
                        anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testInitiateDocumentProcessing_WithErrorResponse() {
        // Arrange
        String documentId = "123";
        List<String> documentProcessorsIds = Collections.singletonList("processor1");
        when(restTemplate.exchange(
                        anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Assert
        assertThrows(
                ApiException.class,
                () ->
                        documentManagementClient.initiateDocumentProcessing(
                                documentId, documentProcessorsIds));
    }
}

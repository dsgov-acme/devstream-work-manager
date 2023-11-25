package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.documentmanagementapi.DocumentManagementClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Manages the calls to the document management app API.
 */
@Component
@RequiredArgsConstructor
public class DocumentManagementService {
    private final DocumentManagementClient client;

    public void initiateDocumentProcessing(UUID documentId, List<String> processorsNames) {
        client.initiateDocumentProcessing(documentId.toString(), processorsNames);
    }
}

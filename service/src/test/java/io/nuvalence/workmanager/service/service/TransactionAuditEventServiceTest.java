package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.auditservice.client.ApiException;
import io.nuvalence.workmanager.auditservice.client.generated.api.AuditEventsApi;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventId;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventRequest;
import io.nuvalence.workmanager.service.audit.AuditServiceApiClient;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionAuditEventServiceTest {

    @Mock private AuditServiceApiClient auditServiceTokenApiClient;

    @Mock private RequestContextTimestamp requestContextTimestamp;

    private TransactionAuditEventService transactionAuditEventService;
    private String oldCorrelationId;

    @BeforeEach
    void setup() {
        transactionAuditEventService =
                Mockito.spy(
                        new TransactionAuditEventService(
                                auditServiceTokenApiClient, requestContextTimestamp));

        // Store the old correlation ID
        oldCorrelationId = CorrelationIdContext.getCorrelationId();
    }

    @AfterEach
    public void tearDown() {
        // Restore the old correlation ID after the test
        CorrelationIdContext.setCorrelationId(oldCorrelationId);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void testPostStateChangeEvent() throws ApiException {
        String originatorId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String summary = "summary";
        UUID businessObjectId = UUID.randomUUID();
        AuditEventBusinessObject auditEventBusinessObject = AuditEventBusinessObject.TRANSACTION;
        String oldState = "oldState";
        String newState = "newState";

        AuditEventsApi auditEventsApi = mock(AuditEventsApi.class);
        ArgumentCaptor<AuditEventRequest> auditEventRequestCaptor =
                ArgumentCaptor.forClass(AuditEventRequest.class);

        doReturn(auditEventsApi).when(transactionAuditEventService).createAuditEventsApi(any());

        AuditEventId expected = new AuditEventId();
        expected.setEventId(UUID.randomUUID());
        when(auditEventsApi.postEvent(anyString(), any(), any())).thenReturn(expected);

        OffsetDateTime eventTime = OffsetDateTime.now();
        when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(eventTime);

        String correlationId = UUID.randomUUID().toString();
        CorrelationIdContext.setCorrelationId(correlationId);

        AuditEventId result =
                transactionAuditEventService.postStateChangeEvent(
                        originatorId,
                        userId,
                        summary,
                        businessObjectId,
                        auditEventBusinessObject,
                        oldState,
                        newState,
                        "someActivityType");

        verify(auditEventsApi).postEvent(anyString(), any(), auditEventRequestCaptor.capture());
        AuditEventRequest capturedAuditEventRequest = auditEventRequestCaptor.getValue();

        UUID capturedTraceId = capturedAuditEventRequest.getRequestContext().getTraceId();
        assertNotNull(capturedTraceId);
        assertEquals(correlationId, capturedTraceId.toString());
        assertEquals(eventTime, capturedAuditEventRequest.getTimestamp());
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void testPostActivityAuditEvent() throws ApiException {
        String originatorId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String summary = "summary";
        UUID businessObjectId = UUID.randomUUID();
        AuditEventBusinessObject auditEventBusinessObject = AuditEventBusinessObject.TRANSACTION;
        String jsonData = "{}";
        AuditActivityType activityType = AuditActivityType.NOTE_ADDED;

        AuditEventsApi auditEventsApi = mock(AuditEventsApi.class);
        ArgumentCaptor<AuditEventRequest> auditEventRequestCaptor =
                ArgumentCaptor.forClass(AuditEventRequest.class);

        doReturn(auditEventsApi).when(transactionAuditEventService).createAuditEventsApi(any());

        AuditEventId expected = new AuditEventId();
        expected.setEventId(UUID.randomUUID());
        when(auditEventsApi.postEvent(anyString(), any(), any())).thenReturn(expected);
        OffsetDateTime eventTime = OffsetDateTime.now();
        when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(eventTime);
        String correlationId = UUID.randomUUID().toString();
        CorrelationIdContext.setCorrelationId(correlationId);

        AuditEventId result =
                transactionAuditEventService.postActivityAuditEvent(
                        originatorId,
                        userId,
                        summary,
                        businessObjectId,
                        auditEventBusinessObject,
                        jsonData,
                        activityType);

        verify(auditEventsApi).postEvent(anyString(), any(), auditEventRequestCaptor.capture());
        AuditEventRequest capturedAuditEventRequest = auditEventRequestCaptor.getValue();
        UUID capturedTraceId = capturedAuditEventRequest.getRequestContext().getTraceId();
        assertNotNull(capturedTraceId);
        assertEquals(correlationId, capturedTraceId.toString());
        assertEquals(eventTime, capturedAuditEventRequest.getTimestamp());
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void testCreateAuditEventsApi() {
        AuditEventsApi auditEventsApi = new AuditEventsApi();
        AuditEventsApi createdApi =
                transactionAuditEventService.createAuditEventsApi(auditServiceTokenApiClient);
        Assertions.assertNotNull(createdApi, "AuditEventsApi should not be null");
        Assertions.assertEquals(
                auditEventsApi.getClass(), createdApi.getClass(), "ApiClients should be equal");
    }

    @Test
    void testPostStateChangeEvent2() throws ApiException, JsonProcessingException {
        // Arrange
        Map<String, String> oldStateMap = new HashMap<>();
        oldStateMap.put("key1", "value1");
        oldStateMap.put("key2", "value2");
        Map<String, String> newStateMap = new HashMap<>();
        newStateMap.put("key3", "value3");
        newStateMap.put("key4", "value4");

        AuditEventsApi auditEventsApi = mock(AuditEventsApi.class);
        final ArgumentCaptor<AuditEventRequest> auditEventRequestCaptor =
                ArgumentCaptor.forClass(AuditEventRequest.class);

        doReturn(auditEventsApi).when(transactionAuditEventService).createAuditEventsApi(any());

        AuditEventId expectedAuditEventId = new AuditEventId();
        UUID expectedEventId = UUID.randomUUID();
        expectedAuditEventId.setEventId(expectedEventId);
        when(auditEventsApi.postEvent(any(), any(), any())).thenReturn(expectedAuditEventId);

        String originatorId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String summary = "summary";
        UUID businessObjectId = UUID.randomUUID();
        AuditEventBusinessObject auditEventBusinessObject = AuditEventBusinessObject.TRANSACTION;

        String correlationId = UUID.randomUUID().toString();
        CorrelationIdContext.setCorrelationId(correlationId);

        // Act
        final AuditEventId result =
                transactionAuditEventService.postStateChangeEvent(
                        originatorId,
                        userId,
                        summary,
                        businessObjectId,
                        auditEventBusinessObject,
                        oldStateMap,
                        newStateMap,
                        "test",
                        "someActivityType");

        verify(auditEventsApi).postEvent(anyString(), any(), auditEventRequestCaptor.capture());
        AuditEventRequest capturedAuditEventRequest = auditEventRequestCaptor.getValue();

        UUID capturedTraceId = capturedAuditEventRequest.getRequestContext().getTraceId();
        assertNotNull(capturedTraceId);
        assertEquals(correlationId, capturedTraceId.toString());
        assertEquals(
                "test",
                capturedAuditEventRequest.getEventData().getStateChangeEventData().getData());

        // Assert
        assertNotNull(result);
        assertEquals(expectedEventId, result.getEventId());
        verify(auditEventsApi).postEvent(any(), any(), any());
    }
}

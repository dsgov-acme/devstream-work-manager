package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.nuvalence.workmanager.auditservice.client.ApiException;
import io.nuvalence.workmanager.service.audit.transaction.StatusChangedAuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.TransactionAuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class StatusChangedAuditHandlerTest {

    @Mock private TransactionAuditEventService transactionAuditEventService;

    @InjectMocks private StatusChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_statusChanged() throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setStatus("Review");
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(originatorId),
                        eq(originatorId),
                        anyString(),
                        eq(transaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        eq("Draft"),
                        eq("Review"),
                        eq(AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue()));
    }

    @Test
    void testPublishAuditEvent_statusDidNotChange() throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService, never())
                .postStateChangeEvent(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(String.class),
                        any(String.class),
                        eq(AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue()));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testPublishAuditEvent_Api_Exception(CapturedOutput output) throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setStatus("Review");
        auditHandler.handlePostUpdateState(transaction);

        OffsetDateTime eventTime = OffsetDateTime.now();
        String originatorId = "originatorId";

        doThrow(ApiException.class)
                .when(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(originatorId),
                        eq(originatorId),
                        anyString(),
                        eq(transaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        eq("Draft"),
                        eq("Review"),
                        eq(AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue()));

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "ApiException occurred when recording audit event for status change"
                                        + " in transaction "
                                        + transaction.getId()));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testPublishAuditEventException(CapturedOutput output) throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .status("Draft")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setStatus("Review");
        auditHandler.handlePostUpdateState(transaction);

        OffsetDateTime eventTime = OffsetDateTime.now();
        String originatorId = "originatorId";

        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(originatorId),
                        eq(originatorId),
                        anyString(),
                        eq(transaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        eq("Draft"),
                        eq("Review"),
                        eq(AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue()));

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " status change in transaction "
                                        + transaction.getId()));
    }
}

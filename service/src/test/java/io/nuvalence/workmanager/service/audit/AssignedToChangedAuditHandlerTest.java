package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.nuvalence.workmanager.auditservice.client.ApiException;
import io.nuvalence.workmanager.service.audit.transaction.AssignedToChangedAuditHandler;
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

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AssignedToChangedAuditHandlerTest {

    @Mock private TransactionAuditEventService transactionAuditEventService;
    @InjectMocks private AssignedToChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_UserAssigned() throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);
        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);
        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        originatorId,
                        originatorId,
                        "User [user2] was assigned transaction externalId. Previously it had been"
                                + " assigned to [user1]",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        "user1",
                        "user2",
                        AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue());
    }

    @Test
    void testPublishAuditEvent_UserAssigned_beforenull() throws ApiException {
        Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).externalId("externalId").build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        originatorId,
                        originatorId,
                        "User [user2] was assigned transaction externalId",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        null,
                        "user2",
                        AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue());
    }

    @Test
    void testPublishAuditEvent_UserAssigned_afternull() throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo(null);
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        originatorId,
                        originatorId,
                        "User [user1] was unassigned from transaction externalId",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        "user1",
                        null,
                        AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue());
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testPublishAuditEvent_Throws_Api_Exception(CapturedOutput output) throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();
        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        doThrow(ApiException.class)
                .when(transactionAuditEventService)
                .postStateChangeEvent(
                        originatorId,
                        originatorId,
                        "User [user2] was assigned transaction externalId. Previously it had been"
                                + " assigned to [user1]",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        "user1",
                        "user2",
                        AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue());

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "ApiException occurred when recording audit event for assigned to"
                                        + " change in transaction "
                                        + transaction.getId()));
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testPublishAuditEvent_Exception_path(CapturedOutput output) throws ApiException {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .assignedTo("user1")
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setAssignedTo("user2");
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(originatorId),
                        eq(originatorId),
                        anyString(),
                        eq(transaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        eq("user1"),
                        eq("user2"),
                        eq(AuditActivityType.TRANSACTION_ASSIGNED_TO_CHANGED.getValue()));

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " assigned to change in transaction "
                                        + transaction.getId()));
    }
}

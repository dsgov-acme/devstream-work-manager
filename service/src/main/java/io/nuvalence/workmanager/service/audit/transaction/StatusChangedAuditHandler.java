package io.nuvalence.workmanager.service.audit.transaction;

import io.nuvalence.workmanager.auditservice.client.ApiException;
import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.TransactionAuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * AuditHandler that records an audit event if the status of a Transaction has changed.
 */
@Slf4j
@RequiredArgsConstructor
public class StatusChangedAuditHandler implements AuditHandler<Transaction> {
    private String transactionExternalId;
    private String before;
    private String after;

    private UUID transactionId;

    private final TransactionAuditEventService transactionAuditEventService;

    @Override
    public void handlePreUpdateState(Transaction subject) {
        transactionId = subject.getId();
        transactionExternalId = subject.getExternalId();
        before = subject.getStatus();
    }

    @Override
    public void handlePostUpdateState(Transaction subject) {
        after = subject.getStatus();
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary;
            if (before != null && !before.equals(after)) {
                eventSummary =
                        String.format(
                                "Transaction %s changed its status to [%s]. Previously it"
                                        + " was [%s]",
                                transactionExternalId, after, before);
            } else {
                return;
            }

            transactionAuditEventService.postStateChangeEvent(
                    originatorId,
                    originatorId,
                    eventSummary,
                    transactionId,
                    AuditEventBusinessObject.TRANSACTION,
                    before,
                    after,
                    AuditActivityType.TRANSACTION_STATUS_CHANGED.getValue());
        } catch (ApiException e) {
            String errorMessage =
                    "ApiException occurred when recording audit event for status change in"
                            + " transaction "
                            + transactionId;
            log.error(errorMessage, e);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for status"
                            + " change in transaction "
                            + transactionId;
            log.error(errorMessage, e);
        }
    }
}

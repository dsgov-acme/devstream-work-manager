package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The filters to filter the transactions by.
 */
@Getter
@Setter
public abstract class TransactionFilters extends BaseFilters {
    private List<String> transactionDefinitionKeys;
    private String category;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private List<TransactionPriority> priority;
    private List<String> status;
    private List<String> assignedTo;
    private Boolean assignedToMe;
    private String subjectUserId;
    private String externalId;
    private String createdBy;

    protected TransactionFilters(
            String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
    }

    public abstract Specification<Transaction> getTransactionSpecifications();
}

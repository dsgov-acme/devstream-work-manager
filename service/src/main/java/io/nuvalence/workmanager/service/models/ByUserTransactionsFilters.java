package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

/**
 * The filters to filter the transactions by.
 */
@Getter
@Setter
public class ByUserTransactionsFilters extends TransactionFilters {

    /**
     * Builder for TransactionFilters.
     *
     * @param subjectUserId            The subjectUserId to filter transactions by
     * @param createdBy                The createdBy user id to filter transactions by
     * @param sortBy                   The column to filter transactions by
     * @param sortOrder                The order to filter transactions by
     * @param pageNumber                     The number of the pages to get transactions
     * @param pageSize                     The number of transactions per pageNumber
     */
    @Builder
    public ByUserTransactionsFilters(
            String subjectUserId,
            String createdBy,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.setSubjectUserId(subjectUserId);
        this.setCreatedBy(createdBy);
    }

    @Override
    public Specification<Transaction> getTransactionSpecifications() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("subjectUserId"), getSubjectUserId()));

            predicates.add(criteriaBuilder.equal(root.get("createdBy"), getCreatedBy()));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}

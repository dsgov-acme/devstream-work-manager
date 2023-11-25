package io.nuvalence.workmanager.service.models;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/**
 * The filters to filter the transactions by.
 */
@Getter
@Setter
public class SearchTransactionsFilters extends TransactionFilters {

    /**
     * Builder for TransactionFilters.
     *
     * @param transactionDefinitionKeys The transaction definition keys to filter transactions by
     * @param category                  The transaction category
     * @param startDate                 The start date to filter transactions by
     * @param endDate                   The end date to filter transactions by
     * @param priority                  The priority to filter transactions by
     * @param status                    The status to filter transactions by
     * @param assignedTo                The assigned user to filter transactions by
     * @param assignedToMe              Filter transactions assigned to yourself
     * @param subjectUserId             The subjectUserId to filter transactions by
     * @param externalId                The externalId to filter transactions by
     * @param createdBy                 The createdBy user id to filter transactions by
     * @param sortBy                    The column to filter transactions by
     * @param sortOrder                 The order to filter transactions by
     * @param pageNumber                The number of the pages to get transactions
     * @param pageSize                  The number of transactions per pageNumber
     */
    @Builder
    public SearchTransactionsFilters(
            List<String> transactionDefinitionKeys,
            String category,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<TransactionPriority> priority,
            List<String> status,
            List<String> assignedTo,
            Boolean assignedToMe,
            String subjectUserId,
            String externalId,
            String createdBy,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.setTransactionDefinitionKeys(transactionDefinitionKeys);
        this.setCategory(category);
        this.setStartDate(startDate);
        this.setEndDate(endDate);
        this.setPriority(priority);
        this.setStatus(status);
        this.setAssignedTo(assignedTo);
        this.setAssignedToMe(assignedToMe);
        this.setSubjectUserId(subjectUserId);
        this.setExternalId(externalId);
        this.setCreatedBy(createdBy);
    }

    @Override
    public Specification<Transaction> getTransactionSpecifications() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<TransactionDefinition, Transaction> transactionDefinitionJoin =
                    root.join("transactionDefinition");

            addInPredicateWithEmptySupport(
                    predicates,
                    root.get("transactionDefinitionKey"),
                    getTransactionDefinitionKeys());

            addLikePredicate(
                    predicates,
                    criteriaBuilder,
                    transactionDefinitionJoin.get("category"),
                    getCategory());

            addGreaterThanOrEqualToPredicate(
                    predicates, criteriaBuilder, root.get("createdTimestamp"), getStartDate());

            addLessThanOrEqualToPredicate(
                    predicates, criteriaBuilder, root.get("createdTimestamp"), getEndDate());

            addInPredicate(predicates, root.get("priority"), getPriority());
            addInPredicate(predicates, root.get("status"), getStatus());
            addInPredicate(predicates, root.get("assignedTo"), getAssignedTo());

            addEqualPredicate(
                    predicates, criteriaBuilder, root.get("subjectUserId"), getSubjectUserId());

            addEqualIgnoreCasePredicate(
                    predicates, criteriaBuilder, root.get("externalId"), getExternalId());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addEqualPredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> attributeExpression,
            Object attributeValue) {
        if (attributeValue != null) {
            predicates.add(criteriaBuilder.equal(attributeExpression, attributeValue));
        }
    }

    private void addLikePredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> attributeExpression,
            String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            predicates.add(criteriaBuilder.like(attributeExpression, attributeValue + "%"));
        }
    }

    private void addGreaterThanOrEqualToPredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<OffsetDateTime> attributeExpression,
            OffsetDateTime attributeValue) {
        if (attributeValue != null) {
            predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(attributeExpression, attributeValue));
        }
    }

    private void addLessThanOrEqualToPredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<OffsetDateTime> attributeExpression,
            OffsetDateTime attributeValue) {
        if (attributeValue != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(attributeExpression, attributeValue));
        }
    }

    private <T> void addInPredicateWithEmptySupport(
            List<Predicate> predicates,
            Expression<T> attributeExpression,
            List<T> attributeValues) {
        if (attributeValues != null) {
            predicates.add(attributeExpression.in(attributeValues));
        }
    }

    private <T> void addInPredicate(
            List<Predicate> predicates,
            Expression<T> attributeExpression,
            List<T> attributeValues) {
        if (attributeValues != null && !attributeValues.isEmpty()) {
            predicates.add(attributeExpression.in(attributeValues));
        }
    }

    private void addEqualIgnoreCasePredicate(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Expression<String> attributeExpression,
            String attributeValue) {
        if (StringUtils.isNotBlank(attributeValue)) {
            predicates.add(
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(attributeExpression),
                            attributeValue.toLowerCase(Locale.ENGLISH)));
        }
    }
}

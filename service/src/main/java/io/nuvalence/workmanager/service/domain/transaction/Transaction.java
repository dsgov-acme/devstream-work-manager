package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.TransactionAccessResourceTranslator;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntityContainer;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntityContainerEventListener;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Defines the structure and behavior of a transaction.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "transaction")
@AccessResource(value = "transaction", translator = TransactionAccessResourceTranslator.class)
@ToString(exclude = {"data"})
@EntityListeners({
    DynamicEntityContainerEventListener.class,
    UpdateTrackedEntityEventListener.class
})
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class Transaction implements DynamicEntityContainer, UpdateTrackedEntity {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Setter
    @Column(name = "transaction_definition_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID transactionDefinitionId;

    @Column(name = "transaction_definition_key", length = 255, nullable = false)
    private String transactionDefinitionKey;

    @Setter
    @Column(name = "external_id")
    private String externalId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(
            name = "transaction_definition_id",
            nullable = false,
            insertable = false,
            updatable = false)
    private TransactionDefinition transactionDefinition;

    @Setter
    @Column(name = "process_instance_id", length = 64, nullable = false)
    private String processInstanceId;

    @Setter
    @Column(name = "status", length = 255, nullable = false)
    private String status;

    @Setter
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Setter
    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "subject_user_id", length = 64, nullable = false)
    private String subjectUserId;

    @Setter
    @Column(name = "priority")
    @Convert(converter = TransactionPriorityConverter.class)
    private TransactionPriority priority;

    @Setter
    @Column(name = "district", length = 255)
    private String district;

    @Setter
    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Setter
    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @Setter
    @Column(name = "submitted_on")
    private OffsetDateTime submittedOn;

    @Setter
    @Column(name = "assigned_to", length = 64)
    private String assignedTo;

    @Setter
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id")
    @Fetch(FetchMode.SUBSELECT)
    private List<CustomerProvidedDocument> customerProvidedDocuments = new ArrayList<>();

    @Setter @Embedded private DynamicEntity data;

    /**
     * Constructs a new instance of a Transaction.
     *
     * @param id                       Transaction ID
     * @param transactionDefinitionId  ID for this transactions definition
     * @param transactionDefinitionKey Key for this transactions definition
     * @param processInstanceId ID for Camunda process instance
     * @param status Transaction status
     * @param createdBy User that created the transaction
     * @param lastUpdatedBy User that last updated the transaction
     * @param subjectUserId User that is the subject of the transaction
     * @param priority Transaction priority
     * @param district Transaction district
     * @param createdTimestamp Timestamp of when transaction was created
     * @param lastUpdatedTimestamp Timestamp of when transaction was last updated
     * @param submittedOn Timestamp of when transaction was submitted
     * @param assignedTo User responsible for the transaction
     * @param externalId Secondary ID for transactions, reader friendly and ZBase32 encoded
     * @param data Dynamic Entity Data
     * @param customerProvidedDocuments Customer Provided Documents
     * @param transactionDefinition Transaction specifications
     */
    @Builder(toBuilder = true)
    public Transaction(
            UUID id,
            UUID transactionDefinitionId,
            String transactionDefinitionKey,
            String processInstanceId,
            String status,
            String createdBy,
            String lastUpdatedBy,
            String subjectUserId,
            TransactionPriority priority,
            String district,
            OffsetDateTime createdTimestamp,
            OffsetDateTime lastUpdatedTimestamp,
            OffsetDateTime submittedOn,
            String assignedTo,
            String externalId,
            DynamicEntity data,
            List<CustomerProvidedDocument> customerProvidedDocuments,
            TransactionDefinition transactionDefinition) {
        this.id = id;
        this.transactionDefinitionId = transactionDefinitionId;
        this.transactionDefinitionKey = transactionDefinitionKey;
        this.processInstanceId = processInstanceId;
        this.status = status;
        this.createdBy = createdBy;
        this.subjectUserId = subjectUserId;
        this.priority = priority;
        this.district = district;
        this.createdTimestamp = createdTimestamp;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        this.submittedOn = submittedOn;
        this.assignedTo = assignedTo;
        this.externalId = externalId;
        this.data = data;
        this.customerProvidedDocuments = customerProvidedDocuments;
        this.transactionDefinition = transactionDefinition;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Transaction that = (Transaction) o;
        return Objects.equals(transactionDefinitionId, that.transactionDefinitionId)
                && Objects.equals(transactionDefinitionKey, that.transactionDefinitionKey)
                && Objects.equals(processInstanceId, that.processInstanceId)
                && Objects.equals(status, that.status)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equals(subjectUserId, that.subjectUserId)
                && Objects.equals(priority, that.priority)
                && Objects.equals(district, that.district)
                && Objects.equals(createdTimestamp, that.createdTimestamp)
                && Objects.equals(lastUpdatedTimestamp, that.lastUpdatedTimestamp)
                && Objects.equals(submittedOn, that.submittedOn)
                && Objects.equals(assignedTo, that.assignedTo)
                && Objects.equals(transactionDefinition, that.transactionDefinition)
                && Objects.equals(externalId, that.externalId)
                && Objects.equals(data, that.data)
                && Objects.equals(customerProvidedDocuments, that.customerProvidedDocuments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                transactionDefinitionId,
                transactionDefinitionKey,
                processInstanceId,
                status,
                createdBy,
                lastUpdatedBy,
                subjectUserId,
                priority,
                district,
                createdTimestamp,
                assignedTo,
                lastUpdatedTimestamp,
                submittedOn,
                transactionDefinition,
                externalId,
                data,
                customerProvidedDocuments);
    }

    @PrePersist
    @PreUpdate
    public void transactionPrePersistAndUpdate() {
        this.externalId = this.getExternalId().toUpperCase(Locale.ROOT);
    }
}

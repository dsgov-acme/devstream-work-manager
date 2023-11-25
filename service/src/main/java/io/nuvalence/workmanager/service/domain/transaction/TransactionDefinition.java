package io.nuvalence.workmanager.service.domain.transaction;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Defines the structure and behavior of a transaction type.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@AccessResource("transaction_definition")
@Table(name = "transaction_definition")
@EntityListeners(UpdateTrackedEntityEventListener.class)
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class TransactionDefinition extends VersionedEntity implements UpdateTrackedEntity {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "transaction_definition_key", length = 255, nullable = false)
    private String key;

    @Column(name = "name", length = 1024, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 1024, nullable = true)
    private String category;

    @Column(name = "process_definition_key", length = 255, nullable = false)
    private String processDefinitionKey;

    @Column(name = "schema_key", length = 1024, nullable = false)
    private String schemaKey;

    @Column(name = "default_status", length = 255, nullable = false)
    private String defaultStatus;

    @Column(name = "default_form_configuration_key", length = 1024, nullable = false)
    private String defaultFormConfigurationKey;

    @Column(name = "transaction_definition_set_key", length = 255)
    private String transactionDefinitionSetKey;

    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    private String lastUpdatedBy;

    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp", nullable = false)
    private OffsetDateTime lastUpdatedTimestamp;

    @OneToMany(
            orphanRemoval = true,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_definition_id", referencedColumnName = "id")
    @OrderColumn(name = "index")
    private List<FormConfigurationSelectionRule> formConfigurationSelectionRules;

    /**
     * Returns the configures form configuration key for a given task, viewer and context.
     *
     * @param task task name the form applies to
     * @param viewer view type the form applies to
     * @param context context the form applies to
     * @return form configuration key if one can be selected
     */
    public Optional<String> getFormConfigurationKey(
            final String task, final String viewer, final String context) {
        for (FormConfigurationSelectionRule rule : formConfigurationSelectionRules) {
            if (rule.matches(task, viewer, context)) {
                return Optional.of(rule.getFormConfigurationKey());
            }
        }

        return Optional.ofNullable(defaultFormConfigurationKey);
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }

        TransactionDefinition that = (TransactionDefinition) o;
        return Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(key, that.key)
                && Objects.equals(processDefinitionKey, that.processDefinitionKey)
                && Objects.equals(schemaKey, that.schemaKey)
                && Objects.equals(defaultStatus, that.defaultStatus)
                && Objects.equals(category, that.category)
                && Objects.equals(transactionDefinitionSetKey, that.transactionDefinitionSetKey)
                && Objects.equals(defaultFormConfigurationKey, that.defaultFormConfigurationKey)
                && Objects.equals(
                        formConfigurationSelectionRules, that.formConfigurationSelectionRules)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equals(createdTimestamp, that.createdTimestamp)
                && Objects.equals(lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                description,
                key,
                processDefinitionKey,
                schemaKey,
                defaultStatus,
                category,
                transactionDefinitionSetKey,
                defaultFormConfigurationKey,
                formConfigurationSelectionRules,
                createdBy,
                lastUpdatedBy,
                createdTimestamp,
                lastUpdatedTimestamp);
    }
}

package io.nuvalence.workmanager.service.domain.formconfig;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntity;
import io.nuvalence.workmanager.service.domain.UpdateTrackedEntityEventListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Defines the structure and behavior of a form config type.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@AccessResource("form_configuration")
@Table(name = "form_configuration")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class FormConfiguration implements UpdateTrackedEntity {
    @Id
    @Column(name = "id", length = 36, insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "transaction_definition_key", length = 1024, nullable = false)
    private String transactionDefinitionKey;

    @Column(name = "form_configuration_key", length = 1024, nullable = false)
    private String key;

    @Column(name = "name", length = 1024, nullable = false)
    private String name;

    @Column(name = "schema_key", length = 1024, nullable = false)
    private String schemaKey;

    @Column(name = "config_schema", length = 1024, nullable = false)
    private String configurationSchema;

    @Column(name = "description", length = 1024)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_config", nullable = false, columnDefinition = "json")
    private Map<String, Object> configuration;

    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;

    @Column(name = "last_updated_by", length = 36)
    private String lastUpdatedBy;

    @Column(
            name = "created_timestamp",
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            updatable = false)
    private OffsetDateTime createdTimestamp;

    @Column(
            name = "last_updated_timestamp",
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime lastUpdatedTimestamp;

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FormConfiguration that = (FormConfiguration) o;
        return Objects.equals(transactionDefinitionKey, that.transactionDefinitionKey)
                && Objects.equals(key, that.key)
                && Objects.equals(name, that.name)
                && Objects.equals(schemaKey, that.schemaKey)
                && Objects.equals(configurationSchema, that.configurationSchema)
                && Objects.equals(configuration, that.configuration)
                && Objects.equals(description, that.description)
                && Objects.equals(createdBy, that.createdBy)
                && Objects.equals(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equals(createdTimestamp, that.createdTimestamp)
                && Objects.equals(lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                transactionDefinitionKey,
                key,
                name,
                schemaKey,
                configurationSchema,
                configuration,
                description,
                createdBy,
                lastUpdatedBy,
                createdTimestamp,
                lastUpdatedTimestamp);
    }
}

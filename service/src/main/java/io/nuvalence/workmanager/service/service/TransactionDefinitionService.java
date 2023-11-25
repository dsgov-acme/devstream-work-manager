package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetDataRequirement;
import io.nuvalence.workmanager.service.mapper.InvalidRegexPatternException;
import io.nuvalence.workmanager.service.models.TransactionDefinitionFilters;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaProperty;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

/**
 * Service layer to manage transaction definitions.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class TransactionDefinitionService {
    private final TransactionDefinitionRepository repository;
    private final TransactionDefinitionSetService transactionDefinitionSetService;
    private final SchemaService schemaService;

    /**
     * Fetches a transaction definition from the database by id (primary key).
     *
     * @param id transaction definition id to fetch
     * @return transaction definition object
     */
    public Optional<TransactionDefinition> getTransactionDefinitionById(final UUID id) {
        return repository.findById(id);
    }

    /**
     * Fetches the latest version of a transaction definition from the database by key.
     *
     * @param key transaction definition key to fetch
     * @return transaction definition object
     */
    public Optional<TransactionDefinition> getTransactionDefinitionByKey(final String key) {
        // TODO When we implement versioned transaction configuration, this will need to select for
        // the newest version
        return repository.searchByKey(key).stream().findFirst();
    }

    /**
     * Returns a list of transaction definitions whose names match the query passed in.
     *
     * @param name Partial name query
     * @return List of transaction definitions matching query
     */
    public List<TransactionDefinition> getTransactionDefinitionsByPartialNameMatch(
            final String name) {
        if (name == null) {
            return repository.getAllDefinitions();
        } else {
            return repository.searchByPartialName(name);
        }
    }

    /**
     * Returns a list of transaction definitions(paged) whose names match the query passed in.
     *
     * @param filter filters
     * @return List of transaction definitions matching query (paged)
     */
    public Page<TransactionDefinition> getTransactionDefinitionsByFilters(
            TransactionDefinitionFilters filter) {
        return repository.findAll(
                filter.getTransactionDefinitionSpecifications(), filter.getPageRequest());
    }

    /**
     * Returns a list of transaction definitions whose names match the query passed in.
     *
     * @param category Partial name query
     * @return List of transaction definitions matching query
     */
    public List<TransactionDefinition> getTransactionDefinitionsByPartialCategoryMatch(
            final String category) {
        if (category == null) {
            return repository.getAllDefinitions();
        } else {
            return repository.searchByPartialCategory(category);
        }
    }

    /**
     * Saves a transaction definition.
     *
     * @param transactionDefinition transaction definition to save
     * @return post-save version of transaction definition
     *
     * @throws UnexpectedException if the transaction definition key is invalid
     */
    public TransactionDefinition saveTransactionDefinition(
            final TransactionDefinition transactionDefinition) {
        try {
            if (!VersionedEntity.isValidName(transactionDefinition.getKey())) {
                throw new InvalidRegexPatternException(
                        transactionDefinition.getKey(),
                        VersionedEntity.Constants.VALID_FILE_NAME_REGEX_PATTERN,
                        "transaction definition");
            }
        } catch (InvalidRegexPatternException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e.getMessage(), e);
        }

        return repository.save(transactionDefinition);
    }

    /**
     * Creates an array of transaction definition keys for a given query.
     *
     * @param transactionDefinitionKey transaction definition set key to query
     * @param transactionDefinitionSetKey transaction definition key to query
     * @return List of transaction definition keys matching query
     */
    public List<String> createTransactionDefinitionKeysList(
            String transactionDefinitionKey, String transactionDefinitionSetKey) {

        if (transactionDefinitionSetKey == null) {
            return transactionDefinitionKey != null ? List.of(transactionDefinitionKey) : null;
        }

        List<String> transactionDefinitionKeysList =
                getTransactionDefinitionsBySetKey(transactionDefinitionSetKey).stream()
                        .map(TransactionDefinition::getKey)
                        .collect(Collectors.toList());

        if (transactionDefinitionKey != null
                && !transactionDefinitionKeysList.contains(transactionDefinitionKey)) {
            return Collections.emptyList();
        }

        return transactionDefinitionKeysList;
    }

    public List<TransactionDefinition> getTransactionDefinitionsBySetKey(
            String transactionDefinitionSetKey) {
        return repository.searchByTransactionDefinitionSetKey(transactionDefinitionSetKey);
    }

    /**
     * Validate that the definition schema conforms to the data requirements of the TransactionDefinitionSet.
     *
     * @param transactionDefinition transaction definition whose schema is to be validated
     *
     * @throws BusinessLogicException if the schema does not conform to the data requirements
     */
    public void validateTransactionDefinitionSetLink(TransactionDefinition transactionDefinition) {

        String transactionDefinitionSetKey = transactionDefinition.getTransactionDefinitionSetKey();
        if (transactionDefinitionSetKey != null) {

            Optional<TransactionDefinitionSet> optionalTransactionDefinitionSet =
                    transactionDefinitionSetService.getTransactionDefinitionSet(
                            transactionDefinitionSetKey);
            if (optionalTransactionDefinitionSet.isEmpty()) {
                throw new BusinessLogicException(
                        String.format(
                                "Transaction definition set with key %s does not exist",
                                transactionDefinitionSetKey));
            }

            TransactionDefinitionSet transactionDefinitionSet =
                    optionalTransactionDefinitionSet.get();

            Optional<Schema> optionalSchema =
                    schemaService.getSchemaByKey(transactionDefinition.getSchemaKey());
            if (optionalSchema.isEmpty()) {
                throw new BusinessLogicException(
                        String.format(
                                "Schema with key %s does not exist",
                                transactionDefinition.getSchemaKey()));
            }
            Schema schema = optionalSchema.get();

            validateTransactionDefinitionSetAndSchema(transactionDefinitionSet, schema);
        }
    }

    private void validateTransactionDefinitionSetAndSchema(
            TransactionDefinitionSet set, Schema schema) {
        for (TransactionDefinitionSetDataRequirement constraint : set.getConstraints()) {
            String[] tokens = constraint.getPath().split("\\.");
            DynaProperty currentDynaProperty =
                    identifyInitialDynaProperty(schema.getDynaProperties(), tokens[0]);
            validateTokenPath(tokens, constraint, currentDynaProperty, schema);
        }
    }

    private void validateTokenPath(
            String[] tokens,
            TransactionDefinitionSetDataRequirement constraint,
            DynaProperty currentDynaProperty,
            Schema currentSchema) {
        for (int i = 1; i <= tokens.length; i++) {
            if (currentDynaProperty == null) {
                throw new BusinessLogicException(
                        String.format(
                                "Schema property not found for path %s", constraint.getPath()));
            }

            if (currentDynaProperty.getType().isAssignableFrom(DynamicEntity.class)) {
                currentSchema = moveToRelatedSchema(currentDynaProperty, currentSchema);
                currentDynaProperty =
                        identifyInitialDynaProperty(currentSchema.getDynaProperties(), tokens[i]);
            } else if (currentDynaProperty.getType().isAssignableFrom(List.class)) {
                validateListType(currentDynaProperty, constraint);
            } else {
                validateSimpleType(currentDynaProperty, constraint);
            }
        }
    }

    private Schema moveToRelatedSchema(DynaProperty currentDynaProperty, Schema currentSchema) {
        String schemaKey = currentSchema.getRelatedSchemas().get(currentDynaProperty.getName());
        if (schemaKey == null) {
            throw new BusinessLogicException(
                    String.format(
                            "Related schema not found for property %s",
                            currentDynaProperty.getName()));
        }
        Optional<Schema> optionalNewSchema = schemaService.getSchemaByKey(schemaKey);
        return optionalNewSchema.orElseThrow(
                () ->
                        new BusinessLogicException(
                                String.format(
                                        "Related schema not found for schema key %s", schemaKey)));
    }

    private void validateListType(
            DynaProperty currentDynaProperty, TransactionDefinitionSetDataRequirement constraint) {
        if (!currentDynaProperty.getContentType().getSimpleName().equals(constraint.getType())) {
            throw new BusinessLogicException(
                    String.format(
                            "Schema and data requirement are not compatible for path %s, invalid"
                                    + " list type, constraint expected %s, got %s",
                            constraint.getPath(),
                            constraint.getType(),
                            currentDynaProperty.getContentType().getSimpleName()));
        }
    }

    private void validateSimpleType(
            DynaProperty currentDynaProperty, TransactionDefinitionSetDataRequirement constraint) {
        if (!currentDynaProperty.getType().getSimpleName().equals(constraint.getType())) {
            throw new BusinessLogicException(
                    String.format(
                            "Schema and data requirement are not compatible for path %s, constraint"
                                    + " expected %s, got %s",
                            constraint.getPath(),
                            constraint.getType(),
                            currentDynaProperty.getType().getSimpleName()));
        }
    }

    private DynaProperty identifyInitialDynaProperty(DynaProperty[] properties, String token) {
        for (DynaProperty property : properties) {
            if (property.getName().equals(token)) {
                return property;
            }
        }
        return null;
    }
}

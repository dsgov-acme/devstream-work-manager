package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetDataRequirement;
import io.nuvalence.workmanager.service.mapper.InvalidRegexPatternException;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionRepository;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionDefinitionServiceTest {
    @Mock private TransactionDefinitionRepository repository;
    @Mock private TransactionDefinitionSetService transactionDefinitionSetService;
    @Mock private SchemaService schemaService;

    private TransactionDefinitionService service;

    @BeforeEach
    void setup() {
        service =
                new TransactionDefinitionService(
                        repository, transactionDefinitionSetService, schemaService);
    }

    @Test
    void getTransactionDefinitionByIdTransactionDefinitionWhenFound() {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction")
                        .build();
        when(repository.findById(transactionDefinition.getId()))
                .thenReturn(Optional.of(transactionDefinition));

        // Act and Assert
        assertEquals(
                Optional.of(transactionDefinition),
                service.getTransactionDefinitionById(transactionDefinition.getId()));
    }

    @Test
    void getTransactionDefinitionByIdEmptyOptionalWhenNotFound() {
        // Arrange
        final UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act and Assert
        assertEquals(Optional.empty(), service.getTransactionDefinitionById(id));
    }

    @Test
    void getTransactionDefinitionByKeyTransactionDefinitionWhenFound() {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("key")
                        .name("test-transaction")
                        .build();
        when(repository.searchByKey(transactionDefinition.getKey()))
                .thenReturn(List.of(transactionDefinition));

        // Act and Assert
        assertEquals(
                Optional.of(transactionDefinition),
                service.getTransactionDefinitionByKey(transactionDefinition.getKey()));
    }

    @Test
    void getTransactionDefinitionByKeyEmptyOptionalWhenNotFound() {
        // Arrange
        final String key = "key";
        when(repository.searchByKey(key)).thenReturn(Collections.emptyList());

        // Act and Assert
        assertEquals(Optional.empty(), service.getTransactionDefinitionByKey(key));
    }

    @Test
    void getTransactionDefinitionsByPartialNameMatchReturnsFoundTransactionDefinitions() {
        // Arrange
        final TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-1")
                        .build();
        final TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-2")
                        .build();
        when(repository.searchByPartialName("test"))
                .thenReturn(List.of(transactionDefinition1, transactionDefinition2));

        // Act and Assert
        assertEquals(
                List.of(transactionDefinition1, transactionDefinition2),
                service.getTransactionDefinitionsByPartialNameMatch("test"));
    }

    @Test
    void saveTransactionDefinitionDoesNotThrowExceptionIfSaveSuccessful() {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test-transaction-key")
                        .name("test-transaction")
                        .build();
        Mockito.lenient()
                .when(repository.save(transactionDefinition))
                .thenReturn(transactionDefinition);

        // Act and Assert
        assertDoesNotThrow(() -> service.saveTransactionDefinition(transactionDefinition));
    }

    @Test
    void saveTransactionDefinitionDoesNotSaveIfInvalidKey() {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction")
                        .key("test-inv@lid-key*")
                        .category("category")
                        .build();

        Exception exception =
                assertThrows(
                        RuntimeException.class,
                        () -> service.saveTransactionDefinition(transactionDefinition));
        Exception expectedException =
                new InvalidRegexPatternException(
                        transactionDefinition.getKey(),
                        VersionedEntity.Constants.VALID_FILE_NAME_REGEX_PATTERN,
                        "transaction definition");
        assertTrue(exception.getMessage().contains(expectedException.getMessage()));
    }

    @Test
    void testGetTransactionDefinitionsByPartialCategoryMatch_WithNullCategory() {

        final TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-1")
                        .build();
        final TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-2")
                        .build();

        // Arrange
        List<TransactionDefinition> allDefinitions =
                Arrays.asList(transactionDefinition1, transactionDefinition2);

        when(repository.getAllDefinitions()).thenReturn(allDefinitions);

        // Act
        List<TransactionDefinition> result =
                service.getTransactionDefinitionsByPartialCategoryMatch(null);

        // Assert
        assertEquals(allDefinitions, result);
        verify(repository, times(1)).getAllDefinitions();
        verify(repository, never()).searchByPartialCategory(anyString());
    }

    @Test
    void testGetTransactionDefinitionsByPartialCategoryMatch_WithNonNullCategory() {
        // Arrange
        String category = "Category";
        final TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-1")
                        .build();
        final TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-2")
                        .build();

        // Arrange
        List<TransactionDefinition> matchedDefinitions =
                Arrays.asList(transactionDefinition1, transactionDefinition2);

        when(repository.searchByPartialCategory(category)).thenReturn(matchedDefinitions);

        // Act
        List<TransactionDefinition> result =
                service.getTransactionDefinitionsByPartialCategoryMatch(category);

        // Assert
        assertEquals(matchedDefinitions, result);
        verify(repository, never()).getAllDefinitions();
        verify(repository, times(1)).searchByPartialCategory(category);
    }

    @Test
    void testGetTransactionDefinitionsByPartialNameMatch_WithNullName() {
        // Arrange
        final TransactionDefinition transactionDefinition1 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-1")
                        .build();
        final TransactionDefinition transactionDefinition2 =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .name("test-transaction-2")
                        .build();

        // Arrange
        List<TransactionDefinition> allDefinitions =
                Arrays.asList(transactionDefinition1, transactionDefinition2);

        when(repository.getAllDefinitions()).thenReturn(allDefinitions);

        // Act
        List<TransactionDefinition> result =
                service.getTransactionDefinitionsByPartialNameMatch(null);

        // Assert
        assertEquals(allDefinitions, result);
        verify(repository, times(1)).getAllDefinitions();
        verify(repository, never()).searchByPartialName(anyString());
    }

    @Test
    void validateTransactionDefinitionSetLink_Success() {

        TransactionDefinitionSet transactionDefinitionSet = createTransactionDefinitionSetMock();
        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();

        when(transactionDefinitionSetService.getTransactionDefinitionSet(
                        transactionDefinition.getTransactionDefinitionSetKey()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        String schemaKey = "schema-key";
        Schema schema = createSchemaMock();
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.ofNullable(schema));

        when(schemaService.getSchemaByKey("child-schema-key"))
                .thenReturn(Optional.ofNullable(createChildMockSchema()));

        assertDoesNotThrow(
                () -> service.validateTransactionDefinitionSetLink(transactionDefinition));
    }

    @Test
    void validateTransactionDefinitionSetLink_TransactionDefinitionSetNotFound() {
        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.empty());

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage =
                "Transaction definition set with key transaction-definition-set-key does not exist";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateTransactionDefinitionSetLink_RelatedSchemaNotFound() {

        TransactionDefinitionSet transactionDefinitionSet = createTransactionDefinitionSetMock();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        Schema schema =
                Schema.builder()
                        .key("schema-key")
                        .name("schema-name")
                        .properties(
                                List.of(
                                        new DynaProperty("simple", String.class),
                                        new DynaProperty("list", List.class, String.class),
                                        new DynaProperty("child", DynamicEntity.class)))
                        .relatedSchemas(new HashMap<>())
                        .build();
        when(schemaService.getSchemaByKey(any())).thenReturn(Optional.ofNullable(schema));

        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();
        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage = "Related schema not found for property child";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateTransactionDefinitionSetLink_RelatedSchemaDoesNotExist() {

        TransactionDefinitionSet transactionDefinitionSet = createTransactionDefinitionSetMock();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        String schemaKey = "schema-key";
        Schema schema = createSchemaMock();
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.ofNullable(schema));

        when(schemaService.getSchemaByKey("child-schema-key")).thenReturn(Optional.empty());

        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();
        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage = "Related schema not found for schema key child-schema-key";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateTransactionDefinitionSetLink_SchemaNotFound() {
        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();

        TransactionDefinitionSet transactionDefinitionSet = createTransactionDefinitionSetMock();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        when(schemaService.getSchemaByKey(any())).thenReturn(Optional.empty());

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage = "Schema with key schema-key does not exist";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateTransactionDefinitionSetLink_NullDynaProperty() {
        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();

        TransactionDefinitionSet transactionDefinitionSet = createTransactionDefinitionSetMock();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        String schemaKey = "schema-key";
        Schema schema = Schema.builder().key(schemaKey).build();
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.ofNullable(schema));

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage = "Schema property not found for path simple";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateTransactionDefinitionSetLink_InvalidList() {
        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();

        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .key("transaction-definition-set-key")
                        .constraints(
                                List.of(
                                        TransactionDefinitionSetDataRequirement.builder()
                                                .path("list.simple")
                                                .type("String")
                                                .build()))
                        .build();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        Schema schema =
                Schema.builder()
                        .key("schema-key")
                        .name("schema-name")
                        .properties(List.of(new DynaProperty("list", List.class, Integer.class)))
                        .build();
        when(schemaService.getSchemaByKey(any())).thenReturn(Optional.ofNullable(schema));

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage =
                "Schema and data requirement are not compatible for path list.simple, invalid list"
                        + " type, constraint expected String, got Integer";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void validateTransactionDefinitionSetLink_InvalidSimpleType() {
        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();

        TransactionDefinitionSet transactionDefinitionSet =
                TransactionDefinitionSet.builder()
                        .key("transaction-definition-set-key")
                        .constraints(
                                List.of(
                                        TransactionDefinitionSetDataRequirement.builder()
                                                .path("simple")
                                                .type("String")
                                                .build()))
                        .build();
        when(transactionDefinitionSetService.getTransactionDefinitionSet(any()))
                .thenReturn(Optional.ofNullable(transactionDefinitionSet));

        Schema schema =
                Schema.builder()
                        .key("schema-key")
                        .name("schema-name")
                        .properties(List.of(new DynaProperty("simple", Integer.class)))
                        .build();
        when(schemaService.getSchemaByKey(any())).thenReturn(Optional.ofNullable(schema));

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            service.validateTransactionDefinitionSetLink(transactionDefinition);
                        });

        String expectedMessage =
                "Schema and data requirement are not compatible for path simple, constraint"
                        + " expected String, got Integer";
        String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void createTransactionDefinitionKeysList_ReturnNull() {
        String transactionDefinitionKey = null;
        String transactionDefinitionSetKey = null;

        List<String> result =
                service.createTransactionDefinitionKeysList(
                        transactionDefinitionKey, transactionDefinitionSetKey);

        assertNull(result);
    }

    @Test
    void createTransactionDefinitionKeysList_ReturnEmpty() {
        String transactionDefinitionKey = "invalid-key";
        String transactionDefinitionSetKey = "set-key";

        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();
        when(repository.searchByTransactionDefinitionSetKey(transactionDefinitionSetKey))
                .thenReturn(List.of(transactionDefinition));
        List<String> result =
                service.createTransactionDefinitionKeysList(
                        transactionDefinitionKey, transactionDefinitionSetKey);

        assertEquals(0, result.size());
    }

    @Test
    void createTransactionDefinitionKeysList_ReturnSetSingleKey() {
        String transactionDefinitionKey = "test-transaction-key";
        String transactionDefinitionSetKey = null;

        List<String> result =
                service.createTransactionDefinitionKeysList(
                        transactionDefinitionKey, transactionDefinitionSetKey);

        assertEquals(1, result.size());
        assertEquals("test-transaction-key", result.get(0));
    }

    @Test
    void createTransactionDefinitionKeysList_ReturnOnlyFromSet() {
        String transactionDefinitionKey = null;
        String transactionDefinitionSetKey = "set-key";

        TransactionDefinition transactionDefinition = createTransactionDefinitionMock();
        when(repository.searchByTransactionDefinitionSetKey(transactionDefinitionSetKey))
                .thenReturn(List.of(transactionDefinition));

        List<String> result =
                service.createTransactionDefinitionKeysList(
                        transactionDefinitionKey, transactionDefinitionSetKey);

        assertEquals(1, result.size());
        assertEquals("test-transaction-key", result.get(0));
    }

    private Schema createSchemaMock() {

        return Schema.builder()
                .key("schema-key")
                .name("schema-name")
                .properties(
                        List.of(
                                new DynaProperty("simple", String.class),
                                new DynaProperty("list", List.class, String.class),
                                new DynaProperty("child", DynamicEntity.class)))
                .relatedSchemas(Map.of("child", "child-schema-key"))
                .build();
    }

    private Schema createChildMockSchema() {
        return Schema.builder()
                .key("child-schema-key")
                .name("child-schema-name")
                .properties(List.of(new DynaProperty("simple", String.class)))
                .build();
    }

    private TransactionDefinition createTransactionDefinitionMock() {
        return TransactionDefinition.builder()
                .id(UUID.randomUUID())
                .key("test-transaction-key")
                .schemaKey("schema-key")
                .name("test-transaction")
                .transactionDefinitionSetKey("transaction-definition-set-key")
                .build();
    }

    private TransactionDefinitionSet createTransactionDefinitionSetMock() {
        return TransactionDefinitionSet.builder()
                .key("transaction-definition-set-key")
                .constraints(
                        List.of(
                                TransactionDefinitionSetDataRequirement.builder()
                                        .path("simple")
                                        .type("String")
                                        .build(),
                                TransactionDefinitionSetDataRequirement.builder()
                                        .path("list.simple")
                                        .type("String")
                                        .build(),
                                TransactionDefinitionSetDataRequirement.builder()
                                        .path("child.simple")
                                        .type("String")
                                        .build()))
                .build();
    }
}

package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.repository.FormConfigurationRepository;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class FormConfigurationServiceTest {

    static String userType = "userType";
    private FormConfigurationRepository repository;
    private TransactionDefinitionService transactionDefinitionService;
    private TransactionTaskService transactionTaskService;
    private FormConfigurationService service;
    private SchemaService schemaService;

    private static MockedStatic<CurrentUserUtility> initCurrentUserUtilityMock() {
        MockedStatic<CurrentUserUtility> staticCurrentUserUtility =
                Mockito.mockStatic(CurrentUserUtility.class);
        staticCurrentUserUtility
                .when(CurrentUserUtility::getCurrentUser)
                .thenReturn(Optional.of(UserToken.builder().userType(userType).build()));

        return staticCurrentUserUtility;
    }

    @BeforeEach
    void setUp() {
        repository = mock(FormConfigurationRepository.class);
        transactionDefinitionService = mock(TransactionDefinitionService.class);
        transactionTaskService = mock(TransactionTaskService.class);
        schemaService = mock(SchemaService.class);
        service =
                new FormConfigurationService(
                        repository,
                        transactionDefinitionService,
                        transactionTaskService,
                        schemaService);
    }

    @Test
    void directRepositoryWrappers() {
        // Arrange
        String transactionDefinitionKey = "transactionDefinitionKey";
        String formConfigurationKey = "formConfigurationKey";

        FormConfiguration formConfigWithId =
                getFormBaseObject(transactionDefinitionKey, formConfigurationKey);

        // test 1
        when(repository.searchByKeys(transactionDefinitionKey, formConfigurationKey))
                .thenReturn(List.of(formConfigWithId));

        var result =
                service.getFormConfigurationByKeys(transactionDefinitionKey, formConfigurationKey)
                        .get();

        // Assert
        assert result != null;
        assertEquals("testerUser", result.getCreatedBy());

        // ---

        // test 2
        when(repository.findByTransactionDefinitionKey(transactionDefinitionKey))
                .thenReturn(List.of(formConfigWithId));
        result =
                service.getFormConfigurationsByTransactionDefinitionKey(transactionDefinitionKey)
                        .get(0);

        // Assert
        assert result != null;
        assertEquals("testerUser", result.getCreatedBy());
    }

    @Test
    void getActiveFormConfiguration() {

        String context = "context";
        String defaultFormDef = "defaultFormDef";
        String transactionDefKey = "transactionDefKey";
        String createdBy = "testerUser";
        String task1 = "task1";
        String task2 = "task2";

        Transaction transaction =
                configTest(
                        transactionDefKey,
                        defaultFormDef,
                        context,
                        createdBy,
                        List.of(task1, task2));

        try (var staticMock = initCurrentUserUtilityMock()) {
            // Act
            var result = service.getActiveFormConfiguration(transaction, context);

            // Assert
            assert result != null;
            assertEquals(2, result.size());
            assertEquals("testerUser", result.get(task1).getCreatedBy());
        }
    }

    @Test
    void getActiveFormConfigurationForCompletedProcess() {

        String context = "context";
        String defaultFormDef = "defaultFormDef";
        String transactionDefKey = "transactionDefKey";
        String createdBy = "testerUser";

        Transaction transaction =
                configTest(transactionDefKey, defaultFormDef, context, createdBy, List.of());

        try (var staticMock = initCurrentUserUtilityMock()) {
            // Act
            var result = service.getActiveFormConfiguration(transaction, context);

            // Assert
            assert result != null;
            assertEquals(1, result.size());
            assertEquals("testerUser", result.get("fallback").getCreatedBy());
        }
    }

    private Transaction configTest(
            String transactionDefKey,
            String defaultFormDef,
            String context,
            String createdBy,
            List<String> tasks) {
        FormConfiguration formConfig = FormConfiguration.builder().createdBy(createdBy).build();

        when(repository.searchByKeys(transactionDefKey, defaultFormDef))
                .thenReturn(List.of(formConfig));

        UUID transactionDefId = UUID.randomUUID();
        Transaction transaction =
                Transaction.builder().transactionDefinitionId(transactionDefId).build();

        when(transactionTaskService.getActiveTasksForCurrentUser(transaction))
                .thenReturn(
                        tasks.stream()
                                .map(task -> WorkflowTask.builder().key(task).build())
                                .collect(Collectors.toList()));

        TransactionDefinition transactionDefinition = mock(TransactionDefinition.class);

        when(transactionDefinition.getFormConfigurationKey(any(), eq(userType), eq(context)))
                .thenReturn(Optional.of(defaultFormDef));
        when(transactionDefinition.getKey()).thenReturn(transactionDefKey);

        when(transactionDefinitionService.getTransactionDefinitionById(transactionDefId))
                .thenReturn(Optional.of(transactionDefinition));

        return transaction;
    }

    private FormConfiguration getFormBaseObject(
            String transactionDefinitionKey, String formConfigurationKey) {
        return FormConfiguration.builder()
                .createdBy("testerUser")
                .transactionDefinitionKey(transactionDefinitionKey)
                .key(formConfigurationKey)
                .configuration(new HashMap<String, Object>())
                .id(UUID.randomUUID())
                .build();
    }

    private Schema getSchemaBaseObject(String schemaKey, boolean isParent) {
        List<DynaProperty> configs = new ArrayList<>();
        DynaProperty dp =
                isParent
                        ? new DynaProperty("child-component", DynamicEntity.class)
                        : new DynaProperty("child-component");
        configs.add(dp);
        return Schema.builder()
                .properties(configs)
                .relatedSchemas(isParent ? Map.of("child-component", "childSchemaKey") : Map.of())
                .key(schemaKey)
                .build();
    }

    private Map<String, Object> getSuccessfulCompleteNestedConfig() {
        return Map.of(
                "components",
                List.of(
                        Map.of(
                                "key",
                                "child-component",
                                "input",
                                true,
                                "components",
                                List.of(
                                        Map.of(
                                                "key",
                                                "child-component.grandchild-component",
                                                "input",
                                                false)))));
    }

    @Test
    void formConfigValidationNullSchema() {

        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");
        fc.setConfiguration(Map.of("components", List.of(Map.of("key", "child-component"))));

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("ROOT_COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals("Null schema key", exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationNotFoundSchema() {

        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);
        fc.setConfiguration(Map.of("components", List.of(Map.of("key", "child-component"))));

        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.empty());

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("ROOT_COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals(
                "Schema with key '" + schemaKey + "' not found.",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationChildComponentNotDataBacked() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(Map.of("key", "NON-SCHEMA-COMPONENT", "input", false))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, false);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        // work with method data
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var result = service.saveFormConfiguration(fc);

        // Assert
        assert result != null;
        assertNotNull(result.getId());
        var components = assertInstanceOf(List.class, result.getConfiguration().get("components"));
        assertEquals(1, components.size());
    }

    @Test
    void formConfigValidationChildComponentNotFoundInSchema() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(Map.of("key", "NON-SCHEMA-COMPONENT", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, false);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("NON-SCHEMA-COMPONENT", exceptionMessages.get(0).getControlName());
        assertEquals(
                "Component key not found in schema with key 'someSchema'",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationComponentMissingKey() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(Map.of("components", List.of(Map.of("input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals("A component is missing its key", exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationComponentDuplicateKey() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(
                                Map.of("key", "child-component", "input", true),
                                Map.of("key", "child-component", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals(
                "Component key is found more than once in the form configuration",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationComponentEmptySectionKey() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of(
                        "components",
                        List.of(Map.of("key", "child-component.    .final", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        var exceptionMessages =
                assertThrows(
                                NuvalenceFormioValidationException.class,
                                () -> service.saveFormConfiguration(fc))
                        .getErrorMessages()
                        .getFormioValidationErrors();

        assertEquals(1, exceptionMessages.size());
        assertEquals(
                "All components must have a non-blank key with non-blank sections",
                exceptionMessages.get(0).getErrorMessage());
    }

    @Test
    void formConfigValidationSuccess() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(
                Map.of("components", List.of(Map.of("key", "child-component", "input", true))));

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, false);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        // work with method data
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var result = service.saveFormConfiguration(fc);

        // Assert
        assert result != null;
        assertNotNull(result.getId());
        var components = assertInstanceOf(List.class, result.getConfiguration().get("components"));
        assertEquals(1, components.size());
        var component = assertInstanceOf(Map.class, components.get(0));
        assertEquals("child-component", component.get("key"));
    }

    @Test
    void formConfigValidationNestedSuccess() {
        FormConfiguration fc =
                getFormBaseObject("transactionDefinitionKey", "formConfigurationKey");

        fc.setConfiguration(getSuccessfulCompleteNestedConfig());

        String schemaKey = "someSchema";
        fc.setSchemaKey(schemaKey);

        Schema schema = getSchemaBaseObject(schemaKey, true);
        when(schemaService.getSchemaByKey(schemaKey)).thenReturn(Optional.of(schema));

        String childSchemaKey = "childSchemaKey";
        when(schemaService.getSchemaByKey(childSchemaKey))
                .thenReturn(Optional.of(getSchemaBaseObject(childSchemaKey, false)));

        // work with method data
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var result = service.saveFormConfiguration(fc);

        // Assert
        assert result != null;
        assertNotNull(result.getId());
        var components = assertInstanceOf(List.class, result.getConfiguration().get("components"));
        assertEquals(1, components.size());
        var component = assertInstanceOf(Map.class, components.get(0));
        assertEquals("child-component", component.get("key"));

        var subComponents = assertInstanceOf(List.class, component.get("components"));
        assertEquals(1, subComponents.size());
        var subComponent = assertInstanceOf(Map.class, subComponents.get(0));
        assertEquals("child-component.grandchild-component", subComponent.get("key"));
    }

    @Test
    void createDefaultFormConfiguration() {

        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("test")
                        .name("test transaction")
                        .category("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("test-schema")
                        .defaultStatus("new")
                        .build();
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());
        FormConfiguration formConfiguration =
                service.createDefaultFormConfiguration(transactionDefinition);

        assertEquals(
                transactionDefinition.getKey(), formConfiguration.getTransactionDefinitionKey());
        assertEquals(transactionDefinition.getSchemaKey(), formConfiguration.getSchemaKey());
        assertEquals("formio", formConfiguration.getConfigurationSchema());
        assertEquals("testDefault", formConfiguration.getKey());
        assertEquals("Default Form", formConfiguration.getName());
        assertEquals("Default form for test transaction", formConfiguration.getDescription());
        assertEquals(new HashMap<>(), formConfiguration.getConfiguration());
    }
}

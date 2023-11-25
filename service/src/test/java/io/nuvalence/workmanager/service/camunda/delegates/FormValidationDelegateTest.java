package io.nuvalence.workmanager.service.camunda.delegates;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.TransactionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class FormValidationDelegateTest {

    @Mock private DelegateExecution execution;

    @Mock private TransactionService transactionService;

    @Mock private FormConfigurationService formConfigurationService;

    private FormValidationDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new FormValidationDelegate(transactionService, formConfigurationService);
    }

    @Test
    void testExecuteNoExceptions() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        String transactionDefinitionKey = "TransactionDefinitionKey";
        String defaultConfigurationKey = "DefaultConfigurationKey";
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .key(transactionDefinitionKey)
                        .defaultFormConfigurationKey(defaultConfigurationKey)
                        .build();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .transactionDefinitionKey(transactionDefinitionKey)
                        .transactionDefinition(transactionDefinition)
                        .build();
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        String stepKeyOne = "keyOne";
        Map<String, Object> componentOne = new HashMap<>();
        componentOne.put("key", stepKeyOne);
        String stepKeyTwo = "keyTwo";
        Map<String, Object> componentTwo = new HashMap<>();
        componentTwo.put("key", stepKeyTwo);
        List<Map<String, Object>> components =
                new ArrayList<>(Arrays.asList(componentOne, componentTwo));
        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("components", components);

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key(transactionDefinitionKey)
                        .configuration(configurationMap)
                        .build();
        when(formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, defaultConfigurationKey))
                .thenReturn(Optional.of(formConfiguration));

        doNothing()
                .when(transactionService)
                .validateForm(
                        eq(defaultConfigurationKey), eq(stepKeyOne), eq(transaction), any(), any());
        doNothing()
                .when(transactionService)
                .validateForm(
                        eq(defaultConfigurationKey), eq(stepKeyTwo), eq(transaction), any(), any());

        delegate.execute(execution);

        verify(transactionService, times(1))
                .validateForm(
                        eq(defaultConfigurationKey), eq(stepKeyOne), eq(transaction), any(), any());
        verify(transactionService, times(1))
                .validateForm(
                        eq(defaultConfigurationKey), eq(stepKeyTwo), eq(transaction), any(), any());
    }

    @Test
    void testExecuteWithExceptions() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        String transactionDefinitionKey = "TransactionDefinitionKey";
        String defaultConfigurationKey = "DefaultConfigurationKey";
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .key(transactionDefinitionKey)
                        .defaultFormConfigurationKey(defaultConfigurationKey)
                        .build();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .transactionDefinitionKey(transactionDefinitionKey)
                        .transactionDefinition(transactionDefinition)
                        .build();
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        String stepKeyOne = "keyOne";
        Map<String, Object> componentOne = new HashMap<>();
        componentOne.put("key", stepKeyOne);
        String stepKeyTwo = "keyTwo";
        Map<String, Object> componentTwo = new HashMap<>();
        componentTwo.put("key", stepKeyTwo);
        List<Map<String, Object>> components =
                new ArrayList<>(Arrays.asList(componentOne, componentTwo));
        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("components", components);

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key(transactionDefinitionKey)
                        .configuration(configurationMap)
                        .build();
        when(formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, defaultConfigurationKey))
                .thenReturn(Optional.of(formConfiguration));

        String errorMessageOne = "Error Message One";
        String errorMessageTwo = "Error Message Two";
        NuvalenceFormioValidationExItem nuvalenceFormioValidationExItemOne =
                NuvalenceFormioValidationExItem.builder().errorName(errorMessageOne).build();
        NuvalenceFormioValidationExItem nuvalenceFormioValidationExItemTwo =
                NuvalenceFormioValidationExItem.builder().errorName(errorMessageTwo).build();

        List<NuvalenceFormioValidationExItem> formioErrorsOne =
                new ArrayList<>(Arrays.asList(nuvalenceFormioValidationExItemOne));
        List<NuvalenceFormioValidationExItem> formioErrorsTwo =
                new ArrayList<>(Arrays.asList(nuvalenceFormioValidationExItemTwo));
        List<NuvalenceFormioValidationExItem> formioErrorsTotal =
                new ArrayList<>(
                        Arrays.asList(
                                nuvalenceFormioValidationExItemOne,
                                nuvalenceFormioValidationExItemTwo));

        NuvalenceFormioValidationExMessage formioValidationExMessageOne =
                NuvalenceFormioValidationExMessage.builder()
                        .formioValidationErrors(formioErrorsOne)
                        .build();
        NuvalenceFormioValidationExMessage formioValidationExMessageTwo =
                NuvalenceFormioValidationExMessage.builder()
                        .formioValidationErrors(formioErrorsTwo)
                        .build();
        NuvalenceFormioValidationExMessage formioValidationExMessageTotal =
                NuvalenceFormioValidationExMessage.builder()
                        .formioValidationErrors(formioErrorsTotal)
                        .build();

        doThrow(new NuvalenceFormioValidationException(formioValidationExMessageOne))
                .when(transactionService)
                .validateForm(
                        eq(defaultConfigurationKey), eq(stepKeyOne), eq(transaction), any(), any());
        doThrow(new NuvalenceFormioValidationException(formioValidationExMessageTwo))
                .when(transactionService)
                .validateForm(
                        eq(defaultConfigurationKey), eq(stepKeyTwo), eq(transaction), any(), any());

        NuvalenceFormioValidationException exception =
                assertThrows(
                        NuvalenceFormioValidationException.class,
                        () -> {
                            delegate.execute(execution);
                        });
        assertEquals(2, exception.getFormioValidationErrors().getFormioValidationErrors().size());
    }

    @Test
    void testExecuteNullComponents() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        String transactionDefinitionKey = "TransactionDefinitionKey";
        String defaultConfigurationKey = "DefaultConfigurationKey";
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .key(transactionDefinitionKey)
                        .defaultFormConfigurationKey(defaultConfigurationKey)
                        .build();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .transactionDefinitionKey(transactionDefinitionKey)
                        .transactionDefinition(transactionDefinition)
                        .build();
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("components", null);

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key(transactionDefinitionKey)
                        .configuration(configurationMap)
                        .build();
        when(formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, defaultConfigurationKey))
                .thenReturn(Optional.of(formConfiguration));

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            delegate.execute(execution);
                        });

        assertEquals(
                "Components could not be obtained for the provided form configuration.",
                exception.getMessage());
    }

    @Test
    void testExecuteInvalidComponentStructure() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        String transactionDefinitionKey = "TransactionDefinitionKey";
        String defaultConfigurationKey = "DefaultConfigurationKey";
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .key(transactionDefinitionKey)
                        .defaultFormConfigurationKey(defaultConfigurationKey)
                        .build();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .transactionDefinitionKey(transactionDefinitionKey)
                        .transactionDefinition(transactionDefinition)
                        .build();
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        String components = "Component with unexpected structure";
        Map<String, Object> configurationMap = new HashMap<>();
        configurationMap.put("components", components);

        FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .key(transactionDefinitionKey)
                        .configuration(configurationMap)
                        .build();
        when(formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, defaultConfigurationKey))
                .thenReturn(Optional.of(formConfiguration));

        BusinessLogicException exception =
                assertThrows(
                        BusinessLogicException.class,
                        () -> {
                            delegate.execute(execution);
                        });

        assertEquals(
                "An unexpected component structure was found for the provided form configuration.",
                exception.getMessage());
    }

    @Test
    void testExecuteTransactionNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

        delegate.execute(execution);

        verify(transactionService, never()).validateForm(any(), any(), any(), any(), any());
    }

    @Test
    void testExecuteFormConfigurationNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(execution.getVariable("transactionId")).thenReturn(transactionId);

        String transactionDefinitionKey = "TransactionDefinitionKey";
        String defaultConfigurationKey = "DefaultConfigurationKey";
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .key(transactionDefinitionKey)
                        .defaultFormConfigurationKey(defaultConfigurationKey)
                        .build();
        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .transactionDefinitionKey(transactionDefinitionKey)
                        .transactionDefinition(transactionDefinition)
                        .build();
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, defaultConfigurationKey))
                .thenReturn(Optional.empty());

        delegate.execute(execution);

        verify(transactionService, never()).validateForm(any(), any(), any(), any(), any());
    }
}

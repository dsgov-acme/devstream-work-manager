package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionExportModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class TransactionDefinitionMapperTest {
    private TransactionDefinition transactionDefinition;
    private TransactionDefinitionResponseModel model;
    private TransactionDefinitionUpdateModel updateModel;
    private TransactionDefinitionExportModel exportModel;
    private TransactionDefinitionMapper mapper;

    @BeforeEach
    void setup() {
        final UUID id = UUID.randomUUID();
        transactionDefinition =
                TransactionDefinition.builder()
                        .id(id)
                        .key("test")
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("testschema")
                        .build();
        exportModel =
                new TransactionDefinitionExportModel()
                        .id(String.valueOf(id))
                        .key("test")
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schema("testschema");
        model =
                new TransactionDefinitionResponseModel()
                        .id(id)
                        .key("test")
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("testschema");
        updateModel =
                new TransactionDefinitionUpdateModel()
                        .name("test transaction")
                        .processDefinitionKey("process-definition-key")
                        .schemaKey("testschema");
        mapper = TransactionDefinitionMapper.INSTANCE;
    }

    @Test
    void transactionDefinitionToResponseModel() {
        assertEquals(model, mapper.transactionDefinitionToResponseModel(transactionDefinition));
        assertNull(mapper.transactionDefinitionToResponseModel(null));
    }

    @Test
    void updateModelToTransactionDefinition() {
        assertTransactionDefinitionsEqual(
                transactionDefinition,
                mapper.updateModelToTransactionDefinition(updateModel),
                true);
        assertNull(mapper.updateModelToTransactionDefinition(null));
    }

    @Test
    void transactionDefinitionToTransactionDefinitionExportModel() {
        assertEquals(
                exportModel,
                mapper.transactionDefinitionToTransactionDefinitionExportModel(
                        transactionDefinition));
        assertNull(mapper.transactionDefinitionToTransactionDefinitionExportModel(null));
    }

    @Test
    void transactionDefinitionExportModelToTransactionDefinition() {
        assertTransactionDefinitionsEqual(
                transactionDefinition,
                mapper.transactionDefinitionExportModelToTransactionDefinition(exportModel),
                true);
        assertNull(mapper.transactionDefinitionExportModelToTransactionDefinition(null));
    }

    @Test
    void transactionDefinitionExportModelToTransactionDefinitionNullModel() {
        mapper.transactionDefinitionExportModelToTransactionDefinition(null, null);
        assertNull(mapper.transactionDefinitionExportModelToTransactionDefinition(null));
    }

    @Test
    void mapTransactionDefinitionExportModelFieldsToTransactionDefinition() {
        mapper.mapTransactionDefinitionExportModelFieldsToTransactionDefinition(null, null);
        TransactionDefinition transactionDefinition1 = mock(TransactionDefinition.class);
        verify(transactionDefinition1, never()).setId(any());
    }

    private void assertTransactionDefinitionsEqual(
            final TransactionDefinition a, final TransactionDefinition b, boolean ignoreIdFields) {
        if (!ignoreIdFields) {
            assertEquals(a.getId(), b.getId());
        }
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getProcessDefinitionKey(), b.getProcessDefinitionKey());
        assertEquals(a.getSchemaKey(), b.getSchemaKey());
    }
}

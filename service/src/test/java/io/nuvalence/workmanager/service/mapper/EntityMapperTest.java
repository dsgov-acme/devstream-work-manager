package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import io.nuvalence.workmanager.service.utils.testutils.DataUtils;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EntityMapperTest {

    @Mock SchemaService schemaService;

    @InjectMocks private final EntityMapper entityMapper = new EntityMapperImpl();
    private Schema schema;
    private Schema officeInfoSchema;

    private Map<String, Object> transactionData;
    private final JsonFileLoader jsonLoader = new JsonFileLoader();

    @BeforeEach
    void setup() throws IOException {
        schema = DataUtils.createSchemaWithoutSavingToDb();
        transactionData = jsonLoader.loadConfigMap("/basicTransactionData.json");

        DynaProperty city = new DynaProperty("city", String.class);
        DynaProperty address = new DynaProperty("address", String.class);
        officeInfoSchema =
                Schema.builder().id(UUID.randomUUID()).properties(List.of(city, address)).build();
    }

    @Test
    void testApplyMappedPropertiesToEntity() throws MissingSchemaException {
        when(schemaService.getSchemaByKey("OfficeInfo"))
                .thenReturn(Optional.ofNullable(officeInfoSchema));
        DynamicEntity result = new DynamicEntity(schema);

        entityMapper.applyMappedPropertiesToEntity(result, transactionData);

        assertEquals("myFirstName", result.get("firstName"));
        assertEquals("myLastName", result.get("lastName"));
        assertEquals("email@something.com", result.get("email"));
        assertEquals("myAddress", result.get("address"));
        assertEquals("myEmploymentStatus", result.get("employmentStatus"));
        assertEquals("myCompany", result.get("company"));
        assertEquals(true, result.get("isMailingAddressNeeded"));
        assertEquals(30, result.get("age"));

        LocalDate localDateResult = (LocalDate) result.get("dateOfBirth");
        assertEquals(1993, localDateResult.getYear());
        assertEquals(12, localDateResult.getMonthValue());
        assertEquals(21, localDateResult.getDayOfMonth());

        Document resultDocument = (Document) result.get("document");
        assertEquals(
                UUID.fromString("f84b20e8-7a64-431f-ad94-440ca0c4b7c1"),
                resultDocument.getDocumentId());
    }

    @Test
    void testApplyMappedPropertiesToEntityKeyNotFound() {
        DynamicEntity result = new DynamicEntity(schema);
        transactionData.clear();
        transactionData.put("keyNotFound", "value");

        Exception e =
                assertThrows(
                        BusinessLogicException.class,
                        () -> entityMapper.applyMappedPropertiesToEntity(result, transactionData));

        assertEquals("Key not found in schema: 'keyNotFound'.", e.getMessage());
    }

    @Test
    void testApplyMappedPropertiesToEntityConversionError() {
        DynaProperty invalidTypeProperty = new DynaProperty("city", Schema.class);
        Schema schema1 =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(List.of(invalidTypeProperty))
                        .build();
        DynamicEntity result = new DynamicEntity(schema1);
        transactionData.clear();
        transactionData.put("city", "value");

        Exception e =
                assertThrows(
                        BusinessLogicException.class,
                        () -> entityMapper.applyMappedPropertiesToEntity(result, transactionData));

        assertEquals("Invalid type for key: 'city'. It should be: 'Schema'.", e.getMessage());
    }

    @Test
    void testApplyMappedPropertiesToEntityInvalidKeyTypeCompositeObject() {
        DynaProperty invalidTypeProperty = new DynaProperty("city", DynamicEntity.class);
        Schema schema1 =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(List.of(invalidTypeProperty))
                        .build();
        DynamicEntity result = new DynamicEntity(schema1);
        transactionData.clear();
        transactionData.put("city", "value");

        Exception e =
                assertThrows(
                        BusinessLogicException.class,
                        () -> entityMapper.applyMappedPropertiesToEntity(result, transactionData));

        assertEquals(
                "Invalid type for key: 'city'. It should be a composite object.", e.getMessage());
    }

    @Test
    void testApplyMappedPropertiesToEntityInvalidKeyTypeList() {
        DynaProperty invalidTypeProperty = new DynaProperty("city", List.class);
        Schema schema1 =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .properties(List.of(invalidTypeProperty))
                        .build();
        DynamicEntity result = new DynamicEntity(schema1);
        transactionData.clear();
        transactionData.put("city", 1);

        Exception e =
                assertThrows(
                        BusinessLogicException.class,
                        () -> entityMapper.applyMappedPropertiesToEntity(result, transactionData));

        assertEquals("Invalid type for key: 'city'. It should be a List.", e.getMessage());
    }
}

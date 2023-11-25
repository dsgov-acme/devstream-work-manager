package io.nuvalence.workmanager.service.utils.formconfig.formio;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class NuvalenceFormioValidatorTest {

    @InjectMocks
    private static final EntityMapper entityMapper = Mappers.getMapper(EntityMapper.class);

    private static final DynamicSchemaMapper schemaMapper =
            Mappers.getMapper(DynamicSchemaMapper.class);

    private final JsonFileLoader jsonLoader = new JsonFileLoader();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private SchemaService schemaService;

    @BeforeAll
    public static void setup() {
        var staticEntityMapper = Mockito.mockStatic(EntityMapper.class);
        staticEntityMapper.when(EntityMapper::getInstance).thenReturn(entityMapper);
    }

    @Test
    void testValidateComponent_WithNoErrors() throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap("/basicTransactionData.json");

        DynaProperty city = new DynaProperty("city", String.class);
        DynaProperty address = new DynaProperty("address", String.class);

        Schema officeInfoSchema =
                Schema.builder().id(UUID.randomUUID()).properties(List.of(city, address)).build();

        when(schemaService.getSchemaByKey(anyString())).thenReturn(Optional.of(officeInfoSchema));

        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertTrue(formioValidationErrors.isEmpty());
    }

    @Test
    void testValidateComponent_WithoutRequiredField() throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/transactionDataWithoutRequiredFirstName.json");
        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent.getComponents().get(0), dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertEquals(1, formioValidationErrors.size());
        Assertions.assertEquals("firstName", formioValidationErrors.get(0).getControlName());
        Assertions.assertEquals("required", formioValidationErrors.get(0).getErrorName());
    }

    @Test
    void testValidateComponent_WithMaxLengthAndPatternError()
            throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/transactionDataWithMaxLengthAndPatternError.json");
        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertEquals(2, formioValidationErrors.size());
        Assertions.assertEquals("company", formioValidationErrors.get(0).getControlName());
        Assertions.assertEquals("maxLength", formioValidationErrors.get(0).getErrorName());
        Assertions.assertEquals("company", formioValidationErrors.get(1).getControlName());
        Assertions.assertEquals("pattern", formioValidationErrors.get(1).getErrorName());
    }

    @Test
    void testValidateComponent_WithMinLengthAndPatternError()
            throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/transactionDataWithMinLengthAndPatternError.json");
        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertEquals(2, formioValidationErrors.size());
        Assertions.assertEquals("company", formioValidationErrors.get(0).getControlName());
        Assertions.assertEquals("minLength", formioValidationErrors.get(0).getErrorName());
        Assertions.assertEquals("company", formioValidationErrors.get(1).getControlName());
        Assertions.assertEquals("pattern", formioValidationErrors.get(1).getErrorName());
    }

    @Test
    void testValidateComponent_checkPattern_NoErrors() throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/TransactionDataWithNoAddressButNotNeeded.json");
        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertTrue(formioValidationErrors.isEmpty());
    }

    @Test
    void testValidateComponent_checkPattern_NoErrors_Expression_Parent_Component()
            throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/basicFormNestedComponentConfig.json");
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/TransactionDataWithNoAddressButNotNeeded.json");
        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertTrue(formioValidationErrors.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "2, day, 3, relativeDate, relativeMaxDate, true",
        "-2, day, -1, relativeDate, relativeMaxDate, true",
        "2, day, 1, relativeDate, relativeMinDate, true",
        "-2, day, -3, relativeDate, relativeMinDate, true",
        "2, day, 2, relativeDate, relativeMaxDate, false",
        "-2, day, -3, relativeDate, relativeMaxDate, false",
        "2, day, 2, relativeDate, relativeMinDate, false",
        "-2, day, -2, relativeDate, relativeMinDate, false",
        "2, week, 3, relativeDate, relativeMaxDate, true",
        "-2, week, -1, relativeDate, relativeMaxDate, true",
        "2, week, 1, relativeDate, relativeMinDate, true",
        "-2, week, -3, relativeDate, relativeMinDate, true",
        "2, week, 2, relativeDate, relativeMaxDate, false",
        "-2, week, -3, relativeDate, relativeMaxDate, false",
        "2, week, 2, relativeDate, relativeMinDate, false",
        "-2, week, -2, relativeDate, relativeMinDate, false",
        "2, month, 3, relativeDate, relativeMaxDate, true",
        "-2, month, -1, relativeDate, relativeMaxDate, true",
        "2, month, 1, relativeDate, relativeMinDate, true",
        "-2, month, -3, relativeDate, relativeMinDate, true",
        "2, month, 2, relativeDate, relativeMaxDate, false",
        "-2, month, -3, relativeDate, relativeMaxDate, false",
        "2, month, 2, relativeDate, relativeMinDate, false",
        "-2, month, -2, relativeDate, relativeMinDate, false",
        "2, year, 3, relativeDate, relativeMaxDate, true",
        "-2, year, -1, relativeDate, relativeMaxDate, true",
        "2, year, 1, relativeDate, relativeMinDate, true",
        "-2, year, -3, relativeDate, relativeMinDate, true",
        "2, year, 2, relativeDate, relativeMaxDate, false",
        "-2, year, -3, relativeDate, relativeMaxDate, false",
        "2, year, 2, relativeDate, relativeMinDate, false",
        "-2, year, -2, relativeDate, relativeMinDate, false",
    })
    void testValidateComponent_withRelativeDatesError(
            int relativeValidationValue,
            String relativeValidationUnit,
            int timeToAddOrSubtract,
            String expectedControlName,
            String expectedValidationName,
            boolean expectedError)
            throws IOException, MissingSchemaException {

        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");

        Map<String, Object> relativeFormComponent =
                createFormComponent(
                        expectedControlName,
                        expectedValidationName,
                        relativeValidationValue + "-" + relativeValidationUnit);
        List<Object> components = (List<Object>) formConfig.get("components");
        components.add(relativeFormComponent);

        Map<String, Object> transactionData =
                testValidateComponent_prepareTransactionData(
                        timeToAddOrSubtract, relativeValidationUnit);

        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        if (expectedError) {
            Assertions.assertEquals(1, formioValidationErrors.size());
            Assertions.assertEquals(
                    expectedControlName, formioValidationErrors.get(0).getControlName());
            Assertions.assertEquals(
                    expectedValidationName, formioValidationErrors.get(0).getErrorName());
        } else {
            Assertions.assertTrue(formioValidationErrors.isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "/formConfigurationJSONTests/transactionDataWithMaxValueError.json, age, max",
        "/formConfigurationJSONTests/transactionDataWithMinValueError.json, age, min",
        "/formConfigurationJSONTests/transactionDataWithEmailError.json, email, email",
        "/formConfigurationJSONTests/transactionDataWithSelectOptionsError.json, countryCode,"
                + " selectOptions",
        "/formConfigurationJSONTests/TransactionDataWithNoAddressButNeeded.json, address, required",
        "/formConfigurationJSONTests/transactionDataWithMaxDateError.json, someDate, maxDate",
        "/formConfigurationJSONTests/transactionDataWithMinDateError.json, someDate, minDate",
    })
    void testValidateComponentWithVariousErrors(
            String transactionDataPath, String expectedControlName, String expectedErrorName)
            throws IOException, MissingSchemaException {
        // Arrange
        Map<String, Object> formConfig =
                jsonLoader.loadConfigMap("/formConfigurationJSONTests/basicFormConfig.json");
        Map<String, Object> transactionData = jsonLoader.loadConfigMap(transactionDataPath);
        DynamicEntity dynaEntity = createDynamicEntity(transactionData);
        List<NuvalenceFormioValidationExItem> formioValidationErrors = new ArrayList<>();

        NuvalenceFormioComponent formioComponent =
                SpringConfig.getMapper().convertValue(formConfig, NuvalenceFormioComponent.class);

        // Act
        NuvalenceFormioValidator.validateComponent(
                formioComponent, dynaEntity, formioValidationErrors);

        // Assert
        Assertions.assertEquals(1, formioValidationErrors.size());
        Assertions.assertEquals(
                expectedControlName, formioValidationErrors.get(0).getControlName());
        Assertions.assertEquals(expectedErrorName, formioValidationErrors.get(0).getErrorName());
    }

    private Schema createSchema() throws IOException {
        String schemaString = jsonLoader.loadConfigString("/basicSchema.json");
        SchemaJson schemaJson = objectMapper.readValue(schemaString, SchemaJson.class);
        return schemaMapper.schemaJsonToSchema(schemaJson, UUID.randomUUID());
    }

    private DynamicEntity createDynamicEntity(Map<String, Object> transactionData)
            throws MissingSchemaException, IOException {
        Schema schema = createSchema();
        DynamicEntity dynaEntity = new DynamicEntity(schema);
        entityMapper.applyMappedPropertiesToEntity(dynaEntity, transactionData);
        return dynaEntity;
    }

    private Map<String, Object> createFormComponent(
            String expectedControlName,
            String expectedValidationName,
            String relativeValidationValue) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("key", expectedControlName);
        jsonMap.put("input", true);

        Map<String, Object> propsMap = new HashMap<>();
        propsMap.put(expectedValidationName, relativeValidationValue);

        jsonMap.put("props", propsMap);

        return jsonMap;
    }

    private Map<String, Object> testValidateComponent_prepareTransactionData(
            int timeToAddOrSubtract, String relativeValidationUnit) throws IOException {
        Map<String, Object> transactionData =
                jsonLoader.loadConfigMap(
                        "/formConfigurationJSONTests/transactionDataWithMaxDateError.json");

        transactionData.remove("someDate");
        LocalDate currentDate = LocalDate.now();

        switch (relativeValidationUnit) {
            case "day":
                currentDate = manipulateDate(currentDate, timeToAddOrSubtract, ChronoUnit.DAYS);
                break;
            case "week":
                currentDate = manipulateDate(currentDate, timeToAddOrSubtract, ChronoUnit.WEEKS);
                break;
            case "month":
                currentDate = manipulateDate(currentDate, timeToAddOrSubtract, ChronoUnit.MONTHS);
                break;
            case "year":
                currentDate = manipulateDate(currentDate, timeToAddOrSubtract, ChronoUnit.YEARS);
                break;
            default:
                throw new IllegalArgumentException("Invalid date unit");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);
        transactionData.put("relativeDate", formattedDate);

        return transactionData;
    }

    private static LocalDate manipulateDate(
            LocalDate currentDate, int timeToAddOrSubtract, ChronoUnit chronoUnit) {
        return timeToAddOrSubtract >= 0
                ? currentDate.plus(timeToAddOrSubtract, chronoUnit)
                : currentDate.minus(-timeToAddOrSubtract, chronoUnit);
    }
}

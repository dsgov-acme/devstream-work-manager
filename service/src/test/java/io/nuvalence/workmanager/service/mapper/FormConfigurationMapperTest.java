package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationExportModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class FormConfigurationMapperTest {

    private FormConfigurationMapper mapper;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper = FormConfigurationMapper.INSTANCE;
    }

    @Test
    void testMapConfigurationAttributes() {

        Map<String, Object> config =
                Map.of(
                        "components",
                        List.of(
                                new HashMap<>(
                                        Map.of(
                                                "key",
                                                "child-component",
                                                "components",
                                                List.of(
                                                        Map.of(
                                                                "key",
                                                                "grandchild-component",
                                                                "input",
                                                                false))))));

        Map<String, Object> mappedConfiguration = mapper.mapConfigurationAttribute(config);

        var parentComponents = assertInstanceOf(List.class, mappedConfiguration.get("components"));
        var parent = assertInstanceOf(Map.class, parentComponents.get(0));
        assertTrue((boolean) parent.get("input"));

        var childComponents = assertInstanceOf(List.class, parent.get("components"));
        var child = assertInstanceOf(Map.class, childComponents.get(0));
        assertFalse((boolean) child.get("input"));
    }

    @Test
    void formConfigurationToFormConfigurationExportModelNullConfiguration() {
        FormConfigurationExportModel formConfigurationExportModel =
                mapper.formConfigurationToFormConfigurationExportModel(null);
        assertNull(formConfigurationExportModel);
    }

    @Test
    void formConfigurationExportModelToFormConfigurationNullValues() {
        FormConfiguration formConfiguration =
                mapper.formConfigurationExportModelToFormConfiguration(null, null);
        assertNull(formConfiguration);
    }

    @Test
    void formConfigurationExportModelFieldsToFormConfigurationReturn() {
        FormConfigurationExportModel model = null;
        FormConfiguration formConfiguration = null;
        mapper.formConfigurationExportModelFieldsToFormConfiguration(model, formConfiguration);
        Assertions.assertNull(model);
        Assertions.assertNull(formConfiguration);
    }

    @Test
    void formConfigurationToFormIoValidationConfig_NestedThenFlat() throws IOException {
        var configString =
                "{ \"configuration\": {"
                        + "  \"components\": ["
                        + "{ \"key\": \"personalInformation\","
                        + "  \"components\": ["
                        + "{ \"key\": \"personalInformation.firstName\" },"
                        + "{ \"key\": \"personalInformation.middleName\" },"
                        + "{ \"key\": \"personalInformation.currentAddress.city\" },"
                        + "{ \"key\": \"personalInformation.currentAddress.country\" },"
                        + "{ \"key\": \"personalInformation.mailingAddress.city\" },"
                        + "{ \"key\": \"personalInformation.mailingAddress.country\" }"
                        + "]}]}}";
        var config = objectMapper.readValue(configString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig =
                mapper.formConfigurationToFormIoValidationConfig(config);

        assertEquals(1, normalizedConfig.getComponents().size());
        var componentsRoot = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(4, componentsRoot.size());
        assertEquals("personalInformation.firstName", componentsRoot.get(1).getKey());
        assertEquals(
                "personalInformation.currentAddress.city",
                componentsRoot.get(0).getComponents().get(0).getKey());
    }

    @Test
    void formConfigurationToFormIoValidationConfig_FlatThenNested() throws IOException {
        var configsString =
                "{ \"configuration\": {"
                        + "  \"components\": ["
                        + "{ \"key\": \"personalInformation.currentAddress\","
                        + "  \"components\": ["
                        + "{ \"key\": \"personalInformation.currentAddress.city\" },"
                        + "{ \"key\": \"personalInformation.currentAddress.country\" }"
                        + "] }]}}";
        var config = objectMapper.readValue(configsString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig =
                mapper.formConfigurationToFormIoValidationConfig(config);

        assertEquals(1, normalizedConfig.getComponents().size());
        var componentsRoot = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(1, componentsRoot.size());
        assertEquals(
                "personalInformation.currentAddress.city",
                componentsRoot.get(0).getComponents().get(0).getKey());
        assertEquals(
                "personalInformation.currentAddress.country",
                componentsRoot.get(0).getComponents().get(1).getKey());
    }

    @Test
    void formConfigurationToFormIoValidationConfig_OnlyFlat() throws IOException {

        var configsString =
                "{ \"configuration\": {"
                        + "  \"components\": ["
                        + "{ \"key\": \"personalInformation.currentAddress.city\" },"
                        + "{ \"key\": \"personalInformation.mailingAddress.city\" }"
                        + "]}}";
        var config = objectMapper.readValue(configsString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig =
                mapper.formConfigurationToFormIoValidationConfig(config);

        assertEquals(1, normalizedConfig.getComponents().size());
        var componentsRoot = normalizedConfig.getComponents().get(0).getComponents();
        assertEquals(2, componentsRoot.size());
        assertEquals(
                "personalInformation.currentAddress.city",
                componentsRoot.get(0).getComponents().get(0).getKey());
        assertEquals(
                "personalInformation.mailingAddress.city",
                componentsRoot.get(1).getComponents().get(0).getKey());
    }

    @Test
    void formConfigurationToFormIoValidationConfig_ParentMismatching() throws IOException {

        var configsString =
                "{ \"configuration\": {  \"components\": [{ \"key\":"
                        + " \"personalInformation.mailingAddress.something.city\" },{ \"key\":"
                        + " \"aGivenTester\",\"components\": [{ \"key\":"
                        + " \"personalInformation.currentAddress.something.city\" },{ \"key\":"
                        + " \"personalInformation.currentAddress\","
                        + " \"props\":{\"pattern\":\"patternToCheck\"} },{\"key\": \"zrootkey\","
                        + " \"props\": { \"pattern\": \"secondPatternToCheck\" }}] }]}}";
        var config = objectMapper.readValue(configsString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig =
                mapper.formConfigurationToFormIoValidationConfig(config);

        assertEquals(3, normalizedConfig.getComponents().size());

        assertEquals("aGivenTester", normalizedConfig.getComponents().get(0).getKey());
        assertNull(normalizedConfig.getComponents().get(0).getComponents());

        var componentsRoot = normalizedConfig.getComponents().get(1).getComponents();
        assertEquals(2, componentsRoot.size());
        assertEquals(
                "personalInformation.currentAddress.something.city",
                componentsRoot.get(0).getComponents().get(0).getComponents().get(0).getKey());

        assertEquals("patternToCheck", componentsRoot.get(0).getProps().getPattern());

        assertEquals(
                "personalInformation.mailingAddress.something.city",
                componentsRoot.get(1).getComponents().get(0).getComponents().get(0).getKey());

        assertEquals("zrootkey", normalizedConfig.getComponents().get(2).getKey());
        assertEquals(
                "secondPatternToCheck",
                normalizedConfig.getComponents().get(2).getProps().getPattern());
    }

    @Test
    void formConfigurationToFormIoValidationConfig_IgnorableElement() throws IOException {
        String configString =
                "{\"configuration\": {"
                        + "    \"components\": ["
                        + "      {\"key\": \"personalInformation\","
                        + "        \"input\": true,"
                        + "        \"components\": ["
                        + "          {\"key\": \"personalInformation.firstName\","
                        + "            \"input\": true}]},"
                        + "      {\"key\": \"documents\","
                        + "        \"input\": true,"
                        + "        \"components\": ["
                        + "          {\"key\": \"proofOfResidency\","
                        + "            \"input\": true,"
                        + "            \"components\": ["
                        + "              {\"key\": \"documents.proofOfResidency\","
                        + "                \"input\": true}]}]}]}}";

        var config = objectMapper.readValue(configString, FormConfiguration.class);

        NuvalenceFormioComponent normalizedConfig =
                mapper.formConfigurationToFormIoValidationConfig(config);

        assertEquals(3, normalizedConfig.getComponents().size());
        assertEquals(true, normalizedConfig.getComponents().get(0).isInput());
        assertEquals(true, normalizedConfig.getComponents().get(1).isInput());
        assertEquals(false, normalizedConfig.getComponents().get(2).isInput());
    }

    @Test
    void testFormConfigurationExportModelFieldsToFormConfiguration() {
        FormConfigurationExportModel model = new FormConfigurationExportModel();
        model.setId(UUID.randomUUID().toString());
        model.setTransactionDefinitionKey("transDefKey");
        model.setFormConfigurationKey("formConfigKey");
        model.setName("Test Name");
        model.setSchemaKey("schemaKey");
        model.setConfigurationSchema("{}");
        model.setCreatedBy("creator");
        model.setLastUpdatedBy("updater");
        model.setCreatedTimestamp(OffsetDateTime.now().toString());
        model.setLastUpdatedTimestamp(OffsetDateTime.now().toString());
        model.setConfiguration(Map.of());

        FormConfiguration formConfiguration = new FormConfiguration();

        mapper.formConfigurationExportModelFieldsToFormConfiguration(model, formConfiguration);

        assertEquals(UUID.fromString(model.getId()), formConfiguration.getId());
        assertEquals(
                model.getTransactionDefinitionKey(),
                formConfiguration.getTransactionDefinitionKey());
        assertEquals(model.getFormConfigurationKey(), formConfiguration.getKey());
        assertEquals(model.getName(), formConfiguration.getName());
        assertEquals(model.getSchemaKey(), formConfiguration.getSchemaKey());
        assertEquals(model.getConfigurationSchema(), formConfiguration.getConfigurationSchema());
        assertEquals(model.getCreatedBy(), formConfiguration.getCreatedBy());
        assertEquals(model.getLastUpdatedBy(), formConfiguration.getLastUpdatedBy());
        assertEquals(
                OffsetDateTime.parse(model.getCreatedTimestamp()),
                formConfiguration.getCreatedTimestamp());
        assertEquals(
                OffsetDateTime.parse(model.getLastUpdatedTimestamp()),
                formConfiguration.getLastUpdatedTimestamp());
        assertEquals(model.getConfiguration(), formConfiguration.getConfiguration());
    }
}

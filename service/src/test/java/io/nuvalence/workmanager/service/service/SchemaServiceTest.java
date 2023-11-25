package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.VersionedEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentProcessingConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.mapper.AttributeConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.InvalidRegexPatternException;
import io.nuvalence.workmanager.service.models.SchemaFilters;
import io.nuvalence.workmanager.service.repository.SchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class SchemaServiceTest {

    private final ObjectMapper objectMapper = SpringConfig.getMapper();
    @Mock private SchemaRepository schemaRepository;
    private SchemaService schemaService;
    private DynamicSchemaMapper mapper;

    @BeforeEach
    void setup() {
        mapper = Mappers.getMapper(DynamicSchemaMapper.class);
        mapper.setObjectMapper(objectMapper);
        mapper.setAttributeConfigurationMapper(
                Mappers.getMapper(AttributeConfigurationMapper.class));
        schemaService = new SchemaService(schemaRepository, mapper);
    }

    @Test
    void testGetAllRelatedSchemas_SchemaNotFound() {
        when(schemaRepository.findByKey("rootKey")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> schemaService.getAllRelatedSchemas("rootKey"));
    }

    @Test
    void getSchemaByKeyReturnsSchemaWhenFound() throws JsonProcessingException {
        // Arrange
        final Schema schema =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .name("testschema")
                        .key("testschemaKey")
                        .property("attribute", String.class)
                        .build();
        final SchemaRow row = mapper.schemaToSchemaRow(schema);
        when(schemaRepository.findByKey(schema.getKey())).thenReturn(Optional.ofNullable(row));

        // Act and Assert
        assertEquals(Optional.of(schema), schemaService.getSchemaByKey(schema.getKey()));
    }

    @Test
    void getSchemaByKeyReturnsEmptyOptionalWhenSchemaNotFound() throws JsonProcessingException {
        // Arrange
        when(schemaRepository.findByKey("testschema")).thenReturn(Optional.empty());

        // Act and Assert
        assertEquals(Optional.empty(), schemaService.getSchemaByKey("testschema"));
    }

    @Test
    void getSchemaByKeyThrowsRuntimeExceptionWhenJsonCannotBeParsed() {
        // Arrange
        final SchemaRow row =
                SchemaRow.builder()
                        .name("testschema")
                        .key("testschemaKey")
                        .schemaJson("Not JSON")
                        .build();
        when(schemaRepository.findByKey(row.getKey())).thenReturn(Optional.of(row));

        String rowKey = row.getKey();
        // Act and Assert
        assertThrows(RuntimeException.class, () -> schemaService.getSchemaByKey(rowKey));
    }

    @Test
    void getSchemasByPartialNameMatchReturnsFoundSchemas() throws JsonProcessingException {
        // Arrange
        final Schema schema1 =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .key("testschemaKey")
                        .name("testschema")
                        .property("attribute", String.class)
                        .build();
        final Schema schema2 =
                Schema.builder()
                        .id(UUID.fromString("e8b75bd3-52a9-4ad0-80c6-2eb2886c3790"))
                        .key("mytestKey")
                        .name("mytest")
                        .property("attribute", String.class)
                        .build();
        final SchemaRow row1 = mapper.schemaToSchemaRow(schema1);
        final SchemaRow row2 = mapper.schemaToSchemaRow(schema2);
        when(schemaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row1, row2)));

        // Act and Assert
        assertEquals(
                List.of(schema1, schema2),
                schemaService
                        .getSchemasByFilters(new SchemaFilters("test", null, "name", "DES", 0, 50))
                        .toList());
    }

    @Test
    void getSchemasBySchemaKey() throws JsonProcessingException {
        // Arrange
        final Schema schema1 =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .key("testschemaKey")
                        .name("testschema")
                        .property("attribute", String.class)
                        .build();
        final SchemaRow row1 = mapper.schemaToSchemaRow(schema1);
        when(schemaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row1)));

        // Act and Assert
        assertEquals(
                List.of(schema1),
                schemaService
                        .getSchemasByFilters(
                                new SchemaFilters(null, "testschemaKey", "name", "DES", 0, 50))
                        .toList());
    }

    @Test
    void getSchemaParents() throws JsonProcessingException {
        // Arrange
        final Schema schema1 =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .key("parentSchemaKey")
                        .name("parentSchema")
                        .property("attribute", String.class)
                        .build();
        final SchemaRow row1 = mapper.schemaToSchemaRow(schema1);

        // Mock the repository call to find parent schemas
        when(schemaRepository.getSchemaParents("childSchemaKey")).thenReturn(List.of(row1));

        // Act
        List<Schema> parentSchemas = schemaService.getSchemaParents("childSchemaKey");

        // Assert
        assertEquals(List.of(schema1), parentSchemas);
    }

    @Test
    void getSchemasByPartialNameMatchReturnsAllSchemasWhenQueryisNull()
            throws JsonProcessingException {
        // Arrange
        final Schema schema1 =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .key("testschemaKey")
                        .name("testschema")
                        .property("attribute", String.class)
                        .build();
        final Schema schema2 =
                Schema.builder()
                        .id(UUID.fromString("e8b75bd3-52a9-4ad0-80c6-2eb2886c3790"))
                        .key("mytestKey")
                        .name("mytest")
                        .property("attribute", String.class)
                        .build();
        final SchemaRow row1 = mapper.schemaToSchemaRow(schema1);
        final SchemaRow row2 = mapper.schemaToSchemaRow(schema2);
        when(schemaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row1, row2)));

        // Act and Assert
        assertEquals(
                List.of(schema1, schema2),
                schemaService
                        .getSchemasByFilters(new SchemaFilters(null, null, "name", "DES", 0, 50))
                        .toList());
    }

    @Test
    void getSchemasByPartialNameMatchThrowsRuntimeExceptionWhenJsonCannotBeParsed()
            throws JsonProcessingException {
        // Arrange
        final SchemaRow row = SchemaRow.builder().name("testschema").schemaJson("Not JSON").build();
        when(schemaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        var filters = new SchemaFilters("test", null, "name", "DES", 0, 50);

        // Act and Assert
        assertThrows(RuntimeException.class, () -> schemaService.getSchemasByFilters(filters));
    }

    @Test
    void saveSchemaDoesNotThrowExceptionIfSaveSuccessful() throws JsonProcessingException {
        // Arrange
        final String childSchemaKey = "ChildSchemaKey";

        final Schema childSchema =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .key(childSchemaKey)
                        .name("testschema")
                        .property("attribute", String.class)
                        .build();

        String parentSchemaString =
                "{\n"
                        + "  \"key\": \"ParentSchema\",\n"
                        + "  \"name\": \"Parent schema\",\n"
                        + "  \"attributes\": [\n"
                        + "    {\n"
                        + "      \"name\": \"test\",\n"
                        + "      \"type\": \"DynamicEntity\",\n"
                        + "      \"entitySchema\": \""
                        + childSchemaKey
                        + "\",\n"
                        + "      \"attributeConfigurations\": []\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}";
        SchemaJson parentSchemaJson = objectMapper.readValue(parentSchemaString, SchemaJson.class);
        Schema parentSchema = mapper.schemaJsonToSchema(parentSchemaJson, UUID.randomUUID());

        final SchemaRow row = mapper.schemaToSchemaRow(parentSchema);
        Mockito.lenient().when(schemaRepository.save(any(SchemaRow.class))).thenReturn(row);
        Mockito.lenient()
                .when(schemaRepository.findByKey(childSchemaKey))
                .thenReturn(Optional.of(mapper.schemaToSchemaRow(childSchema)));

        // Act and Assert
        assertDoesNotThrow(() -> schemaService.saveSchema(parentSchema));
    }

    @Test
    void saveSchema_childNotFound() throws JsonProcessingException {
        // Arrange
        final String childSchemaKey = "ChildSchemaKey";

        String parentSchemaString =
                "{\n"
                        + "  \"key\": \"ParentSchema\",\n"
                        + "  \"name\": \"Parent schema\",\n"
                        + "  \"attributes\": [\n"
                        + "    {\n"
                        + "      \"name\": \"test\",\n"
                        + "      \"type\": \"DynamicEntity\",\n"
                        + "      \"entitySchema\": \""
                        + childSchemaKey
                        + "\",\n"
                        + "      \"attributeConfigurations\": []\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}";
        SchemaJson parentSchemaJson = objectMapper.readValue(parentSchemaString, SchemaJson.class);
        Schema parentSchema = mapper.schemaJsonToSchema(parentSchemaJson, UUID.randomUUID());

        final SchemaRow row = mapper.schemaToSchemaRow(parentSchema);
        Mockito.lenient().when(schemaRepository.save(any(SchemaRow.class))).thenReturn(row);
        Mockito.lenient()
                .when(schemaRepository.findByKey(childSchemaKey))
                .thenReturn(Optional.empty());

        // Act and Assert

        Exception exception =
                assertThrows(NotFoundException.class, () -> schemaService.saveSchema(parentSchema));
        String expectedMessage = "Child schema not found for key: " + childSchemaKey;
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void saveSchema_childIsParent() throws JsonProcessingException {
        // Arrange
        final String childSchemaKey = "ChildSchemaKey";

        SchemaRow schemaRow =
                SchemaRow.builder()
                        .id(UUID.fromString("ba8ef564-8947-11ee-b9d1-0242ac120002"))
                        .name("test")
                        .key(childSchemaKey)
                        .schemaJson("{\"key\": \"" + childSchemaKey + "\"}")
                        .build();

        String parentSchemaString =
                "{\n"
                        + "  \"key\": \"ParentSchema\",\n"
                        + "  \"name\": \"Parent schema\",\n"
                        + "  \"attributes\": [\n"
                        + "    {\n"
                        + "      \"name\": \"test\",\n"
                        + "      \"type\": \"DynamicEntity\",\n"
                        + "      \"entitySchema\": \""
                        + childSchemaKey
                        + "\",\n"
                        + "      \"attributeConfigurations\": []\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}";
        SchemaJson parentSchemaJson = objectMapper.readValue(parentSchemaString, SchemaJson.class);
        Schema parentSchema = mapper.schemaJsonToSchema(parentSchemaJson, UUID.randomUUID());

        final SchemaRow row = mapper.schemaToSchemaRow(parentSchema);
        Mockito.lenient().when(schemaRepository.save(any(SchemaRow.class))).thenReturn(row);
        Mockito.lenient()
                .when(schemaRepository.findByKey(childSchemaKey))
                .thenReturn(Optional.empty());
        Mockito.lenient()
                .when(schemaRepository.getSchemaParents(anyString()))
                .thenReturn(List.of(schemaRow));

        // Act and Assert

        Exception exception =
                assertThrows(
                        ProvidedDataException.class, () -> schemaService.saveSchema(parentSchema));
        String expectedMessage =
                "Schema ChildSchemaKey is a parent of ParentSchema. A sub-schema cannot be a parent"
                        + " of the current schema.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void saveSchema_childIsCurrentSchema() throws JsonProcessingException {
        // Arrange
        final String schemaKey = "TheSchema";

        SchemaRow schemaRow =
                SchemaRow.builder()
                        .id(UUID.fromString("ba8ef564-8947-11ee-b9d1-0242ac120002"))
                        .name("test")
                        .key(schemaKey)
                        .schemaJson("{\"key\": \"" + schemaKey + "\"}")
                        .build();

        String parentSchemaString =
                "{\n"
                        + "  \"key\": \""
                        + schemaKey
                        + "\",\n"
                        + "  \"name\": \"Parent schema\",\n"
                        + "  \"attributes\": [\n"
                        + "    {\n"
                        + "      \"name\": \"test\",\n"
                        + "      \"type\": \"DynamicEntity\",\n"
                        + "      \"entitySchema\": \""
                        + schemaKey
                        + "\",\n"
                        + "      \"attributeConfigurations\": []\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}";
        SchemaJson parentSchemaJson = objectMapper.readValue(parentSchemaString, SchemaJson.class);
        Schema parentSchema = mapper.schemaJsonToSchema(parentSchemaJson, UUID.randomUUID());

        final SchemaRow row = mapper.schemaToSchemaRow(parentSchema);
        Mockito.lenient().when(schemaRepository.save(any(SchemaRow.class))).thenReturn(row);
        Mockito.lenient().when(schemaRepository.findByKey(schemaKey)).thenReturn(Optional.empty());
        Mockito.lenient()
                .when(schemaRepository.getSchemaParents(anyString()))
                .thenReturn(List.of(schemaRow));

        // Act and Assert

        Exception exception =
                assertThrows(
                        ProvidedDataException.class, () -> schemaService.saveSchema(parentSchema));
        String expectedMessage = "The " + schemaKey + " schema cannot be a subSchema of itself.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void saveSchemaDoesNotSaveIfInvalidKey() {
        // Arrange
        final Schema schema =
                Schema.builder().key("inv@alid-schema").property("attribute", String.class).build();

        Exception exception =
                assertThrows(RuntimeException.class, () -> schemaService.saveSchema(schema));
        Exception expectedException =
                new InvalidRegexPatternException(
                        schema.getKey(),
                        VersionedEntity.Constants.VALID_FILE_NAME_REGEX_PATTERN,
                        "schema");
        assertTrue(exception.getMessage().contains(expectedException.getMessage()));
    }

    @Test
    void getDocumentProcessorsInASchemaPath_success() throws JsonProcessingException {
        // Arrange
        DocumentProcessingConfiguration testProcessor = new DocumentProcessingConfiguration();
        testProcessor.setProcessorId("testProcessor");

        final Schema childSchema =
                Schema.builder()
                        .key("childSchemaKey")
                        .name("child schema")
                        .property("attribute", String.class)
                        .property("document1", Document.class)
                        .attributeConfiguration("document1", testProcessor)
                        .build();

        final Schema parentSchema =
                Schema.builder()
                        .key("parentSchemaKey")
                        .name("parent schema")
                        .property("attribute", String.class)
                        .property("childSchema", DynamicEntity.class)
                        .relatedSchemas(
                                new HashMap<>() {
                                    {
                                        put("childSchema", "childSchemaKey");
                                    }
                                })
                        .build();

        String documentPath = "childSchema.document1";

        when(schemaRepository.findByKey("childSchemaKey"))
                .thenReturn(Optional.of(mapper.schemaToSchemaRow(childSchema)));

        List<String> processorsNames =
                schemaService.getDocumentProcessorsInASchemaPath(documentPath, parentSchema);

        assertEquals(1, processorsNames.size());
        assertEquals("testProcessor", processorsNames.get(0));
    }

    @Test
    void getDocumentProcessorsInASchemaPath_schemaNotFound() {
        // Arrange
        final Schema parentSchema =
                Schema.builder()
                        .key("parentSchemaKey")
                        .name("parent schema")
                        .property("attribute", String.class)
                        .property("childSchema", DynamicEntity.class)
                        .relatedSchemas(
                                new HashMap<>() {
                                    {
                                        put("childSchema", "childSchemaKey");
                                    }
                                })
                        .build();

        String documentPath = "childSchema.document1";

        when(schemaRepository.findByKey("childSchemaKey")).thenReturn(Optional.empty());

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () ->
                                schemaService.getDocumentProcessorsInASchemaPath(
                                        documentPath, parentSchema));

        assertEquals("Schema not found: childSchemaKey", thrownException.getMessage());
    }

    @Test
    void getDocumentProcessorsInASchemaPath_wrongPath() {
        // Arrange
        final Schema parentSchema =
                Schema.builder()
                        .key("parentSchemaKey")
                        .name("parent schema")
                        .property("attribute", String.class)
                        .property("childSchema", DynamicEntity.class)
                        .relatedSchemas(
                                new HashMap<>() {
                                    {
                                        put("childSchema", "childSchemaKey");
                                    }
                                })
                        .build();

        String documentPath = "anyPath";

        ProvidedDataException thrownException =
                assertThrows(
                        ProvidedDataException.class,
                        () ->
                                schemaService.getDocumentProcessorsInASchemaPath(
                                        documentPath, parentSchema));

        assertEquals("Wrong data path", thrownException.getMessage());
    }

    @Test
    void getSchemaById_ValidId_ReturnsSchema() throws JsonProcessingException {
        SchemaRow schemaRow = createSchemaRow();
        when(schemaRepository.findById(schemaRow.getId())).thenReturn(Optional.of(schemaRow));

        Schema expectedSchema = mapper.schemaRowToSchema(schemaRow);

        Optional<Schema> result = schemaService.getSchemaById(schemaRow.getId());
        assertTrue(result.isPresent());
        assertEquals(expectedSchema, result.get());
        verify(schemaRepository).findById(schemaRow.getId());
    }

    @Test
    void getSchemaById_InvalidSchemaJson_ThrowsRuntimeException() {
        SchemaRow schemaRow = createSchemaRow();
        schemaRow.setSchemaJson("invalidJson");
        when(schemaRepository.findById(schemaRow.getId())).thenReturn(Optional.of(schemaRow));

        assertThrowsRuntimeException(schemaRow.getId());

        verify(schemaRepository).findById(schemaRow.getId());
    }

    private void assertThrowsRuntimeException(UUID schemaId) {
        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> schemaService.getSchemaById(schemaId));

        assertEquals("Unable to parse schema JSON stored in database.", exception.getMessage());
    }

    @Test
    void saveSchemaThrowRuntimeException() throws JsonProcessingException {
        final Schema schema =
                Schema.builder()
                        .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                        .key("testschemaKey")
                        .name("testschema")
                        .property("attribute", String.class)
                        .build();
        final SchemaRow row = mapper.schemaToSchemaRow(schema);
        row.setSchemaJson("invalidJson"); // set invalid json to throw exception
        Mockito.lenient().when(schemaRepository.save(any(SchemaRow.class))).thenReturn(row);

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> schemaService.saveSchema(schema));

        assertEquals("Unable to marshall schema to JSON.", exception.getMessage());
    }

    private SchemaRow createSchemaRow() {
        return SchemaRow.builder()
                .id(UUID.fromString("d7bb1296-e931-11ed-a05b-0242ac120003"))
                .key("testschemaKey")
                .name("testschema")
                .schemaJson("{\"attribute\": \"string\"}")
                .build();
    }
}

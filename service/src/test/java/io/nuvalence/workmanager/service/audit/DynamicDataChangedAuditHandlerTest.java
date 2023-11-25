package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.auditservice.client.ApiException;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaAttributeJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.SchemaRow;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.models.TransactionUpdateRequest;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.repository.SchemaRepository;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionAuditEventService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.service.TransactionTaskService;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import org.apache.commons.beanutils.DynaProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DynamicDataChangedAuditHandlerTest {

    private final JsonFileLoader jsonLoader = new JsonFileLoader();
    private String nestedSchemaKey = "basicNestedSchema";
    private final ObjectMapper objectMapper = SpringConfig.getMapper();
    private String userId = "2b394536-16a1-11ee-be56-0242ac120002";
    private OffsetDateTime eventTimestamp;
    private Transaction savedTransaction;
    private Map<String, Object> transactionData;

    @MockBean private AuthorizationHandler authorizationHandler;
    @MockBean private RequestContextTimestamp requestContextTimestamp;
    @MockBean private TransactionAuditEventService transactionAuditEventService;
    @MockBean private TransactionTaskService transactionTaskService;

    @Autowired private MockMvc mockMvc;
    @Autowired private DynamicSchemaMapper schemaMapper;
    @Autowired private SchemaService schemaService;
    @Autowired private TransactionDefinitionService transactionDefinitionService;
    @Autowired private TransactionService transactionService;
    @Autowired private EntityMapper entityMapper;
    @SpyBean private SchemaRepository schemaRepository;

    @BeforeEach
    void setup() throws IOException, MissingSchemaException, MissingTransactionException {

        SchemaRow schemaRow =
                SchemaRow.builder()
                        .id(UUID.fromString("ba8ef564-8947-11ee-b9d1-0242ac120002"))
                        .name("Parent Schema")
                        .key("ParentSchema")
                        .schemaJson("{\"key\": \"ParentSchema\"}")
                        .build();
        doReturn(List.of(schemaRow)).when(schemaRepository).getSchemaParents(anyString());

        // Set authenticated user
        Authentication authentication =
                UserToken.builder()
                        .applicationUserId(userId)
                        .providerUserId(userId)
                        .authorities(
                                Stream.of("transaction-admin", "transaction-config-admin")
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList()))
                        .build();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Ensure that all authorization checks pass.
        Mockito.when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        Mockito.when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);

        eventTimestamp = OffsetDateTime.now();
        Mockito.when(requestContextTimestamp.getCurrentTimestamp()).thenReturn(eventTimestamp);

        Mockito.when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Collections.emptyList());

        setInitialDataState();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setInitialDataState()
            throws IOException, MissingSchemaException, MissingTransactionException {
        Schema schema = createSchema();
        TransactionDefinition transactionDefinition = createTransactionDefinition(schema);
        Transaction transaction = transactionService.createTransaction(transactionDefinition);

        transactionData = jsonLoader.loadConfigMap("/basicTransactionData.json");
        DynamicEntity dynaEntity = entityMapper.convertGenericMapToEntity(schema, transactionData);

        transaction.setData(dynaEntity);
        savedTransaction =
                transactionService.updateTransactionFromPartialUpdate(
                        transaction, schema.getAttributeConfigurations());
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_firstNameAndOfficeCityChanged_StringData() throws Exception {
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("firstName", "Thomas");
        Map<String, Object> officeInfo =
                (Map<String, Object>) transactionRequestData.get("officeInfo");
        officeInfo.put("city", "NY");
        transactionRequestData.put("officeInfo", officeInfo);

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setData(transactionRequestData);
        final String postBody =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        .writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.firstName").value("Thomas"))
                .andExpect(jsonPath("$.data.officeInfo.city").value("NY"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();

        Assertions.assertEquals(2, oldStateProperties.size());
        Assertions.assertEquals(2, newStateProperties.size());

        Assertions.assertTrue(oldStateProperties.containsKey("firstName"));
        Assertions.assertEquals("myFirstName", oldStateProperties.get("firstName"));

        Assertions.assertTrue(oldStateProperties.containsKey("officeInfo.city"));
        Assertions.assertEquals("myCity", oldStateProperties.get("officeInfo.city"));

        Assertions.assertTrue(newStateProperties.containsKey("firstName"));
        Assertions.assertEquals("Thomas", newStateProperties.get("firstName"));

        Assertions.assertTrue(newStateProperties.containsKey("officeInfo.city"));
        Assertions.assertEquals("NY", newStateProperties.get("officeInfo.city"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_documentChanged_DocumentData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        Document document =
                Document.builder()
                        .documentId(UUID.fromString("dff856ee-15dc-11ee-be56-0242ac120002"))
                        .build();
        transactionRequestData.put("document", document);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(
                        jsonPath("$.data.document.documentId")
                                .value("dff856ee-15dc-11ee-be56-0242ac120002"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();

        Assertions.assertEquals(1, oldStateProperties.size());
        Assertions.assertEquals(1, newStateProperties.size());

        Assertions.assertTrue(oldStateProperties.containsKey("document"));
        Assertions.assertEquals(
                "f84b20e8-7a64-431f-ad94-440ca0c4b7c1", oldStateProperties.get("document"));

        Assertions.assertTrue(newStateProperties.containsKey("document"));
        Assertions.assertEquals(
                "dff856ee-15dc-11ee-be56-0242ac120002", newStateProperties.get("document"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_ageChanged_numericData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("age", 50);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.age").value("50"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();

        Assertions.assertEquals(1, oldStateProperties.size());
        Assertions.assertEquals(1, newStateProperties.size());

        Assertions.assertTrue(oldStateProperties.containsKey("age"));
        Assertions.assertEquals("30", oldStateProperties.get("age"));

        Assertions.assertTrue(newStateProperties.containsKey("age"));
        Assertions.assertEquals("50", newStateProperties.get("age"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DirtiesContext
    void testPublishAuditEvent_ApiException(CapturedOutput output) throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("age", 50);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);
        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        doThrow(ApiException.class)
                .when(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.age").value("50"));

        assertTrue(
                output.getOut()
                        .contains(
                                "ApiException occurred when recording audit event for dynamic data"
                                        + " change in transaction "
                                        + savedTransaction.getId()));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DirtiesContext
    void testPublishAuditEvent_Exception(CapturedOutput output) throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("age", 50);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);
        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.age").value("50"));

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " dynamic data change in transaction "
                                        + savedTransaction.getId()));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_dateOfBirthChanged_dateData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("dateOfBirth", "1963-12-21");

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1963-12-21"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();

        Assertions.assertEquals(1, oldStateProperties.size());
        Assertions.assertEquals(1, newStateProperties.size());

        Assertions.assertTrue(oldStateProperties.containsKey("dateOfBirth"));
        Assertions.assertEquals("1993-12-21", oldStateProperties.get("dateOfBirth"));

        Assertions.assertTrue(newStateProperties.containsKey("dateOfBirth"));
        Assertions.assertEquals("1963-12-21", newStateProperties.get("dateOfBirth"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_isMailingAddressNeededChanged_booleanData() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        Map<String, Object> transactionRequestData = new HashMap<>();
        transactionRequestData.putAll(transactionData);

        // modify dynamic data
        transactionRequestData.put("isMailingAddressNeeded", false);

        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("complete", "true")
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.isMailingAddressNeeded").value("false"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();

        Assertions.assertEquals(1, oldStateProperties.size());
        Assertions.assertEquals(1, newStateProperties.size());

        Assertions.assertTrue(oldStateProperties.containsKey("isMailingAddressNeeded"));
        Assertions.assertEquals("true", oldStateProperties.get("isMailingAddressNeeded"));

        Assertions.assertTrue(newStateProperties.containsKey("isMailingAddressNeeded"));
        Assertions.assertEquals("false", newStateProperties.get("isMailingAddressNeeded"));
    }

    @Test
    @DirtiesContext
    void testPublishAuditEvent_dynamicDataDidNotChange() throws Exception {

        TransactionUpdateRequest request = new TransactionUpdateRequest();

        request.setData(transactionData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));
        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()));

        verify(transactionAuditEventService, never())
                .postStateChangeEvent(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(Map.class),
                        any(Map.class),
                        any(),
                        any());
    }

    @Test
    @DirtiesContext
    void testRemoveComputedFields() throws Exception {
        Map<String, Object> transactionRequestData = new HashMap<>(transactionData);

        // modify dynamic data
        transactionRequestData.put("firstName", "Thomas");
        Map<String, Object> officeInfo =
                (Map<String, Object>) transactionRequestData.get("officeInfo");
        officeInfo.put("city", "NY");
        transactionRequestData.put("officeInfo", officeInfo);

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setData(transactionRequestData);
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        mockMvc.perform(
                        put("/api/v1/transactions/" + savedTransaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("formStepKey", "formStepKey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId().toString()))
                .andExpect(jsonPath("$.data.firstName").value("Thomas"))
                .andExpect(jsonPath("$.data.officeInfo.city").value("NY"))
                .andExpect(jsonPath("$.data.officeInfo.fullAddress").value("NY myOfficeAddress"));

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transactionAuditEventService)
                .postStateChangeEvent(
                        eq(userId),
                        eq(userId),
                        anyString(),
                        eq(savedTransaction.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        any(),
                        eq(AuditActivityType.TRANSACTION_DATA_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();

        Assertions.assertEquals(2, oldStateProperties.size());
        Assertions.assertEquals(2, newStateProperties.size());

        Assertions.assertFalse(oldStateProperties.containsKey("officeInfo.fullAddress"));
        Assertions.assertFalse(newStateProperties.containsKey("officeInfo.fullAddress"));
        Assertions.assertTrue(oldStateProperties.containsKey("firstName"));
        Assertions.assertEquals("myFirstName", oldStateProperties.get("firstName"));

        Assertions.assertTrue(oldStateProperties.containsKey("officeInfo.city"));
        Assertions.assertEquals("myCity", oldStateProperties.get("officeInfo.city"));

        Assertions.assertTrue(newStateProperties.containsKey("firstName"));
        Assertions.assertEquals("Thomas", newStateProperties.get("firstName"));

        Assertions.assertTrue(newStateProperties.containsKey("officeInfo.city"));
        Assertions.assertEquals("NY", newStateProperties.get("officeInfo.city"));
    }

    private TransactionDefinition createTransactionDefinition(Schema schema) {
        TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("BasicTransactionDefinition")
                        .name("Basic transaction definition")
                        .processDefinitionKey("test_process")
                        .schemaKey(schema.getKey())
                        .defaultStatus("Draft")
                        .category("application")
                        .defaultFormConfigurationKey("defaultFormConfigurationKey")
                        .build();

        return transactionDefinitionService.saveTransactionDefinition(transactionDefinition);
    }

    private Schema createSchema() throws IOException {
        List<DynaProperty> properties = new ArrayList<>();
        properties.add(new DynaProperty("city", String.class));
        properties.add(new DynaProperty("address", String.class));

        Schema nestedSchema =
                Schema.builder()
                        .id(UUID.randomUUID())
                        .key(nestedSchemaKey)
                        .name(nestedSchemaKey)
                        .properties(properties)
                        .computedProperty(
                                "fullAddress", String.class, "#concat(\" \", city, address)")
                        .build();
        schemaService.saveSchema(nestedSchema);

        String schemaString = jsonLoader.loadConfigString("/basicSchema.json");

        SchemaAttributeJson nestedAttribute = new SchemaAttributeJson();
        nestedAttribute.setName("officeInfo");
        nestedAttribute.setType("DynamicEntity");
        nestedAttribute.setEntitySchema(nestedSchemaKey);

        SchemaJson schemaJson = objectMapper.readValue(schemaString, SchemaJson.class);

        schemaJson.getAttributes().add(nestedAttribute);
        Schema schema = schemaMapper.schemaJsonToSchema(schemaJson, UUID.randomUUID());
        return schemaService.saveSchema(schema);
    }
}

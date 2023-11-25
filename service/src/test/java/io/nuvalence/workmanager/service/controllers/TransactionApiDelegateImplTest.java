package io.nuvalence.workmanager.service.controllers;

import static io.nuvalence.workmanager.service.utils.testutils.TransactionUtils.createCustomerProvidedDocument;
import static io.nuvalence.workmanager.service.utils.testutils.TransactionUtils.createCustomerProvidedDocumentModel;
import static io.nuvalence.workmanager.service.utils.testutils.TransactionUtils.createCustomerProvidedDocumentModelRequest;
import static io.nuvalence.workmanager.service.utils.testutils.TransactionUtils.getCommonTransactionBuilder;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.config.exceptions.NuvalenceFormioValidationException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExMessage;
import io.nuvalence.workmanager.service.documentmanagementapi.DocumentManagementClient;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.CustomerProvidedDocument;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReason;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.RejectionReasonType;
import io.nuvalence.workmanager.service.domain.customerprovideddocument.ReviewStatus;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentProcessingConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.dynamicschema.attributes.Document;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.MissingTaskException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionDefinitionException;
import io.nuvalence.workmanager.service.domain.transaction.MissingTransactionException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLink;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkNotAllowedException;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelRequest;
import io.nuvalence.workmanager.service.generated.models.CustomerProvidedDocumentModelResponse;
import io.nuvalence.workmanager.service.generated.models.InitiateDocumentProcessingModelRequest;
import io.nuvalence.workmanager.service.generated.models.LinkedTransaction;
import io.nuvalence.workmanager.service.generated.models.TransactionCountByStatusModel;
import io.nuvalence.workmanager.service.generated.models.TransactionCreationRequest;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkModificationRequest;
import io.nuvalence.workmanager.service.generated.models.TransactionUpdateRequest;
import io.nuvalence.workmanager.service.mapper.MissingSchemaException;
import io.nuvalence.workmanager.service.mapper.OffsetDateTimeMapper;
import io.nuvalence.workmanager.service.models.SearchTransactionsFilters;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionLinkService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.service.TransactionTaskService;
import io.nuvalence.workmanager.service.service.WorkflowTasksService;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.User;
import io.nuvalence.workmanager.service.utils.JsonFileLoader;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(
        authorities = {
            "wm:transaction-submitter",
            "wm:transaction-admin",
            "wm:transaction-config-admin"
        })
@ExtendWith(OutputCaptureExtension.class)
class TransactionApiDelegateImplTest {

    private static final String TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE =
            "{\"messages\":[\"Field transactionDefinitionKey is invalid. Validation pattern that"
                    + " should be followed: not empty with no special characters\"]}";

    private static final String TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"transactionDefinitionSetKey: must match \\\"^[a-zA-Z0-9]+$\\\"\"]}";

    private static final String FORM_STEP_KEY_INVALID_MESSAGE_CONSTRAINT =
            "{\"messages\":[\"formStepKey: must match \\\"^[a-zA-Z0-9]+$\\\"\"]}";

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private TransactionService transactionService;
    @MockBean private SchemaService schemaService;
    @MockBean private TransactionDefinitionService transactionDefinitionService;
    @MockBean private TransactionLinkService transactionLinkService;
    @MockBean private FormConfigurationService formConfigurationService;
    @MockBean private TransactionTaskService transactionTaskService;
    @MockBean private UserManagementService userManagementService;

    @MockBean private DocumentManagementClient documentManagementClient;

    @MockBean private ExecutorService executorService;

    private final JsonFileLoader jsonLoader = new JsonFileLoader();

    @MockBean private WorkflowTasksService workflowTasksService;

    @Autowired private TransactionsApiDelegateImpl transactionsApiDelegateImpl;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        this.objectMapper = SpringConfig.getMapper();
        when(securityContext.getAuthentication())
                .thenReturn(
                        UserToken.builder()
                                .providerUserId("EXT000123")
                                .applicationUserId("APP000123")
                                .authorities(
                                        Stream.of("transaction-admin", "transaction-config-admin")
                                                .map(SimpleGrantedAuthority::new)
                                                .collect(Collectors.toList()))
                                .build());

        // Ensure that all authorization checks pass.
        Mockito.when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        Mockito.when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        Mockito.when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
        Mockito.when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Collections.emptyList());
        Mockito.when(executorService.submit(any(Callable.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        ReflectionTestUtils.setField(
                transactionsApiDelegateImpl, "executorService", executorService);
    }

    @Test
    void getLinkedTransactionsByIdTest() throws Exception {
        UUID linkedTransactionId = UUID.randomUUID();

        LinkedTransaction linkedTransaction = new LinkedTransaction();
        linkedTransaction.setLinkedTransactionId(UUID.randomUUID());

        when(transactionLinkService.getLinkedTransactionsById(linkedTransactionId))
                .thenReturn(List.of(linkedTransaction));

        mockMvc.perform(get("/api/v1/transactions/" + linkedTransactionId + "/links"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(
                        jsonPath("$[0].linkedTransactionId")
                                .value(linkedTransaction.getLinkedTransactionId().toString()));
    }

    @Test
    void getLinkedTransactionsByIdUnauthorized() throws Exception {
        UUID linkedTransactionId = UUID.randomUUID();

        Mockito.when(authorizationHandler.isAllowed("view", "transaction_link")).thenReturn(false);

        mockMvc.perform(get("/api/v1/transactions/" + linkedTransactionId + "/links"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAvailableStatusesTest() throws Exception {
        List<String> statuses = List.of("one", "two");

        when(workflowTasksService.getCamundaStatuses(any(), any(), any())).thenReturn(statuses);

        mockMvc.perform(get("/api/v1/transactions/statuses").queryParam("type", "public"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasItems("one", "two")));
    }

    @Test
    void getAvailableStatusesWithoutPermissions() throws Exception {
        when(authorizationHandler.isAllowed("view", "transaction_config")).thenReturn(false);

        mockMvc.perform(get("/api/v1/transactions/statuses").queryParam("type", "public"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionActiveForms() throws Exception {
        final Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();

        final FormConfiguration formConfiguration = new FormConfiguration();

        Map<String, FormConfiguration> formConfigurationMap = new HashMap<>();
        formConfigurationMap.put("testKey", formConfiguration);

        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        when(formConfigurationService.getActiveFormConfiguration(any(), any()))
                .thenReturn(formConfigurationMap);

        mockMvc.perform(
                        get(
                                "/api/v1/transactions/"
                                        + transaction.getId().toString()
                                        + "/active-forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testKey").exists());
    }

    @Test
    void getTransactionActiveFormsWithoutPermissions() throws Exception {
        when(authorizationHandler.isAllowed("view", FormConfiguration.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/transactions/" + UUID.randomUUID() + "/active-forms"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransaction() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .externalId("test")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(new DynamicEntity(Schema.builder().build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);
        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        // Act and Assert
        mockMvc.perform(get("/api/v1/transactions/" + transaction.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getTransactionByExternalId() throws Exception {
        final Transaction userTransaction1 = getCommonTransactionBuilder().build();
        mockPrivateTransactionDefinition(userTransaction1);

        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder().externalId("test").build();

        final Page<Transaction> pagedResults = new PageImpl<>(List.of(userTransaction1));

        Mockito.when(transactionService.getFilteredTransactions(any())).thenReturn(pagedResults);

        // Act and Assert
        mockMvc.perform(get("/api/v1/transactions?" + "externalid=" + filters.getExternalId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$..externalId").value(filters.getExternalId()));
    }

    @Test
    void getTransaction404() throws Exception {
        // Arrange
        final UUID transactionId = UUID.randomUUID();
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.empty());

        // Act and Assert
        mockMvc.perform(get("/api/v1/transactions/" + transactionId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionMissingAuthorization() throws Exception {
        final UUID transactionId = UUID.randomUUID();
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/v1/my-transactions")).andExpect(status().isForbidden());
    }

    @Test
    void getTransactionsSuccess() throws Exception {
        // Arrange
        final Transaction userTransaction1 = getCommonTransactionBuilder().build();
        mockPrivateTransactionDefinition(userTransaction1);

        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .priority(List.of(TransactionPriority.MEDIUM))
                        .status(List.of("new"))
                        .assignedTo(List.of(UUID.randomUUID().toString()))
                        .subjectUserId("subject")
                        .externalId("Test")
                        .sortBy("id")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(25)
                        .build();

        final Page<Transaction> pagedResults = new PageImpl<>(List.of(userTransaction1));

        Mockito.when(transactionDefinitionService.getTransactionDefinitionsBySetKey("dummy"))
                .thenReturn(List.of(TransactionDefinition.builder().key("dummy").build()));
        Mockito.when(transactionService.getFilteredTransactions(any())).thenReturn(pagedResults);

        // Act and Assert
        mockMvc.perform(
                        get(
                                "/api/v1/transactions?"
                                        + "transactionDefinitionKey="
                                        + filters.getTransactionDefinitionKeys()
                                        + "&category="
                                        + filters.getCategory()
                                        + "&startDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getStartDate())
                                        + "&endDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getEndDate())
                                        + "&priority="
                                        + TransactionPriority.fromRank(
                                                        filters.getPriority().get(0).getRank())
                                                .getLabel()
                                        + "&status="
                                        + filters.getStatus().get(0)
                                        + "&assignedTo="
                                        + filters.getAssignedTo().get(0)
                                        + "&subject="
                                        + filters.getSubjectUserId()
                                        + "&sortBy="
                                        + filters.getSortBy()
                                        + "&sortOrder="
                                        + filters.getSortOrder()
                                        + "&pageNumber="
                                        + filters.getPageNumber()
                                        + "&pageSize="
                                        + filters.getPageSize()
                                        + "&transactionDefinitionSetKey=dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber", comparesEqualTo(0)))
                .andExpect(jsonPath("$.pagingMetadata.pageSize", comparesEqualTo(1)))
                .andExpect(jsonPath("$.pagingMetadata.totalCount", comparesEqualTo(1)));
    }

    @Test
    void getTransactionsSuccessAssignToMe() throws Exception {
        // Arrange
        final Transaction userTransaction1 = getCommonTransactionBuilder().build();
        mockPrivateTransactionDefinition(userTransaction1);

        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder().assignedToMe(true).build();

        final Page<Transaction> pagedResults = new PageImpl<>(List.of(userTransaction1));

        Mockito.when(transactionService.getFilteredTransactions(any())).thenReturn(pagedResults);

        // Act and Assert
        mockMvc.perform(get("/api/v1/transactions?" + "assignedToMe=" + filters.getAssignedToMe()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void getTransactionsWithoutPermissions() throws Exception {

        when(authorizationHandler.isAllowed("view", "transaction_view")).thenReturn(false);
        // Act and Assert
        mockMvc.perform(get("/api/v1/transactions?" + "transactionDefinitionKey=" + "dummy"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionsSuccessDefaultSorting() throws Exception {
        // Arrange
        final Transaction userTransaction1 = getCommonTransactionBuilder().build();
        mockPrivateTransactionDefinition(userTransaction1);

        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .priority(List.of(TransactionPriority.MEDIUM))
                        .status(List.of("new"))
                        .assignedTo(List.of(UUID.randomUUID().toString()))
                        .subjectUserId("subject")
                        .build();

        final Page<Transaction> pagedResults = new PageImpl<>(List.of(userTransaction1));
        Mockito.when(transactionService.getFilteredTransactions(any())).thenReturn(pagedResults);

        // Act and Assert
        mockMvc.perform(
                        get(
                                "/api/v1/transactions?"
                                        + "transactionDefinitionKey="
                                        + filters.getTransactionDefinitionKeys()
                                        + "&category="
                                        + filters.getCategory()
                                        + "&startDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getStartDate())
                                        + "&endDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getEndDate())
                                        + "&priority="
                                        + TransactionPriority.fromRank(
                                                        filters.getPriority().get(0).getRank())
                                                .getLabel()
                                        + "&status="
                                        + filters.getStatus().get(0)
                                        + "&assignedTo="
                                        + filters.getAssignedTo().get(0)
                                        + "&subject="
                                        + filters.getSubjectUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber", comparesEqualTo(0)))
                .andExpect(jsonPath("$.pagingMetadata.pageSize", comparesEqualTo(1)))
                .andExpect(jsonPath("$.pagingMetadata.totalCount", comparesEqualTo(1)));

        Mockito.verify(transactionService)
                .getFilteredTransactions(
                        argThat(
                                argument ->
                                        argument.getSortBy().equals("createdTimestamp")
                                                && argument.getSortOrder().equals("ASC")
                                                && argument.getPageNumber().equals(0)
                                                && argument.getPageSize().equals(50)));
    }

    @Test
    void getTransactionsNotFound() throws Exception {
        // Arrange
        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .sortBy("id")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(25)
                        .build();

        final Page<Transaction> pagedResults = new PageImpl<>(List.of());

        Mockito.when(transactionService.getFilteredTransactions(any())).thenReturn(pagedResults);

        // Act and Assert
        mockMvc.perform(
                        get(
                                "/api/v1/transactions?"
                                        + "transactionDefinitionKey="
                                        + filters.getTransactionDefinitionKeys()
                                        + "&category="
                                        + filters.getCategory()
                                        + "&startDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getStartDate())
                                        + "&endDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getEndDate())
                                        + "&sortBy="
                                        + filters.getSortBy()
                                        + "&sortOrder="
                                        + filters.getSortOrder()
                                        + "&pageNumber="
                                        + filters.getPageNumber()
                                        + "&pageSize="
                                        + filters.getPageSize()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void getTransactionsInvalidMaxsize() throws Exception {
        // Arrange
        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .priority(List.of(TransactionPriority.MEDIUM))
                        .status(List.of("new"))
                        .assignedTo(List.of(UUID.randomUUID().toString()))
                        .subjectUserId("subject")
                        .sortBy("id")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(500)
                        .build();

        // Act and Assert
        mockMvc.perform(
                        get(
                                "/api/v1/transactions?"
                                        + "transactionDefinitionKey="
                                        + filters.getTransactionDefinitionKeys()
                                        + "&category="
                                        + filters.getCategory()
                                        + "&startDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getStartDate())
                                        + "&endDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getEndDate())
                                        + "&priority="
                                        + filters.getPriority().get(0)
                                        + "&status="
                                        + filters.getStatus().get(0)
                                        + "&assignedTo="
                                        + filters.getAssignedTo().get(0)
                                        + "&subject="
                                        + filters.getSubjectUserId()
                                        + "&sortBy="
                                        + filters.getSortBy()
                                        + "&sortOrder="
                                        + filters.getSortOrder()
                                        + "&pageNumber="
                                        + filters.getPageNumber()
                                        + "&pageSize="
                                        + filters.getPageSize()))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                comparesEqualTo("pageSize: must be less than or equal to 200")));
    }

    @Test
    void getTransactions_InvalidTransactionDefinitionSetKeySpecialCharacter() throws Exception {
        getAndAssertBadRequestAndErrorString(
                "/api/v1/transactions?" + "transactionDefinitionSetKey=" + "",
                TRANSACTION_DEFINITION_SET_KEY_INVALID_MESSAGE_CONSTRAINT);
    }

    @Test
    void initiateDocumentProcessing_success() throws Exception {
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
        DynamicEntity dynamicEntity = new DynamicEntity(parentSchema);

        String documentPath = "childSchema.document1";

        final Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).data(dynamicEntity).build();

        Mockito.when(authorizationHandler.isAllowed("update", "customer_provided_document"))
                .thenReturn(true);
        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(schemaService.getSchemaByKey("childSchemaKey"))
                .thenReturn(Optional.of(childSchema));
        Mockito.when(schemaService.getDocumentProcessorsInASchemaPath(documentPath, parentSchema))
                .thenCallRealMethod();
        Mockito.doNothing().when(documentManagementClient).initiateDocumentProcessing(any(), any());

        final InitiateDocumentProcessingModelRequest request =
                new InitiateDocumentProcessingModelRequest();
        request.setPath(documentPath);
        request.setDocuments(
                Arrays.asList(UUID.fromString("f6555792-fef9-11ed-be56-0242ac120002")));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/" + transaction.getId() + "/process-documents")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processors[0]", comparesEqualTo("testProcessor")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"childSchema..document1", ".document1", "document 1"})
    void initiateDocumentProcessing_malformedPath(String path) throws Exception {
        // Arrange
        final InitiateDocumentProcessingModelRequest request =
                new InitiateDocumentProcessingModelRequest();
        request.setPath(path);
        request.setDocuments(
                Arrays.asList(UUID.fromString("f6555792-fef9-11ed-be56-0242ac120002")));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/" + UUID.randomUUID() + "/process-documents")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiateDocumentProcessing_notFound() throws Exception {
        // Arrange
        Mockito.when(
                        authorizationHandler.isAllowedForInstance(
                                eq("process-attachments"), any(Transaction.class)))
                .thenReturn(false);

        final InitiateDocumentProcessingModelRequest request =
                new InitiateDocumentProcessingModelRequest();
        request.setPath("childSchema.document1");
        request.setDocuments(
                Arrays.asList(UUID.fromString("f6555792-fef9-11ed-be56-0242ac120002")));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/" + UUID.randomUUID() + "/process-documents")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void initiateDocumentProcessing_transactionNotFound() throws Exception {
        // Arrange
        final UUID transactionId = UUID.randomUUID();

        Mockito.when(
                        authorizationHandler.isAllowedForInstance(
                                eq("process-attachments"), any(Transaction.class)))
                .thenReturn(true);
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.empty());

        final InitiateDocumentProcessingModelRequest request =
                new InitiateDocumentProcessingModelRequest();
        request.setPath("childSchema.document1");
        request.setDocuments(
                Arrays.asList(UUID.fromString("f6555792-fef9-11ed-be56-0242ac120002")));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/" + transactionId + "/process-documents")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void initiateDocumentProcessing_schemaNotFound() throws Exception {
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
        DynamicEntity dynamicEntity = new DynamicEntity(parentSchema);

        String documentPath = "childSchema.document1";

        final Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).data(dynamicEntity).build();

        Mockito.when(
                        authorizationHandler.isAllowedForInstance(
                                eq("process-attachments"), any(Transaction.class)))
                .thenReturn(true);
        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(schemaService.getSchemaByKey("childSchemaKey")).thenReturn(Optional.empty());
        Mockito.when(schemaService.getDocumentProcessorsInASchemaPath(documentPath, parentSchema))
                .thenCallRealMethod();

        final InitiateDocumentProcessingModelRequest request =
                new InitiateDocumentProcessingModelRequest();
        request.setPath(documentPath);
        request.setDocuments(
                Arrays.asList(UUID.fromString("f6555792-fef9-11ed-be56-0242ac120002")));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/" + transaction.getId() + "/process-documents")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiateDocumentProcessing_wrongPath() throws Exception {
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
        DynamicEntity dynamicEntity = new DynamicEntity(parentSchema);

        String documentPath = "anyPath";

        final Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).data(dynamicEntity).build();

        Mockito.when(
                        authorizationHandler.isAllowedForInstance(
                                eq("process-attachments"), any(Transaction.class)))
                .thenReturn(true);
        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(schemaService.getSchemaByKey("childSchemaKey")).thenReturn(Optional.empty());
        Mockito.when(schemaService.getDocumentProcessorsInASchemaPath(documentPath, parentSchema))
                .thenCallRealMethod();

        final InitiateDocumentProcessingModelRequest request =
                new InitiateDocumentProcessingModelRequest();
        request.setPath(documentPath);
        request.setDocuments(
                Arrays.asList(UUID.fromString("f6555792-fef9-11ed-be56-0242ac120002")));
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/" + transaction.getId() + "/process-documents")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactionsByUser() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction userTransaction1 =
                getCommonTransactionBuilder()
                        .transactionDefinitionKey("Dummy user test")
                        .createdBy(testUser.get().getId().toString())
                        .build();
        mockPrivateTransactionDefinition(userTransaction1);

        final Transaction userTransaction2 =
                getCommonTransactionBuilder()
                        .transactionDefinitionKey("Dummy user test 2")
                        .processInstanceId("Dummy user test 2")
                        .createdBy(testUser.get().getId().toString())
                        .build();
        mockPrivateTransactionDefinition(userTransaction2);

        Mockito.when(transactionService.getFilteredTransactions(any()))
                .thenReturn(new PageImpl<>(List.of(userTransaction1, userTransaction2)));

        // Act and Assert
        mockMvc.perform(get("/api/v1/my-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber", comparesEqualTo(0)))
                .andExpect(jsonPath("$.pagingMetadata.pageSize", comparesEqualTo(2)))
                .andExpect(jsonPath("$.pagingMetadata.totalCount", comparesEqualTo(2)));
    }

    @Test
    void postTransaction() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("key")
                        .processDefinitionKey("key")
                        .build();
        Mockito.when(transactionDefinitionService.getTransactionDefinitionByKey("key"))
                .thenReturn(Optional.of(transactionDefinition));

        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("low")
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(new DynamicEntity(Schema.builder().build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);
        Mockito.when(transactionService.createTransaction(transactionDefinition, "token"))
                .thenReturn(transaction);

        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()));
    }

    @Test
    void postTransactionWithoutPermissions() throws Exception {
        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(authorizationHandler.isAllowed("create", Transaction.class)).thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void postTransactionMissingTransactionDefinition() throws Exception {
        // Arrange
        Mockito.when(transactionDefinitionService.getTransactionDefinitionByKey("key"))
                .thenReturn(Optional.empty());

        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void postTransactionMissingSchema() throws Exception {
        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("key")
                        .processDefinitionKey("key")
                        .build();
        Mockito.when(transactionDefinitionService.getTransactionDefinitionByKey("key"))
                .thenReturn(Optional.of(transactionDefinition));
        Mockito.when(transactionService.createTransaction(transactionDefinition, "token"))
                .thenThrow(MissingSchemaException.class);

        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void postTransaction_InvalidKeyEmpty() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/transactions",
                TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE,
                customTransactionCreationRequest(""));
    }

    @Test
    void postTransaction_InvalidKeySpecialCharacter() throws Exception {
        postAndAssertBadRequestAndErrorString(
                "/api/v1/transactions",
                TRANSACTION_DEFINITION_KEY_INVALID_MESSAGE,
                customTransactionCreationRequest("Invalid Key"));
    }

    @Test
    void updateTransactionWithAdminChangesAndAgencyUser() throws Exception {
        try (MockedStatic<CurrentUserUtility> mock = Mockito.mockStatic(CurrentUserUtility.class)) {
            mock.when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(UserToken.builder().userType("user").build()));

            Optional<User> testUser = createUser();

            final Transaction transaction =
                    Transaction.builder()
                            .id(UUID.randomUUID())
                            .transactionDefinitionId(UUID.randomUUID())
                            .transactionDefinitionKey("Dummy user test")
                            .processInstanceId("Dummy user test")
                            .status("incomplete")
                            .priority(TransactionPriority.MEDIUM)
                            .createdBy(testUser.get().getId().toString())
                            .createdTimestamp(OffsetDateTime.now())
                            .lastUpdatedTimestamp(OffsetDateTime.now())
                            .data(
                                    new DynamicEntity(
                                            Schema.builder().property("foo", String.class).build()))
                            .build();

            final TransactionUpdateRequest request =
                    new TransactionUpdateRequest().putDataItem("foo", "bar");
            request.setAction("foo");
            request.setPriority(TransactionPriority.MEDIUM.getLabel());
            final String postBody = new ObjectMapper().writeValueAsString(request);

            Mockito.when(transactionService.getTransactionById(transaction.getId()))
                    .thenReturn(Optional.of(transaction));
            when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                    .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));
            when(authorizationHandler.isAllowed(eq("update-admin-data"), (Class<?>) any()))
                    .thenReturn(false);
            when(transactionService.hasAdminDataChanges(any(), any(), any())).thenReturn(true);

            mockMvc.perform(
                            put("/api/v1/transactions/" + transaction.getId().toString())
                                    .content(postBody)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .param("completeTask", "false")
                                    .param("taskId", "task")
                                    .param("formStepKey", "foo"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void updateTransactionWithoutTaskId() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);
        transaction.getData().set("foo", "bar");

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdate(
                                any(Transaction.class), any(Map.class)))
                .thenReturn(transaction);

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", "bar");
        request.setAction("foo");
        request.setPriority(TransactionPriority.MEDIUM.getLabel());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("completeTask", "false")
                                .param("taskId", "task")
                                .param("formStepKey", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.data.foo").value("bar"));
    }

    @Test
    void testHttpClientErrorExceptionHandling() throws Exception {

        // Arrange
        final Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        // Mock the behavior of HttpClientErrorException being thrown
        Mockito.when(userManagementService.getUser(any(UUID.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        final TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAssignedTo(UUID.randomUUID().toString());

        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Perform a mock HTTP PUT request to trigger the exception handling code
        ServletException nestedException =
                assertThrows(
                        ServletException.class,
                        () -> {
                            mockMvc.perform(
                                            put("/api/v1/transactions/"
                                                            + transaction.getId().toString())
                                                    .content(postBody)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .param("completeTask", "false")
                                                    .param("taskId", "task")
                                                    .param("formStepKey", "foo"))
                                    .andExpect(status().isInternalServerError())
                                    .andExpect(content().string("Could not verify user existence"));
                        });
        Throwable actualThrowable = nestedException.getRootCause();

        assertTrue(actualThrowable instanceof UnexpectedException);
        assertEquals("Could not verify user existence", actualThrowable.getMessage());
    }

    @Test
    void updateTransactionWithoutTaskId_validateForm_success() throws Exception {
        Optional<User> testUser = createUser();

        final String formKey = "fooFormKey";
        final String formStepKey = "foo";
        final String email = "bar@something.com";

        final Transaction transaction = stubValidateFrom(testUser, formKey, email);
        mockPrivateTransactionDefinition(transaction);
        final String postBody =
                createTransactionUpdateRequestBodyForFormValidation(formKey, formStepKey, email);

        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar@something.com");
        when(transactionService.unifyAttributeMaps(any(), any())).thenReturn(map);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("completeTask", "false")
                                .param("taskId", "task")
                                .param("formStepKey", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.data.foo").value(email));
    }

    @Test
    void updateTransactionWithoutTaskId_validateForm_fail() throws Exception {
        Optional<User> testUser = createUser();

        final String formKey = "fooFormKey";
        final String formStepKey = "foo";
        final String email = "bar";

        final Transaction transaction = stubValidateFrom(testUser, formKey, email);
        final String postBody =
                createTransactionUpdateRequestBodyForFormValidation(formKey, formStepKey, email);

        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        when(transactionService.unifyAttributeMaps(any(), any())).thenReturn(map);

        NuvalenceFormioValidationExItem nuvalenceFormioValidationExItem =
                NuvalenceFormioValidationExItem.builder().errorName("errorMessage").build();
        List<NuvalenceFormioValidationExItem> formioErrors =
                new ArrayList<>(Arrays.asList(nuvalenceFormioValidationExItem));
        NuvalenceFormioValidationExMessage formioValidationExMessage =
                NuvalenceFormioValidationExMessage.builder()
                        .formioValidationErrors(formioErrors)
                        .build();
        doThrow(new NuvalenceFormioValidationException(formioValidationExMessage))
                .when(transactionService)
                .validateForm(
                        eq(formStepKey),
                        eq(transaction.getTransactionDefinitionKey()),
                        any(),
                        any(),
                        any());

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("completeTask", "false")
                                .param("taskId", "task")
                                .param("formStepKey", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.formioValidationErrors").exists())
                .andExpect(jsonPath("$.formioValidationErrors.length()").value(1))
                .andExpect(jsonPath("$.formioValidationErrors[0].errorName").value("errorMessage"));
    }

    @Test
    void updateTransaction_InvalidFormStepKeyEmpty() throws Exception {
        putAndAssertBadRequestAndErrorString(
                "/api/v1/transactions/"
                        + UUID.randomUUID()
                        + "?completeTask=false&taskId=task&formStepKey=",
                FORM_STEP_KEY_INVALID_MESSAGE_CONSTRAINT,
                createTransactionUpdateRequestBodyForFormValidation(
                        "formKey", "formStepKey", "email"));
    }

    @Test
    void updateTransaction_InvalidFormStepKeySpecialCharacter() throws Exception {

        putAndAssertBadRequestAndErrorString(
                "/api/v1/transactions/"
                        + UUID.randomUUID()
                        + "?completeTask=false&taskId=task&formStepKey=Invalid%20Key",
                FORM_STEP_KEY_INVALID_MESSAGE_CONSTRAINT,
                createTransactionUpdateRequestBodyForFormValidation(
                        "formKey", "formStepKey", "email"));
    }

    private Transaction stubValidateFrom(Optional<User> testUser, String formKey, String email)
            throws IOException, MissingTransactionException {
        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();

        final FormConfiguration formConfiguration =
                FormConfiguration.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionKey(transaction.getTransactionDefinitionKey())
                        .key(formKey)
                        .name(formKey)
                        .schemaKey("foo")
                        .configurationSchema("formio")
                        .configuration(
                                jsonLoader.loadConfigMap(
                                        "/formConfigurationJSONTests/simpleForm.json"))
                        .build();

        transaction.getData().set("foo", email);

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdate(
                                any(Transaction.class), any(Map.class)))
                .thenReturn(transaction);

        Mockito.when(
                        formConfigurationService.getFormConfigurationByKeys(
                                transaction.getTransactionDefinitionKey(), formKey))
                .thenReturn(Optional.of(formConfiguration));

        return transaction;
    }

    private String createTransactionUpdateRequestBodyForFormValidation(
            String formKey, String formStepKey, String email) throws JsonProcessingException {
        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", email);
        request.setAction("foo");
        request.setPriority(TransactionPriority.MEDIUM.getLabel());
        request.setContext("");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        return postBody;
    }

    @Test
    void updateTransaction404() throws Exception {
        // Arrange
        final UUID transactionId = UUID.randomUUID();
        Mockito.when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.empty());
        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", "bar");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId)
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("taskId", "taskId")
                                .param("formStepKey", "foo"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransactionMissingTransaction() throws Exception {
        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy("Dummy user")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdateAndCompleteTask(
                                any(Transaction.class), eq("taskId"), eq("foo"), any(Map.class)))
                .thenThrow(MissingTransactionException.class);

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", "bar");
        request.setAction("foo");
        request.setPriority(TransactionPriority.LOW.getLabel());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/"
                                        + transaction.getId().toString()
                                        + "/?taskId=taskId"
                                        + "&completeTask=true")
                                .content(postBody)
                                .param("formStepKey", "foo")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransactionJsonProcessingException() throws Exception {
        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy("Dummy user")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdateAndCompleteTask(
                                any(Transaction.class), eq("taskId"), eq("foo"), any(Map.class)))
                .thenThrow(JsonProcessingException.class);

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", "bar");
        request.setAction("foo");
        request.setPriority(TransactionPriority.MEDIUM.getLabel());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/"
                                        + transaction.getId().toString()
                                        + "?taskId=taskId")
                                .param("completeTask", "true")
                                .param("formStepKey", "foo")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateTransactionAndCompleteTask() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("low")
                        .createdBy("Dummy user")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);
        transaction.getData().set("foo", "bar");

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdateAndCompleteTask(
                                any(Transaction.class), eq("taskId"), eq("foo"), any(Map.class)))
                .thenReturn(transaction);

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", "bar");
        request.setAction("foo");
        request.setPriority(TransactionPriority.MEDIUM.getLabel());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/"
                                        + transaction.getId().toString()
                                        + "?taskId=taskId")
                                .param("completeTask", "true")
                                .param("formStepKey", "foo")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.data.foo").value("bar"));
    }

    @Test
    void updateTransactionMissingTask() throws Exception {
        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy("Dummy user")
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdateAndCompleteTask(
                                any(Transaction.class), eq("taskId"), eq("foo"), any(Map.class)))
                .thenThrow(MissingTaskException.class);

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().putDataItem("foo", "bar");
        request.setAction("foo");
        request.setPriority(TransactionPriority.MEDIUM.getLabel());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("taskId").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/"
                                        + transaction.getId().toString()
                                        + "?taskId=taskId")
                                .param("completeTask", "true")
                                .param("formStepKey", "foo")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void updateTransactionSetAssignedTo() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdate(
                                transactionCaptor.capture(), any(Map.class)))
                .thenReturn(transaction);
        Mockito.when(userManagementService.getUser(any(UUID.class))).thenReturn(testUser);

        doNothing().when(transactionService).validateForm(any(), any(), any(), any(), any());

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().assignedTo(testUser.get().getId().toString());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .content(postBody)
                                .param("formStepKey", "foo")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertEquals(
                testUser.get().getId().toString(), transactionCaptor.getValue().getAssignedTo());
    }

    @Test
    void updateTransactionSetAssignedToMissingUser() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        Mockito.when(userManagementService.getUser(any(UUID.class))).thenReturn(Optional.empty());

        final TransactionUpdateRequest request =
                new TransactionUpdateRequest().assignedTo(testUser.get().getId().toString());
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .param("formStepKey", "foo")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransactionUnassign() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .assignedTo(testUser.get().getId().toString())
                        .data(
                                new DynamicEntity(
                                        Schema.builder().property("foo", String.class).build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        Mockito.when(
                        transactionService.updateTransactionFromPartialUpdate(
                                transactionCaptor.capture(), any(Map.class)))
                .thenReturn(transaction);
        Mockito.when(userManagementService.getUser(any(UUID.class))).thenReturn(testUser);

        final TransactionUpdateRequest request = new TransactionUpdateRequest().assignedTo("");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("task").build()));

        // Act and Assert
        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .param("formStepKey", "foo")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertEquals("", transactionCaptor.getValue().getAssignedTo());
    }

    @Test
    void getTransactionCountByStatus() throws Exception {
        // Arrange

        final SearchTransactionsFilters filters =
                SearchTransactionsFilters.builder()
                        .transactionDefinitionKeys(List.of("dummy"))
                        .category("test")
                        .startDate(OffsetDateTime.now())
                        .endDate(OffsetDateTime.now())
                        .priority(List.of(TransactionPriority.MEDIUM))
                        .status(List.of("new"))
                        .assignedTo(List.of(UUID.randomUUID().toString()))
                        .build();

        final TransactionCountByStatusModel count = new TransactionCountByStatusModel();
        count.setStatus("new");
        count.setCount(123);

        Mockito.when(transactionService.getTransactionCountsByStatus(any()))
                .thenReturn(List.of(count));

        // Act and Assert
        mockMvc.perform(
                        get(
                                "/api/v1/transactions/statuses/count?"
                                        + "transactionDefinitionKey="
                                        + filters.getTransactionDefinitionKeys()
                                        + "&category="
                                        + filters.getCategory()
                                        + "&startDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getStartDate())
                                        + "&endDate="
                                        + OffsetDateTimeMapper.INSTANCE.toString(
                                                filters.getEndDate())
                                        + "&priority="
                                        + TransactionPriority.fromRank(
                                                        filters.getPriority().get(0).getRank())
                                                .getLabel()
                                        + "&status="
                                        + filters.getStatus().get(0)
                                        + "&assignedTo="
                                        + filters.getAssignedTo().get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", comparesEqualTo("new")))
                .andExpect(jsonPath("$[0].count", comparesEqualTo(123)));
    }

    @Test
    void getTransactionCountByStatusWithoutPermissions() throws Exception {

        when(authorizationHandler.isAllowed("view", "transaction_view")).thenReturn(false);
        mockMvc.perform(
                        get(
                                "/api/v1/transactions/statuses/count?"
                                        + "transactionDefinitionKey="
                                        + "dummy"))
                .andExpect(status().isForbidden());
    }

    @Test
    void linkTransactionsSuccess() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        final UUID fromId = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();
        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);

        final TransactionLink transactionLink =
                TransactionLink.builder()
                        .id(id)
                        .fromTransactionId(fromId)
                        .toTransactionId(toId)
                        .build();

        Mockito.when(authorizationHandler.isAllowed("create", "transaction_link")).thenReturn(true);

        Mockito.when(
                        transactionLinkService.saveTransactionLink(
                                ArgumentMatchers.any(TransactionLink.class),
                                ArgumentMatchers.any(UUID.class)))
                .thenReturn(transactionLink);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/{id}/links/{toId}", fromId, toId)
                                .header("Authorization", "token")
                                .content(new ObjectMapper().writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));

        Mockito.verify(authorizationHandler).isAllowed("create", "transaction_link");
        Mockito.verify(transactionLinkService)
                .saveTransactionLink(
                        ArgumentMatchers.any(TransactionLink.class),
                        ArgumentMatchers.any(UUID.class));
    }

    @Test
    void linkTransactionsMissingTransactionDefinitionException() throws Exception {
        // Arrange
        final UUID fromId = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();
        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);
        Mockito.when(authorizationHandler.isAllowed("create", "transaction_link")).thenReturn(true);

        doThrow(MissingTransactionDefinitionException.class)
                .when(transactionLinkService)
                .saveTransactionLink(
                        ArgumentMatchers.any(TransactionLink.class),
                        ArgumentMatchers.any(UUID.class));

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/{id}/links/{toId}", fromId, toId)
                                .header("Authorization", "token")
                                .content(new ObjectMapper().writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isFailedDependency());
    }

    @Test
    void linkTransactionsTransactionLinkNotAllowedException() throws Exception {
        // Arrange
        final UUID fromId = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();
        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);
        Mockito.when(authorizationHandler.isAllowed("create", "transaction_link")).thenReturn(true);

        doThrow(TransactionLinkNotAllowedException.class)
                .when(transactionLinkService)
                .saveTransactionLink(
                        ArgumentMatchers.any(TransactionLink.class),
                        ArgumentMatchers.any(UUID.class));

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/{id}/links/{toId}", fromId, toId)
                                .header("Authorization", "token")
                                .content(new ObjectMapper().writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkTransactionsMissingTransactionException() throws Exception {
        // Arrange
        final UUID fromId = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();
        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);
        Mockito.when(authorizationHandler.isAllowed("create", "transaction_link")).thenReturn(true);

        doThrow(MissingTransactionException.class)
                .when(transactionLinkService)
                .saveTransactionLink(
                        ArgumentMatchers.any(TransactionLink.class),
                        ArgumentMatchers.any(UUID.class));

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/{id}/links/{toId}", fromId, toId)
                                .header("Authorization", "token")
                                .content(new ObjectMapper().writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void linkTransactionsUnauthorized() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        final UUID fromId = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();
        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);

        Mockito.when(authorizationHandler.isAllowed("create", "transaction_link"))
                .thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/{id}/links/{toId}", fromId, toId)
                                .header("Authorization", "token")
                                .content(new ObjectMapper().writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        Mockito.verify(authorizationHandler).isAllowed("create", "transaction_link");
    }

    @Test
    void linkTransactionsNotFound() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        final UUID fromId = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();
        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);

        Mockito.when(authorizationHandler.isAllowed("create", "transaction_link")).thenReturn(true);

        Mockito.when(
                        transactionLinkService.saveTransactionLink(
                                ArgumentMatchers.any(TransactionLink.class),
                                ArgumentMatchers.any(UUID.class)))
                .thenThrow(new NotFoundException("Link request references missing transaction."));

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions/{id}/links/{toId}", fromId, toId)
                                .header("Authorization", "token")
                                .content(new ObjectMapper().writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        Mockito.verify(authorizationHandler).isAllowed("create", "transaction_link");
        Mockito.verify(transactionLinkService)
                .saveTransactionLink(
                        ArgumentMatchers.any(TransactionLink.class),
                        ArgumentMatchers.any(UUID.class));
    }

    @Test
    void deleteTransactionLinkOk() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();

        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);

        Mockito.when(authorizationHandler.isAllowed("delete", "transaction_link")).thenReturn(true);

        Mockito.doNothing()
                .when(transactionLinkService)
                .removeTransactionLink(transactionLinkTypeId, id, toId);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/transactions/{id}/links/{toId}", id, toId)
                                .header("Authorization", "token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(authorizationHandler).isAllowed("delete", "transaction_link");
        Mockito.verify(transactionLinkService)
                .removeTransactionLink(eq(request.getTransactionLinkTypeId()), eq(id), eq(toId));
    }

    @Test
    void deleteTransactionLinkUnauthorized() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();

        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);

        Mockito.when(authorizationHandler.isAllowed("delete", "transaction_link"))
                .thenReturn(false);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/transactions/{id}/links/{toId}", id, toId)
                                .header("Authorization", "token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Mockito.verify(authorizationHandler).isAllowed("delete", "transaction_link");
    }

    private UUID setupCustomerProvidedDocumentAuditEventTest(
            UUID documentId,
            ReviewStatus existingReviewStatus,
            List<RejectionReason> existingRejectedReason,
            CustomerProvidedDocument newDocument) {
        final Document document = Document.builder().documentId(documentId).build();

        final Schema parentSchema =
                Schema.builder()
                        .key("SchemaKey")
                        .name("schema")
                        .property("document", Document.class)
                        .build();
        DynamicEntity dynamicEntity = new DynamicEntity(parentSchema);
        dynamicEntity.set("document", document);

        final Transaction transaction = getCommonTransactionBuilder().build();
        transaction.setData(dynamicEntity);
        final UUID transactionId = transaction.getId();

        final CustomerProvidedDocument existingDocument =
                CustomerProvidedDocument.builder()
                        .id(documentId)
                        .active(true)
                        .reviewStatus(existingReviewStatus)
                        .dataPath("document")
                        .build();
        existingDocument.setRejectionReasons(existingRejectedReason);

        Mockito.when(authorizationHandler.isAllowed("update", "customer_provided_document"))
                .thenReturn(true);

        when(transactionService.getTransactionIfExists(transactionId.toString()))
                .thenReturn(transaction);

        when(transactionService.getCustomerProvidedDocumentInATransactionById(
                        transaction, documentId.toString()))
                .thenReturn(existingDocument);

        when(transactionService.updateCustomerProvidedDocument(
                        any(CustomerProvidedDocument.class), eq(transaction)))
                .thenReturn(newDocument);

        return transactionId;
    }

    @Test
    void updateCustomerProvidedDocumentTest_accepted_success() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final CustomerProvidedDocument newDocument =
                CustomerProvidedDocument.builder()
                        .id(documentId)
                        .active(true)
                        .reviewStatus(ReviewStatus.ACCEPTED)
                        .dataPath("document")
                        .build();
        final UUID transactionId =
                setupCustomerProvidedDocumentAuditEventTest(
                        documentId, ReviewStatus.NEW, new ArrayList<>(), newDocument);

        CustomerProvidedDocumentModelRequest request = new CustomerProvidedDocumentModelRequest();
        request.setReviewStatus(ReviewStatus.ACCEPTED.getValue());
        final String body = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/documents/" + documentId)
                                .header("Authorization", "token")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value(ReviewStatus.ACCEPTED.getValue()));

        Mockito.verify(transactionService)
                .postAuditEventForDocumentStatusChanged(
                        newDocument,
                        "document",
                        AuditActivityType.DOCUMENT_ACCEPTED,
                        "Document accepted");
    }

    @Test
    void updateCustomerProvidedDocumentTest_rejected_success() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final List<RejectionReason> rejectionReasons =
                new ArrayList<>(
                        List.of(
                                RejectionReason.builder()
                                        .rejectionReasonValue(RejectionReasonType.POOR_QUALITY)
                                        .build(),
                                RejectionReason.builder()
                                        .rejectionReasonValue(RejectionReasonType.INCORRECT_TYPE)
                                        .build()));

        final CustomerProvidedDocument newDocument =
                CustomerProvidedDocument.builder()
                        .id(documentId)
                        .active(true)
                        .reviewStatus(ReviewStatus.REJECTED)
                        .dataPath("document")
                        .build();
        newDocument.setRejectionReasons(rejectionReasons);

        final UUID transactionId =
                setupCustomerProvidedDocumentAuditEventTest(
                        documentId, ReviewStatus.NEW, rejectionReasons, newDocument);

        CustomerProvidedDocumentModelRequest request = new CustomerProvidedDocumentModelRequest();
        request.setReviewStatus(ReviewStatus.REJECTED.getValue());
        request.setRejectionReasons(
                rejectionReasons.stream()
                        .map(
                                rejectionReason ->
                                        rejectionReason.getRejectionReasonValue().getValue())
                        .collect(Collectors.toList()));
        final String body = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/documents/" + documentId)
                                .header("Authorization", "token")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value(ReviewStatus.REJECTED.getValue()));

        Mockito.verify(transactionService)
                .postAuditEventForDocumentStatusChanged(
                        newDocument,
                        "document",
                        AuditActivityType.DOCUMENT_REJECTED,
                        "Document rejected");
    }

    @Test
    void updateCustomerProvidedDocumentTest_unaccepted_success() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final CustomerProvidedDocument newDocument =
                CustomerProvidedDocument.builder()
                        .id(documentId)
                        .active(true)
                        .reviewStatus(ReviewStatus.NEW)
                        .dataPath("document")
                        .build();
        final UUID transactionId =
                setupCustomerProvidedDocumentAuditEventTest(
                        documentId, ReviewStatus.ACCEPTED, new ArrayList<>(), newDocument);

        CustomerProvidedDocumentModelRequest request = new CustomerProvidedDocumentModelRequest();
        request.setReviewStatus(ReviewStatus.NEW.getValue());
        final String body = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/documents/" + documentId)
                                .header("Authorization", "token")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value(ReviewStatus.NEW.getValue()));

        Mockito.verify(transactionService)
                .postAuditEventForDocumentStatusChanged(
                        newDocument,
                        "document",
                        AuditActivityType.DOCUMENT_UNACCEPTED,
                        "Document unaccepted");
    }

    @Test
    void updateCustomerProvidedDocumentTest_unrejected_success() throws Exception {
        final UUID documentId = UUID.randomUUID();
        final CustomerProvidedDocument newDocument =
                CustomerProvidedDocument.builder()
                        .id(documentId)
                        .active(true)
                        .reviewStatus(ReviewStatus.NEW)
                        .dataPath("document")
                        .build();
        final UUID transactionId =
                setupCustomerProvidedDocumentAuditEventTest(
                        documentId, ReviewStatus.REJECTED, new ArrayList<>(), newDocument);

        CustomerProvidedDocumentModelRequest request = new CustomerProvidedDocumentModelRequest();
        request.setReviewStatus(ReviewStatus.NEW.getValue());
        final String body = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transactionId + "/documents/" + documentId)
                                .header("Authorization", "token")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value(ReviewStatus.NEW.getValue()));

        Mockito.verify(transactionService)
                .postAuditEventForDocumentStatusChanged(
                        newDocument,
                        "document",
                        AuditActivityType.DOCUMENT_UNREJECTED,
                        "Document unrejected");
    }

    @Test
    void updateCustomerProvidedDocumentUnauthorized() throws Exception {
        final Transaction transaction = getCommonTransactionBuilder().build();
        final CustomerProvidedDocumentModelResponse customerProvidedDocumentModel =
                createCustomerProvidedDocumentModel(transaction);

        Mockito.when(authorizationHandler.isAllowed("update", "customer_provided_document"))
                .thenReturn(false);

        mockMvc.perform(
                        put(
                                        "/api/v1/transactions/{transactionId}/documents/{documentId}",
                                        transaction.getId(),
                                        customerProvidedDocumentModel.getId())
                                .header("Authorization", "token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(customerProvidedDocumentModel)))
                .andExpect(status().isForbidden());

        Mockito.verify(authorizationHandler).isAllowed("update", "customer_provided_document");
    }

    @Test
    void updateCustomerProvidedDocumentTest_FailsTransactionNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();

        when(transactionService.getTransactionIfExists(transactionId.toString()))
                .thenThrow(new NotFoundException());

        CustomerProvidedDocumentModelRequest request = new CustomerProvidedDocumentModelRequest();
        request.setReviewStatus(ReviewStatus.NEW.getValue());
        final String body = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/"
                                        + transactionId
                                        + "/documents/"
                                        + UUID.randomUUID())
                                .header("Authorization", "token")
                                .content(body)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransactionLinkNotFound() throws Exception {
        // Arrange
        final UUID id = UUID.randomUUID();
        final UUID toId = UUID.randomUUID();
        final UUID transactionLinkTypeId = UUID.randomUUID();

        final TransactionLinkModificationRequest request = new TransactionLinkModificationRequest();
        request.setTransactionLinkTypeId(transactionLinkTypeId);

        Mockito.when(authorizationHandler.isAllowed("delete", "transaction_link")).thenReturn(true);

        Mockito.doThrow(new NotFoundException("Resource not found"))
                .when(transactionLinkService)
                .removeTransactionLink(request.getTransactionLinkTypeId(), id, toId);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/v1/transactions/{id}/links/{toId}", id, toId)
                                .header("Authorization", "token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isNotFound());

        Mockito.verify(authorizationHandler).isAllowed("delete", "transaction_link");
        Mockito.verify(transactionLinkService)
                .removeTransactionLink(eq(request.getTransactionLinkTypeId()), eq(id), eq(toId));
    }

    @Test
    void getTransactionWithCustomerDocument() throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("incomplete")
                        .priority(TransactionPriority.MEDIUM)
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(new DynamicEntity(Schema.builder().build()))
                        .customerProvidedDocuments(new ArrayList<>())
                        .build();
        mockPrivateTransactionDefinition(transaction);

        CustomerProvidedDocument customerProvidedDocument =
                createCustomerProvidedDocument(transaction);
        transaction.getCustomerProvidedDocuments().add(customerProvidedDocument);

        Mockito.when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        // Act and Assert
        mockMvc.perform(get("/api/v1/transactions/" + transaction.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerProvidedDocuments.length()", Matchers.equalTo(1)))
                .andExpect(
                        jsonPath(
                                "$.customerProvidedDocuments[0].id",
                                Matchers.equalTo(customerProvidedDocument.getId().toString())));
    }

    @Test
    void updateCustomerProvidedDocumentNotFound() throws Exception {
        final Transaction transaction = getCommonTransactionBuilder().build();
        transaction.setCustomerProvidedDocuments(new ArrayList<>());

        final CustomerProvidedDocumentModelRequest customerProvidedDocumentModel =
                createCustomerProvidedDocumentModelRequest();

        Mockito.when(authorizationHandler.isAllowed("update", "customer_provided_document"))
                .thenReturn(true);

        Mockito.when(transactionService.updateCustomerProvidedDocument(any(), any()))
                .thenThrow(BusinessLogicException.class);

        Mockito.when(transactionService.getTransactionIfExists(transaction.getId().toString()))
                .thenReturn(transaction);

        Mockito.when(
                        transactionService.getCustomerProvidedDocumentInATransactionById(
                                any(Transaction.class), any(String.class)))
                .thenCallRealMethod();

        mockMvc.perform(
                        put(
                                        "/api/v1/transactions/{transactionId}/documents/{documentId}",
                                        transaction.getId(),
                                        UUID.randomUUID())
                                .header("Authorization", "token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(customerProvidedDocumentModel)))
                .andExpect(status().isNotFound());
    }

    private Map<String, Object> createFormConfigMap() {
        final Map<String, Object> formMap = new HashMap<>();
        final Map<String, Object> propsMap = new HashMap<>();
        propsMap.put("required", true);

        final Map<String, Object> validatorsMap = new HashMap<>();
        final List<String> validationList = new ArrayList<>();
        validationList.add("email");
        validatorsMap.put("validation", validationList);

        final Map<String, Object> componentMap = new HashMap<>();
        componentMap.put("key", "fooFormStepKey");
        componentMap.put("props", propsMap);
        componentMap.put("validators", validatorsMap);

        final List<Object> componentsList = new ArrayList<>();
        componentsList.add(componentMap);

        formMap.put("components", componentsList);

        return formMap;
    }

    private Optional<User> createUser() {
        return Optional.ofNullable(
                User.builder()
                        .id(UUID.randomUUID())
                        .externalId("EXT000123")
                        .email("someEmail@something.com")
                        .build());
    }

    private void mockPrivateTransactionDefinition(Transaction transaction) {
        ReflectionTestUtils.setField(
                transaction, "transactionDefinition", new TransactionDefinition());
    }

    @Test
    void postTransactionExceptionPath(CapturedOutput output) throws Exception {
        Optional<User> testUser = createUser();

        // Arrange
        final TransactionDefinition transactionDefinition =
                TransactionDefinition.builder()
                        .id(UUID.randomUUID())
                        .key("key")
                        .processDefinitionKey("key")
                        .build();
        Mockito.when(transactionDefinitionService.getTransactionDefinitionByKey("key"))
                .thenReturn(Optional.of(transactionDefinition));

        final Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .transactionDefinitionId(UUID.randomUUID())
                        .transactionDefinitionKey("Dummy user test")
                        .processInstanceId("Dummy user test")
                        .status("low")
                        .createdBy(testUser.get().getId().toString())
                        .createdTimestamp(OffsetDateTime.now())
                        .lastUpdatedTimestamp(OffsetDateTime.now())
                        .data(new DynamicEntity(Schema.builder().build()))
                        .build();
        mockPrivateTransactionDefinition(transaction);
        Mockito.when(transactionService.createTransaction(transactionDefinition, "token"))
                .thenReturn(transaction);

        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        doThrow(new RuntimeException("Test Exception"))
                .when(transactionService)
                .postAuditEventForTransactionCreated(any());

        // Act and Assert
        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header("Authorization", "token")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()));

        assertTrue(
                output.getOut()
                        .contains(
                                "An error has occurred when recording a creation audit event for a"
                                        + " transaction with user id "
                                        + transaction.getCreatedBy()
                                        + " for transaction with id "
                                        + transaction.getId()));
    }

    @Test
    void testUpdateTransactionWorkflowValidation_FailsEmptyActiveTasks() throws Exception {
        Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();
        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Collections.emptyList());

        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTransactionWorkflowValidation_FailsWithTaskId_TaskDoesNotMatch()
            throws Exception {
        Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();
        when(transactionService.getTransactionById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        when(transactionTaskService.getActiveTasksForCurrentUser(any()))
                .thenReturn(Arrays.asList(WorkflowTask.builder().key("other").build()));

        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey("key");
        final String postBody = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(
                        put("/api/v1/transactions/" + transaction.getId().toString())
                                .param("taskId", "taskId")
                                .content(postBody)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private String customTransactionCreationRequest(String transactionDefinitionKey)
            throws JsonProcessingException {
        final TransactionCreationRequest request =
                new TransactionCreationRequest().transactionDefinitionKey(transactionDefinitionKey);

        return bodyToString(request);
    }

    private String bodyToString(Object body) throws JsonProcessingException {
        return objectMapper.writeValueAsString(body);
    }

    private void getAndAssertBadRequestAndErrorString(String url, String errorString)
            throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }

    private void putAndAssertBadRequestAndErrorString(String url, String errorString, String body)
            throws Exception {
        mockMvc.perform(put(url).content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }

    private void postAndAssertBadRequestAndErrorString(String url, String errorString, String body)
            throws Exception {
        mockMvc.perform(post(url).content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(equalTo(errorString)));
    }
}

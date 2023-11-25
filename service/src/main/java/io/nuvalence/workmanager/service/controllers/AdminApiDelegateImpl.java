package io.nuvalence.workmanager.service.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.workmanager.service.config.exceptions.ConflictException;
import io.nuvalence.workmanager.service.config.exceptions.FileReadException;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.domain.dynamicschema.Schema;
import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.AllowedLink;
import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinition;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.generated.controllers.AdminApiDelegate;
import io.nuvalence.workmanager.service.generated.models.AllowedLinkCreationRequest;
import io.nuvalence.workmanager.service.generated.models.AllowedLinkModel;
import io.nuvalence.workmanager.service.generated.models.DashboardCountsModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationCreateModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationResponseModel;
import io.nuvalence.workmanager.service.generated.models.FormConfigurationUpdateModel;
import io.nuvalence.workmanager.service.generated.models.PagedSchemaModel;
import io.nuvalence.workmanager.service.generated.models.PagedTransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.PagedTransactionDefinitionSetModel;
import io.nuvalence.workmanager.service.generated.models.PagedWorkflowModel;
import io.nuvalence.workmanager.service.generated.models.ParentSchemas;
import io.nuvalence.workmanager.service.generated.models.SchemaCreateModel;
import io.nuvalence.workmanager.service.generated.models.SchemaModel;
import io.nuvalence.workmanager.service.generated.models.SchemaUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TaskModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionUpdateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionLinkTypeModel;
import io.nuvalence.workmanager.service.generated.models.WorkflowModel;
import io.nuvalence.workmanager.service.mapper.AllowedLinkMapper;
import io.nuvalence.workmanager.service.mapper.DashboardConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.DynamicSchemaMapper;
import io.nuvalence.workmanager.service.mapper.FormConfigurationMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionMapper;
import io.nuvalence.workmanager.service.mapper.TransactionDefinitionSetMapper;
import io.nuvalence.workmanager.service.mapper.TransactionLinkTypeMapper;
import io.nuvalence.workmanager.service.mapper.WorkflowAndTaskMapper;
import io.nuvalence.workmanager.service.models.SchemaFilters;
import io.nuvalence.workmanager.service.models.TransactionDefinitionFilters;
import io.nuvalence.workmanager.service.models.TransactionDefinitionSetFilter;
import io.nuvalence.workmanager.service.service.AllowedLinkService;
import io.nuvalence.workmanager.service.service.DashboardConfigurationService;
import io.nuvalence.workmanager.service.service.FormConfigurationService;
import io.nuvalence.workmanager.service.service.SchemaService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionSetOrderService;
import io.nuvalence.workmanager.service.service.TransactionDefinitionSetService;
import io.nuvalence.workmanager.service.service.TransactionLinkTypeService;
import io.nuvalence.workmanager.service.service.WorkflowTasksService;
import io.nuvalence.workmanager.service.utils.ConfigurationUtility;
import io.nuvalence.workmanager.service.utils.camunda.ConsistencyChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Controller layer for API.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
public class AdminApiDelegateImpl implements AdminApiDelegate {
    public static final String CREATE_CERBOS_ACTION = "create";
    public static final String UPDATE_CERBOS_ACTION = "update";
    public static final String CONFIGURATION_CERBOS_RESORCE = "configuration";
    public static final String EXPORT_CERBOS_ACTION = "export";
    private final AllowedLinkService allowedLinkService;
    private final SchemaService schemaService;
    private final TransactionDefinitionService transactionDefinitionService;
    private final TransactionDefinitionSetService transactionDefinitionSetService;
    private final TransactionLinkTypeService transactionLinkTypeService;
    private final FormConfigurationService formConfigurationService;
    private final DynamicSchemaMapper dynamicSchemaMapper;
    private final ConsistencyChecker consistencyChecker;
    private final ConfigurationUtility configurationUtility;
    private final AuthorizationHandler authorizationHandler;
    private final WorkflowTasksService workflowTasksService;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final WorkflowAndTaskMapper workflowAndTaskMapper;
    private final DashboardConfigurationService dashboardConfigurationService;
    private final DashboardConfigurationMapper dashboardConfigurationMapper;
    private final TransactionDefinitionSetOrderService transactionDefinitionSetOrderService;

    @Override
    public ResponseEntity<SchemaModel> getSchema(String key, Boolean includeChildren) {
        final Optional<SchemaModel> schema =
                schemaService
                        .getSchemaByKey(key)
                        .filter(
                                schemaInstance ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", schemaInstance))
                        .map(
                                authorizedSchema ->
                                        dynamicSchemaMapper.schemaToSchemaModel(
                                                authorizedSchema,
                                                includeChildren
                                                        ? schemaService.getAllRelatedSchemas(key)
                                                        : null));

        return schema.map(schemaModel -> ResponseEntity.status(200).body(schemaModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<Void> deleteSchema(String key) {
        if (!authorizationHandler.isAllowed("delete", Schema.class)) {
            throw new ForbiddenException();
        }

        Optional<Schema> schema = schemaService.getSchemaByKey(key);

        if (schema.isEmpty()) {
            throw new NotFoundException("Schema not found: " + key);
        }

        try {
            schemaService.deleteSchema(schema.get());
        } catch (RuntimeException e) {
            throw new ConflictException("Schema is being used and cannot be deleted");
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Failed to delete schema", e);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<FormConfigurationResponseModel>> getListOfFormConfigurations(
            String transactionDefinitionKey) {
        return transactionDefinitionService
                .getTransactionDefinitionByKey(transactionDefinitionKey)
                .filter(authorizationHandler.getAuthFilter("view", TransactionDefinition.class))
                .map(
                        definition ->
                                formConfigurationService
                                        .getFormConfigurationsByTransactionDefinitionKey(
                                                definition.getKey())
                                        .stream()
                                        .filter(
                                                authorizationHandler.getAuthFilter(
                                                        "view", FormConfiguration.class))
                                        .map(
                                                FormConfigurationMapper.INSTANCE
                                                        ::mapFormConfigurationToModel)
                                        .collect(Collectors.toList()))
                .map(results -> ResponseEntity.status(200).body(results))
                .orElse(ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<PagedSchemaModel> getSchemas(
            String name,
            String key,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", Schema.class)) {
            throw new ForbiddenException();
        }

        Page<SchemaModel> results =
                schemaService
                        .getSchemasByFilters(
                                new SchemaFilters(
                                        name, key, sortBy, sortOrder, pageNumber, pageSize))
                        .map(dynamicSchemaMapper::schemaToSchemaModel);

        PagedSchemaModel response = new PagedSchemaModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<SchemaModel> updateSchema(
            String key, SchemaUpdateModel schemaUpdateModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        UUID schemaId;
        Optional<Schema> existingSchema = schemaService.getSchemaByKey(key);
        if (existingSchema.isEmpty()) {
            throw new NotFoundException("Schema not found");
        }
        schemaId = existingSchema.get().getId();
        schemaService.saveSchema(
                dynamicSchemaMapper.schemaUpdateModelToSchema(schemaUpdateModel, key, schemaId));

        Schema schema =
                schemaService
                        .getSchemaByKey(key)
                        .orElseThrow(
                                () -> new UnexpectedException("Schema not found after saving"));

        SchemaModel schemaModel = dynamicSchemaMapper.schemaToSchemaModel(schema);

        return ResponseEntity.status(200).body(schemaModel);
    }

    @Override
    public ResponseEntity<SchemaModel> createSchema(SchemaCreateModel schemaCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, Schema.class)) {
            throw new ForbiddenException();
        }

        if (schemaService.getSchemaByKey(schemaCreateModel.getKey()).isPresent()) {
            throw new ConflictException("Schema already exists");
        }

        Schema schema =
                schemaService.saveSchema(
                        dynamicSchemaMapper.schemaCreateModelToSchema(schemaCreateModel));

        SchemaModel schemaModel = dynamicSchemaMapper.schemaToSchemaModel(schema);

        return ResponseEntity.status(200).body(schemaModel);
    }

    @Override
    public ResponseEntity<ParentSchemas> getSchemaParents(String key) {
        if (!authorizationHandler.isAllowed("view", Schema.class)) {
            throw new ForbiddenException();
        }

        Schema schema =
                schemaService
                        .getSchemaByKey(key)
                        .orElseThrow(() -> new NotFoundException("Schema not found: " + key));

        List<String> parents =
                schemaService.getSchemaParents(schema.getKey()).stream()
                        .map(Schema::getKey)
                        .collect(Collectors.toList());
        ParentSchemas response = new ParentSchemas();
        response.setParentSchemas(parents);

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<Resource> exportConfiguration() {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }

        String zipFileName =
                String.format("backup-%s.zip", ConfigurationUtility.getImportTimestampString());

        try {
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            String.format("attachment; filename=\"%s\"", zipFileName))
                    .contentType(MediaType.valueOf("application/zip"))
                    .body(configurationUtility.getConfiguration());
        } catch (IOException e) {
            throw new FileReadException("Failed to get Configuration", e);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<TransactionDefinitionResponseModel> getTransactionDefinition(String key) {
        final Optional<TransactionDefinitionResponseModel> transactionDefinition =
                transactionDefinitionService
                        .getTransactionDefinitionByKey(key)
                        .filter(
                                definition ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", definition))
                        .map(
                                TransactionDefinitionMapper.INSTANCE
                                        ::transactionDefinitionToResponseModel);

        return transactionDefinition
                .map(
                        transactionDefinitionResponseModel ->
                                ResponseEntity.status(200).body(transactionDefinitionResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<PagedTransactionDefinitionResponseModel> getTransactionDefinitions(
            String name, String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {

        if (!authorizationHandler.isAllowed("view", TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        TransactionDefinitionFilters filters =
                new TransactionDefinitionFilters(name, sortBy, sortOrder, pageNumber, pageSize);

        Page<TransactionDefinitionResponseModel> results =
                transactionDefinitionService
                        .getTransactionDefinitionsByFilters(filters)
                        .map(
                                TransactionDefinitionMapper.INSTANCE
                                        ::transactionDefinitionToResponseModel);

        PagedTransactionDefinitionResponseModel response =
                new PagedTransactionDefinitionResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<TransactionDefinitionResponseModel> putTransactionDefinition(
            String key, TransactionDefinitionUpdateModel transactionDefinitionModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinition> existingTransactionDefinition =
                transactionDefinitionService.getTransactionDefinitionByKey(key);

        if (existingTransactionDefinition.isEmpty()) {
            throw new NotFoundException("Transaction definition not found");
        }

        // Set key from request
        final TransactionDefinition transactionDefinition =
                TransactionDefinitionMapper.INSTANCE.updateModelToTransactionDefinition(
                        transactionDefinitionModel);
        transactionDefinition.setKey(key);

        // ensure that any existing ID is used for the given key
        transactionDefinition.setId(existingTransactionDefinition.get().getId());
        transactionDefinition.setCreatedBy(existingTransactionDefinition.get().getCreatedBy());

        if (!validateDefaultFormConfigKeyNullOrEmpty(transactionDefinition)) {
            transactionDefinition.setDefaultFormConfigurationKey(
                    transactionDefinition.getDefaultFormConfigurationKey());
        } else {
            transactionDefinition.setDefaultFormConfigurationKey(
                    existingTransactionDefinition.get().getDefaultFormConfigurationKey());
        }

        transactionDefinition.setCreatedTimestamp(
                existingTransactionDefinition.get().getCreatedTimestamp());

        // validate link with transaction definition set
        transactionDefinitionService.validateTransactionDefinitionSetLink(transactionDefinition);

        final TransactionDefinition updated =
                transactionDefinitionService.saveTransactionDefinition(transactionDefinition);

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionMapper.INSTANCE.transactionDefinitionToResponseModel(
                                updated));
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> postFormConfiguration(
            String transactionDefinitionKey,
            FormConfigurationCreateModel formConfigurationCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, formConfigurationCreateModel.getKey());

        if (existingFormConfig.isPresent()) {
            throw new ConflictException("Form configuration already exists");
        }

        final FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapCreationModelToFormConfiguration(
                        formConfigurationCreateModel);
        formConfiguration.setTransactionDefinitionKey(transactionDefinitionKey);

        return ResponseEntity.status(200)
                .body(
                        FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                                formConfigurationService.saveFormConfiguration(formConfiguration)));
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> getFormConfiguration(
            String transactionDefinitionKey, String formKey) {
        final Optional<FormConfigurationResponseModel> result =
                formConfigurationService
                        .getFormConfigurationByKeys(transactionDefinitionKey, formKey)
                        .filter(
                                formConfiguration ->
                                        authorizationHandler.isAllowedForInstance(
                                                "view", formConfiguration))
                        .map(FormConfigurationMapper.INSTANCE::mapFormConfigurationToModel);

        return result.map(
                        formConfigurationResponseModel ->
                                ResponseEntity.status(200).body(formConfigurationResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<FormConfigurationResponseModel> putFormConfiguration(
            String transactionDefinitionKey,
            String formKey,
            FormConfigurationUpdateModel formConfigurationUpdateModel) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, FormConfiguration.class)) {
            throw new ForbiddenException();
        }

        Optional<FormConfiguration> existingFormConfig =
                formConfigurationService.getFormConfigurationByKeys(
                        transactionDefinitionKey, formKey);

        if (existingFormConfig.isEmpty()) {
            throw new NotFoundException("Form configuration not found");
        }

        final FormConfiguration formConfiguration =
                FormConfigurationMapper.INSTANCE.mapModelToFormConfiguration(
                        formConfigurationUpdateModel);
        formConfiguration.setTransactionDefinitionKey(transactionDefinitionKey);
        formConfiguration.setKey(formKey);

        return ResponseEntity.ok(
                FormConfigurationMapper.INSTANCE.mapFormConfigurationToModel(
                        formConfigurationService.saveFormConfiguration(formConfiguration)));
    }

    @Override
    public ResponseEntity<TransactionDefinitionResponseModel> postTransactionDefinition(
            TransactionDefinitionCreateModel transactionDefinitionCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionDefinition.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinition> existingTransactionDefinition =
                transactionDefinitionService.getTransactionDefinitionByKey(
                        transactionDefinitionCreateModel.getKey());

        if (existingTransactionDefinition.isPresent()) {
            throw new ConflictException("Transaction definition already exists");
        }

        final TransactionDefinition transactionDefinition =
                TransactionDefinitionMapper.INSTANCE.createModelToTransactionDefinition(
                        transactionDefinitionCreateModel);

        transactionDefinitionService.validateTransactionDefinitionSetLink(transactionDefinition);

        FormConfiguration formConfiguration =
                formConfigurationService.createDefaultFormConfiguration(transactionDefinition);

        if (validateDefaultFormConfigKeyNullOrEmpty(transactionDefinition)) {
            transactionDefinition.setDefaultFormConfigurationKey(formConfiguration.getKey());
        } else {
            transactionDefinition.setDefaultFormConfigurationKey(
                    transactionDefinition.getDefaultFormConfigurationKey());
        }

        TransactionDefinition savedTransaction =
                transactionDefinitionService.saveTransactionDefinition(transactionDefinition);

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionMapper.INSTANCE.transactionDefinitionToResponseModel(
                                savedTransaction));
    }

    @Override
    public ResponseEntity<TransactionLinkTypeModel> postTransactionLinkType(
            TransactionLinkTypeModel transactionLinkTypeModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionLinkType.class)) {
            throw new ForbiddenException();
        }

        final TransactionLinkType transactionLinkType =
                transactionLinkTypeService.saveTransactionLinkType(
                        TransactionLinkTypeMapper.INSTANCE
                                .transactionLinkTypeModelToTransactionLinkType(
                                        transactionLinkTypeModel));

        return ResponseEntity.status(201)
                .body(
                        TransactionLinkTypeMapper.INSTANCE
                                .transactionLinkTypeToTransactionLinkTypeModel(
                                        transactionLinkType));
    }

    @Override
    public ResponseEntity<List<TransactionLinkTypeModel>> getTransactionLinkTypes() {
        final List<TransactionLinkTypeModel> results =
                transactionLinkTypeService.getTransactionLinkTypes().stream()
                        .filter(
                                authorizationHandler.getAuthFilter(
                                        "view", TransactionLinkType.class))
                        .map(
                                TransactionLinkTypeMapper.INSTANCE
                                        ::transactionLinkTypeToTransactionLinkTypeModel)
                        .collect(Collectors.toList());

        return ResponseEntity.status(200).body(results);
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetResponseModel> getTransactionSet(String key) {
        if (!authorizationHandler.isAllowed("view", "transaction_definition_set")) {
            throw new ForbiddenException();
        }
        return transactionDefinitionSetService
                .getTransactionDefinitionSet(key)
                .map(
                        TransactionDefinitionSetMapper.INSTANCE
                                ::transactionDefinitionSetToResponseModel)
                .map(
                        transactionDefinitionSetResponseModel ->
                                ResponseEntity.status(200)
                                        .body(transactionDefinitionSetResponseModel))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @Override
    public ResponseEntity<PagedTransactionDefinitionSetModel> getTransactionSets(
            String sortOrder, Integer pageNumber, Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", "transaction_definition_set")) {
            throw new ForbiddenException();
        }
        TransactionDefinitionSetFilter filters =
                new TransactionDefinitionSetFilter(sortOrder, pageNumber, pageSize);

        final Page<TransactionDefinitionSetResponseModel> results =
                transactionDefinitionSetService
                        .getAllTransactionDefinitionSets(filters)
                        .map(
                                TransactionDefinitionSetMapper.INSTANCE
                                        ::transactionDefinitionSetToResponseModel);

        PagedTransactionDefinitionSetModel response = new PagedTransactionDefinitionSetModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetResponseModel> putTransactionSet(
            String key, TransactionDefinitionSetUpdateModel transactionDefinitionSetRequestModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionDefinitionSet.class)) {
            throw new ForbiddenException();
        }
        Optional<TransactionDefinitionSet> existingTransactionDefinitionSet =
                transactionDefinitionSetService.getTransactionDefinitionSet(key);

        if (existingTransactionDefinitionSet.isEmpty()) {
            throw new NotFoundException("Transaction definition set not found");
        }

        TransactionDefinitionSet saved =
                transactionDefinitionSetService.save(
                        key,
                        TransactionDefinitionSetMapper.INSTANCE
                                .updateModelToTransactionDefinitionSet(
                                        transactionDefinitionSetRequestModel));

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionSetMapper.INSTANCE
                                .transactionDefinitionSetToResponseModel(saved));
    }

    @Override
    public ResponseEntity<Void> deleteTransactionSet(String key) {
        if (!authorizationHandler.isAllowed("delete", TransactionDefinitionSet.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinitionSet> transactionDefinitionSet =
                transactionDefinitionSetService.getTransactionDefinitionSet(key);

        if (transactionDefinitionSet.isEmpty()) {
            throw new NotFoundException("Transaction definition set not found: " + key);
        }

        try {
            transactionDefinitionSetService.deleteTransactionDefinitionSet(
                    transactionDefinitionSet.get());
        } catch (RuntimeException e) {
            throw new ConflictException(
                    "Transaction definition set is being used and cannot be deleted");
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetResponseModel> postTransactionSet(
            TransactionDefinitionSetCreateModel transactionDefinitionSetCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, TransactionDefinitionSet.class)) {
            throw new ForbiddenException();
        }

        Optional<TransactionDefinitionSet> existingTransactionDefinitionSet =
                transactionDefinitionSetService.getTransactionDefinitionSet(
                        transactionDefinitionSetCreateModel.getKey());

        if (existingTransactionDefinitionSet.isPresent()) {
            throw new ConflictException("Transaction definition set already exists");
        }

        TransactionDefinitionSet saved =
                transactionDefinitionSetService.save(
                        transactionDefinitionSetCreateModel.getKey(),
                        TransactionDefinitionSetMapper.INSTANCE
                                .createModelToTransactionDefinitionSet(
                                        transactionDefinitionSetCreateModel));

        return ResponseEntity.status(200)
                .body(
                        TransactionDefinitionSetMapper.INSTANCE
                                .transactionDefinitionSetToResponseModel(saved));
    }

    @Override
    public ResponseEntity<AllowedLinkModel> postAllowedLinkToDefinition(
            AllowedLinkCreationRequest request) {
        if (!authorizationHandler.isAllowed(CREATE_CERBOS_ACTION, AllowedLink.class)) {
            throw new ForbiddenException();
        }

        final AllowedLink allowedLink =
                allowedLinkService.saveAllowedLink(
                        AllowedLinkMapper.INSTANCE.allowedLinkRequestToAllowedLink(request),
                        request.getTransactionLinkTypeId());

        return ResponseEntity.status(201)
                .body(AllowedLinkMapper.INSTANCE.allowedLinkToAllowedLinkModel(allowedLink));
    }

    @Override
    public ResponseEntity<List<AllowedLinkModel>> getTransactionDefinitionAllowedLinksByKey(
            String key) {
        final List<AllowedLinkModel> results =
                allowedLinkService.getAllowedLinksByDefinitionKey(key).stream()
                        .filter(authorizationHandler.getAuthFilter("view", AllowedLink.class))
                        .map(AllowedLinkMapper.INSTANCE::allowedLinkToAllowedLinkModel)
                        .collect(Collectors.toList());

        return ResponseEntity.status(200).body(results);
    }

    @Override
    public ResponseEntity<List<String>> consistencyCheck() {
        if (!authorizationHandler.isAllowed("view", "admin_dashboard")) {
            throw new ForbiddenException();
        }

        final List<String> results = consistencyChecker.check();
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<PagedWorkflowModel> listWorkflows(
            String sortOrder, Integer pageNumber, Integer pageSize) {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<WorkflowModel> workflowModels =
                workflowTasksService
                        .getAllWorkflows(pageable, sortOrder)
                        .map(workflowAndTaskMapper::processDefinitionToWorkflowModel);
        return ResponseEntity.ok(generatePagedWorkflowModel(workflowModels));
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflowByProcessDefinitionKey(
            String processDefinitionKey) {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                workflowAndTaskMapper.processDefinitionToWorkflowModel(
                        workflowTasksService.getSingleWorkflow(processDefinitionKey)));
    }

    @Override
    public ResponseEntity<List<TaskModel>> getUsersTasksByProcessDefinitionKey(
            String processDefinitionKey) {
        if (!authorizationHandler.isAllowed(EXPORT_CERBOS_ACTION, CONFIGURATION_CERBOS_RESORCE)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                workflowTasksService
                        .getListOfTasksByProcessDefinitionKey(processDefinitionKey)
                        .stream()
                        .map(workflowAndTaskMapper::userTaskToTaskModel)
                        .collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<List<TransactionDefinitionSetDashboardResultModel>> getDashboards() {
        if (!authorizationHandler.isAllowed("view", "dashboard_configuration")) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                dashboardConfigurationService.getAllDashboards().stream()
                        .map(
                                dashboardConfiguration -> {
                                    List<String> transactionDefinitionKeys =
                                            transactionDefinitionService
                                                    .getTransactionDefinitionsBySetKey(
                                                            dashboardConfiguration
                                                                    .getTransactionDefinitionSet()
                                                                    .getKey())
                                                    .stream()
                                                    .map(TransactionDefinition::getKey)
                                                    .collect(Collectors.toList());
                                    return dashboardConfigurationMapper
                                            .dashboardConfigurationToDashboardResultModel(
                                                    dashboardConfiguration,
                                                    transactionDefinitionKeys);
                                })
                        .collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<TransactionDefinitionSetDashboardResultModel>
            getDashboardByTransactionSetKey(String key) {
        if (!authorizationHandler.isAllowed("view", "dashboard_configuration")) {
            throw new ForbiddenException();
        }

        DashboardConfiguration dashboardConfiguration =
                dashboardConfigurationService.getDashboardByTransactionSetKey(key);
        List<String> transactionDefinitionKeys =
                transactionDefinitionService
                        .getTransactionDefinitionsBySetKey(
                                dashboardConfiguration.getTransactionDefinitionSet().getKey())
                        .stream()
                        .map(TransactionDefinition::getKey)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(
                dashboardConfigurationMapper.dashboardConfigurationToDashboardResultModel(
                        dashboardConfiguration, transactionDefinitionKeys));
    }

    @Override
    public ResponseEntity<List<String>> getDashboardOrder() {
        if (!authorizationHandler.isAllowed("view", "dashboard_configuration")) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(
                transactionDefinitionSetOrderService.getTransactionDefinitionSetOrderAsString());
    }

    @Override
    public ResponseEntity<Void> updateDashboardOrder(List<String> newOrder) {
        if (!authorizationHandler.isAllowed(UPDATE_CERBOS_ACTION, "dashboard_configuration")) {
            throw new ForbiddenException();
        }

        transactionDefinitionSetOrderService.updateTransactionSetKeyOrder(newOrder);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<List<DashboardCountsModel>> getTransactionDefinitionSetCounts(
            String transactionSetKey) {
        if (!authorizationHandler.isAllowed("view", "dashboard_configuration")) {
            throw new ForbiddenException();
        }

        List<DashboardCountsModel> result =
                dashboardConfigurationService
                        .countTabsForDashboard(transactionSetKey)
                        .entrySet()
                        .stream()
                        .map(
                                countEntry ->
                                        dashboardConfigurationMapper.mapCount(
                                                countEntry.getKey(), countEntry.getValue()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok().body(result);
    }

    private PagedWorkflowModel generatePagedWorkflowModel(Page<WorkflowModel> workflows) {
        PagedWorkflowModel model = new PagedWorkflowModel();
        model.items(workflows.toList());
        model.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(workflows));
        return model;
    }

    private boolean validateDefaultFormConfigKeyNullOrEmpty(
            TransactionDefinition transactionDefinition) {
        return transactionDefinition.getDefaultFormConfigurationKey() == null
                || transactionDefinition.getDefaultFormConfigurationKey().isEmpty();
    }
}

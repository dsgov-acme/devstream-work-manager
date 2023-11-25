package io.nuvalence.workmanager.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.logging.util.CorrelationIdContext;
import io.nuvalence.workmanager.auditservice.client.ApiException;
import io.nuvalence.workmanager.auditservice.client.generated.api.AuditEventsApi;
import io.nuvalence.workmanager.auditservice.client.generated.models.ActivityEventData;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventId;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventRequest;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventRequestEventData;
import io.nuvalence.workmanager.auditservice.client.generated.models.RequestContext;
import io.nuvalence.workmanager.auditservice.client.generated.models.StateChangeEventData;
import io.nuvalence.workmanager.service.audit.AuditServiceApiClient;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for managing transaction audit events.
 */
@Service
@RequiredArgsConstructor
public class TransactionAuditEventService {

    private final AuditServiceApiClient auditServiceTokenApiClient;

    private final RequestContextTimestamp requestContextTimestamp;

    /**
     * Create an api client to send requests to Audit Service.
     *
     * @param apiClient The token authenticated client.
     * @return The api client.
     */
    protected AuditEventsApi createAuditEventsApi(AuditServiceApiClient apiClient) {
        return new AuditEventsApi(apiClient);
    }

    /**
     * Post state change events to audit service.
     *
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @param oldState state of the business object previous to event.
     * @param newState state of the business object after the event.
     * @param activityType type of event action.
     * @return Result audit event id.
     * @throws ApiException for possible errors reaching audit service.
     */
    public AuditEventId postStateChangeEvent(
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType,
            String oldState,
            String newState,
            String activityType)
            throws ApiException {

        StateChangeEventData stateChangeEventData =
                createStateChangeEventData(oldState, newState, activityType, null);
        return postAuditEvent(
                stateChangeEventData,
                originatorId,
                userId,
                summary,
                businessObjectId,
                businessObjectType);
    }

    /**
     * Post state change events to audit service.
     *
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @param oldStateMap map state of the business object previous to event.
     * @param newStateMap map state of the business object after the event.
     * @param data data of the event in json form.
     * @param activityType type of event action.
     * @return Result audit event id.
     * @throws ApiException for possible errors reaching audit service.
     * @throws JsonProcessingException for possible errors converting Map states to String
     */
    public AuditEventId postStateChangeEvent(
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType,
            Map<String, String> oldStateMap,
            Map<String, String> newStateMap,
            String data,
            String activityType)
            throws ApiException, JsonProcessingException {

        String oldState = SpringConfig.getMapper().writeValueAsString(oldStateMap);
        String newState = SpringConfig.getMapper().writeValueAsString(newStateMap);

        StateChangeEventData stateChangeEventData =
                createStateChangeEventData(oldState, newState, activityType, data);
        return postAuditEvent(
                stateChangeEventData,
                originatorId,
                userId,
                summary,
                businessObjectId,
                businessObjectType);
    }

    /**
     * Post state change events to audit service.
     *
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @param jsonData data of the event in json form .
     * @param activityType type of activity that occurred.
     * @return Result audit event id.
     * @throws ApiException for possible errors reaching audit service.
     */
    public AuditEventId postActivityAuditEvent(
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType,
            String jsonData,
            AuditActivityType activityType)
            throws ApiException {

        ActivityEventData auditEventRequestEventData =
                createActivityEventData(jsonData, activityType);
        return postAuditEvent(
                auditEventRequestEventData,
                originatorId,
                userId,
                summary,
                businessObjectId,
                businessObjectType);
    }

    /**
     * Post state change events to audit service.
     *
     * @param data object containing specifics of the audit evet.
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @return Result audit event id.
     * @throws ApiException for possible errors reaching audit service.
     */
    private AuditEventId postAuditEvent(
            Object data,
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType)
            throws ApiException {
        AuditEventRequestEventData eventData = new AuditEventRequestEventData();
        eventData.setActualInstance(data);

        RequestContext requestContext =
                createRequestContextWithOriginatorAndUserIds(
                        UUID.fromString(originatorId), UUID.fromString(userId));

        AuditEventRequest auditEventRequest = new AuditEventRequest();
        auditEventRequest.setSummary(summary);
        auditEventRequest.setTimestamp(requestContextTimestamp.getCurrentTimestamp());
        auditEventRequest.setEventData(eventData);
        auditEventRequest.setRequestContext(requestContext);

        AuditEventsApi auditEventsApi = createAuditEventsApi(auditServiceTokenApiClient);

        return auditEventsApi.postEvent(
                businessObjectType.getValue(), businessObjectId, auditEventRequest);
    }

    private StateChangeEventData createStateChangeEventData(
            String oldState, String newState, String activityType, String data) {
        StateChangeEventData stateChangeEventData = new StateChangeEventData();
        stateChangeEventData.setOldState(oldState);
        stateChangeEventData.setNewState(newState);
        stateChangeEventData.setActivityType(activityType);
        if (data != null) {
            stateChangeEventData.setData(data);
        }

        return stateChangeEventData;
    }

    private ActivityEventData createActivityEventData(
            String jsonData, AuditActivityType activityType) {
        ActivityEventData activityEventData = new ActivityEventData();
        activityEventData.setData(jsonData);
        activityEventData.setActivityType(activityType.getValue());

        return activityEventData;
    }

    private RequestContext createRequestContextWithOriginatorAndUserIds(
            UUID originatorId, UUID userId) {
        RequestContext requestContext = new RequestContext();
        requestContext.setOriginatorId(originatorId);
        requestContext.setUserId(userId);
        requestContext.setTraceId(UUID.fromString(CorrelationIdContext.getCorrelationId()));

        return requestContext;
    }
}

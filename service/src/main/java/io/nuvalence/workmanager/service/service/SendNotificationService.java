package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.notification.client.ApiException;
import io.nuvalence.workmanager.notification.client.generated.api.SendNotificationApi;
import io.nuvalence.workmanager.notification.client.generated.models.MessageRequestModel;
import io.nuvalence.workmanager.notification.client.generated.models.MessageResponseModel;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.notification.NotificationServiceApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to send notification requests to notification service.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SendNotificationService {

    private final NotificationServiceApiClient apiClient;

    @Value("${dashboard.url}")
    private String dashboardUrl;

    protected SendNotificationApi createSendNotificationsApi(
            NotificationServiceApiClient apiClient) {
        return new SendNotificationApi(apiClient);
    }

    private Map<String, String> createNotificationParameterMap(
            Map<String, String> camundaPropertyMap, Transaction transaction) {
        Map<String, String> propertyMap = new HashMap<>();
        camundaPropertyMap.forEach(
                (key, value) -> {
                    try {
                        if (key.equals("portal-url")) {
                            propertyMap.put("portal-url", dashboardUrl);
                        } else {
                            propertyMap.put(
                                    key, PropertyUtils.getProperty(transaction, value).toString());
                        }
                    } catch (Exception e) {
                        log.error("Error occurred getting value for property", e);
                    }
                });

        return propertyMap;
    }

    /**
     * Sends a notification request.
     * @param transaction Transaction whose notification is to be sent.
     * @param notificationKey Key for the message template.
     * @param properties Properties to complete message template.
     * @return An optional response.
     */
    public Optional<MessageResponseModel> sendNotification(
            Transaction transaction, String notificationKey, Map<String, String> properties) {
        MessageRequestModel messageRequestModel = new MessageRequestModel();
        messageRequestModel.setUserId(UUID.fromString(transaction.getSubjectUserId()));
        messageRequestModel.setTemplateKey(notificationKey);
        messageRequestModel.setParameters(createNotificationParameterMap(properties, transaction));

        SendNotificationApi sendNotificationApi = createSendNotificationsApi(apiClient);

        try {
            MessageResponseModel messageResponseModel =
                    sendNotificationApi.sendMessage(messageRequestModel);
            log.trace(
                    "Obtained the following response when sending a notification {}",
                    messageResponseModel);
            return Optional.of(messageResponseModel);
        } catch (ApiException e) {
            log.warn(
                    "An exception occurred when sending a notification for transaction {}: {}",
                    transaction.getId(),
                    e.getMessage());
            log.warn("Exception: ", e);
        }

        return Optional.empty();
    }
}

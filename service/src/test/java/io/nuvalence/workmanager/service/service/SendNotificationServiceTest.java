package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.nuvalence.workmanager.notification.client.ApiException;
import io.nuvalence.workmanager.notification.client.generated.api.SendNotificationApi;
import io.nuvalence.workmanager.notification.client.generated.models.MessageRequestModel;
import io.nuvalence.workmanager.notification.client.generated.models.MessageResponseModel;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.notification.NotificationServiceApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SendNotificationServiceTest {

    @Mock private NotificationServiceApiClient notificationServiceTokenApiClient;

    private SendNotificationService sendNotificationService;

    @BeforeEach
    void setup() {
        sendNotificationService =
                Mockito.spy(new SendNotificationService(notificationServiceTokenApiClient));
    }

    @Test
    void testSendNotification() throws ApiException {
        Map<String, String> properties = new HashMap<>();
        properties.put("transactionId", "id");

        SendNotificationApi sendNotificationApi = mock(SendNotificationApi.class);
        doReturn(sendNotificationApi)
                .when(sendNotificationService)
                .createSendNotificationsApi(notificationServiceTokenApiClient);

        MessageResponseModel expectedMessageResponseModel = new MessageResponseModel();
        expectedMessageResponseModel.setId(UUID.randomUUID());

        String notificationKey = "TestTemplate";
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .subjectUserId(UUID.randomUUID().toString())
                        .build();
        MessageRequestModel requestModel = new MessageRequestModel();
        requestModel.setUserId(UUID.fromString(transaction.getSubjectUserId()));
        requestModel.setTemplateKey(notificationKey);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("transactionId", transaction.getId().toString());
        requestModel.setParameters(parameters);

        when(sendNotificationApi.sendMessage(requestModel))
                .thenReturn(expectedMessageResponseModel);
        Optional<MessageResponseModel> optionalResult =
                sendNotificationService.sendNotification(transaction, notificationKey, properties);

        assertTrue(optionalResult.isPresent());
        assertEquals(expectedMessageResponseModel, optionalResult.get());
    }
}

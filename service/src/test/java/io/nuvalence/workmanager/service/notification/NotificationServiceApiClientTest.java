package io.nuvalence.workmanager.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.utils.api.ApiClientCommonInterceptorProvider;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

class NotificationServiceApiClientTest {

    @Test
    void testProperInterceptorUsage() {

        var wantedInterceptor = mock(Consumer.class);
        var provider = mock(ApiClientCommonInterceptorProvider.class);
        when(provider.getRequestInterceptor(any())).thenReturn(wantedInterceptor);

        NotificationServiceApiClient apiClient = new NotificationServiceApiClient(provider);

        var requestInterceptor = apiClient.getRequestInterceptor();

        assertEquals(wantedInterceptor, requestInterceptor);
    }
}

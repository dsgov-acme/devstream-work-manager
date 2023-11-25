package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.utils.api.ApiClientCommonInterceptorProvider;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

class AuditServiceApiClientTest {

    @Test
    void testProperInterceptorUsage() {

        var provider = mock(ApiClientCommonInterceptorProvider.class);
        var wantedInterceptor = mock(Consumer.class);
        when(provider.getRequestInterceptor(any())).thenReturn(wantedInterceptor);

        AuditServiceApiClient apiClient = new AuditServiceApiClient(provider);

        var requestInterceptor = apiClient.getRequestInterceptor();

        assertEquals(wantedInterceptor, requestInterceptor);
    }
}

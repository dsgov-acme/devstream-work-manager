package io.nuvalence.workmanager.service.notification;

import io.nuvalence.workmanager.notification.client.ApiClient;
import io.nuvalence.workmanager.service.utils.api.ApiClientCommonInterceptorProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;

/**
 * Notification client with token.
 */
@Component
@RequiredArgsConstructor
public class NotificationServiceApiClient extends ApiClient {

    private final ApiClientCommonInterceptorProvider interceptorProvider;

    @Value("${notificationService.baseUrl}")
    private String baseUrl;

    /**
     * Adds token to client.
     */
    @PostConstruct
    public void init() {
        this.updateBaseUri(baseUrl);
    }

    /**
     * Interceptor for notification service client.
     *
     * @return authenticated Consumer.
     */
    @Override
    public Consumer<HttpRequest.Builder> getRequestInterceptor() {
        return interceptorProvider.getRequestInterceptor(super.getRequestInterceptor());
    }
}

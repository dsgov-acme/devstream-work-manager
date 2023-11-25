package io.nuvalence.workmanager.service.audit;

import io.nuvalence.workmanager.auditservice.client.ApiClient;
import io.nuvalence.workmanager.service.utils.api.ApiClientCommonInterceptorProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;

/**
 * Provides authentication for audit service.
 */
@Component
@RequiredArgsConstructor
public class AuditServiceApiClient extends ApiClient {

    private final ApiClientCommonInterceptorProvider interceptorProvider;

    @Value("${auditService.baseUrl}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        this.updateBaseUri(baseUrl);
    }

    /**
     * Interceptor for audit service client.
     *
     * @return authenticated Consumer.
     */
    @Override
    public Consumer<HttpRequest.Builder> getRequestInterceptor() {
        return interceptorProvider.getRequestInterceptor(super.getRequestInterceptor());
    }
}

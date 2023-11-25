package io.nuvalence.workmanager.service.config;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.access.cerbos.CerbosAuthorizationHandler;
import io.nuvalence.workmanager.service.utils.JacocoIgnoreInGeneratedReport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

/**
 * Configures CerbosAuthorizationHandler.
 */
@Configuration
@RequiredArgsConstructor
@Profile("!test")
@JacocoIgnoreInGeneratedReport(
        reason =
                "Initialization has side effects making unit tests difficult. Tested in acceptance"
                        + " tests.")
public class CerbosConfig {

    @Value("${cerbos.uri}")
    private String cerbosUri;

    /**
     * Initializes a CerbosAuthorizationHandler as a singleton bean.
     *
     * @return AuthorizationHandler
     * @throws CerbosClientBuilder.InvalidClientConfigurationException if cerbos URI is invalid
     */
    @Bean
    @Scope("singleton")
    public AuthorizationHandler getAuthorizationHandler()
            throws CerbosClientBuilder.InvalidClientConfigurationException {
        final CerbosBlockingClient cerbosClient =
                new CerbosClientBuilder(cerbosUri).withPlaintext().buildBlockingClient();

        return new CerbosAuthorizationHandler(cerbosClient);
    }
}

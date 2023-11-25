package io.nuvalence.workmanager.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementClient;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.usermanagementapi.models.ApplicationRoles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * ApplicationRoleConfig class.
 */
@Component
@Slf4j
@Profile("!test")
public abstract class ApplicationRoleConfig {

    private static final String CONFIG = "roles.json";
    private final UserManagementService client;

    protected ApplicationRoleConfig(UserManagementClient client) {
        this.client = client;
    }

    /**
     * Publishes roles to User Management from JSON file.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publishRoles() {
        try {
            final Resource rolesResource = new ClassPathResource(CONFIG);

            if (!rolesResource.exists()) {
                throw new UnexpectedException("Role configuration file does not exist.");
            }

            try (final InputStream fileStream = rolesResource.getInputStream()) {
                ApplicationRoles roles =
                        new ObjectMapper().readValue(fileStream, ApplicationRoles.class);

                this.client.publishRoles(roles);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    protected abstract Resource getResource();

    protected abstract ObjectMapper getObjectMapper();
}

package io.nuvalence.workmanager.service.repository;

import io.nuvalence.workmanager.service.domain.formconfig.FormConfiguration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Form Configurations.
 */
public interface FormConfigurationRepository extends CrudRepository<FormConfiguration, UUID> {

    List<FormConfiguration> findByTransactionDefinitionKey(String transactionDefinitionKey);

    @Query(
            "SELECT td FROM FormConfiguration td WHERE td.transactionDefinitionKey ="
                    + " :transactionDefinitionKey AND td.key = :key")
    List<FormConfiguration> searchByKeys(
            @Param("transactionDefinitionKey") String transactionDefinitionKey,
            @Param("key") String keu);
}

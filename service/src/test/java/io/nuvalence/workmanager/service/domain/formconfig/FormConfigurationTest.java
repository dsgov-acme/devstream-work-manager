package io.nuvalence.workmanager.service.domain.formconfig;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class FormConfigurationTest {
    @Test
    void equalsHashcodeContract() {
        EqualsVerifier.forClass(FormConfiguration.class).usingGetClass().verify();
    }
}

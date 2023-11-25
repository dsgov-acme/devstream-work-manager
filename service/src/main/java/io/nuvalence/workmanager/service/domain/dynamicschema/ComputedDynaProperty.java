package io.nuvalence.workmanager.service.domain.dynamicschema;

import lombok.Getter;
import org.apache.commons.beanutils.DynaProperty;

/**
 * DynaProperty with an expression to compute the value.
 */
public class ComputedDynaProperty extends DynaProperty {
    private static final long serialVersionUID = 8941849384116171194L;

    @Getter private final String expression;

    public ComputedDynaProperty(String name, Class<?> type, String expression) {
        super(name, type);
        this.expression = expression;
    }
}

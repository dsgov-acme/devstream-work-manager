package io.nuvalence.workmanager.service.utils.formconfig.formio;

import io.nuvalence.workmanager.service.config.exceptions.model.NuvalenceFormioValidationExItem;
import io.nuvalence.workmanager.service.domain.dynamicschema.DynamicEntity;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponent;
import io.nuvalence.workmanager.service.domain.formconfig.formio.NuvalenceFormioComponentOption;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.validator.GenericValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * DynaBeans validator for transactions based on a specific form configuration converted to a NuvalenceFormioComponent.
 */
@Slf4j
public class NuvalenceFormioValidator {

    private NuvalenceFormioValidator() {
        throw new AssertionError(
                "Utility class should not be instantiated, use the static methods.");
    }

    /**
     * Transaction update dynaEntity validator.
     *
     * @param component Form configuration converted to the Nuvalence altered version of the Form.io specification
     * @param dynaEntity Data submitted when requesting a transaction update
     * @param formioValidationErrors list of validation errors found
     */
    public static void validateComponent(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        if (component.getKey() != null) {
            Pair<Object, Class<?>> field = getFieldValue(component.getKey(), dynaEntity);

            // Validate expressions
            validateExpressions(component, dynaEntity, formioValidationErrors);

            // Validate props
            validateComponentProps(component, field, formioValidationErrors);

            // Apply validators
            applyValidators(component, field, formioValidationErrors);
        }

        validateSubComponents(component, formioValidationErrors, dynaEntity);
    }

    private static void validateExpressions(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        if (component.getExpressions() != null) {
            overrideValidationsBasedOnExpressions(component, dynaEntity, formioValidationErrors);
        }
    }

    private static void applyValidators(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        if (component.getValidators() != null
                && component.getValidators().getValidation() != null) {
            for (String validation : component.getValidators().getValidation()) {
                if (validation.equals("email")
                        && fieldIsNotNull(field)
                        && field.getSecond().equals(String.class)) {
                    String fieldValue = (String) field.getFirst();
                    if (!GenericValidator.isEmail(fieldValue)) {
                        addValidationError(component, "email", formioValidationErrors);
                    }
                }
            }
        }
    }

    private static void validateSubComponents(
            NuvalenceFormioComponent component,
            List<NuvalenceFormioValidationExItem> formioValidationErrors,
            DynamicEntity dynaEntity) {
        if (component.getComponents() != null) {
            for (NuvalenceFormioComponent subComponent : component.getComponents()) {
                validateComponent(subComponent, dynaEntity, formioValidationErrors);
            }
        }
    }

    private static void overrideValidationsBasedOnExpressions(
            NuvalenceFormioComponent component,
            DynamicEntity dynaEntity,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        final EntityMapper mapper = EntityMapper.getInstance();
        Map<String, Object> data = mapper.convertAttributesToGenericMap(dynaEntity);

        for (Map.Entry<String, String> expressionEntry : component.getExpressions().entrySet()) {
            String expressionKey = expressionEntry.getKey();
            String expressionValue = expressionEntry.getValue();

            if ((expressionKey.equals("hide") || expressionKey.equals("props.hidden"))
                    && evaluateExpression(
                            expressionValue, data, component, formioValidationErrors)) {
                clearValidationProps(component);
            }
            if ((expressionKey.equals("require") || expressionKey.equals("props.required"))
                    && evaluateExpression(
                            expressionValue, data, component, formioValidationErrors)) {
                component.getProps().setRequired(true);
            }
        }
    }

    private static void clearValidationProps(NuvalenceFormioComponent component) {
        component.getProps().setRequired(false);
        component.getProps().setMax(null);
        component.getProps().setMin(null);
        component.getProps().setMaxLength(null);
        component.getProps().setMinLength(null);
        component.getProps().setPattern(null);

        if (component.getComponents() != null) {
            for (NuvalenceFormioComponent childComponent : component.getComponents()) {
                clearValidationProps(childComponent);
            }
        }
    }

    private static boolean evaluateExpression(
            String expression,
            Map<String, Object> data,
            NuvalenceFormioComponent component,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        String normalizedExpression = expression.replace("?.", ".");
        final ScriptEngine javaScriptEngine =
                new ScriptEngineManager().getEngineByName("JavaScript");

        javaScriptEngine.put("model", data);
        try {
            return Boolean.valueOf(javaScriptEngine.eval(normalizedExpression).toString());
        } catch (ScriptException e) {
            log.debug("javascript expression error", e);
            addValidationError(component, "expression", formioValidationErrors);

            return false;
        }
    }

    private static void validateComponentProps(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        if (component.getProps() == null) {
            return;
        }

        validateRequiredField(component, field, formioValidationErrors);

        if (!fieldIsNotNull(field)) {
            return;
        }

        validateMaxValue(component, field, formioValidationErrors);
        validateMinValue(component, field, formioValidationErrors);
        validateRelativeMaxDate(component, field, formioValidationErrors);
        validateRelativeMinDate(component, field, formioValidationErrors);
        validateMaxDate(component, field, formioValidationErrors);
        validateMinDate(component, field, formioValidationErrors);
        validateFieldLength(component, field, formioValidationErrors, true);
        validateFieldLength(component, field, formioValidationErrors, false);
        validatePattern(component, field, formioValidationErrors);
        validateSelectOptions(component, field, formioValidationErrors);
    }

    private static void validateRequiredField(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        if (component.getProps().isRequired() && !isFieldPresent(field)) {
            addValidationError(component, "required", formioValidationErrors);
        }
    }

    private static void validateMaxValue(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        BigDecimal max = component.getProps().getMax();
        if (max == null) {
            return;
        }

        BigDecimal value = getNumericValue(field);
        if (value != null && value.compareTo(max) > 0) {
            addValidationError(component, "max", formioValidationErrors);
        }
    }

    private static void validateMinValue(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        BigDecimal min = component.getProps().getMin();
        if (min == null) {
            return;
        }

        BigDecimal value = getNumericValue(field);
        if (value != null && value.compareTo(min) < 0) {
            addValidationError(component, "min", formioValidationErrors);
        }
    }

    private static void validateRelativeMaxDate(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        validateRelativeDate(
                component,
                field,
                formioValidationErrors,
                component.getProps().getRelativeMaxDate(),
                LocalDate::isAfter,
                "relativeMaxDate");
    }

    private static void validateRelativeMinDate(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        validateRelativeDate(
                component,
                field,
                formioValidationErrors,
                component.getProps().getRelativeMinDate(),
                LocalDate::isBefore,
                "relativeMinDate");
    }

    private static void validateRelativeDate(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors,
            String relativeDate,
            DateValidationCondition validationCondition,
            String errorType) {

        if (relativeDate == null) {
            return;
        }

        String[] parts = relativeDate.replaceFirst("^-", "").split("-");
        int numberOfUnitInt =
                Integer.parseInt((relativeDate.startsWith("-") ? "-" : "") + parts[0]);
        String dateUnit = parts[1];

        LocalDate validationDate = LocalDate.now();

        switch (dateUnit) {
            case "day":
                validationDate = validationDate.plusDays(numberOfUnitInt);
                break;
            case "week":
                validationDate = validationDate.plusWeeks(numberOfUnitInt);
                break;
            case "month":
                validationDate = validationDate.plusMonths(numberOfUnitInt);
                break;
            case "year":
                validationDate = validationDate.plusYears(numberOfUnitInt);
                break;
            default:
                throw new IllegalArgumentException("Invalid date unit");
        }

        LocalDate value = (LocalDate) field.getFirst();

        if (validationCondition.test(value, validationDate)) {
            addValidationError(component, errorType, formioValidationErrors);
        }
    }

    private static void validateMaxDate(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        LocalDate max = component.getProps().getMaxDate();
        if (max == null) {
            return;
        }

        LocalDate value = (LocalDate) field.getFirst();
        if (value.isAfter(max)) {
            addValidationError(component, "maxDate", formioValidationErrors);
        }
    }

    private static void validateMinDate(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        LocalDate max = component.getProps().getMaxDate();
        if (max == null) {
            return;
        }

        LocalDate value = (LocalDate) field.getFirst();
        if (value.isBefore(max)) {
            addValidationError(component, "minDate", formioValidationErrors);
        }
    }

    private static void validateFieldLength(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors,
            boolean isMaxLength) {
        Integer length =
                isMaxLength
                        ? component.getProps().getMaxLength()
                        : component.getProps().getMinLength();
        if (length == null || !field.getSecond().equals(String.class)) {
            return;
        }

        String fieldValue = (String) field.getFirst();
        boolean condition =
                isMaxLength ? fieldValue.length() > length : fieldValue.length() < length;
        if (condition) {
            addValidationError(
                    component, isMaxLength ? "maxLength" : "minLength", formioValidationErrors);
        }
    }

    private static void validatePattern(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        String pattern = component.getProps().getPattern();
        if (pattern == null || !field.getSecond().equals(String.class)) {
            return;
        }

        String fieldValue = (String) field.getFirst();
        if (!GenericValidator.matchRegexp(fieldValue, pattern)) {
            addValidationError(component, "pattern", formioValidationErrors);
        }
    }

    private static void validateSelectOptions(
            NuvalenceFormioComponent component,
            Pair<Object, Class<?>> field,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {
        List<NuvalenceFormioComponentOption> options = component.getProps().getSelectOptions();
        if (options == null || !field.getSecond().equals(String.class)) {
            return;
        }

        String fieldValue = (String) field.getFirst();
        Optional<String> valueMatched =
                options.stream()
                        .map(NuvalenceFormioComponentOption::getKey)
                        .filter(option -> option.equals(fieldValue))
                        .findFirst();

        if (valueMatched.isEmpty()) {
            addValidationError(component, "selectOptions", formioValidationErrors);
        }
    }

    private static void addValidationError(
            NuvalenceFormioComponent control,
            String errorName,
            List<NuvalenceFormioValidationExItem> formioValidationErrors) {

        NuvalenceFormioValidationExItem errorItem =
                NuvalenceFormioValidationExItem.builder()
                        .controlName(control.getKey())
                        .errorName(errorName)
                        .build();

        if (control.getProps() != null
                && control.getProps().getFormErrorLabel() != null
                && !control.getProps().getFormErrorLabel().isBlank()) {
            errorItem.setErrorMessage(control.getProps().getFormErrorLabel());
        }

        formioValidationErrors.add(errorItem);
    }

    private static Pair<Object, Class<?>> getFieldValue(String fieldPath, DynaBean dynaBean) {
        int dotIndex = fieldPath.indexOf(".");
        try {
            if (dotIndex == -1) {
                DynaProperty property = dynaBean.getDynaClass().getDynaProperty(fieldPath);
                return new Pair<>(dynaBean.get(fieldPath), property.getType());
            } else {
                String firstFieldName = fieldPath.substring(0, dotIndex);
                String remainingFieldName = fieldPath.substring(dotIndex + 1);

                DynaProperty dynaProperty = dynaBean.getDynaClass().getDynaProperty(firstFieldName);
                Object fieldValue = dynaBean.get(dynaProperty.getName());

                if (fieldValue != null && fieldValue instanceof DynaBean) {
                    return getFieldValue(remainingFieldName, (DynaBean) fieldValue);
                } else {
                    return null;
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Field {} not property in the schema", fieldPath);
            return null;
        }
    }

    private static boolean isFieldPresent(Pair<Object, Class<?>> field) {
        boolean isPresent = false;
        if (fieldIsNotNull(field)) {
            if (field.getSecond().equals(String.class)) {
                String fieldValue = (String) field.getFirst();
                isPresent = !fieldValue.isBlank();
            } else {
                isPresent = true;
            }
        }
        return isPresent;
    }

    private static boolean fieldIsNotNull(Pair<Object, Class<?>> field) {
        return field != null && field.getFirst() != null && field.getSecond() != null;
    }

    private static BigDecimal getNumericValue(Pair<Object, Class<?>> field) {
        BigDecimal fieldValue = null;
        if (field.getSecond().equals(Integer.class)) {
            fieldValue = new BigDecimal((Integer) field.getFirst());
        } else if (field.getSecond().equals(BigDecimal.class)) {
            fieldValue = (BigDecimal) field.getFirst();
        }
        return fieldValue;
    }

    @FunctionalInterface
    interface DateValidationCondition {
        boolean test(LocalDate value, LocalDate validationDate);
    }
}

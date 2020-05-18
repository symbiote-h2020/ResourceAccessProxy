package eu.h2020.symbiote.validation.value;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author <a href="mailto:michael.jacoby@iosb.fraunhofer.de">Michael Jacoby</a>
 * @param <T> actual type of the value
 */
public interface Value<T> {

    T get();

    default boolean isPrimitive() {
        return false;
    }

    default boolean isComplex() {
        return false;
    }

    default boolean isCustomType() {
        return false;
    }

    default boolean isComplexArray() {
        return false;
    }

    default boolean isPrimitiveArray() {
        return false;
    }

    default PrimitiveValue<T> asPrimitive() {
        if (PrimitiveValue.class.isAssignableFrom(this.getClass())) {
            return (PrimitiveValue<T>) this;
        }
        throw new RuntimeException("Value cannot be cast to primitive value");
    }

    default ComplexValue asComplex() {
        if (ComplexValue.class.isAssignableFrom(this.getClass())) {
            return (ComplexValue) this;
        }
        throw new RuntimeException("Value cannot be cast to complex value");
    }

    default ComplexValueArray asComplexArray() {
        if (ComplexValueArray.class.isAssignableFrom(this.getClass())) {
            return (ComplexValueArray) this;
        }
        throw new RuntimeException("Value cannot be cast to complex value array");
    }

    @SuppressWarnings("rawtypes")
    default PrimitiveValueArray asPrimitiveArray() {
        if (PrimitiveValueArray.class.isAssignableFrom(this.getClass())) {
            return (PrimitiveValueArray) this;
        }
        throw new RuntimeException("Value cannot be cast to primitive value array");
    }

    JsonNode asJson();
}

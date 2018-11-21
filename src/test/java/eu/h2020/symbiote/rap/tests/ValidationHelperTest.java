/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rap.tests;

import com.netflix.util.Pair;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.Datatype;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.PrimitiveProperty;
import eu.h2020.symbiote.validation.ValidationException;
import eu.h2020.symbiote.validation.ValidationHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.assertj.core.api.Assertions;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ValidationHelperTest {

    private static final String PARAMETER_1_NAME = "parameter_name_1";
    private static final Object PARAMETER_1_VALUE = "parameter_value_1";
    private static final String PARAMETER_2_NAME = "parameter_name_2";
    private static final Object PARAMETER_2_VALUE = 100;

    private static final String DATATYPE_STRING = "xsd:string";
    private static final String DATATYPE_INT = "xsd:int";

    private PrimitiveDatatype createPrimitiveDatatype(String name) {
        PrimitiveDatatype result = new PrimitiveDatatype();
        result.setBaseDatatype(name);
        return result;
    }

    private Parameter createPrimitiveParameter(String name, String datatype) {
        Parameter result = new Parameter();
        result.setDatatype(createPrimitiveDatatype(datatype));
        result.setName(name);
        return result;
    }

    @Test
    public void testValidateServicePayloadWithJSON() {
        List<Parameter> definedParameters = Arrays.asList(
                createPrimitiveParameter(PARAMETER_1_NAME, DATATYPE_STRING),
                createPrimitiveParameter(PARAMETER_2_NAME, DATATYPE_INT));
        String payload
                = "[ "
                + "{ \"" + PARAMETER_1_NAME + "\": \"" + PARAMETER_1_VALUE + "\" }, "
                + "{ \"" + PARAMETER_2_NAME + "\": " + PARAMETER_2_VALUE + " } "
                + "] ";
        try {
            ValidationHelper.validateServicePayload(definedParameters, payload);
        } catch (ValidationException ex) {
            fail("validation failed, reason: " + ex.getMessage());
        }
    }

    private ComplexDatatype createComplexDatatype(Pair<String, String>... properties) {
        ComplexDatatype result = new ComplexDatatype();     
        result.setDataProperties(new ArrayList<>());
        for (Pair<String, String> property : properties) {
            PrimitiveProperty primitiveProperty = new PrimitiveProperty();
            primitiveProperty.setName(property.first());
            primitiveProperty.setPrimitiveDatatype(createPrimitiveDatatype(property.second()));
            result.getDataProperties().add(primitiveProperty);
        }
        return result;
    }

    @Test
    public void testValidateTypeWithJSON() {

        Datatype definedDatatype = createComplexDatatype(
                new Pair(PARAMETER_1_NAME, DATATYPE_STRING),
                new Pair(PARAMETER_2_NAME, DATATYPE_INT));
        String payload
                = "{ "
                + " \"" + PARAMETER_1_NAME + "\": \"" + PARAMETER_1_VALUE + "\" , "
                + " \"" + PARAMETER_2_NAME + "\": " + PARAMETER_2_VALUE + "  "
                + "} ";
        try {
            ValidationHelper.validateType(definedDatatype, payload, null);
        } catch (ValidationException ex) {
            fail("validation failed, reason: " + ex.getMessage());
        }
    }
    
        @Test
    public void testValidateTypeWithJSONLD() {

        Datatype definedDatatype = createComplexDatatype(
                new Pair(PARAMETER_1_NAME, DATATYPE_STRING),
                new Pair(PARAMETER_2_NAME, DATATYPE_INT));
        String payload
                = "{ "
                + " \"@context\": \"{}\" , "
                + " \"@type\": [\"foo\", \"bar\"] , "
                + " \"" + PARAMETER_1_NAME + "\": \"" + PARAMETER_1_VALUE + "\" , "
                + " \"" + PARAMETER_2_NAME + "\": " + PARAMETER_2_VALUE + "  "
                + "} ";
        try {
            ValidationHelper.validateType(definedDatatype, payload, null);
        } catch (ValidationException ex) {
            fail("validation failed, reason: " + ex.getMessage());
        }
    }
}

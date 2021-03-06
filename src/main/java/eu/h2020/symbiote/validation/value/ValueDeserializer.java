/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.validation.value;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.Lists;
import eu.h2020.symbiote.validation.ValidationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class ValueDeserializer extends StdDeserializer<Value> {

    public ValueDeserializer() {
        this((Class<?>) null);
    }

    private ValueDeserializer(Class<?> vc) {
        super(vc);
    }

    private Value deserialize(JsonNode node) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jsonParser = mapper.getFactory().createParser(node.toString());
        DeserializationContext deserializationContext = mapper.getDeserializationContext();
        return deserialize(jsonParser, deserializationContext);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Value deserialize(JsonParser jp, DeserializationContext dc) throws JsonProcessingException, IOException {
        JsonNode root = jp.getCodec().readTree(jp);
        if (root.isValueNode()) {
            Object foo = root.asText();
            if (!root.isTextual()) {
                foo = new ObjectMapper().readValue(foo.toString(), Object.class);
            }
            return new PrimitiveValue(foo);
        } else if (root.isArray()) {
            List<JsonNode> elements = Lists.newArrayList(root.elements());
            Long primitiveElementCount = elements.stream()
                    .filter(x -> x.isValueNode())
                    .collect(Collectors.counting());
            if (primitiveElementCount == 0) {
                // all elements are complex                
                List<ComplexValue> values = new ArrayList<>();
                Iterator<JsonNode> elementIterator = root.elements();
                while (elementIterator.hasNext()) {
                    values.add(parseComplex(elementIterator.next()));
                }
                return new ComplexValueArray(values);
            } else if (primitiveElementCount == elements.size()) {
                // all elements are primitive
                return new PrimitiveValueArray(jp.getCurrentValue());
            } else {
                throw new RuntimeException("found mixed array of primitive and non-primitive values");
            }
        } else if (root.isObject()) {
            return parseComplex(root);            
        }
        throw new RuntimeException("unkown JSON content");
    }

    private ComplexValue parseComplex(JsonNode node) throws IOException {
        ComplexValue result = new ComplexValue();
        for (Map.Entry<String, JsonNode> field : ValidationUtils.toMap(node.fields()).entrySet()) {
            result.addValue(field.getKey(), deserialize(field.getValue()));
        }
        return result;
    }
}

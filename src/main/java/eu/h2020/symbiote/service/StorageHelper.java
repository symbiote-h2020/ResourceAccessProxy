/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.interfaces.ResourcesRepository;
import eu.h2020.symbiote.messages.ResourceAccessGetMessage;
import eu.h2020.symbiote.messages.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.messages.ResourceAccessMessage;
import eu.h2020.symbiote.messages.ResourceAccessSetService;
import eu.h2020.symbiote.core.model.Observation;
import eu.h2020.symbiote.resources.ResourceInfo;
import eu.h2020.symbiote.resources.query.Comparison;
import eu.h2020.symbiote.resources.query.Filter;
import eu.h2020.symbiote.resources.query.Operator;
import eu.h2020.symbiote.resources.query.Query;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.cloud.model.resources.Service;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
public class StorageHelper {

    private ResourcesRepository resourcesRepo;
    private RabbitTemplate rabbitTemplate;
    private TopicExchange exchange;

    private List<Entity> resourceList;
    private List<Entity> observationList;

    private static final Pattern PATTERN = Pattern.compile(
            "\\p{Digit}{1,4}-\\p{Digit}{1,2}-\\p{Digit}{1,2}"
            + "T\\p{Digit}{1,2}:\\p{Digit}{1,2}(?::\\p{Digit}{1,2})?"
            + "(Z|([-+]\\p{Digit}{1,2}:\\p{Digit}{2}))?");

    public StorageHelper(ResourcesRepository resourcesRepository, RabbitTemplate rabbit, TopicExchange topicExchange) {
        resourceList = new ArrayList<Entity>();
        observationList = new ArrayList<Entity>();
        //initSampleData();
        resourcesRepo = resourcesRepository;
        rabbitTemplate = rabbit;
        exchange = topicExchange;
    }

    public ResourceInfo getResourceInfo(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) {
        ResourceInfo resInfo = null;
        for (final UriParameter key : keyParams) {
            // key
            String keyName = key.getName();
            String keyText = key.getText();

            //remove quote
            keyText = keyText.replaceAll("'", "");

            try {
                if (keyName.equals("id")) {
                    Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(keyText);
                    if (resInfoOptional.isPresent()) {
                        resInfo = resInfoOptional.get();
                    }
                }
            } catch (Exception e) {
                int a = 0;
            }

            //SOLO MOMENTANEO
            /*if (resInfo == null) {
                List<ResourceInfo> resInfo2 = resourcesRepo.findAll();
                resInfo = resInfo2.get(0);
            }*/
        }

        return resInfo;
    }

    public Object getRelatedObject(ResourceInfo resourceInfo, EdmEntityType sourceEntityType, EdmEntityType targetEntityType,
            Integer top, Query filterQuery) throws ODataApplicationException {
        FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
        if (sourceEntityType.getName().equals(RAPEdmProvider.ET_SENSOR_NAME)
                && relatedEntityFqn.equals(RAPEdmProvider.ET_OBSERVATION_FQN)) {

            try {
                List<Observation> observations = null;
                ResourceAccessMessage msg;
                String routingKey;

                if (top != null && top == 1) {
                    msg = new ResourceAccessGetMessage(resourceInfo);
                    routingKey = ResourceAccessMessage.AccessType.GET.toString().toLowerCase();
                } else {
                    msg = new ResourceAccessHistoryMessage(resourceInfo,filterQuery);
                    routingKey = ResourceAccessMessage.AccessType.HISTORY.toString().toLowerCase();
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                String json = mapper.writeValueAsString(msg);

                Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
                String response = null;
                if (obj != null) {
                    response = new String((byte[]) obj, "UTF-8");
                }
                observations = mapper.readValue(response, new TypeReference<List<Observation>>() {
                });

                if (top != null && top == 1 && observations != null && observations.size() > 0) {
                    Observation obs = observations.get(0);
                    return obs;
                } else {
                    return observations;
                }
            } catch (Exception e) {
                String err = "Unable to read resource with id: " + resourceInfo.getSymbioteId();
                //log.error(err + "\n" + e.getMessage());
                throw new ODataApplicationException(err, HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
            }
        }
        return null;
    }

    public void setService(ResourceInfo resourceInfo, String serviceId, Entity requestBody, EdmEntityType targetEntityType) throws ODataApplicationException {
        if (targetEntityType.getName().equals(RAPEdmProvider.ET_SERVICE_NAME)) {
            List<InputParameter> inputParameterList = new ArrayList<InputParameter>();
            ResourceAccessMessage msg;
            String routingKey = ResourceAccessMessage.AccessType.SET.toString().toLowerCase();

            Property updateProperty = requestBody.getProperty("inputParameters");
            if (updateProperty.isCollection()) {
                List<ComplexValue> name_value = (List<ComplexValue>) updateProperty.asCollection();
                for (ComplexValue complexValue : name_value) {
                    List<Property> properties = complexValue.getValue();
                    String name = null;
                    String value = null;
                    for (Property p : properties){
                        String pName = p.getName();
                        if(pName.equals("name"))
                            name = (String) p.getValue();
                        else if(pName.equals("value"))
                            value = (String) p.getValue();
                    }
                    InputParameter ip = new InputParameter(name);
                    ip.setValue(value);
                    inputParameterList.add(ip);
                }
            }
            Service service = new Service();
            service.setName(serviceId);
            service.setInputParameters(inputParameterList);

            msg = new ResourceAccessSetService(resourceInfo, service);

            String json = "";
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

                json = mapper.writeValueAsString(msg);
            } catch (JsonProcessingException ex) {
                Logger.getLogger(StorageHelper.class.getName()).log(Level.SEVERE, null, ex);
            }

            Object obj = rabbitTemplate.convertSendAndReceive(exchange.getName(), routingKey, json);
        } else {
            throw new ODataApplicationException("Internal Error", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ROOT);
        }
    }

    /* PUBLIC FACADE */
    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {

        // actually, this is only required if we have more than one Entity Sets
        if (edmEntitySet.getName().equals(RAPEdmProvider.ES_SENSORS_NAME)) {
            return getResources();
        } else if (edmEntitySet.getName().equals(RAPEdmProvider.ES_OBSERVATIONS_NAME)) {
            return getObservations();
        }

        return null;
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        return getElement(edmEntityType, keyParams);
    }

    /*  INTERNAL */
    private EntityCollection getObservations() {
        EntityCollection retEntitySet = new EntityCollection();
        for (Entity productEntity : this.observationList) {
            retEntitySet.getEntities().add(productEntity);
        }
        return retEntitySet;
    }

    private EntityCollection getResources() {
        EntityCollection retEntitySet = new EntityCollection();
        for (Entity productEntity : this.resourceList) {
            retEntitySet.getEntities().add(productEntity);
        }
        return retEntitySet;
    }

    private Entity getElement(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {
        // the list of entities at runtime
        EntityCollection entitySet;
        if (edmEntityType.getName().equals(RAPEdmProvider.ET_SENSOR_NAME)) {
            entitySet = getResources();
        } else if (edmEntityType.getName().equals(RAPEdmProvider.ET_OBSERVATION_NAME)) {
            entitySet = getObservations();
        } else {
            return null;
        }

        /*  generic approach  to find the requested entity */
        Entity requestedEntity = findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null) {
            // this variable is null if our data doesn't contain an entity for the requested key
            // Throw suitable exception
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    /* HELPER */
    private void initSampleData() {

        final ComplexValue location1 = new ComplexValue();
        location1.getValue().add(new Property(null, "name", ValueType.PRIMITIVE, "Spansko"));
        location1.getValue().add(new Property(null, "description", ValueType.PRIMITIVE, "City of Zagreb"));
        location1.getValue().add(new Property(null, "longitude", ValueType.PRIMITIVE, 15.9));
        location1.getValue().add(new Property(null, "latitude", ValueType.PRIMITIVE, 45.8));
        location1.getValue().add(new Property(null, "altitude", ValueType.PRIMITIVE, 145));

        final ComplexValue location2 = new ComplexValue();
        location2.getValue().add(new Property(null, "name", ValueType.PRIMITIVE, "Rome"));
        location2.getValue().add(new Property(null, "description", ValueType.PRIMITIVE, "City of Rome"));
        location2.getValue().add(new Property(null, "longitude", ValueType.PRIMITIVE, 175.2));
        location2.getValue().add(new Property(null, "latitude", ValueType.PRIMITIVE, 120.5));
        location2.getValue().add(new Property(null, "altitude", ValueType.PRIMITIVE, 20));

        // add some sample observation entities
        final Entity e1 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "1"))
                .addProperty(new Property(null, "location", ValueType.COMPLEX, location1))
                .addProperty(new Property(null, "resultTime", ValueType.PRIMITIVE, 150))
                .addProperty(new Property(null, "samplingTime", ValueType.PRIMITIVE, 200));
        e1.setId(createId(RAPEdmProvider.ES_OBSERVATIONS_NAME, 1));
        observationList.add(e1);

        final Entity e2 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "2"))
                .addProperty(new Property(null, "location", ValueType.COMPLEX, location2))
                .addProperty(new Property(null, "resultTime", ValueType.PRIMITIVE, 70))
                .addProperty(new Property(null, "samplingTime", ValueType.PRIMITIVE, 35));
        e2.setId(createId(RAPEdmProvider.ES_OBSERVATIONS_NAME, 2));
        observationList.add(e2);

        // add some sample resource entities
        final Entity r1 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "res1"))
                .addProperty(new Property(null, "platformResourceId", ValueType.PRIMITIVE, "1+100"))
                .addProperty(new Property(null, "platformId", ValueType.PRIMITIVE, "100"));
        r1.setId(createId(RAPEdmProvider.ES_SENSORS_NAME, "res1"));
        resourceList.add(r1);

        final Entity r2 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "res2"))
                .addProperty(new Property(null, "platformResourceId", ValueType.PRIMITIVE, "2+100"))
                .addProperty(new Property(null, "platformId", ValueType.PRIMITIVE, "100"));
        r2.setId(createId(RAPEdmProvider.ES_SENSORS_NAME, "res2"));
        resourceList.add(r2);
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }

    public EntityCollection getRelatedEntityCollection(Entity sourceEntity, EdmEntityType sourceEntityType, EdmEntityType targetEntityType) {
        EntityCollection navigationTargetEntityCollection = new EntityCollection();

        FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
        //String sourceEntityFqn = sourceEntity.getType();

        if (sourceEntityType.getName().equals(RAPEdmProvider.ET_OBSERVATION_NAME)
                && relatedEntityFqn.equals(RAPEdmProvider.ET_SENSOR_FQN)) {
            // relation Products->Category (result all categories)
            String observationID = sourceEntity.getProperty("resourceId").getValue().toString();
            if (observationID.equals("1")) {
                navigationTargetEntityCollection.getEntities().add(resourceList.get(0));
            } else if (observationID.equals("2")) {
                navigationTargetEntityCollection.getEntities().add(resourceList.get(2));
            }

        } else if (sourceEntityType.getName().equals(RAPEdmProvider.ET_SENSOR_NAME)
                && relatedEntityFqn.equals(RAPEdmProvider.ET_OBSERVATION_FQN)) {
            // relation Category->Products (result all products)

            String resourceID = (String) sourceEntity.getProperty("resourceId").getValue();
            if (resourceID.equals("res1")) {
                // the first 2 products are notebooks
                navigationTargetEntityCollection.getEntities().addAll(observationList.subList(0, 1));
            } else if (resourceID.equals("res2")) {
                // the next 2 products are organizers
                navigationTargetEntityCollection.getEntities().addAll(observationList.subList(1, 2));
            }
        }

        if (navigationTargetEntityCollection.getEntities().isEmpty()) {
            return null;
        }

        return navigationTargetEntityCollection;
    }

    public static EdmEntitySet getEdmEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // To get the entity set we have to interpret all URI segments
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

        return uriResource.getEntitySet();
    }

    public static Entity findEntity(EdmEntityType edmEntityType,
            EntityCollection rt_entitySet, List<UriParameter> keyParams)
            throws ODataApplicationException {

        List<Entity> entityList = rt_entitySet.getEntities();

        // loop over all entities in order to find that one that matches all keys in request
        // an example could be e.g. contacts(ContactID=1, CompanyID=1)
        for (Entity rt_entity : entityList) {
            boolean foundEntity = entityMatchesAllKeys(edmEntityType, rt_entity, keyParams);
            if (foundEntity) {
                return rt_entity;
            }
        }

        return null;
    }

    public static boolean entityMatchesAllKeys(EdmEntityType edmEntityType, Entity rt_entity, List<UriParameter> keyParams)
            throws ODataApplicationException {

        // loop over all keys
        for (final UriParameter key : keyParams) {
            // key
            String keyName = key.getName();
            String keyText = key.getText();

            //remove quote
            keyText = keyText.replaceAll("'", "");

            // Edm: we need this info for the comparison below
            EdmProperty edmKeyProperty = (EdmProperty) edmEntityType.getProperty(keyName);
            Boolean isNullable = edmKeyProperty.isNullable();
            Integer maxLength = edmKeyProperty.getMaxLength();
            Integer precision = edmKeyProperty.getPrecision();
            Boolean isUnicode = edmKeyProperty.isUnicode();
            Integer scale = edmKeyProperty.getScale();
            // get the EdmType in order to compare
            EdmType edmType = edmKeyProperty.getType();
            // Key properties must be instance of primitive type
            EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType) edmType;

            // Runtime data: the value of the current entity
            Object valueObject = rt_entity.getProperty(keyName).getValue(); // null-check is done in FWK

            // now need to compare the valueObject with the keyText String
            // this is done using the type.valueToString //
            String valueAsString = null;
            try {
                valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength,
                        precision, scale, isUnicode);
            } catch (EdmPrimitiveTypeException e) {
                throw new ODataApplicationException("Failed to retrieve String value",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }

            if (valueAsString == null) {
                return false;
            }

            boolean matches = valueAsString.equals(keyText);
            if (!matches) {
                // if any of the key properties is not found in the entity, we don't need to search further
                return false;
            }
        }

        return true;
    }

    public static EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet startEdmEntitySet,
            EdmNavigationProperty edmNavigationProperty) throws ODataApplicationException {

        EdmEntitySet navigationTargetEntitySet = null;

        String navPropName = edmNavigationProperty.getName();
        EdmBindingTarget edmBindingTarget = startEdmEntitySet.getRelatedBindingTarget(navPropName);
        if (edmBindingTarget == null) {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        if (edmBindingTarget instanceof EdmEntitySet) {
            navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        return navigationTargetEntitySet;
    }

    public static UriResourceNavigation getLastNavigation(final UriInfoResource uriInfo) {

        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        int navigationCount = 1;
        while (navigationCount < resourcePaths.size()
                && resourcePaths.get(navigationCount) instanceof UriResourceNavigation) {
            navigationCount++;
        }

        return (UriResourceNavigation) resourcePaths.get(--navigationCount);
    }

    
    public static Query calculateFilter(Expression expression) throws ODataApplicationException {

        if (expression instanceof Binary) {
            Expression left = ((Binary) expression).getLeftOperand();
            BinaryOperatorKind operator = ((Binary) expression).getOperator();
            Expression right = ((Binary) expression).getRightOperand();

            if (left instanceof Binary && right instanceof Binary) {
                ArrayList<Query> exprs = new ArrayList();
                Operator op = null;
                try {
                    op = new Operator(operator.name());
                } catch (Exception ex) {
                    throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                }
                
                
                Query leftQuery = calculateFilter(left);
                exprs.add(0, leftQuery);
                
                
                Query rightQuery = calculateFilter(right);
                exprs.add(1, (Query) rightQuery);
                
                Filter f = new Filter(op.getLop(),exprs);
                return f;
            } else if (left instanceof Member && right instanceof Literal) {
                Member member = (Member) left;
                String key = member.toString();

                Literal literal = (Literal) right;
                String value = literal.getText();
                if (literal.getType() instanceof EdmString) {
                    value = value.substring(1, value.length() - 1);
                }

                if (key.contains("resultTime") || key.contains("samplingTime")) {
                    Matcher matcher = PATTERN.matcher(value);
                    if (!matcher.matches()) {
                        throw new ODataApplicationException("Data format not correct",
                                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                    }
                    value = parseDate(value);

                }
                
                Comparison cmp;
                try {
                    cmp = new Comparison(operator.name());
                } catch (Exception ex) {
                    throw new ODataApplicationException(ex.getMessage(), HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                }
                
                eu.h2020.symbiote.resources.query.Expression expr = new eu.h2020.symbiote.resources.query.Expression(key, cmp.getCmp(), value);

                return expr;
            } else {
                throw new ODataApplicationException("Not implement", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }
        }
        return null;
    }
    
    
    private static String parseDate(String dateParse) throws ODataApplicationException {

        TimeZone zoneUTC = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(zoneUTC);
        DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        dateFormat1.setTimeZone(zoneUTC);
        DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");
        dateFormat2.setTimeZone(zoneUTC);
        DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        dateFormat3.setTimeZone(zoneUTC);

        dateParse = dateParse.replaceAll("Z", "+00:00");
        Date date = null;
        String parsedData = null;
        try {
            date = dateFormat3.parse(dateParse);
        } catch (ParseException e3) {
            try {
                date = dateFormat2.parse(dateParse);
            } catch (ParseException e2) {
                try {
                    date = dateFormat.parse(dateParse);
                } catch (ParseException e) {
                    try {
                        date = dateFormat1.parse(dateParse);
                    } catch (ParseException e1) {

                    }
                }
            }
        }

        if (date == null) {
            throw new ODataApplicationException("Data format not correct",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
        }

        parsedData = dateFormat.format(date);
        return parsedData;
    }
}

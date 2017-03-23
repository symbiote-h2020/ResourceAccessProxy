/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 *
 * @author luca-
 */
public class StorageHelper {

    private List<Entity> resourceList;
    private List<Entity> observationList;

    public StorageHelper() {
        resourceList = new ArrayList<Entity>();
        observationList = new ArrayList<Entity>();
        initSampleData();
    }

    /* PUBLIC FACADE */
    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {

        // actually, this is only required if we have more than one Entity Sets
        if (edmEntitySet.getName().equals(ResourceAccessProxyEdmProvider.ES_RESOURCES_NAME)) {
            return getResources();
        } else if (edmEntitySet.getName().equals(ResourceAccessProxyEdmProvider.ES_OBSERVATIONS_NAME)) {
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
        if (edmEntityType.getName().equals(ResourceAccessProxyEdmProvider.ET_RESOURCE_NAME)) {
            entitySet = getResources();
        } else if (edmEntityType.getName().equals(ResourceAccessProxyEdmProvider.ET_OBSERVATION_NAME)) {
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
        e1.setId(createId(ResourceAccessProxyEdmProvider.ES_OBSERVATIONS_NAME, 1));
        observationList.add(e1);

        final Entity e2 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "2"))
                .addProperty(new Property(null, "location", ValueType.COMPLEX, location2))
                .addProperty(new Property(null, "resultTime", ValueType.PRIMITIVE, 70))
                .addProperty(new Property(null, "samplingTime", ValueType.PRIMITIVE, 35));
        e2.setId(createId(ResourceAccessProxyEdmProvider.ES_OBSERVATIONS_NAME, 2));
        observationList.add(e2);

        // add some sample resource entities
        final Entity r1 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "res1"))
                .addProperty(new Property(null, "platformResourceId", ValueType.PRIMITIVE, "1+100"))
                .addProperty(new Property(null, "platformId", ValueType.PRIMITIVE, "100"));
        r1.setId(createId(ResourceAccessProxyEdmProvider.ES_RESOURCES_NAME, "res1"));
        resourceList.add(r1);

        final Entity r2 = new Entity()
                .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "res2"))
                .addProperty(new Property(null, "platformResourceId", ValueType.PRIMITIVE, "2+100"))
                .addProperty(new Property(null, "platformId", ValueType.PRIMITIVE, "100"));
        r2.setId(createId(ResourceAccessProxyEdmProvider.ES_RESOURCES_NAME, "res2"));
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

        if (sourceEntityType.getName().equals(ResourceAccessProxyEdmProvider.ET_OBSERVATION_NAME)
                && relatedEntityFqn.equals(ResourceAccessProxyEdmProvider.ET_RESOURCE_FQN)) {
            // relation Products->Category (result all categories)
            String observationID = sourceEntity.getProperty("resourceId").getValue().toString();
            if (observationID.equals("1")) {
                navigationTargetEntityCollection.getEntities().add(resourceList.get(0));
            } else if (observationID.equals("2")) {
                navigationTargetEntityCollection.getEntities().add(resourceList.get(2));
            }

        } else if (sourceEntityType.getName().equals(ResourceAccessProxyEdmProvider.ET_RESOURCE_NAME)
                && relatedEntityFqn.equals(ResourceAccessProxyEdmProvider.ET_OBSERVATION_FQN)) {
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
    
    public static void calculateFilter (Expression expression, List<String> operatorsIn,
        List<String> operatorsOut, Map<String, Object> map) throws ODataApplicationException{
        
        if(expression instanceof Binary){
            Expression left = ((Binary) expression).getLeftOperand();
            BinaryOperatorKind operator = ((Binary) expression).getOperator();
            Expression right = ((Binary) expression).getRightOperand();
            
            if(left instanceof Binary && right instanceof Binary){
                operatorsOut.add(operator.name());
                calculateFilter(left,operatorsIn,operatorsOut,map);
                calculateFilter(right,operatorsIn,operatorsOut,map);
            }
            else if (left instanceof Member && right instanceof Literal){
                operatorsIn.add(operator.name());
                
                Member member = (Member) left;
                String key = member.toString();      
                                
                Literal literal = (Literal) right;
                String value = literal.getText();
                if(literal.getType() instanceof EdmString)
                    value = value.substring(1, value.length() - 1);
                
                map.put(key, value);
            }
            else{
                throw new ODataApplicationException("Not implement", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }
        }
    }
}

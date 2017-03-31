/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * this class is supposed to declare the metadata of the OData service it is
 * invoked by the Olingo framework e.g. when the metadata document of the
 * service is invoked e.g.
 * http://localhost:8080/ExampleService1/ExampleService1.svc/$metadata
 *
 * @author Matteo Pardi m.pardi@nextworks.it
 */
//@Component
public class RAPEdmProvider_1 extends CsdlAbstractEdmProvider {

    @Autowired
    private ApplicationContext ctx;

    // Service Namespace
    public static final String NAMESPACE = "OData.Model";
    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
    // Entity Types Names
    public static final String ET_OBSERVATION_NAME = "Observation";
    public static final FullQualifiedName ET_OBSERVATION_FQN = new FullQualifiedName(NAMESPACE, ET_OBSERVATION_NAME);
    
    public static final String ET_RESOURCE_NAME = "Resource";
    public static final FullQualifiedName ET_RESOURCE_FQN = new FullQualifiedName(NAMESPACE, ET_RESOURCE_NAME);
    
    
    public static final String ET_ACTUATOR_NAME = "Actuator";
    public static final FullQualifiedName ET_ACTUATOR_FQN = new FullQualifiedName(NAMESPACE, ET_ACTUATOR_NAME);
    public static final String ET_SERVICE_NAME = "Service";
    public static final FullQualifiedName ET_SERVICE_FQN = new FullQualifiedName(NAMESPACE, ET_SERVICE_NAME);

    
    // Complex Type Names
    public static final String WGS84LOCATION = "WGS84Location";
    public static final FullQualifiedName CT_WGS84LOCATION = new FullQualifiedName(NAMESPACE, WGS84LOCATION);
    public static final String PROPERTY = "Property";
    public static final FullQualifiedName CT_PROPERTY = new FullQualifiedName(NAMESPACE, PROPERTY);
    public static final String UOM = "uom";
    public static final FullQualifiedName CT_UOM = new FullQualifiedName(NAMESPACE, UOM);
    public static final String OBSERVATIONVALUE = "ObservationValue";
    public static final FullQualifiedName CT_OBSERVATIONVALUE = new FullQualifiedName(NAMESPACE, OBSERVATIONVALUE);
    public static final String INPUTPARAMETER = "InputParameter";
    public static final FullQualifiedName CT_INPUTPARAMETER = new FullQualifiedName(NAMESPACE, INPUTPARAMETER);

    // Entity Set Names
    public static final String ES_OBSERVATIONS_NAME = "Observations";
    public static final String ES_RESOURCES_NAME = "Resources";
    

    public static final String ES_SERVICES_NAME = "Services";
    public static final String ES_ACTUATORS_NAME = "Actuators";
    

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {

        // create Schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
        entityTypes.add(getEntityType(ET_OBSERVATION_FQN));
        entityTypes.add(getEntityType(ET_RESOURCE_FQN));
        
        entityTypes.add(getEntityType(ET_ACTUATOR_FQN));
        entityTypes.add(getEntityType(ET_SERVICE_FQN));
        schema.setEntityTypes(entityTypes);

        //add ComplexTypes
        List<CsdlComplexType> complexTypes = new ArrayList<CsdlComplexType>();
        complexTypes.add(getComplexType(CT_WGS84LOCATION));
        complexTypes.add(getComplexType(CT_PROPERTY));
        complexTypes.add(getComplexType(CT_UOM));
        complexTypes.add(getComplexType(CT_OBSERVATIONVALUE));
        schema.setComplexTypes(complexTypes);

        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());

        // finally
        List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {

        // this method is called for one of the EntityTypes that are configured in the Schema
        if (entityTypeName.equals(ET_OBSERVATION_FQN)) {

            //create EntityType properties
            //CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty resourceId = new CsdlProperty().setName("resourceId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty location = new CsdlProperty().setName("location").setType(CT_WGS84LOCATION);
            CsdlProperty resultTime = new CsdlProperty().setName("resultTime").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty samplingTime = new CsdlProperty().setName("samplingTime").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty observationValue = new CsdlProperty().setName("observationValue").setType(CT_OBSERVATIONVALUE);
            
            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("resourceId");

            
            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                    .setName(ET_RESOURCE_NAME)
                    .setType(ET_RESOURCE_FQN)
                    .setNullable(true)
                    .setPartner(ES_OBSERVATIONS_NAME);
            List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);
            
            // configure EntityType
            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_OBSERVATION_NAME);
            entityType.setProperties(Arrays.asList(resourceId, location, resultTime, samplingTime, observationValue));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);

            return entityType;
        }
        else if (entityTypeName.equals(ET_RESOURCE_FQN)) {

            //create EntityType properties
            //CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty resourceId = new CsdlProperty().setName("resourceId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty platformResourceId = new CsdlProperty().setName("platformResourceId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty platformId = new CsdlProperty().setName("platformId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("resourceId");
            
            
            // navigation property: one-to-many
            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                            .setName(ES_OBSERVATIONS_NAME)
                            .setType(ET_OBSERVATION_FQN)
                            .setCollection(true)
                            .setPartner(ET_RESOURCE_NAME);
            List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);

            // configure EntityType
            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_RESOURCE_NAME);
            entityType.setProperties(Arrays.asList(resourceId, platformResourceId, platformId));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);

            return entityType;
        }
        if (entityTypeName.equals(ET_SERVICE_FQN)) {

            //create EntityType properties
            //CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty resourceId = new CsdlProperty().setName("resourceId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty inputParameterList = new CsdlProperty().setName("inputParameter").setType(CT_INPUTPARAMETER).setCollection(true);
            
            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("resourceId");

            
            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                    .setName(ET_ACTUATOR_NAME)
                    .setType(ET_ACTUATOR_FQN)
                    .setNullable(true)
                    .setPartner(ES_SERVICES_NAME);
            List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);
            
            // configure EntityType
            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_SERVICE_NAME);
            entityType.setProperties(Arrays.asList(resourceId,inputParameterList));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);

            return entityType;
        }
        else if (entityTypeName.equals(ET_ACTUATOR_FQN)) {

            //create EntityType properties
            //CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty resourceId = new CsdlProperty().setName("resourceId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty platformResourceId = new CsdlProperty().setName("platformResourceId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty platformId = new CsdlProperty().setName("platformId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("resourceId");
            
            
            // navigation property: one-to-many
            CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                            .setName(ES_SERVICES_NAME)
                            .setType(ET_SERVICE_FQN)
                            .setCollection(true)
                            .setPartner(ET_ACTUATOR_NAME);
            List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);

            // configure EntityType
            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_ACTUATOR_NAME);
            entityType.setProperties(Arrays.asList(resourceId, platformResourceId, platformId));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);

            return entityType;
        }

        return null;
    }
    
    public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) throws ODataException {
        if (CT_WGS84LOCATION.equals(complexTypeName)) {
            return new CsdlComplexType()
                .setName(CT_WGS84LOCATION.getName())
                .setProperties(Arrays.asList(
                  new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("longitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName()),
                  new CsdlProperty().setName("latitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName()),
                  new CsdlProperty().setName("altitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName())));
        }
        else if (CT_PROPERTY.equals(complexTypeName)) {
            return new CsdlComplexType()
                .setName(CT_PROPERTY.getName())
                .setProperties(Arrays.asList(
                  new CsdlProperty().setName("label").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("comment").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())));
        }
        else if (CT_UOM.equals(complexTypeName)) {
            return new CsdlComplexType()
                .setName(CT_UOM.getName())
                .setProperties(Arrays.asList(
                  new CsdlProperty().setName("symbol").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("label").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("comment").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())));
        }
        else if (CT_OBSERVATIONVALUE.equals(complexTypeName)) {
            return new CsdlComplexType()
                .setName(CT_OBSERVATIONVALUE.getName())
                .setProperties(Arrays.asList(
                  new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("obsProperty").setType(CT_PROPERTY),
                  new CsdlProperty().setName("uom").setType(CT_UOM)));
        }
        else if (CT_INPUTPARAMETER.equals(complexTypeName)) {
            return new CsdlComplexType()
                .setName(CT_INPUTPARAMETER.getName())
                .setProperties(Arrays.asList(
                  new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                  new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())));
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {

        if (entityContainer.equals(CONTAINER)) {
            if (entitySetName.equals(ES_OBSERVATIONS_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_OBSERVATIONS_NAME);
                entitySet.setType(ET_OBSERVATION_FQN);
                
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setPath(ET_RESOURCE_NAME); // the path from entity type to navigation property
                navPropBinding.setTarget(ES_RESOURCES_NAME); //target entitySet, where the nav prop points to
                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
                navPropBindingList.add(navPropBinding);
                entitySet.setNavigationPropertyBindings(navPropBindingList);

                return entitySet;
            }
            else if (entitySetName.equals(ES_RESOURCES_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_RESOURCES_NAME);
                entitySet.setType(ET_RESOURCE_FQN);
                
                
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setTarget(ES_OBSERVATIONS_NAME);//target entitySet, where the nav prop points to
                navPropBinding.setPath(ES_OBSERVATIONS_NAME); // the path from entity type to navigation property
                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
                navPropBindingList.add(navPropBinding);
                entitySet.setNavigationPropertyBindings(navPropBindingList);
                
                return entitySet;
            }
            else if (entitySetName.equals(ES_SERVICES_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_SERVICES_NAME);
                entitySet.setType(ET_SERVICE_FQN);
                
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setPath(ET_ACTUATOR_NAME); // the path from entity type to navigation property
                navPropBinding.setTarget(ES_ACTUATORS_NAME); //target entitySet, where the nav prop points to
                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
                navPropBindingList.add(navPropBinding);
                entitySet.setNavigationPropertyBindings(navPropBindingList);

                return entitySet;
            }
            else if (entitySetName.equals(ES_ACTUATORS_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_ACTUATORS_NAME);
                entitySet.setType(ET_ACTUATOR_FQN);
                
                
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setTarget(ES_SERVICES_NAME);//target entitySet, where the nav prop points to
                navPropBinding.setPath(ES_SERVICES_NAME); // the path from entity type to navigation property
                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
                navPropBindingList.add(navPropBinding);
                entitySet.setNavigationPropertyBindings(navPropBindingList);
                
                return entitySet;
            }
        }

        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {

        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(getEntitySet(CONTAINER, ES_OBSERVATIONS_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_RESOURCES_NAME));
        
        entitySets.add(getEntitySet(CONTAINER, ES_SERVICES_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_ACTUATORS_NAME));

        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {

        // This method is invoked when displaying the service document at e.g. http://localhost:8080/DemoService/DemoService.svc
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }
}

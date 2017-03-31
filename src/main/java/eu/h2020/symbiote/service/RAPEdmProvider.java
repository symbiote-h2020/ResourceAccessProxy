/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;


import java.lang.reflect.Field;
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

import eu.h2020.symbiote.cloud.model.Sensor;
import eu.h2020.symbiote.core.model.Observation;
import eu.h2020.symbiote.interfaces.ResourceAccessRestController;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is supposed to declare the metadata of the OData service it is
 * invoked by the Olingo framework e.g. when the metadata document of the
 * service is invoked e.g.
 * http://localhost:8080/ExampleService1/ExampleService1.svc/$metadata
 *
 * @author Matteo Pardi m.pardi@nextworks.it
 */
@Component
public class RAPEdmProvider extends CsdlAbstractEdmProvider {

    private static final Logger log = LoggerFactory.getLogger(ResourceAccessRestController.class);
    
    @Autowired
    private ApplicationContext ctx;

    // Service Namespace
    public static final String NAMESPACE = "OData.Model";
    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
    // Entity Types Names
    public static final String ET_OBSERVATION_NAME = Observation.class.getSimpleName();
    public static final FullQualifiedName ET_OBSERVATION_FQN = new FullQualifiedName(NAMESPACE, ET_OBSERVATION_NAME);
    
    public static final String ET_RESOURCE_NAME = Sensor.class.getSimpleName();
    public static final FullQualifiedName ET_RESOURCE_FQN = new FullQualifiedName(NAMESPACE, ET_RESOURCE_NAME);

    // Entity Set Names
    public static final String ES_OBSERVATIONS_NAME =  Observation.class.getSimpleName() + "s";
    public static final String ES_RESOURCES_NAME = Sensor.class.getSimpleName() + "s";
    
    
    public static final String ET_ACTUATOR_NAME = "Actuator";
    public static final FullQualifiedName ET_ACTUATOR_FQN = new FullQualifiedName(NAMESPACE, ET_ACTUATOR_NAME);
    public static final String ET_SERVICE_NAME = "Service";
    public static final FullQualifiedName ET_SERVICE_FQN = new FullQualifiedName(NAMESPACE, ET_SERVICE_NAME);

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        try {
            // create Schema
            CsdlSchema schema = new CsdlSchema();
            schema.setNamespace(NAMESPACE);
            
            // add EntityTypes
            List<CsdlEntityType> entityTypes = new ArrayList();
            entityTypes.add(getEntityType(ET_OBSERVATION_FQN));
            entityTypes.add(getEntityType(ET_RESOURCE_FQN));
            schema.setEntityTypes(entityTypes);
            
            //add ComplexTypes
            List<CsdlComplexType> complexTypes = new ArrayList();
            Class objectClass = Class.forName(getClassLongName(ET_OBSERVATION_NAME, Observation.class));
            List<Field> fields = getAllFields(objectClass);
            for(Field f : fields) {
                if(f.getType().isPrimitive())
                    continue;
                complexTypes.add(getComplexType(new FullQualifiedName(NAMESPACE, f.getGenericType().toString())));
            }
            schema.setComplexTypes(complexTypes);            
            // add EntityContainer
            schema.setEntityContainer(getEntityContainer());            
            // finally
            List<CsdlSchema> schemas = new ArrayList();
            schemas.add(schema);
            
            return schemas;
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(RAPEdmProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }
    
    private Class getInternalTypeClass(Field f) {
        Class type = null;
        Type t = f.getGenericType();
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] ta = pt.getActualTypeArguments();
            type = (Class) ta[0];
        }        
        return type;
    }
                 
        
    private String getClassLongName(String simpleName, Class father) {
        String name = null;
        if(father.getSimpleName().equals(simpleName)) {
            name = father.getName();
            return name;    
        }
        
        List<Field> fields = getAllFields(father);
        for(Field f : fields) {
            Class cl = f.getType();
            if(cl == List.class) {
                cl = getInternalTypeClass(f);
                if(cl != null)
                    cl = f.getType();
            }
            if((cl != null) && (cl.getSimpleName().equals(simpleName))) {
                name = cl.getName();
                break;
            }
        }
        if(name==null) {
            for(Field f : fields) {
                Class cl = f.getType();            
                if(cl == List.class) {
                    name = getClassLongName(simpleName, getInternalTypeClass(f));
                } else {
                    if(!(cl.isPrimitive()) && (cl != String.class)) {
                        name = getClassLongName(simpleName, (Class)f.getType());
                    }
                }
                if(name != null)
                    break;
            }
        }
        
        return name;
    }
    
    private String getShortClassName(String type) {
        String[] gt = type.split("\\.");
        String genericType = gt[gt.length-1];

        return genericType;        
    }
    
    private FullQualifiedName getFullQualifiedName(String type) {
        FullQualifiedName fqn = null;
        String[] gt = type.split("\\.");
        String genericType = gt[gt.length-1];

        if(genericType.compareToIgnoreCase("String") == 0) {
            fqn = EdmPrimitiveTypeKind.String.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Double") == 0) {
            fqn = EdmPrimitiveTypeKind.Double.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Long") == 0) {
            fqn = EdmPrimitiveTypeKind.Int64.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Int") == 0) {
            fqn = EdmPrimitiveTypeKind.Int32.getFullQualifiedName();
        } else if (genericType.compareToIgnoreCase("Boolean") == 0) {
            fqn = EdmPrimitiveTypeKind.Boolean.getFullQualifiedName();
        } 
        
        return fqn;        
    }
    
    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {

        CsdlEntityType entityType = null;
        //create EntityType properties
        try {            
            Class objectClass = Class.forName(getClassLongName(entityTypeName.getName(), Sensor.class));
            List<Field> fields = getAllFields(objectClass);
            ArrayList<CsdlProperty> lst = new ArrayList();
            List<CsdlNavigationProperty> navPropList = new ArrayList<>();
            String keyEl = "";
            for(Field f : fields) {
                Class cl;
                boolean isList=false;
                if(f.getType() == List.class) {
                    cl = getInternalTypeClass(f);
                    String shortName = getShortClassName(cl.getName());
                    // adding navigation for this collection
                    CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                                            .setName(shortName + "s")
                                            .setType(new FullQualifiedName(NAMESPACE, shortName))
                                            .setCollection(true)
                                            .setPartner(entityTypeName.getName());
                    navPropList.add(navProp);
                    isList = true;
                    log.info("LIST");                    
                } else {
                    cl = f.getType();                        
                }
                String shortName = getShortClassName(cl.getName());
                if((cl.isPrimitive()) || (cl == String.class)) {
                    FullQualifiedName fqn = getFullQualifiedName(cl.getName());
                    CsdlProperty propId = new CsdlProperty()
                            .setName(f.getName())
                            .setType(fqn);
                    lst.add(propId);
                    log.info("PRIMITIVE: " + f.getName() + " - " + fqn.getFullQualifiedNameAsString());
                } else {                        
                    CsdlProperty propId = new CsdlProperty()
                            .setName(f.getName())
                            .setType(new FullQualifiedName(NAMESPACE, shortName));                            
                    if(isList)
                        propId.setCollection(true);
                    lst.add(propId);
                    log.info("COMPLEX: " + f.getName() + " - " + shortName);
                }
                // TO UPDATE WITH (?) ANNOTATIONS?
                String nm = f.getName();
                if(nm.contains("Id") ||
                   nm.contains("id") ||
                   nm.contains("ID")) {
                   keyEl = nm;
                }                
            }
            CsdlPropertyRef propertyRef = null;
            if(keyEl.length() > 0) {
                // create CsdlPropertyRef for Key element
                propertyRef = new CsdlPropertyRef();
                propertyRef.setName(keyEl);
            }
            // configure EntityType
            entityType = new CsdlEntityType();
            entityType.setName(entityTypeName.getName());
            entityType.setProperties(lst);
            if(propertyRef != null)
                entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setNavigationProperties(navPropList);
        } catch (Exception e) {
           log.error(e.toString());
        }

        return entityType;
    }
    
    @Override
    public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) throws ODataException {
        
        try {
            String longName = getClassLongName(complexTypeName.getName(), Sensor.class);
            Class objectClass = Class.forName(longName);
            List<Field> fields = getAllFields(objectClass);
            ArrayList<CsdlProperty> propList = new ArrayList();
            for(Field f : fields) {
                String name = f.getName();
                if((f.getType().isPrimitive()) || (f.getType() == String.class)) {
                    FullQualifiedName fqn = getFullQualifiedName(f.getType().getName());
                    CsdlProperty prop = new CsdlProperty().setName(name).setType(fqn);
                    propList.add(prop);
                } else {
                    String shortName = getShortClassName(f.getType().getSimpleName());
                    CsdlProperty prop = new CsdlProperty().setName(name).setType(new FullQualifiedName(NAMESPACE, shortName));
                    propList.add(prop);
                }
            }            
            CsdlComplexType cpx = new CsdlComplexType()
                        .setName(complexTypeName.getName())
                        .setProperties(propList);
                
            return cpx;
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(RAPEdmProvider.class.getName()).log(Level.SEVERE, null, e);
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
        }

        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {

        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(getEntitySet(CONTAINER, ES_OBSERVATIONS_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_RESOURCES_NAME));

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

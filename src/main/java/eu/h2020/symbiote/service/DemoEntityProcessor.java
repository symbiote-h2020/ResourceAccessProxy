/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author luca-
 */

@Component
public class DemoEntityProcessor implements EntityProcessor{

    @Autowired
    private ApplicationContext ctx;
    
    private OData odata;
    private ServiceMetadata serviceMetadata;
    
    @Override
    public void init(OData odata, ServiceMetadata sm) {
        this.odata = odata;
        this.serviceMetadata = sm;
    }
    
    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) 
            throws ODataApplicationException, ODataLibraryException {
        // 1. retrieve the Entity Type
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2. retrieve the data from backend
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        Entity entity = readEntityData(edmEntitySet, keyPredicates);

        // 3. serialize
        EdmEntityType entityType = edmEntitySet.getEntityType();

        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
        // expand and select currently not supported
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entity(serviceMetadata, entityType, entity, options);
        InputStream entityStream = serializerResult.getContent();

        //4. configure the response object
        response.setContent(entityStream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest odr, ODataResponse odr1, UriInfo ui, ContentType ct, ContentType ct1) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateEntity(ODataRequest odr, ODataResponse odr1, UriInfo ui, ContentType ct, ContentType ct1) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteEntity(ODataRequest odr, ODataResponse odr1, UriInfo ui) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    private Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException{

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if(edmEntityType.getName().equals(ResourceAccessProxyEdmProvider.ET_PRODUCT_NAME)){
            // the list of entities at runtime
            EntityCollection entitySet = DemoEntityCollectionProcessor.getData(edmEntitySet);

            /*  generic approach  to find the requested entity */
            Entity requestedEntity = findEntity(edmEntityType, entitySet, keyParams);

            if(requestedEntity == null){
                // this variable is null if our data doesn't contain an entity for the requested key
                // Throw suitable exception
                throw new ODataApplicationException("Entity for requested key doesn't exist",
                                           HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }

            return requestedEntity;
        }

        return null;
    }
    
    
    private static Entity findEntity(EdmEntityType edmEntityType,
                                    EntityCollection rt_entitySet, List<UriParameter> keyParams)
                                    throws ODataApplicationException {

        List<Entity> entityList = rt_entitySet.getEntities();

        // loop over all entities in order to find that one that matches all keys in request
        // an example could be e.g. contacts(ContactID=1, CompanyID=1)
        for(Entity rt_entity : entityList){
            boolean foundEntity = entityMatchesAllKeys(edmEntityType, rt_entity, keyParams);
            if(foundEntity){
                return rt_entity;
            }
        }

        return null;
    }
    
     public static boolean entityMatchesAllKeys(EdmEntityType edmEntityType, Entity rt_entity,  List<UriParameter> keyParams)
                                                throws ODataApplicationException {

        // loop over all keys
        for (final UriParameter key : keyParams) {
            // key
            String keyName = key.getName();
            String keyText = key.getText();

            // Edm: we need this info for the comparison below
            EdmProperty edmKeyProperty = (EdmProperty )edmEntityType.getProperty(keyName);
            Boolean isNullable = edmKeyProperty.isNullable();
            Integer maxLength = edmKeyProperty.getMaxLength();
            Integer precision = edmKeyProperty.getPrecision();
            Boolean isUnicode = edmKeyProperty.isUnicode();
            Integer scale = edmKeyProperty.getScale();
            // get the EdmType in order to compare
            EdmType edmType = edmKeyProperty.getType();
            // Key properties must be instance of primitive type
            EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType)edmType;

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
                                             HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),Locale.ENGLISH, e);
            }

            if (valueAsString == null){
                return false;
            }

            boolean matches = valueAsString.equals(keyText);
            if(!matches){
                // if any of the key properties is not found in the entity, we don't need to search further
                return false;
            }
        }

        return true;
    }
}

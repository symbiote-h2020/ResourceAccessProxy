/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author luca-
 */
@Component
public class RAPEntityCollectionProcessor implements EntityCollectionProcessor {

    @Autowired
    private ApplicationContext ctx;

    private OData odata;
    private ServiceMetadata serviceMetadata;

    private StorageHelper storageHelper;

    @Override
    public void init(OData odata, ServiceMetadata sm) {
        this.odata = odata;
        this.serviceMetadata = sm;

        storageHelper = new StorageHelper();
    }

    /*
    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) 
            throws ODataApplicationException, ODataLibraryException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        int segmentCount = resourcePaths.size();
        
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

        
        // 2nd: fetch the data from backend for this requested EntitySetName
        // it has to be delivered as EntitySet object
        EntityCollection entitySet = getData(edmEntitySet);

        // 3rd: create a serializer based on the requested format (json)
        ODataSerializer serializer = odata.createSerializer(responseFormat);

        // 4th: Now serialize the content: transform from the EntitySet object to InputStream
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts);
        InputStream serializedContent = serializerResult.getContent();

        // Finally: configure the response object: set the body, headers and status code
        response.setContent(serializedContent);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
     */
    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        EdmEntitySet responseEdmEntitySet = null; // for building ContextURL
        EntityCollection responseEntityCollection = null; // for the response body

        // 1st retrieve the requested EntitySet from the uriInfo
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Only EntitySet is supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

        if (segmentCount == 1) { // this is the case for: DemoService/DemoService.svc/Categories
            responseEdmEntitySet = startEdmEntitySet; // first (and only) entitySet

            // 2nd: fetch the data from backend for this requested EntitySetName
            responseEntityCollection = storageHelper.readEntitySetData(startEdmEntitySet);
        } else if (segmentCount == 2) { //navigation: e.g. DemoService.svc/Categories(3)/Products
            UriResource lastSegment = resourceParts.get(1); // don't support more complex URIs
            if (lastSegment instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
                EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                EdmEntityType targetEntityType = edmNavigationProperty.getType();
                responseEdmEntitySet = storageHelper.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);

                // 2nd: fetch the data from backend
                // first fetch the entity where the first segment of the URI points to
                // e.g. Categories(3)/Products first find the single entity: Category(3)
                List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                Entity sourceEntity = storageHelper.readEntityData(startEdmEntitySet, keyPredicates);
                // error handling for e.g.  DemoService.svc/Categories(99)/Products
                if (sourceEntity == null) {
                    throw new ODataApplicationException("Entity not found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                }
                // then fetch the entity collection where the entity navigates to
                EdmEntityType startEntityType = startEdmEntitySet.getEntityType();
                responseEntityCollection = storageHelper.getRelatedEntityCollection(sourceEntity, startEntityType, targetEntityType);
            }
        } else { // this would be the case for e.g. Products(1)/Category/Products
            throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }
        // 3rd: create and configure a serializer
        ContextURL contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).build();
        final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().contextURL(contextUrl).id(id).build();
        EdmEntityType edmEntityType = responseEdmEntitySet.getEntityType();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, responseEntityCollection, opts);

        // 4th: configure the response object: set the body, headers and status code
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    /*
    public static EntityCollection getData(EdmEntitySet edmEntitySet) {

        EntityCollection productsCollection = new EntityCollection();
        // check for which EdmEntitySet the data is requested
        if (ResourceAccessProxyEdmProvider.ES_OBSERVATIONS_NAME.equals(edmEntitySet.getName())) {
            List<Entity> productList = productsCollection.getEntities();

            
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
            
            
            // add some sample product entities
            final Entity e1 = new Entity()
                    .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "1"))
                    .addProperty(new Property(null, "location", ValueType.COMPLEX, location1))
                    .addProperty(new Property(null, "resultTime", ValueType.PRIMITIVE, 150))
                    .addProperty(new Property(null, "samplingTime", ValueType.PRIMITIVE,200));
            e1.setId(createId(ResourceAccessProxyEdmProvider.ES_OBSERVATIONS_NAME, 1));
            productList.add(e1);

            final Entity e2 = new Entity()
                    .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "2"))
                    .addProperty(new Property(null, "location", ValueType.COMPLEX, location2))
                    .addProperty(new Property(null, "resultTime", ValueType.PRIMITIVE, 70))
                    .addProperty(new Property(null, "samplingTime", ValueType.PRIMITIVE,35));
            e2.setId(createId(ResourceAccessProxyEdmProvider.ES_OBSERVATIONS_NAME, 2));
            productList.add(e2);

//            final Entity e3 = new Entity()
//                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3))
//                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
//                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
//                            "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
//            e3.setId(createId("Products", 3));
//            productList.add(e3);
        }
        
        else if (ResourceAccessProxyEdmProvider.ES_RESOURCES_NAME.equals(edmEntitySet.getName())) {
            List<Entity> productList = productsCollection.getEntities();         
            
            // add some sample product entities
            final Entity e1 = new Entity()
                    .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "res1"))
                    .addProperty(new Property(null, "platformResourceId", ValueType.PRIMITIVE, "1+100"))
                    .addProperty(new Property(null, "platformId", ValueType.PRIMITIVE,"100"));
            e1.setId(createId(ResourceAccessProxyEdmProvider.ES_RESOURCES_NAME, "res1"));
            productList.add(e1);

            final Entity e2 = new Entity()
                    .addProperty(new Property(null, "resourceId", ValueType.PRIMITIVE, "res2"))
                    .addProperty(new Property(null, "platformResourceId", ValueType.PRIMITIVE, "2+100"))
                    .addProperty(new Property(null, "platformId", ValueType.PRIMITIVE,"100"));
            e2.setId(createId(ResourceAccessProxyEdmProvider.ES_RESOURCES_NAME, "res2"));
            productList.add(e2);

//            final Entity e3 = new Entity()
//                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3))
//                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
//                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
//                            "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
//            e3.setId(createId("Products", 3));
//            productList.add(e3);
        }

        return productsCollection;
    }

    public static URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
     */
}

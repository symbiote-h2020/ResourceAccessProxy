/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.interfaces.ResourcesRepository;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *
 * @author luca-
 */
@Component
public class RAPEntityCollectionProcessor implements EntityCollectionProcessor {

    @Autowired
    private ApplicationContext ctx;
    
    private static final Logger log = LoggerFactory.getLogger(RAPEntityCollectionProcessor.class);

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Autowired
    ResourcesRepository resourcesRepo;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;
    
    private StorageHelper storageHelper;

    @Override
    public void init(OData odata, ServiceMetadata sm) {
        this.odata = odata;
        this.serviceMetadata = sm;

        storageHelper = new StorageHelper(resourcesRepo,rabbitTemplate,exchange);
    }
    
      
    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        EdmEntitySet responseEdmEntitySet = null; // for building ContextURL
        EntityCollection responseEntityCollection = null; // for the response body
        Object obj = null;
        InputStream stream = null;
        
        String jsonFilter = null;
        Integer top = null;
        //TOP
        TopOption topOption = uriInfo.getTopOption();
        if (topOption != null) {
            int topNumber = topOption.getValue();
            if (topNumber >= 0) {
                log.info("Top: "+topNumber);
                top = topNumber;
            } else {
                throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }
        
        //FILTER
        FilterOption filter = uriInfo.getFilterOption();
        Object o = null;
        if(filter != null){
            Expression expression = filter.getExpression();
            o = storageHelper.calculateFilter(expression);
            
            try{
                ObjectMapper map = new ObjectMapper();
                map.configure(SerializationFeature.INDENT_OUTPUT, true);
                map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                jsonFilter = map.writeValueAsString(o);
                log.info("JsonFilter:");
                log.info(jsonFilter);
            }
            catch(Exception e){
                log.error(e.getMessage());
            }
        }
        
        
        
        
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
                ResourceInfo resource = storageHelper.getResourceInfo(startEdmEntitySet,keyPredicates);
                if (resource == null) {
                    throw new ODataApplicationException("Entity not found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                }
                EdmEntityType startEntityType = startEdmEntitySet.getEntityType();
                obj = storageHelper.getRelatedObject(resource, startEntityType, targetEntityType, top, jsonFilter);
                ////Entity sourceEntity = storageHelper.readEntityData(startEdmEntitySet, keyPredicates);
                // error handling for e.g.  DemoService.svc/Categories(99)/Products
                ////if (sourceEntity == null) {
                    ////throw new ODataApplicationException("Entity not found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                ////}
                // then fetch the entity collection where the entity navigates to
                ////EdmEntityType startEntityType = startEdmEntitySet.getEntityType();
                ////esponseEntityCollection = storageHelper.getRelatedEntityCollection(sourceEntity, startEntityType, targetEntityType);
            }
        } else { // this would be the case for e.g. Products(1)/Category/Products
            throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }
        // 3rd: create and configure a serializer
        ////ContextURL contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).build();
        ////final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
        ////EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().contextURL(contextUrl).id(id).build();
        ////EdmEntityType edmEntityType = responseEdmEntitySet.getEntityType();

        ////ODataSerializer serializer = odata.createSerializer(responseFormat);
        ////SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, responseEntityCollection, opts);

        
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(obj);
            stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        }
        catch(Exception e){
            log.error(e.getMessage());
        }
        
        // 4th: configure the response object: set the body, headers and status code
        //response.setContent(serializerResult.getContent());
        response.setContent(stream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
}

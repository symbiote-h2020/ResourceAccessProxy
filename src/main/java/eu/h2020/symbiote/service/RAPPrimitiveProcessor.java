/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.messages.resourceAccessNotification.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.cloud.model.rap.query.Query;
import static eu.h2020.symbiote.service.RAPEntityCollectionProcessor.setErrorResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.core.uri.UriResourcePrimitivePropertyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Luca Tomaselli
 */
@Component
public class RAPPrimitiveProcessor implements PrimitiveProcessor {

    private static final Logger log = LoggerFactory.getLogger(RAPPrimitiveProcessor.class);
    
    @Autowired
    private ResourcesRepository resourcesRepo;
    
    @Autowired
    private PluginRepository pluginRepo;
    
    @Autowired
    private AuthorizationManager authManager;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
        
    @Value("${rabbit.replyTimeout}")
    private int rabbitReplyTimeout;

    private StorageHelper storageHelper;
    
    @Override
    public void init(OData odata, ServiceMetadata sm) {
        storageHelper = new StorageHelper(resourcesRepo, pluginRepo, authManager,
                rabbitTemplate, rabbitReplyTimeout, exchange,notificationUrl);
    }
    
    //Sensor('id')/name

    /**
     * This method is used to read an primitive in OData
     *
     * @param request OData request
     * @param response OData response
     * @param uriInfo info about OData URI
     * @param responseFormat content type returned
     * @throws ODataApplicationException application exception in OData
     * @throws ODataLibraryException exception in OData library
     */
    @Override
    public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) 
            throws ODataApplicationException, ODataLibraryException {
        Object obj;
        Integer top = null;  
        Query filterQuery = null;
        InputStream stream = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);        
        CustomODataApplicationException customOdataException = null;
        
        List<String> typeNameList = new ArrayList<>();

        // 1st retrieve the requested EntitySet from the uriInfo
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            customOdataException = new CustomODataApplicationException(null,"Only EntitySet is supported", 
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            //throw customOdataException;
                setErrorResponse(response, customOdataException, responseFormat);
                return;
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;

        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();
        String typeName = startEdmEntitySet.getEntityType().getName();

        typeNameList.add(typeName);

        if (segmentCount > 1) {
            for (int i = 1; i < segmentCount; i++) {
                UriResource segment = resourceParts.get(i);
                if (segment instanceof UriResourceNavigation) {
                    UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) segment;
                    EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                    EdmEntityType targetEntityType = edmNavigationProperty.getType();
                    String typeNameEntity = targetEntityType.getName();
                    typeNameList.add(typeNameEntity);
                }
                else if (segment instanceof UriResourcePrimitivePropertyImpl) {
                    UriResourcePrimitivePropertyImpl uriResourcePrimitivePropertyImpl = (UriResourcePrimitivePropertyImpl) segment;
                    EdmProperty edmProperty = uriResourcePrimitivePropertyImpl.getProperty();
                    String typeNameEntity = edmProperty.getName();
                    typeNameList.add(typeNameEntity);
                }
            }
        }

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        String symbioteId = null;
        List<DbResourceInfo> resourceInfoList;
        try {
            resourceInfoList = storageHelper.getResourceInfoList(typeNameList,keyPredicates);
            for(DbResourceInfo resourceInfo: resourceInfoList){
                String symbioteIdTemp = resourceInfo.getSymbioteId();
                if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty())
                    symbioteId = symbioteIdTemp;
            }
        } catch(ODataApplicationException odataExc){
            log.error("Entity not found: " + odataExc.getMessage());
            customOdataException = new CustomODataApplicationException(null,
                    "Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
            setErrorResponse(response, customOdataException, responseFormat);
            return;
        }
        
        // checking access policies
        try {
            for(DbResourceInfo resource : resourceInfoList) {
                String sid = resource.getSymbioteId();
                if(sid != null && sid.length() > 0)
                    storageHelper.checkAccessPolicies(request, sid);
            }
        } catch (Exception ex) {
            log.error("Access policy check error: " + ex.getMessage());
            customOdataException = new CustomODataApplicationException(symbioteId, ex.getMessage(), 
                    HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
            setErrorResponse(response, customOdataException, responseFormat);
            return;
        }
        
        
        
        try{
            obj = storageHelper.getRelatedObject(DbResourceInfo.toResourceInfos(resourceInfoList), top, filterQuery);
        }
        catch(ODataApplicationException odataExc){
            log.error(odataExc.getMessage());
            customOdataException = new CustomODataApplicationException(symbioteId,odataExc.getMessage(), 
                    odataExc.getStatusCode(), odataExc.getLocale());
            //throw customOdataException;
                setErrorResponse(response, customOdataException, responseFormat);
                return;
        }
        

        try {
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = map.writeValueAsString(obj);
            stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        } catch (UnsupportedEncodingException ex) {
            log.error(ex.getMessage());
        }
        
        if(customOdataException == null && stream != null)
            storageHelper.sendSuccessfulAccessMessage(symbioteId,
                    SuccessfulAccessMessageInfo.AccessType.NORMAL.name());
        
        // 4th: configure the response object: set the body, headers and status code
        //response.setContent(serializerResult.getContent());
        response.setContent(stream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        
    }

    @Override
    public void updatePrimitive(ODataRequest odr, ODataResponse odr1, UriInfo ui, ContentType ct, ContentType ct1) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deletePrimitive(ODataRequest odr, ODataResponse odr1, UriInfo ui) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}

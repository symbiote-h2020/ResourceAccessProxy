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
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import static eu.h2020.symbiote.service.RAPEntityCollectionProcessor.setErrorResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.data.Entity;
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
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */

@Component
public class RAPEntityProcessor implements EntityProcessor{
    
    private static final Logger log = LoggerFactory.getLogger(RAPEntityProcessor.class);
    
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
    private TopicExchange exchange;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    private OData odata;
    
    private StorageHelper storageHelper;
        
    @Value("${rabbit.replyTimeout}")
    private int rabbitReplyTimeout;
    
    @Override
    public void init(OData odata, ServiceMetadata sm) {
        this.odata = odata;   
    //    this.serviceMetadata = sm;
        storageHelper = new StorageHelper(resourcesRepo, pluginRepo, authManager, 
                rabbitTemplate, rabbitReplyTimeout, exchange,notificationUrl);
    }
    
    //Sensor('id')
    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) 
            throws ODataApplicationException, ODataLibraryException{
        
        try {
            InputStream stream = null;

            ObjectMapper map = new ObjectMapper();
            map.configure(SerializationFeature.INDENT_OUTPUT, true);

            CustomODataApplicationException customOdataException = null;

            ArrayList<String> typeNameList = new ArrayList();
            // 1st retrieve the requested EntitySet from the uriInfo
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            int segmentCount = resourceParts.size();

            UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
            if (!(uriResource instanceof UriResourceEntitySet)) {
                customOdataException = new CustomODataApplicationException(null, "Only EntitySet is supported", 
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
                //throw customOdataException;
                    RAPEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                    log.error("Only EntitySet is supported");
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
                }
            }

            List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
            ResourceInfo resource = storageHelper.getResourceInfo(keyPredicates);
            if (resource == null) {
                customOdataException = new CustomODataApplicationException(null, "Resource ID has not been found.", 
                                                                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                RAPEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                log.error("Resource ID has not been found");
                return;
            }
            
            // checking access policies
            try {
                String sid = resource.getSymbioteId();
                if(sid != null && sid.length() > 0)
                    storageHelper.checkAccessPolicies(request, sid);
            } catch (Exception ex) {
                customOdataException = new CustomODataApplicationException(resource.getSymbioteId(), ex.getMessage(), 
                        HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                log.error("Access policy check error", ex);
                return;
            }
            
            try {
                map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                String json = map.writeValueAsString(resource);
                stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
            } catch (JsonProcessingException e) {
                log.error("Error while trying to convert json", e);
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding", e);
            }

            if(customOdataException == null && stream != null)
                storageHelper.sendSuccessfulAccessMessage(resource.getSymbioteId(),
                        SuccessfulAccessMessageInfo.AccessType.NORMAL.name());

            // 4th: configure the response object: set the body, headers and status code
            //response.setContent(serializerResult.getContent());
            response.setContent(stream);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception ex) {
            log.error("Generic Error", ex);
            throw ex;
        }
    }
    
    @Override
    public void createEntity(ODataRequest odr, ODataResponse odr1, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        try {
            CustomODataApplicationException customOdataException = null;        
            String responseString;
            InputStream stream = null;
            String body = null;

            ArrayList<String> typeNameList = new ArrayList();

            // 1st retrieve the requested EntitySet from the uriInfo
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            int segmentCount = resourceParts.size();

            UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
            if (!(uriResource instanceof UriResourceEntitySet)) {
                customOdataException = new CustomODataApplicationException(null, "Only EntitySet is supported", 
                                                                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
                RAPEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                log.error("Only EntitySet is supported");
                return;
            }

            InputStream requestInputStream = request.getBody();
            ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);

            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
            EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();
            EdmEntityType startEntityType = startEdmEntitySet.getEntityType();


            EdmEntityType targetEntityType = startEntityType;
            String typeName = startEntityType.getName();
            typeNameList.add(typeName);

            if (segmentCount > 1) {
                for (int i = 1; i < segmentCount; i++) {
                    UriResource segment = resourceParts.get(i);
                    if (segment instanceof UriResourceNavigation) {
                        UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) segment;
                        EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                        targetEntityType = edmNavigationProperty.getType();
                        String typeNameEntity = targetEntityType.getName();
                        typeNameList.add(typeNameEntity);
                    }
                }
            }

            List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();        
            try { 
                body = IOUtils.toString(requestInputStream, "UTF-8");
            } catch (IOException ex) {
                log.error("Invalid message body", ex);
            }
            requestInputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            DeserializerResult result = deserializer.entity(requestInputStream, targetEntityType);
            Entity requestEntity = result.getEntity();

            String symbioteId = null;
            ArrayList<ResourceInfo> resourceInfoList;
            try {
                resourceInfoList = storageHelper.getResourceInfoList(typeNameList,keyPredicates);
                for(ResourceInfo resourceInfo: resourceInfoList){
                    String symbioteIdTemp = resourceInfo.getSymbioteId();
                    if(symbioteIdTemp != null && !symbioteIdTemp.isEmpty())
                        symbioteId = symbioteIdTemp;
                }
            } catch (ODataApplicationException odataExc){
                customOdataException = new CustomODataApplicationException(null,"Entity not found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                RAPEntityCollectionProcessor.setErrorResponse(response, customOdataException, responseFormat);
                log.error("Entity not found", odataExc);
                return;
            }

            // checking access policies
            try {
                for(ResourceInfo resource : resourceInfoList) {
                    String sid = resource.getSymbioteId();
                    if(sid != null && sid.length() > 0)
                        storageHelper.checkAccessPolicies(request, sid);
                }
            } catch (Exception ex) {
                customOdataException = new CustomODataApplicationException(symbioteId, ex.getMessage(), 
                        HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                log.error("Access policy check error", ex);
                return;
            }

            Object obj = storageHelper.setService(resourceInfoList, body);        
            responseString = "";        
            if ((obj != null) && (obj instanceof byte[])) {
                try {
                    responseString = new String((byte[]) obj, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    log.warn(ex.getMessage());
                }
            } else {
                responseString = (String) obj;
            }       

            try {
                stream = new ByteArrayInputStream(responseString.getBytes("UTF-8"));
            } catch(Exception e){
                log.error(e.getMessage());
            }

            if(customOdataException == null && stream != null)
                storageHelper.sendSuccessfulAccessMessage(resourceInfoList.get(0).getSymbioteId(),SuccessfulAccessMessageInfo.AccessType.NORMAL.name());

            // 4th: configure the response object: set the body, headers and status code
            //response.setContent(serializerResult.getContent());
            response.setContent(stream);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
         } catch (Exception ex) {
            log.error("Generic Error", ex);
            throw ex;
        }
    }

    @Override
    public void deleteEntity(ODataRequest odr, ODataResponse odr1, UriInfo ui) throws ODataApplicationException, ODataLibraryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

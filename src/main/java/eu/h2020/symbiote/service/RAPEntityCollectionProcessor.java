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
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.interfaces.ResourceAccessNotificationService;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.messages.plugin.RapPluginOkResponse;
import eu.h2020.symbiote.messages.plugin.RapPluginResponse;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.cloud.model.rap.query.Query;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Locale;
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
import org.springframework.stereotype.Component;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOptionKind;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Luca Tomaselli
 */
@Component
public class RAPEntityCollectionProcessor implements EntityCollectionProcessor {

    private static final Logger log = LoggerFactory.getLogger(RAPEntityCollectionProcessor.class);
    
    @Autowired
    private ResourceAccessNotificationService notificationService;
    
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
    
    @Value("${rabbit.replyTimeout}")
    private int rabbitReplyTimeout;
    
    @Value("${rap.validateServiceRequestPayload:false}")
    private boolean validateServicePayload;
    
    @Value("${rap.validateActuatorRequestPayload:false}")
    private boolean validateActuatorPayload;
    
    @Value("${rap.validateServiceResponsePayload:false}")
    private boolean validateServiceResult;

    private StorageHelper storageHelper;
    
    @Override
    public void init(OData odata, ServiceMetadata sm) {
    //    this.odata = odata;
    //    this.serviceMetadata = sm;
        storageHelper = new StorageHelper(resourcesRepo, pluginRepo, authManager, 
                rabbitTemplate, rabbitReplyTimeout, exchange, notificationService);
        storageHelper.setValidateActuatorRequestPayload(validateActuatorPayload);
        storageHelper.setValidateServiceRequestPayload(validateServicePayload);
        storageHelper.setValidateServiceResponsePayload(validateServiceResult);

    }

    //Sensor('id')/Observation

    /**
     * This method is used to read a collection of entities in OData
     *
     * @param request OData request
     * @param response OData response
     * @param uriInfo info about OData URI
     * @param responseFormat content type of response
     * @throws ODataApplicationException application exception in OData
     * @throws ODataLibraryException exception in OData library
     */
    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        try {
            RapPluginResponse rapPluginResponse;
            ObjectMapper map = new ObjectMapper();
            map.configure(SerializationFeature.INDENT_OUTPUT, true);        
            CustomODataApplicationException customOdataException = null;
            String jsonFilter;
            Integer top = null;
            
            // check if all query options are valid
            StringBuffer sb = new StringBuffer();
            for(SystemQueryOption option: uriInfo.getSystemQueryOptions()) {
                if(option.getKind() != SystemQueryOptionKind.TOP && option.getKind() != SystemQueryOptionKind.FILTER)
                    sb.append("Query option '" + option.getName() + "' is not supported. ");
            }
            if(sb.length() != 0) {
                setErrorResponse(response, 
                        new CustomODataApplicationException(null, sb.toString().trim(), HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT),
                        responseFormat);
                return;
            }
                
            //TOP
            TopOption topOption = uriInfo.getTopOption();
            if (topOption != null) {
                int topNumber = topOption.getValue();
                if (topNumber >= 0) {
                    log.debug("Top: " + topNumber);
                    top = topNumber;
                } else {
                    log.error("Invalid value for $top");
                    customOdataException = new CustomODataApplicationException(null, "Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
                    //throw customOdataException;
                    setErrorResponse(response, customOdataException, responseFormat);
                    return;
                }
            }

            //FILTER
            FilterOption filter = uriInfo.getFilterOption();
            Query filterQuery = null;
            if (filter != null) {
                Expression expression = filter.getExpression();
                try {
                    filterQuery = StorageHelper.calculateFilter(expression);
                } catch (ODataApplicationException odataExc) {
                    log.error("Error while reading filters: ", odataExc.getMessage());
                    customOdataException = new CustomODataApplicationException(null, odataExc.getMessage(),
                            odataExc.getStatusCode(), odataExc.getLocale());
                    //throw customOdataException;
                    setErrorResponse(response, customOdataException, responseFormat);
                    return;
                }
                try {
                    map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    jsonFilter = map.writeValueAsString(filterQuery);
                    log.info("Filter:");
                    log.info(jsonFilter);
                } catch (Exception e) {
                    log.error("Error while converting filter", e);
                }
            }

            ArrayList<String> typeNameList = new ArrayList<>();
            // 1st retrieve the requested EntitySet from the uriInfo
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            int segmentCount = resourceParts.size();
            UriResource uriResource = resourceParts.get(0); // the first segment is the EntitySet
            if (!(uriResource instanceof UriResourceEntitySet)) {
                customOdataException = new CustomODataApplicationException(null, "Only EntitySet is supported", 
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
                //throw customOdataException;
                setErrorResponse(response, customOdataException, responseFormat);
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
                log.error("Entity not found: ", odataExc);
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
                log.error("Access policy check error: ", ex);
                customOdataException = new CustomODataApplicationException(symbioteId, ex.getMessage(), 
                        HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ROOT);
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }

            try {
                rapPluginResponse = storageHelper.getRelatedObject(DbResourceInfo.toResourceInfos(resourceInfoList), top, filterQuery);
            } catch(ODataApplicationException odataExc) {
                log.error("Resource ID has not been served. Cause:\n" + odataExc.getMessage(), odataExc);
                customOdataException = new CustomODataApplicationException(symbioteId, odataExc.getMessage(), 
                        odataExc.getStatusCode(), odataExc.getLocale());
                //throw customOdataException;
                setErrorResponse(response, customOdataException, responseFormat);
                return;
            }


//            try {
//                map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
//                String json = map.writeValueAsString(rapPluginResponse);
//                stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
//            } catch (JsonProcessingException e) {
//                log.error("Error while trying to convert json", e);
//            } catch (UnsupportedEncodingException e) {
//                log.error("Unsupported encoding", e);
//            }
            if(customOdataException == null && rapPluginResponse instanceof RapPluginOkResponse)
                storageHelper.sendSuccessfulAccessMessage(symbioteId, SuccessfulAccessMessageInfo.AccessType.NORMAL.name());

            // 4th: configure the response object: set the body, headers and status code
            //response.setContent(serializerResult.getContent());
            response.setContent(new ByteArrayInputStream(rapPluginResponse.getContent().getBytes(StandardCharsets.UTF_8)));
            response.setStatusCode(rapPluginResponse.getResponseCode());
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception ex) {
            log.error("Generic error", ex);
            throw ex;
        }
    }

    /**
     * This method is used to set the error in response message
     * @param response OData response
     * @param customOdataException application exception
     * @param responseFormat content type of OData response
     * @return OData error response
     */
    public static ODataResponse setErrorResponse(ODataResponse response, 
            CustomODataApplicationException customOdataException, ContentType responseFormat){
        InputStream stream = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        try {
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = map.writeValueAsString(customOdataException);
            stream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        } catch (JsonProcessingException e) {
                log.error("Error while trying to convert json", e);
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding", e);
            }
        response.setContent(stream);
        response.setStatusCode(customOdataException.getStatusCode());
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        
        return response;
    }
}

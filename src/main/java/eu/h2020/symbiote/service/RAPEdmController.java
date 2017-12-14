/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
import eu.h2020.symbiote.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceODataCondition;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.apache.olingo.commons.api.ex.ODataException;

import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.core.ODataHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.messages.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import java.io.InputStream;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;

/*
*
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Conditional(NBInterfaceODataCondition.class)
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.PUT, RequestMethod.GET})
@RestController
@RequestMapping("rap")
public class RAPEdmController {

    private static final Logger log = LoggerFactory.getLogger(RAPEdmController.class);

    private static final String URI = "rap/";
    private int split = 0;
    public final String SECURITY_RESPONSE_HEADER = "x-auth-response";

    @Autowired
    private RAPEdmProvider edmProvider;

    @Autowired
    private RAPEntityCollectionProcessor entityCollectionProcessor;

    @Autowired
    private RAPEntityProcessor entityProcessor;
    
    //@Autowired
    //private RAPPrimitiveProcessor primitiveProcessor;
    
    @Autowired
    private IComponentSecurityHandler securityHandler;
            
    @Value("${symbiote.notification.url}") 
    private String notificationUrl;
    
    
    /**
     * Process.
     *
     * @param req the req
     * @return the response entity
     * @throws java.lang.Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "**")
    public ResponseEntity<String> process(HttpServletRequest req) throws Exception {
        return processRequestPrivate(req);
    }

    @RequestMapping(value = "*('{resourceId}')/*")
    public ResponseEntity<String> processResources(HttpServletRequest req) throws Exception {
        return processRequestPrivate(req);
    }

    private ResponseEntity<String> processRequestPrivate(HttpServletRequest req) throws Exception {
        ODataResponse response = null;
        String responseStr = null;
        MultiValueMap<String, String> headers = new HttpHeaders();
        HttpStatus httpStatus = null;
        try {            
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList());
            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);
            //handler.register(primitiveProcessor);

            response = handler.process(createODataRequest(req, split));
            
            
            if(response.getStatusCode() != HttpStatus.OK.value()){
                String errorMessage = Integer.toString(response.getStatusCode());
                InputStream inputStream = response.getContent();
                if(inputStream != null)
                    errorMessage = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
                responseStr = sendFailMessage(req, errorMessage);
            }
            else 
                responseStr = StreamUtils.copyToString(
                    response.getContent(), Charset.defaultCharset());


            httpStatus = HttpStatus.valueOf(response.getStatusCode());            
        } catch (IOException | ODataException e) {
            responseStr = sendFailMessage(req, e.getMessage());
            log.error(e.getMessage(), e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try{
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Credentials", "true");
            headers.add("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT");
            headers.add("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");
            
            String securityResponseHrd = securityHandler.generateServiceResponse();
            headers.add(SECURITY_RESPONSE_HEADER, securityResponseHrd);
        }
        catch(SecurityHandlerException sce){
            log.error(sce.getMessage(), sce);
            throw sce;
        }
        catch(Exception e){
            log.error(e.getMessage(), e);
        }
        
        return new ResponseEntity(responseStr, headers, httpStatus);
    }

    private String sendFailMessage(HttpServletRequest request, String error) {
        String message = "";
        try{
            String jsonNotificationMessage = null;
            String symbioTeId = "";
            String appId = "";
            String issuer = ""; 
            String validationStatus = "";
            CustomODataApplicationException customOdataExc = null;
            ObjectMapper mapper = new ObjectMapper();

            String code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
            message = "Error: " + error;               
            try {
                customOdataExc = mapper.readValue(message, CustomODataApplicationException.class);
            } catch (IOException ex) {
            }
            if(customOdataExc != null){
                if(customOdataExc.getSymbioteId() != null)
                    symbioTeId = customOdataExc.getSymbioteId();
                message = customOdataExc.getMessage();
            }            
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            List<Date> dateList = new ArrayList<>();
            dateList.add(new Date());
            NotificationMessage notificationMessage = new NotificationMessage(securityHandler,notificationUrl);
            try {
                notificationMessage.SetFailedAttempts(symbioTeId, dateList, 
                code, message, appId, issuer, validationStatus, request.getRequestURI()); 
                jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException jsonEx) {
                log.error(jsonEx.toString(), jsonEx);
            }
            notificationMessage.SendFailAccessMessage(jsonNotificationMessage);
        }catch(Exception e){
            log.error("Error to send FailAccessMessage to CRAM");
            log.error(e.getMessage(),e);
        }
        return message;
    }
    
    

    /**
     * Creates the o data request.
     *
     * @param httpRequest the http request
     * @param split the split
     * @return the o data request
     * @throws ODataTranslatedException the o data translated exception
     */
    private ODataRequest createODataRequest(final HttpServletRequest httpRequest, final int split) throws ODataException {
        try {
            ODataRequest odRequest = new ODataRequest();

            odRequest.setBody(httpRequest.getInputStream());
            extractHeaders(odRequest, httpRequest);
            extractMethod(odRequest, httpRequest);
            extractUri(odRequest, httpRequest, split);

            return odRequest;
        } catch (final IOException e) {
            throw new SerializerException("An I/O exception occurred.", e,
                    SerializerException.MessageKeys.IO_EXCEPTION);
        }
    }

    /**
     * Extract method.
     *
     * @param odRequest the od request
     * @param httpRequest the http request
     * @throws ODataTranslatedException the o data translated exception
     */
    private void extractMethod(final ODataRequest odRequest, final HttpServletRequest httpRequest) throws ODataException {
        try {
            HttpMethod httpRequestMethod = HttpMethod.valueOf(httpRequest
                    .getMethod());

            if (httpRequestMethod == HttpMethod.POST) {
                String xHttpMethod = httpRequest
                        .getHeader(HttpHeader.X_HTTP_METHOD);
                String xHttpMethodOverride = httpRequest
                        .getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE);

                if (xHttpMethod == null && xHttpMethodOverride == null) {
                    odRequest.setMethod(httpRequestMethod);
                } else if (xHttpMethod == null) {
                    odRequest
                            .setMethod(HttpMethod.valueOf(xHttpMethodOverride));
                } else if (xHttpMethodOverride == null) {
                    odRequest.setMethod(HttpMethod.valueOf(xHttpMethod));
                } else {
                    if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
                        throw new ODataHandlerException(
                                "Ambiguous X-HTTP-Methods",
                                ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD,
                                xHttpMethod, xHttpMethodOverride);
                    }
                    odRequest.setMethod(HttpMethod.valueOf(xHttpMethod));
                }
            } else {
                odRequest.setMethod(httpRequestMethod);
            }
        } catch (IllegalArgumentException e) {
            throw new ODataHandlerException("Invalid HTTP method"
                    + httpRequest.getMethod(),
                    ODataHandlerException.MessageKeys.INVALID_HTTP_METHOD,
                    httpRequest.getMethod());
        }
    }

    /**
     * Extract uri.
     *
     * @param odRequest the od request
     * @param httpRequest the http request
     * @param split the split
     */
    private void extractUri(final ODataRequest odRequest, final HttpServletRequest httpRequest, final int split) {
        String rawRequestUri = httpRequest.getRequestURL().toString();

        String rawODataPath;
        if (!"".equals(httpRequest.getServletPath())) {
            int beginIndex;
            beginIndex = rawRequestUri.indexOf(URI);
            beginIndex += URI.length();
            rawODataPath = rawRequestUri.substring(beginIndex);
        } else if (!"".equals(httpRequest.getContextPath())) {
            int beginIndex;
            beginIndex = rawRequestUri.indexOf(httpRequest.getContextPath());
            beginIndex += httpRequest.getContextPath().length();
            rawODataPath = rawRequestUri.substring(beginIndex);
        } else {
            rawODataPath = httpRequest.getRequestURI();
        }

        String rawServiceResolutionUri;
        if (split > 0) {
            rawServiceResolutionUri = rawODataPath;
            for (int i = 0; i < split; i++) {
                int e = rawODataPath.indexOf("/", 1);
                if (-1 == e) {
                    rawODataPath = "";
                } else {
                    rawODataPath = rawODataPath.substring(e);
                }
            }
            int end = rawServiceResolutionUri.length() - rawODataPath.length();
            rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
        } else {
            rawServiceResolutionUri = null;
        }

        String rawBaseUri = rawRequestUri.substring(0, rawRequestUri.length()
                - rawODataPath.length());

        String rawQueryPath = httpRequest.getQueryString();
        String rawRequestUriComplete = rawRequestUri
                + (httpRequest.getQueryString() == null ? "" : "?"
                + httpRequest.getQueryString());
        odRequest.setRawQueryPath(rawQueryPath);
        odRequest.setRawRequestUri(rawRequestUriComplete);
        odRequest.setRawODataPath(rawODataPath);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
    }

    /**
     * Extract headers.
     *
     * @param odRequest the od request
     * @param req the req
     */ 
    private void extractHeaders(final ODataRequest odRequest, final HttpServletRequest req) {
        for (Enumeration<?> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
            String headerName = (String) headerNames.nextElement();
            List<String> headerValues = new ArrayList();
            for (Enumeration<?> headers = req.getHeaders(headerName); headers.hasMoreElements();) {
                String value = (String) headers.nextElement();
                headerValues.add(value);
            }
            odRequest.addHeader(headerName, headerValues);
        }
    }

    
}

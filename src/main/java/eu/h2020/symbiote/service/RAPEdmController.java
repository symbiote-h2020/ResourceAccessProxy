/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

/**
 *
 * @author Luca Tomaselli
 */
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.exceptions.CustomODataApplicationException;
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceODataCondition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
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
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.interfaces.ResourceAccessNotificationService;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.ServiceResponseResult;
import springfox.documentation.annotations.ApiIgnore;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

/*
*
* @author Luca Tomaselli
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
    ResourceAccessNotificationService notificationService;
    
    @Autowired
    private RAPEdmProvider edmProvider;

    @Autowired
    private RAPEntityCollectionProcessor entityCollectionProcessor;

    @Autowired
    private RAPEntityProcessor entityProcessor;
    
    @Autowired
    private RAPPrimitiveProcessor primitiveProcessor;
    
    @Autowired
    private AuthorizationManager authManager;
    
    /**
     * Process.
     *
     * @param req the req
     * @return the response entity
     * @throws java.lang.Exception can throw exception
     */
    @ApiIgnore
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "**")
    public ResponseEntity<String> process(HttpServletRequest req) throws Exception {
        return processRequestPrivate(req);
    }

    @ApiIgnore
    @RequestMapping(value = "*('{resourceId}')/*")
    public ResponseEntity<String> processResources(HttpServletRequest req) throws Exception {
        return processRequestPrivate(req);
    }

    @ApiOperation(value = "Get observations" ,
            notes = "This is the endpoint for getting observations from the sensors. The form of the endpoint follows" +
                    " an ODATA-like format (e.g. /rap/Sensor('symbIoTeId')/Observations), which can be obtained from the symbIoTe core, as described " +
                    "<a href=\"https://github.com/symbiote-h2020/SymbioteCloud/wiki/3.3-Obtaining-resource-access-URL\">here</a> " +
                    "and <a href=\"https://symbiote-h2020.github.io/SymbioteCore/coreInterface/#_getresourceurlsusingget\">here</a>. " +
                    "You can find examples on how to get observations " +
                    "<a href=\"https://github.com/symbiote-h2020/SymbioteCloud/wiki/3.4-Accessing-the-resource-and-actuating-and-invoking-service-for-default-(dummy)-resources\">here</a> " +
                    "and you examples using java clients " +
                    "<a href=\"https://github.com/symbiote-h2020/ExampleClient/blob/master/src/main/java/eu/h2020/symbiote/client/example/L1ClientWithGuestToken.java\">here</a>.",
            response = Observation.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "An array of observations", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Not authorized")})
    @GetMapping(value = "*('{resourceId}')/*")
    public ResponseEntity<String> processResourcesGet(@ApiParam(value = "timestamp of the request", required = true) @RequestHeader(SecurityConstants.SECURITY_CREDENTIALS_TIMESTAMP_HEADER) String timestamp,
                                                      @ApiParam(value = "securityCredentials set size header", required = true) @RequestHeader(SecurityConstants.SECURITY_CREDENTIALS_SIZE_HEADER) String size,
                                                      @ApiParam(value = "each SecurityCredentials entry header, they are numbered 1..size", required = true) @RequestHeader(SecurityConstants.SECURITY_CREDENTIALS_HEADER_PREFIX + "*") String credentials,
                                                      @ApiParam(value = "the symbIoTe id of the resource as obtained from the symbIoTe core", required = true) @RequestParam String resourceId,
                                                      HttpServletRequest req) throws Exception {
        return processRequestPrivate(req);
    }

    @ApiOperation(value = "Actuate" ,
            notes = "This is the endpoint for actuating both on actuators and services. The form of the endpoint follows" +
                    " an ODATA-like format (e.g. /rap/Service('symbIoTeId')), which can be obtained from the symbIoTe core, as described " +
                    "<a href=\"https://github.com/symbiote-h2020/SymbioteCloud/wiki/3.3-Obtaining-resource-access-URL\">here</a> " +
                    "and <a href=\"https://symbiote-h2020.github.io/SymbioteCore/coreInterface/#_getresourceurlsusingget\">here</a>. " +
                    "You can find examples on how to get observations " +
                    "<a href=\"https://github.com/symbiote-h2020/SymbioteCloud/wiki/3.4-Accessing-the-resource-and-actuating-and-invoking-service-for-default-(dummy)-resources\">here</a> " +
                    "and you examples using java clients " +
                    "<a href=\"https://github.com/symbiote-h2020/ExampleClient/blob/master/src/main/java/eu/h2020/symbiote/client/example/L1ClientWithGuestToken.java\">here</a>.",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The service response", response = String.class),
            @ApiResponse(code = 204, message = "Actuators return NO CONTENT"),
            @ApiResponse(code = 401, message = "Not authorized")})
    @PutMapping(value = "*('{resourceId}')/*")
    public ResponseEntity<String> processResourcesPut(@ApiParam(value = "timestamp of the request", required = true) @RequestHeader(SecurityConstants.SECURITY_CREDENTIALS_TIMESTAMP_HEADER) String timestamp,
                                                      @ApiParam(value = "securityCredentials set size header", required = true) @RequestHeader(SecurityConstants.SECURITY_CREDENTIALS_SIZE_HEADER) String size,
                                                      @ApiParam(value = "each SecurityCredentials entry header, they are numbered 1..size", required = true) @RequestHeader(SecurityConstants.SECURITY_CREDENTIALS_HEADER_PREFIX + "*") String credentials,
                                                      @ApiParam(value = "the symbIoTe id of the resource as obtained from the symbIoTe core", required = true) @RequestParam String resourceId,
                                                      HttpServletRequest req) throws Exception {
        return processRequestPrivate(req);
    }

    private ResponseEntity<String> processRequestPrivate(HttpServletRequest req) throws Exception {
        ODataResponse response = null;
        String responseStr = null;
        MultiValueMap<String, String> headers = new HttpHeaders();
        HttpStatus httpStatus = null;
        try {            
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<>());
            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);
            handler.register(primitiveProcessor);

            response = handler.process(createODataRequest(req, split));
            
            responseStr = StreamUtils.copyToString(response.getContent(), StandardCharsets.UTF_8);
            if(response.getStatusCode() != HttpStatus.OK.value()){
                if(responseStr != null && !responseStr.isEmpty())
                    sendFailMessage(req, responseStr);
                else
                    sendFailMessage(req, Integer.toString(response.getStatusCode()));
            }

            httpStatus = HttpStatus.valueOf(response.getStatusCode());            
        } catch (IOException | ODataException e) {
            responseStr = sendFailMessage(req, e.getClass().getName() + ": " + e.getMessage());
            log.error(e.getMessage(), e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            headers.add("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT");
            headers.add("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");
            
            ServiceResponseResult serResponse = authManager.generateServiceResponse();
            if(serResponse.isCreatedSuccessfully()) {
                headers.add(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        
        return new ResponseEntity<String>(responseStr, headers, httpStatus);
    }

    private String sendFailMessage(HttpServletRequest request, String error) {
        String message = "";
        try{
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

            List<Date> dateList = new ArrayList<>();
            dateList.add(new Date());
            
            notificationService.addFailedAttempts(symbioTeId, dateList, 
                code, message, appId, issuer, validationStatus, request.getRequestURI());
            notificationService.sendAccessData();
        }catch(Exception e){
            log.error("Error to send FailAccessMessage to Monitoring", e);
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

            extractHeaders(odRequest, httpRequest);
            extractMethod(odRequest, httpRequest);
            extractUri(odRequest, httpRequest, split);

            // set body
            StringWriter writer = new StringWriter();
            IOUtils.copy(httpRequest.getInputStream(), writer, StandardCharsets.UTF_8);
            String input = writer.toString();
            log.info("Input: {}", input);
            
            // TODO check if service is called and body need to be JSON objs separated with comma (not array)
            if(odRequest.getRawODataPath().toLowerCase().startsWith("service(") ||
            		odRequest.getRawODataPath().toLowerCase().startsWith("services(")) 
            {
                // service - coverting input JSON to input parametres separated by comma
                ObjectMapper mapper = new ObjectMapper();
                List<Object> objects = mapper.readValue(input, new TypeReference<List<Object>>() { });
                StringWriter sw = new StringWriter();
                for (Iterator<Object> iter = objects.iterator(); iter.hasNext();) {
                    Object o = iter.next();
                    mapper.writeValue(sw, o);
                    if(iter.hasNext())
                        sw.append(",\n");
                }
                input = sw.toString();
            }

            odRequest.setBody(new ReaderInputStream(new StringReader(input), StandardCharsets.UTF_8));
            
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
            int beginIndex;
            beginIndex = rawRequestUri.indexOf(URI);
            if(beginIndex > 0){
                beginIndex += URI.length();
                rawODataPath = rawRequestUri.substring(beginIndex);
            }
            else{
                rawODataPath = httpRequest.getRequestURI();
            }
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
            List<String> headerValues = new ArrayList<>();
            for (Enumeration<?> headers = req.getHeaders(headerName); headers.hasMoreElements();) {
                String value = (String) headers.nextElement();
                headerValues.add(value);
            }
            odRequest.addHeader(headerName, headerValues);
        }
    }

    
}

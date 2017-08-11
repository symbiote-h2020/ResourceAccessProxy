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
import eu.h2020.symbiote.security.InternalSecurityHandler;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.security.enums.ValidationStatus;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.messages.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.messages.accessNotificationMessages.SuccessfulAccessMessageInfo;
import io.jsonwebtoken.Claims;
import java.sql.Timestamp;

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

    @Autowired
    private RAPEdmProvider edmProvider;

    @Autowired
    private RAPEntityCollectionProcessor entityCollectionProcessor;

    @Autowired
    private RAPEntityProcessor entityProcessor;

    @Autowired
    private InternalSecurityHandler securityHandler;

    @Value("${platform.id}")
    private String platformId;

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
        split = 0;
        return processRequestPrivate(req);
    }

    @RequestMapping(value = "*('{resourceId}')/*")
    public ResponseEntity<String> processResources(HttpServletRequest req) throws Exception {
        split = 0;
        return processRequestPrivate(req);
    }

    private ResponseEntity<String> processRequestPrivate(HttpServletRequest req) throws Exception {
        ODataResponse response = null;
        String token = null;
        Exception e = null;
        try {

            token = req.getHeader("X-Auth-Token");
            checkToken(token);
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList());

            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);

            response = handler.process(createODataRequest(req, split));
            
            String responseStr = "";
            if(response.getStatusCode() != HttpStatus.OK.value())
                responseStr = sendFailMessage(req, token, response);
            else 
                responseStr = StreamUtils.copyToString(
                    response.getContent(), Charset.defaultCharset());
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Credentials", "true");
            headers.add("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT");
            headers.add("Access-Control-Allow-Headers", "Origin, Content-Type, X-Auth-Token");

            return new ResponseEntity(responseStr, headers, HttpStatus.valueOf(response.getStatusCode()));
        } catch (TokenValidationException ex) { 
            e = ex;
            log.error(e.toString());
        } catch (Exception exc) {
            e = exc;
            log.error(e.toString());
        }
        
        sendFailMessage(req, token, response);
        throw e;
    }

    private String sendFailMessage(HttpServletRequest request, String token, ODataResponse response) {
        String jsonNotificationMessage = null;
        String symbioTeId = "";
        String appId = "";String issuer = ""; String validationStatus = "";
        CustomODataApplicationException customOdataExc = null;
        ObjectMapper mapper = new ObjectMapper();
        
        String code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String message = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
        
        if(response != null){
            code = Integer.toString(response.getStatusCode());
            try {
                message = StreamUtils.copyToString(response.getContent(), Charset.defaultCharset());
            } catch (IOException ex) {
                log.error(ex.getMessage());
            }
        }
        
        try {
            customOdataExc = mapper.readValue(message, CustomODataApplicationException.class);
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
        if(customOdataExc != null){
            if(customOdataExc.getSymbioteId() != null)
                symbioTeId = customOdataExc.getSymbioteId();
            message = customOdataExc.getMessage();
        }
        
        if(token != null && !token.isEmpty()){
            try{
                Token tok = new Token(token);
                Claims claims = tok.getClaims();
                appId = claims.getSubject();
                issuer = claims.getIssuer();
                ValidationStatus status = securityHandler.verifyHomeToken(tok);
                validationStatus = status.name();
            }
            catch(TokenValidationException tokenExc){
                validationStatus = tokenExc.getErrorMessage();
            }
            catch(Exception e){
                log.error(e.getMessage());
            }
        }
            
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        NotificationMessage notificationMessage = new NotificationMessage();
        try {
            notificationMessage.SetFailedAttempts(symbioTeId, timestamp, 
            code, message, appId, issuer, validationStatus, request.getRequestURI()); 
            jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException jsonEx) {
            log.error(jsonEx.getMessage());
        }
        NotificationMessage.SendFailAccessMessage(jsonNotificationMessage);
        return message;
    }
    
    public static void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        String jsonNotificationMessage = null;
        if(accessType == null || accessType.isEmpty())
            accessType = SuccessfulAccessMessageInfo.AccessType.NORMAL.name();
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        NotificationMessage notificationMessage = new NotificationMessage();
        
        try{
            notificationMessage.SetSuccessfulAttempts(symbioteId, timestamp, accessType);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        NotificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
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

    private void checkToken(String tokenString) throws TokenValidationException {
        log.debug("RAP received a request for the following token: " + tokenString);

        Token token = new Token(tokenString);

        ValidationStatus status = securityHandler.verifyHomeToken(token);
        switch (status) {
            case VALID: {
                log.info("Token is VALID");
                break;
            }
            case VALID_OFFLINE: {
                log.info("Token is VALID_OFFLINE");
                break;
            }
            case EXPIRED: {
                log.info("Token is EXPIRED");
                throw new TokenValidationException("Token is EXPIRED");
            }
            case REVOKED: {
                log.info("Token is REVOKED");
                throw new TokenValidationException("Token is REVOKED");
            }
            case INVALID: {
                log.info("Token is INVALID");
                throw new TokenValidationException("Token is INVALID");
            }
            case NULL: {
                log.info("Token is NULL");
                throw new TokenValidationException("Token is NULL");
            }
        }
    }
}

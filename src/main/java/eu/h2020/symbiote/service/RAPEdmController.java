/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.service;

/**
 *
 * @author luca-
 */
import eu.h2020.symbiote.interfaces.conditions.NBInterfaceODataCondition;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.apache.olingo.commons.api.edmx.EdmxReference;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/*
*
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
@Conditional(NBInterfaceODataCondition.class)
@RestController
@RequestMapping("rap")
public class RAPEdmController {

    private static String URI = "rap/";

    /**
     * The split.
     */
    private int split = 0;
    /**
     * The ctx.
     */
    @Autowired
    private ApplicationContext ctx;

    /**
     * The edm provider.
     */
    @Autowired 
    private RAPEdmProvider edmProvider;
    
    /**
     * The enity collection processor.
     */
    @Autowired
    private RAPEntityCollectionProcessor entityCollectionProcessor;
    
    @Autowired
    private RAPEntityProcessor entityProcessor;

    /**
     * Process.
     *
     * @param req the req
     * @return the response entity
     */
    //@CrossOrigin(origins = "http://localhost:8080")
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "*")
    public ResponseEntity<String> process(HttpServletRequest req) throws Exception {
        split = 0;
        try {
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider,
                    new ArrayList<EdmxReference>());
            
            //ODataHandler handler = new ODataHandler(odata, edm);
            //handler.register(entityCollectionProcessor);

//            ServiceMetadata edm = odata.createServiceMetadata(new DemoEdmProvider(), new ArrayList<EdmxReference>());
//            ODataHttpHandler handler = odata.createHandler(edm);
//            handler.register(new DemoEntityCollectionProcessor());

            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);


            ODataResponse response = handler.process(createODataRequest(req,split));
            String responseStr = StreamUtils.copyToString(
                    response.getContent(), Charset.defaultCharset());
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
//          for (String key : response.getHeaders().keySet()) {
//              headers.add(key, response.getHeaders().get(key).toString());
//          }
//            for (String key : response.getHeaders(URI)) {
//                headers.add(key, response.getHeaders(URI).get(key).toString());
//            }
            return new ResponseEntity<String>(responseStr, headers,
                    HttpStatus.valueOf(response.getStatusCode()));
        } catch (Exception ex) {
            //throw new EdmException();
            throw ex;
        }

    }

    
    @RequestMapping(value = "Subscribe/*")
    public ResponseEntity<String> processSubscribe(HttpServletRequest req) throws Exception {
        split = 1;
        try {
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider,
                    new ArrayList<EdmxReference>());
            
            //ODataHandler handler = new ODataHandler(odata, edm);
            //handler.register(entityCollectionProcessor);

//            ServiceMetadata edm = odata.createServiceMetadata(new DemoEdmProvider(), new ArrayList<EdmxReference>());
//            ODataHttpHandler handler = odata.createHandler(edm);
//            handler.register(new DemoEntityCollectionProcessor());

            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);


            ODataResponse response = handler.process(createODataRequest(req,split));
            String responseStr = StreamUtils.copyToString(
                    response.getContent(), Charset.defaultCharset());
            MultiValueMap<String, String> headers = new HttpHeaders();
//          for (String key : response.getHeaders().keySet()) {
//              headers.add(key, response.getHeaders().get(key).toString());
//          }
//            for (String key : response.getHeaders(URI)) {
//                headers.add(key, response.getHeaders(URI).get(key).toString());
//            }
            return new ResponseEntity<String>(responseStr, headers,
                    HttpStatus.valueOf(response.getStatusCode()));
        } catch (Exception ex) {
            //throw new EdmException();
            throw ex;
        }

    }
    
    
    
    @RequestMapping(value="*('{resourceId}')/*")
    public ResponseEntity<String> processResources(HttpServletRequest req) throws Exception {
        split = 0;
        return processRequestPrivate(req);
    }
    
    /*
    @RequestMapping(value="Actuators('{resourceId}')/*")
    public ResponseEntity<String> processActuators(HttpServletRequest req) throws Exception {
        split = 0;
        return processRequestPrivate(req);
    }*/
    
    private ResponseEntity<String> processRequestPrivate (HttpServletRequest req) throws Exception{
        try {
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider,
                    new ArrayList<EdmxReference>());

            ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(entityCollectionProcessor);
            handler.register(entityProcessor);


            ODataResponse response = handler.process(createODataRequest(req,split));
            String responseStr = StreamUtils.copyToString(
                    response.getContent(), Charset.defaultCharset());
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            return new ResponseEntity<String>(responseStr, headers,
                    HttpStatus.valueOf(response.getStatusCode()));
        } catch (Exception ex) {
            throw ex;
        }
    }
    
    
    
    
    
    
    
    
    
    
    /**
     * Creates the o data request.
     *
     * @param httpRequest the http request
     * @param split the split
     * @return the o data request
     * @throws ODataTranslatedException the o data translated exception
     */
    private ODataRequest createODataRequest(
            final HttpServletRequest httpRequest, final int split)
            throws ODataException {
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
    private void extractMethod(final ODataRequest odRequest,
            final HttpServletRequest httpRequest)
            throws ODataException {
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
    private void extractUri(final ODataRequest odRequest,
            final HttpServletRequest httpRequest, final int split) {
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

        //rawODataPath = "Products";
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
    private void extractHeaders(final ODataRequest odRequest,
            final HttpServletRequest req) {
        for (Enumeration<?> headerNames = req.getHeaderNames(); headerNames
                .hasMoreElements();) {
            String headerName = (String) headerNames.nextElement();
            List<String> headerValues = new ArrayList<String>();
            for (Enumeration<?> headers = req.getHeaders(headerName); headers
                    .hasMoreElements();) {
                String value = (String) headers.nextElement();
                headerValues.add(value);
            }
            odRequest.addHeader(headerName, headerValues);
        }
    }

}

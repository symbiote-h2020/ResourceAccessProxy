/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import org.apache.olingo.server.api.ODataApplicationException;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class CustomODataApplicationException {//extends ODataApplicationException{
    @JsonProperty("symbioteId")
    private String symbioteId;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("statusCode")
    private int statusCode;
    
    @JsonProperty("locale")
    private Locale locale;
    
    @JsonCreator
    public CustomODataApplicationException(@JsonProperty("symbioteId") String symbioteId, 
            @JsonProperty("message") String msg, @JsonProperty("statusCode") int statusCode, 
            @JsonProperty("locale") Locale locale) {
        //super(msg, statusCode, locale);
        this.message = msg;
        this.statusCode = statusCode;
        this.locale = locale;
        this.symbioteId = symbioteId;
    }

    /*public CustomODataApplicationException(String symbioteId, String msg, int statusCode, Locale locale, String oDataErrorCode) {
        super(msg, statusCode, locale, oDataErrorCode);
        this.symbioteId = symbioteId;
    }
    
    public CustomODataApplicationException(String symbioteId, String msg, int statusCode, Locale locale, Throwable cause) {
        super(msg, statusCode, locale, cause);
        this.symbioteId = symbioteId;
    }

    public CustomODataApplicationException(String symbioteId, String msg, int statusCode, Locale locale, Throwable cause, String oDataErrorCode) {
        super(msg, statusCode, locale, cause, oDataErrorCode);
        this.symbioteId = symbioteId;
    }*/

    
    public String getSymbioteId() {
        return symbioteId;
    }
    
    public int getStatusCode(){
        return statusCode;
    }
    
    public String getMessage(){
        return message;
    }
}

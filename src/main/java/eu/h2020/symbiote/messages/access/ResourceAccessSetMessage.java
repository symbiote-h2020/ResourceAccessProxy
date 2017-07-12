/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.resources.db.ResourceInfo;
import java.util.List;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
public class ResourceAccessSetMessage extends ResourceAccessMessage{
    @JsonProperty("resourceInfo")
    private final ResourceInfo resInfo;
    
    @JsonProperty("inputParameters")
    private final List<InputParameter> inputParameters;
    
    @JsonProperty("requestInfo")
    private List<RequestInfo> requestInfo;
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information
     * @param inputParameters       the list of parameters to set
     */
    @JsonCreator
    public ResourceAccessSetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
                                    @JsonProperty("inputParameters") List<InputParameter> inputParameters) {
        this.accessType = ResourceAccessMessage.AccessType.SET;
        this.resInfo = resInfo;
        this.inputParameters = inputParameters;
        requestInfo = null;
    }
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information
     * @param inputParameters       the list of parameters to set
     * @param requestInfo           the path of request
     */
    @JsonCreator
    public ResourceAccessSetMessage(@JsonProperty("resourceInfo") ResourceInfo resInfo, 
                                    @JsonProperty("inputParameters") List<InputParameter> inputParameters,
                                    @JsonProperty("requestInfo") List<RequestInfo> requestInfo) {
        this.accessType = ResourceAccessMessage.AccessType.SET;
        this.resInfo = resInfo;
        this.inputParameters = inputParameters;
        this.requestInfo = requestInfo;
    }
    
    @JsonProperty("resourceInfo")
    public ResourceInfo getResourceInfo() {
        return resInfo;
    }
    
    @JsonProperty("inputParameters")
    public List<InputParameter> getInputParameters() {
        return inputParameters;
    }
}

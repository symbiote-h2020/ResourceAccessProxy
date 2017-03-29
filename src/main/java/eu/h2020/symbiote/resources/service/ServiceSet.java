/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author luca-
 */
public class ServiceSet extends Service{
    @JsonProperty("inputParameter")
    private List<InputParameter> inputParameter;
    
    @JsonCreator
    public ServiceSet(String ID, List<InputParameter> inputParameter) {
        super(ID);
        this.inputParameter = inputParameter;
    }
}

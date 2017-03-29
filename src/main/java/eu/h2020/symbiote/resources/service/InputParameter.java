/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author luca-
 */
public class InputParameter {
    @JsonProperty("name")
    private String name;
    @JsonProperty("value")
    private String value;
    
    @JsonCreator
    public InputParameter(String name, String value){
        this.name = name;
        this.value = value;
    }
    
    public String getname() {
        return name;
    }
    
    public String getvalue() {
        return value;
    }
}

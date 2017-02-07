/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.model.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.h2020.symbiote.messages.RegisterPluginMessage;
import eu.h2020.symbiote.messages.UnregisterPluginMessage;

/**
 *
 * @author Aleksandar Antonic <aleksandar.antonic@fer.hr>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "locationType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WGS84Location.class,   name = "WGS84Location")
})
abstract public class Location {
    @JsonProperty("label")
    private final String label;
    @JsonProperty("comment")
    private final String comment;

    public Location(String label, String comment) {
        this.label = label;
        this.comment = comment;
    }

    public String getLabel() {
        return label;
    }

    public String getComment() {
        return comment;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.plugin;

import eu.h2020.symbiote.model.data.Observation;
import eu.h2020.symbiote.model.data.ObservationValue;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public interface PlatformPlugin {
    
    public Observation readResource(String resourceId);
    public void writeResource(String resourceId, ObservationValue value);
}

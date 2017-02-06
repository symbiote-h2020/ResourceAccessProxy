/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.model.data;

/**
 *
 * @author Aleksandar Antonic <aleksandar.antonic@fer.hr>
 */
public class ObservationValue {
    
    private Object value;
    private Property obsProperty;
    private UnitOfMeasurement uom;

    public ObservationValue(Object value, Property obsProperty, UnitOfMeasurement uom) {
        this.value = value;
        this.obsProperty = obsProperty;
        this.uom = uom;
    }

    public Object getValue() {
        return value;
    }

    public Property getObsProperty() {
        return obsProperty;
    }

    public UnitOfMeasurement getUom() {
        return uom;
    }

}

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
public class UnitOfMeasurement {
    
    private String symbol;
    private String label;
    private String comment;

    public UnitOfMeasurement(String symbol, String label, String comment) {
        this.symbol = symbol;
        this.label = label;
        this.comment = comment;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLabel() {
        return label;
    }

    public String getComment() {
        return comment;
    }
    
}

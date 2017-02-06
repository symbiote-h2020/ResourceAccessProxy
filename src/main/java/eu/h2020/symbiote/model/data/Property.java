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
public class Property {
    
    private String label;
    private String comment;

    public Property(String label, String comment) {
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

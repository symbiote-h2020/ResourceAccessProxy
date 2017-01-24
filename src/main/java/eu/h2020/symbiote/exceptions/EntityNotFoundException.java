/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String entityId) {
        super ("Could not find object with id " + entityId);
    }
}
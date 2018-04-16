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
 * @author Matteo Pardi
 */
@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class TimeoutException extends RuntimeException {
    private static final long serialVersionUID = -8566221444898253954L;

    public TimeoutException(String reason) {
        super ("Timeout failure: " + reason);
    }   
}

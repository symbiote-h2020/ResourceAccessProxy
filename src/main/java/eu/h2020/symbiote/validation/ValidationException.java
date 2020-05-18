/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.validation;

/**
 *
 * @author <a href="mailto:michael.jacoby@iosb.fraunhofer.de">Michael Jacoby</a>
 */
@SuppressWarnings("serial")
public class ValidationException extends Exception {

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(String message) {
        super(message);
    }
}

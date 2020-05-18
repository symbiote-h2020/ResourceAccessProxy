/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.validation.value;

/**
 *
 * @author <a href="mailto:michael.jacoby@iosb.fraunhofer.de">Michael Jacoby</a>
 */
@SuppressWarnings("serial")
public class ValueCastException extends RuntimeException {

    public ValueCastException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValueCastException(String message) {
        super(message);
    }
}

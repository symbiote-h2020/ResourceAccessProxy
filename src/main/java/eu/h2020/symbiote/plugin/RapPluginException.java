package eu.h2020.symbiote.plugin;

import eu.h2020.symbiote.messages.plugin.RapPluginErrorResponse;

public class RapPluginException extends RuntimeException {
    private RapPluginErrorResponse response;

    public RapPluginException(int responseCode, String message) {
        super(message);
        response = new RapPluginErrorResponse(responseCode, message);
    }

    public RapPluginException(int responseCode, String message, Throwable reason) {
        super(message, reason);
        response = new RapPluginErrorResponse(responseCode, message);
    }

    public RapPluginException(int responseCode, Throwable reason) {
        super(reason);
        response = new RapPluginErrorResponse(responseCode, reason.getMessage());
    }
    
    public RapPluginErrorResponse getResponse() {
        return response;
    }
}

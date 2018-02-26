package eu.h2020.symbiote.plugin;

import eu.h2020.symbiote.messages.plugin.RapPluginErrorResponse;

public class RapPluginException extends RuntimeException {
    private RapPluginErrorResponse response;

    public RapPluginException(int responseCode, String message) {
        super(message);
        response = new RapPluginErrorResponse(responseCode, message);
    }

    public RapPluginErrorResponse getResponse() {
        return response;
    }
}

package eu.h2020.symbiote.messages.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.olingo.commons.api.http.HttpStatusCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.plugin.RapPluginException;

public class RapPluginOkResponse extends RapPluginResponse {
    private String jsonBody;
    
    public RapPluginOkResponse() {
        setResponseCode(204);
    }
    
    public RapPluginOkResponse(String jsonBody) throws RapPluginException {
        setBody(jsonBody);
        updateResponseCode();
    }

    private void updateResponseCode() {
        if(jsonBody == null)
            setResponseCode(204);
        else
            setResponseCode(200);
    }
    
    public RapPluginOkResponse(int responseCode, String jsonBody) throws RapPluginException {
        setBody(jsonBody);
        setResponseCode(responseCode);
    }

    public static RapPluginOkResponse createFromJson(int responseCode, String json) {
        RapPluginOkResponse response = new RapPluginOkResponse();
        response.setResponseCode(responseCode);
        response.updateJsonBody(json);
        return response;
    }
    
    public static RapPluginOkResponse createFromJson(String json) {
        RapPluginOkResponse response = new RapPluginOkResponse();
        response.updateJsonBody(json);
        response.updateResponseCode();
        return response;
    }
    
    public static RapPluginOkResponse createFromObject(int responseCode, Object object) {
        RapPluginOkResponse response = new RapPluginOkResponse();
        response.setResponseCode(responseCode);
        response.updateBody(object);
        return response;
    }

    public static RapPluginOkResponse createFromObject(Object object) {
        RapPluginOkResponse response = new RapPluginOkResponse();
        response.updateBody(object);
        response.updateResponseCode();
        return response;
    }
    
    @Override
    public void setResponseCode(int responseCode) {
        if(responseCode < 200 || responseCode > 299)
            throw new IllegalArgumentException("Response code should be in range from 200-299");
        super.setResponseCode(responseCode);
    }

    public void updateJsonBody(String jsonBody) throws RapPluginException {
        this.jsonBody = jsonBody;
    }

    public void setBody(String body) throws RapPluginException {
        updateJsonBody(body);
    }
    
    public void updateBody(Object object) {
      ObjectMapper mapper = new ObjectMapper();
      try {
          updateJsonBody(mapper.writeValueAsString(object));
      } catch (JsonProcessingException e) {
          throw new RapPluginException(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), "Content of body can not be serialized to JSON. Body is of type " + object.getClass().getName());
      }
    }
    
    public String getBody() {
        return jsonBody;
    }

    /**
     * Try to parse Observation from jsonBody 
     * @return optional of observation
     */
    public Optional<Observation> bodyToObservation() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Optional.of(mapper.readValue(jsonBody, Observation.class));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Try to parse List<Observation> from jsonBody 
     * @return optional of list of observations
     */
    public Optional<List<Observation>> bodyToObservations() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Optional.of(mapper.readValue(jsonBody, new TypeReference<List<Observation>>() {}));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jsonBody) + super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof RapPluginOkResponse))
            return false;
        RapPluginOkResponse other = (RapPluginOkResponse) obj;
        
        return super.equals(other) && Objects.equals(jsonBody, other.jsonBody);
    }

    @Override
    public String getContent() throws RapPluginException {
        return jsonBody;
    }
}

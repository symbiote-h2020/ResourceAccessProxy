package eu.h2020.symbiote.messages.plugin;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.model.cim.Observation;

public class RapPluginResponseTest {
    ObjectMapper mapper;
    RapPluginResponse response;
    String json;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
    }
    
    @Test
    public void testSerializingOkResponseWithoutBody() throws Exception {
        response = new RapPluginOkResponse();
        json = mapper.writeValueAsString(response);
        assertThat(json).isEqualTo("{\"@c\":\".RapPluginOkResponse\",\"body\":null,\"responseCode\":204}");
    }

    @Test
    public void testSerializingOkResponseWithBody() throws Exception {
        response = new RapPluginOkResponse("body text");
        json = mapper.writeValueAsString(response);
        assertThat(json).isEqualTo("{\"@c\":\".RapPluginOkResponse\",\"body\":\"body text\",\"responseCode\":200}");
    }

    @Test
    public void testSerializingOkResponseWithCustomStatusCode() throws Exception {
        response = new RapPluginOkResponse(201, "body text");
        json = mapper.writeValueAsString(response);
        assertThat(json).isEqualTo("{\"@c\":\".RapPluginOkResponse\",\"body\":\"body text\",\"responseCode\":201}");
    }

    @Test
    public void testSerializingErrorResponse() throws Exception {
        response = new RapPluginErrorResponse(404, "body text");
        json = mapper.writeValueAsString(response);
        assertThat(json).isEqualTo("{\"@c\":\".RapPluginErrorResponse\",\"message\":\"body text\",\"responseCode\":404}");
    }
    
    @Test
    public void testDeserializingOkResponse() throws Exception {
        response = mapper.readValue("{\"@c\":\".RapPluginOkResponse\",\"body\":\"some text\",\"responseCode\":201}", RapPluginResponse.class);
        
        assertThat(response).isInstanceOf(RapPluginOkResponse.class);
        assertThat(response.getResponseCode()).isEqualTo(201);
        assertThat(((RapPluginOkResponse)response).getBody()).isEqualTo("some text");
    }

    @Test
    public void testDeserializingOkResponseWithObservation() throws Exception {
        String jsonContent = "{\n" + 
                "  \"@c\" : \".RapPluginOkResponse\",\n" + 
                "  \"body\" : \"{\\n" + 
                "    \\\"resourceId\\\" : \\\"symbIoTeID1\\\",\\n" + 
                "    \\\"location\\\" : {\\n" + 
                "      \\\"@c\\\" : \\\".WGS84Location\\\",\\n" + 
                "      \\\"longitude\\\" : 15.9,\\n" + 
                "      \\\"latitude\\\" : 45.8,\\n" + 
                "      \\\"altitude\\\" : 145.0,\\n" + 
                "      \\\"name\\\" : \\\"Spansko\\\",\\n" + 
                "      \\\"description\\\" : [ \\\"City of Zagreb\\\" ]\\n" + 
                "    },\\n" + 
                "    \\\"resultTime\\\" : \\\"2018-02-26T09:04:52\\\",\\n" + 
                "    \\\"samplingTime\\\" : \\\"2018-02-26T09:04:51\\\",\\n" + 
                "    \\\"obsValues\\\" : [ {\\n" + 
                "      \\\"value\\\" : \\\"7\\\",\\n" + 
                "      \\\"obsProperty\\\" : {\\n" + 
                "        \\\"name\\\" : \\\"Temperature\\\",\\n" + 
                "        \\\"description\\\" : [ \\\"Air temperature\\\" ]\\n" + 
                "      },\\n" + 
                "      \\\"uom\\\" : {\\n" + 
                "        \\\"symbol\\\" : \\\"C\\\",\\n" + 
                "        \\\"name\\\" : \\\"degree Celsius\\\",\\n" + 
                "        \\\"description\\\" : [ \\\"Temperature in degree Celsius\\\" ]\\n" + 
                "      }\\n" + 
                "    } ]\\n" + 
                "  }\",\n" + 
                "  \"responseCode\" : 200\n" + 
                "}";
        response = mapper.readValue(jsonContent, RapPluginResponse.class);
        
        assertThat(response).isInstanceOf(RapPluginOkResponse.class);
        assertThat(response.getResponseCode()).isEqualTo(200);
        assertThat(((RapPluginOkResponse)response).bodyToObservation().isPresent()).isTrue();
    }
    
    @Test
    public void testDeserializingOkResponseWithListOfObservations() throws Exception {
        String jsonContent = "{\n" + 
                "  \"@c\" : \".RapPluginOkResponse\",\n" + 
                "  \"body\" : \"[ {\\n" + 
                "    \\\"resourceId\\\" : \\\"symbIoTeID1\\\",\\n" + 
                "    \\\"location\\\" : {\\n" + 
                "      \\\"@c\\\" : \\\".WGS84Location\\\",\\n" + 
                "      \\\"longitude\\\" : 15.9,\\n" + 
                "      \\\"latitude\\\" : 45.8,\\n" + 
                "      \\\"altitude\\\" : 145.0,\\n" + 
                "      \\\"name\\\" : \\\"Spansko\\\",\\n" + 
                "      \\\"description\\\" : [ \\\"City of Zagreb\\\" ]\\n" + 
                "    },\\n" + 
                "    \\\"resultTime\\\" : \\\"2018-02-26T10:06:36\\\",\\n" + 
                "    \\\"samplingTime\\\" : \\\"2018-02-26T10:06:35\\\",\\n" + 
                "    \\\"obsValues\\\" : [ {\\n" + 
                "      \\\"value\\\" : \\\"7\\\",\\n" + 
                "      \\\"obsProperty\\\" : {\\n" + 
                "        \\\"name\\\" : \\\"Temperature\\\",\\n" + 
                "        \\\"description\\\" : [ \\\"Air temperature\\\" ]\\n" + 
                "      },\\n" + 
                "      \\\"uom\\\" : {\\n" + 
                "        \\\"symbol\\\" : \\\"C\\\",\\n" + 
                "        \\\"name\\\" : \\\"degree Celsius\\\",\\n" + 
                "        \\\"description\\\" : [ \\\"Temperature in degree Celsius\\\" ]\\n" + 
                "      }\\n" + 
                "    } ]\\n" + 
                "  }, {\\n" + 
                "    \\\"resourceId\\\" : \\\"symbIoTeID1\\\",\\n" + 
                "    \\\"location\\\" : {\\n" + 
                "      \\\"@c\\\" : \\\".WGS84Location\\\",\\n" + 
                "      \\\"longitude\\\" : 15.9,\\n" + 
                "      \\\"latitude\\\" : 45.8,\\n" + 
                "      \\\"altitude\\\" : 145.0,\\n" + 
                "      \\\"name\\\" : \\\"Spansko\\\",\\n" + 
                "      \\\"description\\\" : [ \\\"City of Zagreb\\\" ]\\n" + 
                "    },\\n" + 
                "    \\\"resultTime\\\" : \\\"2018-02-26T10:06:36\\\",\\n" + 
                "    \\\"samplingTime\\\" : \\\"2018-02-26T10:06:35\\\",\\n" + 
                "    \\\"obsValues\\\" : [ {\\n" + 
                "      \\\"value\\\" : \\\"7\\\",\\n" + 
                "      \\\"obsProperty\\\" : {\\n" + 
                "        \\\"name\\\" : \\\"Temperature\\\",\\n" + 
                "        \\\"description\\\" : [ \\\"Air temperature\\\" ]\\n" + 
                "      },\\n" + 
                "      \\\"uom\\\" : {\\n" + 
                "        \\\"symbol\\\" : \\\"C\\\",\\n" + 
                "        \\\"name\\\" : \\\"degree Celsius\\\",\\n" + 
                "        \\\"description\\\" : [ \\\"Temperature in degree Celsius\\\" ]\\n" + 
                "      }\\n" + 
                "    } ]\\n" + 
                "  } ]\",\n" + 
                "  \"responseCode\" : 200\n" + 
                "}";
        response = mapper.readValue(jsonContent, RapPluginResponse.class);

        assertThat(response).isInstanceOf(RapPluginOkResponse.class);
        assertThat(response.getResponseCode()).isEqualTo(200);
        Optional<List<Observation>> optional = ((RapPluginOkResponse)response).bodyToObservations();
        assertThat(optional.isPresent()).isTrue();
    }

    @Test
    public void testDeserializingErrorResponse() throws Exception {
        response = mapper.readValue("{\"@c\":\".RapPluginErrorResponse\",\"message\":\"message text\",\"responseCode\":404}", RapPluginResponse.class);
        
        assertThat(response).isInstanceOf(RapPluginErrorResponse.class);
        assertThat(response.getResponseCode()).isEqualTo(404);
        assertThat(((RapPluginErrorResponse)response).getMessage()).isEqualTo("message text");
    }
    
    static class ClassWithSelfReference {
        private ClassWithSelfReference self;
        
        public ClassWithSelfReference() {
            self = this;
        }

        public ClassWithSelfReference getSelf() {
            return self;
        }

        public void setSelf(ClassWithSelfReference self) {
            this.self = self;
        }
    }
}

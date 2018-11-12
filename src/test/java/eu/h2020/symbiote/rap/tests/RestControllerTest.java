package eu.h2020.symbiote.rap.tests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.interfaces.ResourceAccessNotificationService;
import eu.h2020.symbiote.interfaces.ResourceAccessRestController;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.managers.ServiceResponseResult;
import eu.h2020.symbiote.messages.plugin.RapPluginOkResponse;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.plugin.PlatformSpecificPlugin;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.runner.RunWith;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;

import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 *
 * @author Luca Tomaselli, Mario Kusek
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ResourceAccessRestController.class)
@ActiveProfiles("test")
public class RestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    ResourceAccessNotificationService notificationService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    private TopicExchange exchange;

    @MockBean
    private ResourcesRepository resourcesRepository;

    @MockBean
    private PluginRepository pluginRepo;

    @MockBean
    private AuthorizationManager authorizationManager;

    @SuppressWarnings("unused")
    @MockBean
    private AccessPolicyRepository accessPolicyRepo;

    private void securityOk() {
        setExpectedSecurity(true);
    }

    private void securityNotOk() {
        setExpectedSecurity(false);
    }

    private void setExpectedSecurity(boolean valid) {
        when(authorizationManager.checkResourceUrlRequest(any(), any()))
                .thenReturn(new AuthorizationResult("Validated", valid));
        when(authorizationManager.generateServiceResponse()).thenReturn(new ServiceResponseResult("", false));
    }

    @Test
    public void testGetWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        when(resourcesRepository.findById(resourceId)).thenReturn(Optional.empty());

        // when
        mockMvc.perform(get("/rap/Sensor/" + resourceId).headers(getHeaders())
                .accept(new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(),
                        Charset.forName("utf8"))))
                // then
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetWhenOneResourceRegisteredInInternalPlugin_shouldReturnResource() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(RapPluginOkResponse.createFromObject(new Observation(resourceId, null, null, null, null)));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor/" + resourceId).headers(getHeaders()));

        // then
        res.andExpect(status().isOk());
        String content = res.andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        Observation observation = mapper.readValue(content, Observation.class);

        assertThat(observation).isNotNull();
    }

    @Test
    public void testGetWhenOneResourceRegisteredInExternalPluginThatIsNotRegistered_shouldReturnGatewayTimeout()
            throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor/" + resourceId).headers(getHeaders()));

        // then
        res.andExpect(status().isGatewayTimeout());
    }

    @Test
    public void testGetWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor/" + resourceId).headers(getHeaders()));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testHistoryWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        when(resourcesRepository.findById(resourceId)).thenReturn(Optional.empty());

        // when
        mockMvc.perform(
                get("/rap/Sensor/" + resourceId + "/history").headers(getHeaders())
                        .accept(new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"))))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testHistoryWhenOneResourceRegisteredInInternalPlugin_shouldReturnResources() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(RapPluginOkResponse.createFromObject(Arrays.asList(new Observation(resourceId, null, null, null, null),
                        new Observation(resourceId, null, null, null, null))));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor/" + resourceId + "/history").headers(getHeaders()));

        // then
        res.andExpect(status().isOk());
        String content = res.andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<Observation> observations = mapper.readValue(content, new TypeReference<List<Observation>>() {
        });

        assertThat(observations)
            .isNotNull()
            .hasSize(2);
    }

    @Test
    public void testHistoryWhenOneResourceRegisteredInExternalPluginThatIsNotRegistered_shouldReturnGatewayTimeout()
            throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor/" + resourceId + "/history").headers(getHeaders()));

        // then
        res.andExpect(status().isGatewayTimeout());
    }
    
    @Test
    public void testHistoryWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor/" + resourceId + "/history").headers(getHeaders()));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testSetActuateWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        when(resourcesRepository.findById(resourceId)).thenReturn(Optional.empty());

        // when
        mockMvc.perform(
                post("/rap/Actuator/" + resourceId).headers(getHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("null")
                        .accept(new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"))))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSetActuateWhenOneResourceRegisteredInInternalPlugin_shouldReturnResource() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(post("/rap/Actuator/" + resourceId).headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("null"));

        // then
        res.andExpect(status().isNoContent());
        String content = res.andReturn().getResponse().getContentAsString();

        assertThat(content).isEqualTo("");
    }
    
    @Test
    public void testSetActuateWhenOneResourceRegisteredInExternalPluginThatIsNotRegistered_shouldReturnGatewayTimeout()
            throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(post("/rap/Actuator/" + resourceId).headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("null"));

        // then
        res.andExpect(status().isGatewayTimeout());
    }

    @Test
    public void testSetActuateWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";

        // when
        ResultActions res = mockMvc.perform(post("/rap/Actuator/" + resourceId).headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("null"));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testSetServiceWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        when(resourcesRepository.findById(resourceId)).thenReturn(Optional.empty());

        // when
        mockMvc.perform(
                post("/rap/Service/" + resourceId).headers(getHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("null")
                        .accept(new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"))))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSetServiceWhenOneResourceRegisteredInInternalPlugin_shouldReturnResource() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(RapPluginOkResponse.createFromObject("service response"));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(post("/rap/Service/" + resourceId).headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("null"));

        // then
        res.andExpect(status().isOk());
        String content = res.andReturn().getResponse().getContentAsString();
        assertThat(content).isEqualTo("\"service response\"");

    }
 
    @Test
    public void testSetServiceWhenOneResourceRegisteredInExternalPluginThatIsNotRegistered_shouldReturnInternalServerError()
            throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(post("/rap/Service/" + resourceId).headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("null"));

        // then
        res.andExpect(status().isGatewayTimeout());
    }

    @Test
    public void testSetServiceWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";

        // when
        ResultActions res = mockMvc.perform(post("/rap/Service/" + resourceId).headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("null"));

        // then
        res.andExpect(status().isUnauthorized());
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-auth-timestamp", "1");
        headers.add("x-auth-size", "0");
        return headers;
    }

    private DbResourceInfo createResource(String resourceId, String platformResourceId, List<String> obsProperties,
            String pluginId) {
        DbResourceInfo resourceInfo = new DbResourceInfo(resourceId, platformResourceId);
        resourceInfo.setObservedProperties(obsProperties);
        resourceInfo.setPluginId(pluginId);
        return resourceInfo;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rap.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.bim.OwlapiHelp;
import eu.h2020.symbiote.interfaces.ResourceAccessNotificationService;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.managers.ServiceResponseResult;
import eu.h2020.symbiote.messages.plugin.RapPluginOkResponse;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Builders;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.model.cim.LengthRestriction;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.RangeRestriction;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.plugin.PlatformSpecificPlugin;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.resources.db.RegistrationInfoODataRepository;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.service.RAPEdmController;
import eu.h2020.symbiote.service.RAPEdmProvider;
import eu.h2020.symbiote.service.RAPEntityCollectionProcessor;
import eu.h2020.symbiote.service.RAPEntityProcessor;
import eu.h2020.symbiote.service.RAPPrimitiveProcessor;

/**
 *
 * @author Luca Tomaselli, Mario Kusek
 */
@RunWith(SpringRunner.class)
@WebMvcTest(RAPEdmController.class)
@Import({OwlapiHelp.class, RAPEdmProvider.class, RAPEntityProcessor.class, RAPEntityCollectionProcessor.class, RAPPrimitiveProcessor.class})
@ActiveProfiles("test")
public class TestODataController {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    OwlapiHelp owlapiHelp;
    
    @MockBean
    private AuthorizationManager authorizationManager;
    
    @MockBean
    ResourceAccessNotificationService notificationService;

    @MockBean
    private ResourcesRepository resourcesRepository;
    
    @MockBean
    private PluginRepository pluginRepository;
    
    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    @Qualifier(RapDefinitions.PLUGIN_EXCHANGE_OUT)
    TopicExchange exchange;

    @MockBean
    RegistrationInfoODataRepository registrationInfoODataRepository;
    
    @Before
    public void setup() throws Exception {
        //owlapiHelp.clearAll();
    }
    
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
        int top = 1;

        // when
        mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top).headers(getHeaders())
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
        int top = 1;

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse(new Observation(resourceId, null, null, null, null)));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top).headers(getHeaders())
                .accept(new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(),
                        Charset.forName("utf8"))));

        // then
        res.andExpect(status().isOk());
        String content = res.andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<Observation> observations = mapper.readValue(content, new TypeReference<List<Observation>>() {});

        assertThat(observations)
            .isNotNull()
            .hasSize(1)
            .extracting("resourceId")
                .containsExactly("1");
    }

    @Test
    public void testGetWhenOneResourceRegisteredInExternalPluginThatIsNotRegistered_shouldReturnGatewayTimeout()
            throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        int top = 1;

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top).headers(getHeaders())
                .accept(new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(),
                        Charset.forName("utf8"))));

        // then
        res.andExpect(status().isGatewayTimeout());
    }

    @Test
    public void testGetWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";
        int top = 1;

        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top).headers(getHeaders())
                .accept(new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(),
                        Charset.forName("utf8"))));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testHistoryWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        int top = 10;

        // when
        mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top)
                .headers(getHeaders())
                .accept(
                        new MediaType(MediaType.APPLICATION_JSON.getType(),
                                MediaType.APPLICATION_JSON.getSubtype(),
                                Charset.forName("utf8") )
                ))
                // then
                .andExpect(status().isNotFound());
    }


    @Test
    public void testHistoryWhenOneResourceRegisteredInInternalPlugin_shouldReturnResources() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        int top = 10;

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse(Arrays.asList(new Observation(resourceId, null, null, null, null),
                        new Observation(resourceId, null, null, null, null))));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top)
                .headers(getHeaders()));
        
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
        int top = 10;

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top)
                .headers(getHeaders()));

        // then
        res.andExpect(status().isGatewayTimeout());
    }

    @Test
    public void testHistoryWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";
        int top = 10;

        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));

        // when
        ResultActions res = mockMvc.perform(get("/rap/Sensor('"+resourceId+"')/Observation?$top="+top)
                .headers(getHeaders()));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testSetActuateWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // when
        mockMvc.perform(
                put("/rap/ImaginaryActuator('"+resourceId+"')").headers(getHeaders()).contentType(MediaType.APPLICATION_JSON)
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
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"r\":0,\"g\":0,\"b\":0}]}"));

        // then
        res.andExpect(status().isNoContent());
        String content = res.andReturn().getResponse().getContentAsString();

        assertThat(content).isEqualTo("null");
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
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"r\":0,\"g\":0,\"b\":0}]}"));

        // then
        res.andExpect(status().isGatewayTimeout());
    }

    @Test
    public void testSetActuateWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";

        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));

        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"r\":0,\"g\":0,\"b\":0}]}"));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testSetActuateOnOffCapability_shouldReturnNoContent() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"OnOffCapability\": [{\"on\":true}]}"));

        // then
        res.andExpect(status().isNoContent());
        String content = res.andReturn().getResponse().getContentAsString();

        assertThat(content).isEqualTo("null");
    }

    @Test
    public void testSetActuateRgbLight_shouldReturnNoContent() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(put("/rap/Light('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"r\":0,\"g\":0,\"b\":0}]}"));

        // then
        res.andExpect(status().isNoContent());
        String content = res.andReturn().getResponse().getContentAsString();

        assertThat(content).isEqualTo("null");
    }
    
    @Test
    public void testSetActuateDimmerLight_shouldReturnNoContent() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Light('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"DimmerCapability\": [{\"level\":0}]}"));
        
        // then
        res.andExpect(status().isNoContent());
        String content = res.andReturn().getResponse().getContentAsString();
        
        assertThat(content).isEqualTo("null");
    }
    
    @Test
    public void testSetActuateCurtain_shouldReturnNoContent() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Curtain('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"SetPositionCapability\": [{\"position\":0}]}"));
        
        // then
        res.andExpect(status().isNoContent());
        String content = res.andReturn().getResponse().getContentAsString();
        
        assertThat(content).isEqualTo("null");
    }

    @Test
    public void testSetActuateWrongCapability_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"WrongCapability\": [{\"position\":0}]}"));
        
        // then
        res.andExpect(status().isBadRequest());
        res.andExpect(content().string(containsString("WrongCapability")));
    }

    @Test
    public void testSetActuateWrongParameterName_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"wrongParameterName\":0}]}"));
        
        // then
        res.andExpect(status().isBadRequest());
        res.andExpect(content().string(containsString("wrongParameterName")));
    }

    @Test
    public void testSetActuateWrongParameterValueType_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"r\":\"zero\",\"g\":0,\"b\":0}]}"));
        
        // then
        res.andExpect(status().isBadRequest());
        res.andExpect(content().string(containsString("'r'")));
        res.andExpect(content().string(containsString("Invalid value")));
    }

    @Test
    public void testSetActuateWrongParameterValue_restrictionViolation_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        Actuator actuator = new Actuator();
        actuator.setId(resourceId);
        actuator.setName("Light 1");
        actuator.setDescription(Arrays.asList("This is light 1"));
        Capability rgb = new Capability();
        rgb.setName("RGBCapability");
        // int type
        PrimitiveDatatype primitiveDatatypeInt = new PrimitiveDatatype();
        primitiveDatatypeInt.setArray(false);
        primitiveDatatypeInt.setBaseDatatype("http://www.w3.org/2001/XMLSchema#int");
        // color restriction
        RangeRestriction colorRangeRestriction = new RangeRestriction();
        colorRangeRestriction.setMin(0.0);
        colorRangeRestriction.setMax(100.0);

        Parameter rParam = new Parameter();
        rParam.setDatatype(primitiveDatatypeInt);
        rParam.setName("r");
        rParam.setMandatory(true);
        rParam.setRestrictions(Arrays.asList(colorRangeRestriction));
        
        Parameter gParam = new Parameter();
        gParam.setDatatype(primitiveDatatypeInt);
        gParam.setName("r");
        gParam.setMandatory(true);
        gParam.setRestrictions(Arrays.asList(colorRangeRestriction));
        
        Parameter bParam = new Parameter();
        bParam.setDatatype(primitiveDatatypeInt);
        bParam.setName("r");
        bParam.setMandatory(true);
        bParam.setRestrictions(Arrays.asList(colorRangeRestriction));

        rgb.setParameters(Arrays.asList(rParam, gParam, bParam));
        actuator.setCapabilities(Arrays.asList(rgb));
        resourceInfoResult.setResource(actuator);
        
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Actuator('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("{\"RGBCapability\": [{\"r\":9999,\"g\":0,\"b\":0}]}"));
        
        // then
        res.andExpect(status().isBadRequest());
        res.andExpect(content().string(containsString("Validation error")));
        res.andExpect(content().string(containsString("capability 'RGBCapability'")));
        res.andExpect(content().string(containsString("parameter: 'r'")));
    }    

    @Test
    public void testSetServiceWhenNoResource_shouldReturnNotFound() throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // when
        mockMvc.perform(
                put("/rap/Service('"+resourceId+"')").headers(getHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("[{}]")
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
        String platformResourceId = "isrid1";
        List<String> obsProperties = null;
        String pluginId = PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);

        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse("some service response"));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(responseFromPlugin);

        // when
        ResultActions res = mockMvc.perform(put("/rap/Service('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("[{}]"));

        // then
        res.andExpect(status().isOk());
        String content = res.andReturn().getResponse().getContentAsString();

        assertThat(content).isEqualTo("\"some service response\"");
    }

    @Test
    public void testSetServiceWhenOneResourceRegisteredInExternalPluginThatIsNotRegistered_shouldReturnGatewayTimeout()
            throws Exception {
        // given
        securityOk();
        String resourceId = "1";

        // insert
        String platformResourceId = "isrid1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);

        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(null);

        // when
        ResultActions res = mockMvc.perform(put("/rap/Service('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("[{}]"));

        // then
        res.andExpect(status().isGatewayTimeout());
    }
    
    @Test
    public void testSetServiceWhenDontHavePermisionsForAccess_shouldReturnUnauthorized() throws Exception {
        // given
        securityNotOk();
        String resourceId = "1";

        String platformResourceId = "isrid1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);

        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));

        // when
        ResultActions res = mockMvc.perform(put("/rap/Service('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("[{}]"));

        // then
        res.andExpect(status().isUnauthorized());
    }

    @Test
    public void testSetServiceValidation_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "isrid1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        Service service = new Service();
        service.setId(resourceId);
        service.setName("Light service 1");
        service.setDescription(Arrays.asList("This is light service 1"));
        service.setInterworkingServiceURL("https://symbiotedoc.tel.fer.hr");
        
        PrimitiveDatatype stringDataType = new PrimitiveDatatype();
        stringDataType.setArray(false);
        stringDataType.setBaseDatatype("http://www.w3.org/2001/XMLSchema#string");
        
        Parameter inputParameter = new Parameter();
        inputParameter.setName("inputParam1");
        inputParameter.setMandatory(true);
        
        LengthRestriction lengthRestriction = new LengthRestriction();
        lengthRestriction.setMin(2);
        lengthRestriction.setMax(10);
        inputParameter.setRestrictions(Arrays.asList(lengthRestriction));
        
        inputParameter.setDatatype(stringDataType);
        
        service.setParameters(Arrays.asList(inputParameter));
        
        service.setResultType(stringDataType);
        
        resourceInfoResult.setResource(service);
        
        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse("service response"));
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Service('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("[{\"inputParam1\": \"o\"}]"));
        
        // then
        res.andExpect(status().isBadRequest());
        res.andExpect(content().string(containsString("Validation error")));
        res.andExpect(content().string(containsString("parameter: 'inputParam1'")));
//        res.andExpect(content().string("x"));
    }

    @Test
    public void testSetServiceWrongParameterName_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "isrid1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        Service service = new Service();
        service.setId(resourceId);
        service.setName("Light service 1");
        service.setDescription(Arrays.asList("This is light service 1"));
        service.setInterworkingServiceURL("https://symbiotedoc.tel.fer.hr");
        
        PrimitiveDatatype stringDataType = new PrimitiveDatatype();
        stringDataType.setArray(false);
        stringDataType.setBaseDatatype("http://www.w3.org/2001/XMLSchema#string");

        Parameter inputParameter = new Parameter();
            inputParameter.setName("inputParam1");
            inputParameter.setMandatory(true);
            
            LengthRestriction lengthRestriction = new LengthRestriction();
            lengthRestriction.setMin(2);
            lengthRestriction.setMax(10);
            inputParameter.setRestrictions(Arrays.asList(lengthRestriction));
            
            inputParameter.setDatatype(stringDataType);
        
        service.setParameters(Arrays.asList(inputParameter));

        service.setResultType(stringDataType);
        
        resourceInfoResult.setResource(service);

        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Service('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("[{\"wrongParameterName\":0}]"));
        
        // then
        res.andExpect(status().isBadRequest());
        res.andExpect(content().string(containsString("'wrongParameterName'")));
    }

    @Test
    public void testSetServiceWrongParameterValueType_shouldReturnBadRequest() throws Exception {
        // given
        securityOk();
        String resourceId = "1";
        
        // insert
        String platformResourceId = "isrid1";
        List<String> obsProperties = null;
        String pluginId = "notRegisteredPluginId";
        DbResourceInfo resourceInfoResult = createResource(resourceId, platformResourceId, obsProperties, pluginId);
        Service service = new Service();
        service.setId(resourceId);
        service.setName("Light service 1");
        service.setDescription(Arrays.asList("This is light service 1"));
        service.setInterworkingServiceURL("https://symbiotedoc.tel.fer.hr");
        
        PrimitiveDatatype stringDataType = new PrimitiveDatatype();
        stringDataType.setArray(false);
        stringDataType.setBaseDatatype("http://www.w3.org/2001/XMLSchema#string");

        Parameter inputParameter = new Parameter();
            inputParameter.setName("inputParam1");
            inputParameter.setMandatory(true);
            
            LengthRestriction lengthRestriction = new LengthRestriction();
            lengthRestriction.setMin(2);
            lengthRestriction.setMax(10);
            inputParameter.setRestrictions(Arrays.asList(lengthRestriction));
            
            inputParameter.setDatatype(stringDataType);
        
        service.setParameters(Arrays.asList(inputParameter));

        service.setResultType(stringDataType);
        
        resourceInfoResult.setResource(service);

        when(resourcesRepository.findById(any())).thenReturn(Optional.of(resourceInfoResult));
        when(resourcesRepository.findByInternalId(any())).thenReturn(Arrays.asList(resourceInfoResult));
        String responseFromPlugin = new ObjectMapper()
                .writeValueAsString(new RapPluginOkResponse());
        when(rabbitTemplate.convertSendAndReceive(any(String.class), any(String.class), any(Object.class)))
        .thenReturn(responseFromPlugin);
        
        AtomicReference<RegistrationInfoOData> collected = new AtomicReference<>();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                RegistrationInfoOData arg = invocation.getArgumentAt(0, RegistrationInfoOData.class);
                collected.set(arg);
                return arg;
            }
        }).when(registrationInfoODataRepository).insertNew(any(RegistrationInfoOData.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                RegistrationInfoOData data = collected.get();
                return Arrays.asList(data);
            }
        }).when(registrationInfoODataRepository).findAll();
        
        // register resource in owlapi
        owlapiHelp.addCloudResourceList(Arrays.asList(Builders.cloudResourceBuilder()
                .internalId(platformResourceId)
                .pluginId(pluginId)
                .resource(service)
                .build()));
        
        // when
        ResultActions res = mockMvc.perform(put("/rap/Service('"+resourceId+"')").headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON).content("[{\"inputParam1\": 0}]"));
        
        // then
        res.andExpect(status().isBadRequest());
        String content = res.andReturn().getResponse().getContentAsString();
        assertThat(content).containsIgnoringCase("invalid value for property");
        assertThat(content).contains("inputParam1");
    }

    private HttpHeaders getHeaders(){
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

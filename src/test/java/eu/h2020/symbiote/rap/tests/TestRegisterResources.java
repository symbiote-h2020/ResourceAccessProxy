/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rap.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.managers.AuthorizationManager;
import eu.h2020.symbiote.managers.AuthorizationResult;
import eu.h2020.symbiote.managers.ServiceRequest;
import eu.h2020.symbiote.model.cim.FeatureOfInterest;
import eu.h2020.symbiote.model.cim.Location;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.plugin.PlatformSpecificPlugin;
import eu.h2020.symbiote.resources.RapDefinitions;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.service.RAPEdmController;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@org.springframework.boot.test.context.TestConfiguration 
@ActiveProfiles("test")
public class TestRegisterResources {
    @InjectMocks
    @Autowired
    RAPEdmController controller;
    
    @Value("${rap.enableSpecificPlugin}")
    private Boolean enableSpecificPlugin;
    
    private MockMvc mockMvc;
    
    @Autowired
    private AuthorizationManager authorizationManager;
    
    @Autowired
    private ResourcesRepository resourcesRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    
    private static final Logger log = LoggerFactory.getLogger(TestRegisterResources.class);
    
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = standaloneSetup(controller)
                //.setSingleView(mockView)
                .build();
        doReturn(new AuthorizationResult("Validated", true)).when(authorizationManager)
                .checkResourceUrlRequest(any(), any());
    }
    
    
    @Test
    public void TestRegisterStationarySensor(){
        Optional<DbResourceInfo> resInfoOptional = resourcesRepository.findById(GetStationarySensorId());
        assert(resInfoOptional.isPresent() == false);
        //StationarySensor
        String json = GetStationarySensor();
        byte[] b_json = json.getBytes(StandardCharsets.UTF_8);
        Object obj = rabbitTemplate.convertSendAndReceive(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN, 
                RapDefinitions.RESOURCE_REGISTRATION_KEY, b_json);
        assert(obj == null);
        
        resInfoOptional = resourcesRepository.findById(GetStationarySensorId());
        assert(resInfoOptional.isPresent() == true);
    }
    
    @Test
    public void TestUnregisterStationarySensor(){
        Optional<DbResourceInfo> resInfoOptional = resourcesRepository.findById(GetStationarySensorId());
        assert(resInfoOptional.isPresent() == true);
        
        String json = "";
        List<String> ids = Arrays.asList(GetStationarySensorInternalId());
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);      
        try {
            json = map.writeValueAsString(ids);
        } catch (JsonProcessingException ex) {
            java.util.logging.Logger.getLogger(TestRegisterResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] b_json = json.getBytes(StandardCharsets.UTF_8);
        Object obj = rabbitTemplate.convertSendAndReceive(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN, 
                RapDefinitions.RESOURCE_UNREGISTRATION_KEY, b_json);
        assert(obj == null);
        resInfoOptional = resourcesRepository.findById(GetStationarySensorId());
        assert(resInfoOptional.isPresent() == false);
        //log.info(obj.toString());
    }
    
    
    
    private String GetPluginId(){
        return PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
    }
    
    private String GetStationarySensorId(){
        return "isen1";
    }
    
    private String GetStationarySensorInternalId(){
        return "500";
    }
    
    private String GetStationarySensor(){
        String stationarySensorStr = "";
        String id = GetStationarySensorId();
        //String json = "[{\"internalId\":\"1600\",\"pluginId\":\""+GetPluginId()+"\",\"cloudMonitoringHost\":\"cloudMonitoringHostIP\",\"resource\":{\"@c\":\".StationarySensor\",\"id\":\""+id+"\",\"labels\":[\"lamp\"],\"comments\":[\"A comment\"],\"interworkingServiceURL\":\"https://symbiote-h2020.eu/example/interworkingService/\",\"locatedAt\":{\"@c\":\".WGS84Location\",\"longitude\":2.349014,\"latitude\":48.864716,\"altitude\":15.0,\"name\":[\"Paris\"],\"description\":[\"This is paris\"]},\"featureOfInterest\":{\"labels\":[\"Room1\"],\"comments\":[\"This is room 1\"],\"hasProperty\":[\"temperature\"]},\"observesProperty\":[\"temperature\",\"humidity\"]},\"params\":{\"type\":\"Type of device, used in monitoring\"}}]";
        String json = "[{\"internalId\":\"1600\",\"pluginId\":\""+GetPluginId()+"\",\"resource\":"
                + "{\"@c\":\".StationarySensor\",\"id\":\""+id+"\",\"labels\":[\"lamp\"],\"comments\":[\"A comment\"],"
                + "\"interworkingServiceURL\":\"https://symbiote-h2020.eu/example/interworkingService/\","
                + "\"locatedAt\":{\"@c\":\".WGS84Location\",\"longitude\":2.349014,\"latitude\":48.864716,\"altitude\":15.0,\"name\":[\"Paris\"],\"description\":[\"This is paris\"]},"
                + "\"featureOfInterest\":{\"labels\":[\"Room1\"],\"comments\":[\"This is room 1\"],\"hasProperty\":[\"temperature\"]},"
                + "\"observesProperty\":[\"temperature\",\"humidity\"]},\"params\":{\"type\":\"Type of device, used in monitoring\"}}]";
        

        List<String> descriptions = Arrays.asList("A comment");
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        String interworkingServiceURL = "https://symbiote-h2020.eu/example/interworkingService/";
        Location location = new WGS84Location(2.349014, 48.864716, 15.0, "Paris", descriptions);
        String name = "name";
        List<String> observesProperties = new ArrayList<>();
        
        StationarySensor stationarySensor = new StationarySensor();
        
        stationarySensor.setDescription(descriptions);
        stationarySensor.setFeatureOfInterest(featureOfInterest);
        stationarySensor.setId(id);
        stationarySensor.setInterworkingServiceURL(interworkingServiceURL);
        stationarySensor.setLocatedAt(location);
        stationarySensor.setName(name);
        stationarySensor.setObservesProperty(observesProperties);
        
        String internalId = GetStationarySensorInternalId();
        CloudResource cloudResource = GetCloudResource(internalId, stationarySensor);
        List<CloudResource> cloudResourceList = Arrays.asList(cloudResource);
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);      
        try {
            stationarySensorStr = map.writeValueAsString(cloudResourceList);
        } catch (JsonProcessingException ex) {
            java.util.logging.Logger.getLogger(TestRegisterResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        //stationarySensorStr = json;
        return stationarySensorStr;
    }
    
    
    private CloudResource GetCloudResource(String internalId, Resource resource){
        CloudResource cloudResource = new CloudResource();
        SingleTokenAccessPolicySpecifier accessPolicySpecifier = null;
        try {
            accessPolicySpecifier = new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, new HashMap<>());
        } catch (InvalidArgumentsException ex) {
            java.util.logging.Logger.getLogger(TestRegisterResources.class.getName()).log(Level.SEVERE, null, ex);
        }
        cloudResource.setAccessPolicy(accessPolicySpecifier);
        cloudResource.setFilteringPolicy(accessPolicySpecifier);
        cloudResource.setInternalId(internalId);
        cloudResource.setPluginId(GetPluginId());
        cloudResource.setResource(resource);
        return cloudResource;
    }
}

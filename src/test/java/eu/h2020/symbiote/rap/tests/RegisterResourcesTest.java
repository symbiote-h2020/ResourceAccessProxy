/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rap.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.bim.OwlapiHelp;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.interfaces.ResourceRegistration;
import eu.h2020.symbiote.model.cim.Builders;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.plugin.PlatformSpecificPlugin;
import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@RunWith(SpringRunner.class)
@OverrideAutoConfiguration(enabled = false)
@Import({ResourceRegistration.class})
@ImportAutoConfiguration

@ActiveProfiles("test")
public class RegisterResourcesTest {

    @Autowired
    private ResourceRegistration resourceRegistration;
    
    @MockBean
    private ResourcesRepository resourcesRepository;
    
    @MockBean
    AccessPolicyRepository accessPolicyRepository;

    @MockBean
    OwlapiHelp owlApiHelp;
      
    @Test
    public void testRegisterStationarySensor() throws Exception {
        // given
        CloudResource stationarySensor = getStationarySensor();
        String json = serialize(asList(stationarySensor));
        byte[] b_json = json.getBytes(StandardCharsets.UTF_8);
        
        // when
        resourceRegistration.receiveL1RegistrationMessage(b_json);
        
        // then
            // resourcesRepository
        ArgumentCaptor<DbResourceInfo> resourceCaptor = ArgumentCaptor.forClass(DbResourceInfo.class);
        verify(resourcesRepository).save(resourceCaptor.capture());
        
        DbResourceInfo actualResource = resourceCaptor.getValue();
        assertThat(actualResource.getInternalId()).isEqualTo(stationarySensor.getInternalId());
        assertThat(actualResource.getPluginId()).isEqualTo(stationarySensor.getPluginId());
        assertThat(serialize(actualResource.getResource())).isEqualTo(serialize(stationarySensor.getResource()));

            // accessPolicyRepository
        ArgumentCaptor<AccessPolicy> accessPolicyCaptor = ArgumentCaptor.forClass(AccessPolicy.class);
        verify(accessPolicyRepository).save(accessPolicyCaptor.capture());
        
        AccessPolicy accessPolicy = accessPolicyCaptor.getValue();
        assertThat(accessPolicy.getInternalId()).isEqualTo(stationarySensor.getInternalId());
        assertThat(accessPolicy.getResourceId()).isEqualTo(stationarySensor.getResource().getId());
        assertThat(accessPolicy.getPolicy()).isInstanceOf(SingleTokenAccessPolicy.class);
        
            // owlApiHelp.addCloudResourceList(cloudResourceList);
        ArgumentCaptor<List<CloudResource>> cloudResourceCaptor =  ArgumentCaptor.forClass(null);
        verify(owlApiHelp).addCloudResourceList(cloudResourceCaptor.capture());
        
        CloudResource actualCloudResource = cloudResourceCaptor.getValue().get(0);
        assertThat(serialize(actualCloudResource)).isEqualTo(serialize(stationarySensor));
    }
    
    @Test
    public void testUnregisterStationarySensor() throws Exception {
        // given
        DbResourceInfo dbResource = new DbResourceInfo(getStationarySensorSymbioteId(), getStationarySensorInternalId());
        when(resourcesRepository.findByInternalId(getStationarySensorInternalId())).thenReturn(Arrays.asList(dbResource));

        AccessPolicy policy = new AccessPolicy(getStationarySensorSymbioteId(), getStationarySensorInternalId(), null);
        when(accessPolicyRepository.findByInternalId(getStationarySensorInternalId())).thenReturn(Optional.of(policy));
        
        String json = serialize(asList(getStationarySensorInternalId()));
        byte[] b_json = json.getBytes(StandardCharsets.UTF_8);

        // when
        resourceRegistration.receiveL1UnregistrationMessage(b_json);
        
        // then
        verify(accessPolicyRepository).delete(getStationarySensorSymbioteId());
        verify(resourcesRepository).delete(getStationarySensorSymbioteId());
    }
    
    private String getPluginId(){
        return PlatformSpecificPlugin.PLUGIN_PLATFORM_ID;
    }
    
    private String getStationarySensorSymbioteId(){
        return "sybmioteId-1";
    }
    
    private String getStationarySensorInternalId(){
        return "internalId-1";
    }
    
    private CloudResource getStationarySensor() throws InvalidArgumentsException, JsonProcessingException{
        StationarySensor stationarySensor = Builders.stationarySensorBuilder()
            .description(asList("A comment"))
            .featureOfInterest(Builders.featureOfInterestBuilder().build())
            .id(getStationarySensorSymbioteId())
            .interworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/")
            .locatedAt(new WGS84Location(2.349014, 48.864716, 15.0, "Paris", asList("A comment")))
            .name("name")
            .observesProperty(Collections.emptyList())
            .build();
        
        CloudResource cloudResource = getCloudResource(getStationarySensorInternalId(), stationarySensor);
        return cloudResource;
    }


    private String serialize(Object obj) throws JsonProcessingException {
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);      
        return map.writeValueAsString(obj);
    }
    
    
    private CloudResource getCloudResource(String internalId, Resource resource) throws InvalidArgumentsException{
        CloudResource cloudResource = Builders.cloudResourceBuilder()
                .accessPolicySpecifier(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, new HashMap<>()))
                .filteringPolicySpecifier(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, new HashMap<>()))
                .internalId(internalId)
                .pluginId(getPluginId())
                .resource(resource)
                .build();
        return cloudResource;
    }
}

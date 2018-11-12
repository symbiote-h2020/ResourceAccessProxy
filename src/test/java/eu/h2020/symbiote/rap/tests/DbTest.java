package eu.h2020.symbiote.rap.tests;

import eu.h2020.symbiote.resources.db.AccessPolicy;
import eu.h2020.symbiote.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.resources.db.ParameterInfo;
import eu.h2020.symbiote.resources.db.PlatformInfo;
import eu.h2020.symbiote.resources.db.PluginRepository;
import eu.h2020.symbiote.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.resources.db.RegistrationInfoODataRepository;
import eu.h2020.symbiote.resources.db.DbResourceInfo;
import eu.h2020.symbiote.resources.db.ResourcesRepository;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.SingleTokenAccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Luca Tomaselli
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestConfiguration
@DataMongoTest
@ActiveProfiles("test")
public class DbTest {
    @Autowired
    private ResourcesRepository resourcesRepository;
    
    @Autowired
    private PluginRepository pluginRepository;
        
    @Autowired
    private AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    private RegistrationInfoODataRepository registrationInfoODataRepository;
    
    
    @Test
    public void testResourceInfo() throws Exception{
        //insert
        String resourceId = "1";
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "plugin_1";
        DbResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
        assert(resourceInfoResult != null);
        //search
        Optional<DbResourceInfo> resInfoOptional = resourcesRepository.findById(resourceId);
        assert(resInfoOptional.isPresent() == true);
        resInfoOptional = resourcesRepository.findById(resourceId+"2");
        assert(resInfoOptional.isPresent() == false);        
        List<DbResourceInfo> resourceInfoList = resourcesRepository.findByInternalId(platformResourceId+"2");
        assert(resourceInfoList == null || resourceInfoList.isEmpty());
        resourceInfoList = resourcesRepository.findByInternalId(platformResourceId);
        assert(resourceInfoList != null);
        assert(!resourceInfoList.isEmpty());
        //delete
        resourcesRepository.delete(resourceInfoList.get(0).getSymbioteId());
        resourceInfoList = resourcesRepository.findByInternalId(platformResourceId);
        assert(resourceInfoList == null || resourceInfoList.isEmpty());
    }
    
    private DbResourceInfo addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId) {
        DbResourceInfo resourceInfo = new DbResourceInfo(resourceId, platformResourceId);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        if(pluginId != null && pluginId.length()>0)
            resourceInfo.setPluginId(pluginId);
        
        DbResourceInfo resourceInfoResult = resourcesRepository.save(resourceInfo);
        return resourceInfoResult;
    }
    

    @Test
    public void testPlatformInfo() throws Exception{
        //insert
        String platformId = "plugin_1";
        boolean hasFilters = false;
        boolean hasNotifications = false;
        PlatformInfo platformInfo = addPlugin(platformId, hasFilters, hasNotifications);
        assert(platformInfo != null);
        //search
        Optional<PlatformInfo> platformInfoOptional = pluginRepository.findById(platformId);
        assert(platformInfoOptional.isPresent());
        platformInfoOptional = pluginRepository.findById(platformId+"2");
        assert(!platformInfoOptional.isPresent());
        //delete
        pluginRepository.delete(platformId);
        platformInfoOptional = pluginRepository.findById(platformId+"2");
        assert(!platformInfoOptional.isPresent());
    }
    
    private PlatformInfo addPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        PlatformInfo platformInfo = new PlatformInfo(platformId, hasFilters, hasNotifications);
        PlatformInfo platformInfoResponse = pluginRepository.save(platformInfo);
        return platformInfoResponse;
    }
    
    @Test 
    public void testAccessPolicy() throws Exception{
        //insert resource
        String resourceId = "1";
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "plugin_1";
        DbResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
        assert(resourceInfoResult != null);
        //insert
        AccessPolicyType policyType = AccessPolicyType.PUBLIC;
        Map<String, String> requiredClaims = new HashMap<>();
        SingleTokenAccessPolicySpecifier accPolicy = new SingleTokenAccessPolicySpecifier(policyType,requiredClaims);
        AccessPolicy accessPolicy = addPolicy(resourceId,platformResourceId,accPolicy);
        assert(accessPolicy != null);
        //search
        Optional<AccessPolicy> accessPolicyOptional = accessPolicyRepository.findById(resourceId);
        assert(accessPolicyOptional.isPresent());
        accessPolicyOptional = accessPolicyRepository.findByInternalId(platformResourceId);
        assert(accessPolicyOptional.isPresent());
        accessPolicyOptional = accessPolicyRepository.findById(platformResourceId);
        assert(!accessPolicyOptional.isPresent());
        accessPolicyOptional = accessPolicyRepository.findByInternalId(resourceId);
        assert(!accessPolicyOptional.isPresent());
        //delete
        accessPolicyRepository.delete(resourceId);
        accessPolicyOptional = accessPolicyRepository.findById(resourceId);
        assert(!accessPolicyOptional.isPresent());
        //delete resource
        //delete
        resourcesRepository.delete(resourceId);
        Optional<DbResourceInfo> resourceInfoOptional = resourcesRepository.findById(resourceId);
        assert(!resourceInfoOptional.isPresent());
    }
    
    private AccessPolicy addPolicy(String resourceId, String internalId, SingleTokenAccessPolicySpecifier accPolicy) throws InvalidArgumentsException {
        IAccessPolicy policy = SingleTokenAccessPolicyFactory.getSingleTokenAccessPolicy(accPolicy);
        AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy);
        AccessPolicy apNew = accessPolicyRepository.save(ap);
        return apNew;
    } 
    
    @Test
    public void testRegistrationInfoOData() throws Exception{
        //insert
        String symbioteId = "1";
        String className = "className";
        String superClass = "superClass";
        Set<ParameterInfo> parameters = new HashSet<ParameterInfo>();
        ParameterInfo parameterInfo = new ParameterInfo("type","name");
        parameters.add(parameterInfo);
        RegistrationInfoOData rio = new RegistrationInfoOData(symbioteId, className, superClass, parameters);
        registrationInfoODataRepository.insertNew(rio);
        //search
        List<RegistrationInfoOData> registrationInfoOdataList = registrationInfoODataRepository.findByClassName(className);
        assert(registrationInfoOdataList != null);
        assert(!registrationInfoOdataList.isEmpty());
        registrationInfoOdataList = registrationInfoODataRepository.findByClassName(className+"2");
        assert(registrationInfoOdataList == null || registrationInfoOdataList.isEmpty());
        //delete
        registrationInfoODataRepository.delete(rio.getId());
        registrationInfoOdataList = registrationInfoODataRepository.findByClassName(className);
        assert(registrationInfoOdataList == null || registrationInfoOdataList.isEmpty());
    }
}

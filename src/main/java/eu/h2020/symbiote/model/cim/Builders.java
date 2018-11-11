package eu.h2020.symbiote.model.cim;

import java.util.List;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederationInfoBean;
import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.core.internal.RDFFormat;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;

import lombok.Builder;

public class Builders {

    @Builder(builderMethodName = "stationarySensorBuilder")
    public static StationarySensor newStationarySensor(String id, String name, List<String> description, Location locatedAt, FeatureOfInterest featureOfInterest, String interworkingServiceURL, List<String> observesProperty, List<Service> services) {
        StationarySensor sensor = new StationarySensor();
        sensor.setDescription(description);
        sensor.setFeatureOfInterest(featureOfInterest);
        sensor.setId(id);
        sensor.setInterworkingServiceURL(interworkingServiceURL);
        sensor.setLocatedAt(locatedAt);
        sensor.setName(name);
        sensor.setObservesProperty(observesProperty);
        sensor.setServices(services);
        return sensor;
    }
    
    @Builder(builderMethodName = "mobileSensorBuilder")
    public static MobileSensor newMobileSensor(List<String> description, String id, String interworkingServiceURL, Location locatedAt, String name, List<String> observesProperty, List<Service> services) {
        MobileSensor m = new MobileSensor();
        m.setDescription(description);
        m.setId(id);
        m.setInterworkingServiceURL(interworkingServiceURL);
        m.setLocatedAt(locatedAt);
        m.setName(name);
        m.setObservesProperty(observesProperty);
        m.setServices(services);
        return m;
    }
 
    @Builder(builderMethodName = "actuatorBuilder")
    public static Actuator newActuator(String id, String name, List<String> description, Location locatedAt, String interworkingServiceURL, List<Capability> capabilities, List<Service> services) {
        Actuator a = new Actuator();
        a.setCapabilities(capabilities);
        a.setDescription(description);
        a.setId(id);
        a.setInterworkingServiceURL(interworkingServiceURL);
        a.setLocatedAt(locatedAt);
        a.setName(name);
        a.setServices(services);
        return a;
    }
    
    @Builder(builderMethodName = "coreResourceBuilder")
    public static CoreResource newCoreResource(String id, String name, List<String> description, String interworkingServiceURL, IAccessPolicySpecifier policySpecifier, String rdf, RDFFormat rdfFormat, CoreResourceType type) {
        CoreResource c = new CoreResource();
        c.setDescription(description);
        c.setId(id);
        c.setInterworkingServiceURL(interworkingServiceURL);
        c.setName(name);
        c.setPolicySpecifier(policySpecifier);
        c.setRdf(rdf);
        c.setRdfFormat(rdfFormat);
        c.setType(type);
        return c;
    }
    
    @Builder(builderMethodName = "cloudResourceBuilder")
    public static CloudResource newCloudResource(String internalId, String pluginId, IAccessPolicySpecifier accessPolicySpecifier, IAccessPolicySpecifier filteringPolicySpecifier, FederationInfoBean federationInfo, Resource resource) {
        CloudResource r = new CloudResource();
        r.setAccessPolicy(accessPolicySpecifier);
        r.setFederationInfo(federationInfo);
        r.setFilteringPolicy(filteringPolicySpecifier);
        r.setInternalId(internalId);
        r.setPluginId(pluginId);
        r.setResource(resource);
        return r;
    }
    
    @Builder(builderMethodName = "capabilityBuilder")
    public static Capability newCapability(String name, List<Parameter> parameters, List<Effect> effects) {
        Capability c = new Capability();
        c.setName(name);
        c.setParameters(parameters);
        c.setEffects(effects);
        return c;
    }
    
    public static Effect newEffect(FeatureOfInterest actsOn, List<String> affects) {
        Effect e = new Effect();
        e.setActsOn(actsOn);
        e.setAffects(affects);
        return e;
    }
    
    @Builder(builderMethodName = "wgs84LocationBuilder")
    public static WGS84Location newWGS84Location(double longitude, double latitude, double altitude, String name, List<String> description) {
       return new WGS84Location(longitude, latitude, altitude, name, description);
    }
    
    @Builder(builderMethodName = "wktLocationBuilder")
    public static WKTLocation newWKTLocation(String name, String value, List<String> description) {
        WKTLocation l = new WKTLocation();
        l.setName(name);
        l.setValue(value);
        l.setDescription(description);
        return l;
    }
    
    @Builder(builderMethodName = "symbolicLocationBuilder")
    public static SymbolicLocation newSymbolicLocation(String name, List<String> description) {
        SymbolicLocation l = new SymbolicLocation();
        l.setName(name);
        l.setDescription(description);
        return l;
    }
    
    @Builder(builderMethodName = "featureOfInterestBuilder")
    public static FeatureOfInterest newFeatureOfInterest(String name, List<String> hasProperty, List<String> description) {
        FeatureOfInterest featureOfInterest = new FeatureOfInterest();
        featureOfInterest.setName(name);
        featureOfInterest.setDescription(description);
        featureOfInterest.setHasProperty(hasProperty);
        return featureOfInterest;
    }
    
    @Builder(builderMethodName = "serviceBuilder")
    public static Service newService(String id, String name, List<String> description, String interworkingServiceURL, List<Parameter> parameters, Datatype resultType) {
        Service service = new Service();
        service.setDescription(description);
        service.setId(id);
        service.setInterworkingServiceURL(interworkingServiceURL);
        service.setName(name);
        service.setParameters(parameters);
        service.setResultType(resultType);
        return service;
    }
    
    @Builder(builderMethodName = "serviceBuilder")
    public static Parameter newParameter(String name, Datatype datatype, boolean mandatory, List<Restriction> restrictions) {
        Parameter paremeter = new Parameter();
        paremeter.setDatatype(datatype);
        paremeter.setMandatory(mandatory);
        paremeter.setName(name);
        paremeter.setRestrictions(restrictions);
        return paremeter;
    }
    
    @Builder(builderMethodName = "primitiveDatatypeBuilder")
    public static PrimitiveDatatype newPrimitiveDatatype(String baseDatatype, boolean array) {
        PrimitiveDatatype primitiveDatatype = new PrimitiveDatatype();
        primitiveDatatype.setBaseDatatype(baseDatatype);
        primitiveDatatype.setArray(array);
        return primitiveDatatype;
    }
    
    @Builder(builderMethodName = "complexDatatypeBuilder")
    public static ComplexDatatype newComplexDatatype(String basedOnClass, boolean array, List<DataProperty> dataProperties) {
        ComplexDatatype complexDatatype = new ComplexDatatype();
        complexDatatype.setBasedOnClass(basedOnClass);
        complexDatatype.setArray(array);
        complexDatatype.setDataProperties(dataProperties);
        return complexDatatype;
    }
    
    public static DataProperty newDataProperty(String name) {
        DataProperty dataProperty = new DataProperty();
        dataProperty.setName(name);
        return dataProperty;
    }
    
    public static EnumRestriction newEnumRestriction(List<String> values) {
        EnumRestriction e = new EnumRestriction();
        e.setValues(values);
        return e;
    }

    public static LengthRestriction newLengthRestriction(Integer min, Integer max) {
        LengthRestriction l = new LengthRestriction();
        l.setMin(min);
        l.setMax(max);
        return l;
    }
    
    public static RangeRestriction newRangeRestriction(Double min, Double max) {
        RangeRestriction r = new RangeRestriction();
        r.setMin(min);
        r.setMax(max);
        return r;
    }
    
    public static RegExRestriction newRegExRestriction(String pattern) {
        RegExRestriction r = new RegExRestriction();
        r.setPattern(pattern);
        return r;
    }
    
    public static InstanceOfRestriction newInstanceOfRestriction(String instanceOfClass, String valueProperty) {
        InstanceOfRestriction i = new InstanceOfRestriction();
        i.setInstanceOfClass(instanceOfClass);
        i.setValueProperty(valueProperty);
        return i;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.model.data;

/**
 *
 * @author Aleksandar Antonic <aleksandar.antonic@fer.hr>
 */
public class Observation {
    
    private String resourceId;
    private Location location;
    private long resultTime;
    private long samplingTime;
    private ObservationValue obsValue;    

    public Observation(String resourceId, Location location, long resultTime, long samplingTime, ObservationValue obsValue) {
        this.resourceId = resourceId;
        this.location = location;
        this.resultTime = resultTime;
        this.samplingTime = samplingTime;
        this.obsValue = obsValue;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Location getLocation() {
        return location;
    }

    public long getResultTime() {
        return resultTime;
    }

    public long getSamplingTime() {
        return samplingTime;
    }

    public ObservationValue getObsValue() {
        return obsValue;
    }
    
    
    public static void main (String[] args) {
        String sensorId = "symbIoTeID1";
        WGS84Location loc = new WGS84Location(15.9, 45.8, 145, "Spansko", "City of Zagreb");
        long timestamp = System.currentTimeMillis();
        ObservationValue obsval = new ObservationValue((double)7, new Property("Temperature", "Air temperature"), new UnitOfMeasurement("C", "degree Celsius", ""));
        Observation o = new Observation(sensorId, loc, timestamp, timestamp-1000 , obsval);
        
        System.out.println(o);
    }
}

package eu.h2020.symbiote.rap.tests;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.semanticweb.owlapi.model.IRI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.bim.OwlapiHelp;
import eu.h2020.symbiote.service.CustomField;

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
@Import({OwlapiHelp.class})
@ActiveProfiles({"test", "pim"})
public class ReadPimTest {
    
    @Autowired
    private OwlapiHelp owlApiHelp;
    
    @Test
    public void readPimFromClasspath() {
        //map
        HashMap<String,HashMap<String,String>> owlMap = owlApiHelp.getMap();
        assertThat(owlMap).isNotNull();
        assertThat(owlMap).isNotEmpty();
        String entityName = "Actuator";
        Optional<String> classLongName = owlMap.keySet().stream().filter(str -> (getShortClassName(str)).equalsIgnoreCase(entityName)).findFirst();
        assertThat(classLongName.isPresent()).isTrue();
        HashMap<String,String> mapOfEntityName = owlMap.get(classLongName.get());
        assertThat(mapOfEntityName).isNotNull();
        assertThat(mapOfEntityName).isNotEmpty();
        String superclass = "Superclass";
        assertThat(mapOfEntityName).containsKey(superclass);
        String entityNameSuperClass = "Device";
        String entityNameSuperClassLongName = mapOfEntityName.get(superclass);
        assertThat(getShortClassName(entityNameSuperClassLongName)).isEqualTo(entityNameSuperClass);
        //classes
        HashMap<String,HashMap<String,String>> owlClasses = owlApiHelp.getClasses();
        assertThat(owlClasses).isNotNull();
        assertThat(owlClasses).isNotEmpty();
        HashMap<String,String> classOfActuator = owlClasses.get(classLongName.get());
        assertThat(classOfActuator).isNotNull();
        assertThat(classOfActuator).isNotEmpty();
        HashMap<String,String> classOfDevice = owlClasses.get(entityNameSuperClassLongName);
        assertThat(classOfDevice).isNotNull();
        assertThat(classOfDevice).isNotEmpty();
        assertThat(classOfDevice).containsKey("id");
        for(String key: classOfDevice.keySet()){
            assertThat(classOfActuator).containsKey(key);
            assertThat(classOfActuator.get(key)).isEqualTo(classOfDevice.get(key));
        }
        
        assertEquals("./pim_cwd.owl", owlApiHelp.getLoadedOntology());
    }
    
    private String getShortClassName(String type) {
        String simpleName = type.replace("[]", "");
        if (!CustomField.typeIsPrimitive(simpleName)) {
            IRI iri = IRI.create(simpleName);
            simpleName = iri.getShortForm();
            if (simpleName.contains("#")) {
                String[] array = simpleName.split("#");
                simpleName = array[array.length - 1];
            }
        }
        return simpleName;
    }
}

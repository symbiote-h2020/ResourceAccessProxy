/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.bim;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class TripleStore {

    private final static boolean SHOULD_PRINT_DATASET = false;

    private final String currentDirectory;
    private final Dataset dataset;
    private static final String BASE_REPO = "base";
    private static final String SPATIAL_REPO = "spatial";

    private static final String CIM_FILE = "/core-v0.6.owl";
    private static final String BIM_FILE = "/bim-0.3.owl";
    private static final String MIM_FILE = "/meta-v0.3.owl";
    private static final String QU_FILE = "/qu-rec20.owl";

    private static final Log log = LogFactory.getLog(TripleStore.class);

    public HashMap<String,HashMap<String,ArrayList<String>>> map;
    //For tests only - in memory
    public TripleStore() {
        currentDirectory = null;

        Dataset baseDataset = DatasetFactory.create();

        dataset = null;
        //dataset = SpatialDatasetFactory.createLucene(baseDataset, ramDir, entDef);
        try {
            
            String cim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(CIM_FILE));
            
            map = insertGraph("", cim_data, RDFFormat.Turtle);
            
            /*
            String bim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(BIM_FILE));
            map = insertGraph("", bim_data, RDFFormat.Turtle);
            
            
            String mim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(MIM_FILE));
            insertGraph("", mim_data, RDFFormat.Turtle);

            String qureq20_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(QU_FILE));
            insertGraph("", qureq20_data, RDFFormat.RDFXML);
            */

        } catch (Exception e) {
            log.fatal("Could not load CIM file: " + e.getMessage(), e);
        }
    }
    
    public TripleStore(String nullValue) {
        currentDirectory = null;

        Dataset baseDataset = DatasetFactory.create();

        dataset = null;
        //dataset = SpatialDatasetFactory.createLucene(baseDataset, ramDir, entDef);
        try {
            
            String cim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(CIM_FILE));
            
            map = insertGraph2("", cim_data, RDFFormat.Turtle);
            
            /*
            String bim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(BIM_FILE));
            map = insertGraph2("", bim_data, RDFFormat.Turtle);
            */
        } catch (Exception e) {
            log.fatal("Could not load CIM file: " + e.getMessage(), e);
        }
    }



    public HashMap<String,HashMap<String,ArrayList<String>>> insertGraph(String uri, String rdf, RDFFormat format) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(rdf.getBytes()), null, format.toString());
        
        /*ArrayList<String> objectsList = new ArrayList<String>();
        NodeIterator ni = model.listObjects();
        while(ni.hasNext()){
            RDFNode rdfnode = ni.next();
            String rdfNodeString = rdfnode.toString();
            objectsList.add(rdfNodeString);
        }*/
        
        HashMap<String,HashMap<String,ArrayList<String>>> map = new HashMap<String,HashMap<String,ArrayList<String>>>();
        ArrayList<String> subjectsList = new ArrayList<String>();
        ResIterator ri = model.listSubjects();
        while(ri.hasNext()){
            Resource res = ri.next();
            String resLocalName = res.getLocalName();
            String resString;
            if(resLocalName != null)
                resString = resLocalName;
            else
                resString = res.toString();
            
            String breckpoint ="";
            if(resString.equals("microgramDayPerCubicMetre"))
                breckpoint = "YES";
            
            
            HashMap<String,ArrayList<String>> key2values = new HashMap<String,ArrayList<String>>();
            Model resModel = res.getModel();
            StmtIterator pi = res.listProperties();
            while(pi.hasNext()){
                Statement statement = pi.next();
                try{
                Triple tiple = statement.asTriple();
                
                Node predicate = tiple.getPredicate();
                String predicateString = predicate.toString();
                //Node_URI
                try{
                    if(predicate.getLocalName() != null)
                        predicateString = predicate.getLocalName();
                }catch(Exception e){
                    log.debug(e);
                }
                
                Node object = tiple.getObject();
                String objectString = object.toString();
                if(object.getClass().equals(Node_Literal.class)){
                    Node_Literal objectLiteral = (Node_Literal) object;
                    objectString = objectLiteral.getLiteralLexicalForm() +" ("+
                            objectLiteral.getLiteralDatatype().toString() + ")";
                }
                else{
                    try{
                        if(object.getLocalName() != null)
                            objectString = object.getLocalName();
                    }catch(Exception e){
                        log.info(e);
                    }
                }
                ArrayList<String> valueOfKey = new ArrayList<String>();
                if(key2values.containsKey(predicateString))
                    valueOfKey = key2values.get(predicateString);
                valueOfKey.add(objectString);
                key2values.put(predicateString, valueOfKey);
                }catch(Exception e){
                    log.info(e);
                };
            }
            map.put(resString, key2values);
            subjectsList.add(resString);
        }
        
        String a = "A";
        //insertGraph(uri, model);
        
        //model.
        return map;
    }

    
    public HashMap<String,HashMap<String,ArrayList<String>>> insertGraph2(String uri, String rdf, RDFFormat format) {
        HashMap<String,HashMap<String,ArrayList<String>>> classes = new HashMap<String,HashMap<String,ArrayList<String>>>();
        
        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(rdf.getBytes()), null, format.toString());
        
        
        HashMap<String,HashMap<String,ArrayList<String>>> map = new HashMap<String,HashMap<String,ArrayList<String>>>();
        ArrayList<String> subjectsList = new ArrayList<String>();
        ResIterator ri = model.listSubjects();
        while(ri.hasNext()){
            Resource res = ri.next();
            String resLocalName = res.getLocalName();
            String resString;
            if(resLocalName != null)
                resString = resLocalName;
            else
                resString = res.toString();
            
            String breckpoint ="";
            if(resString.equals("WGS84Location"))
                breckpoint = "YES";
            
            
            HashMap<String,ArrayList<String>> key2values = new HashMap<String,ArrayList<String>>();
            Model resModel = res.getModel();
            StmtIterator pi = res.listProperties();
            while(pi.hasNext()){
                Statement statement = pi.next();
                Triple tiple = statement.asTriple();
                
                Node predicate = tiple.getPredicate();
                String predicateString = predicate.toString();
                //Node_URI
                try{
                    if(predicate.getLocalName() != null)
                        predicateString = predicate.getLocalName();
                }catch(Exception e){
                    log.debug(e);
                }
                
                Node object = tiple.getObject();
                String objectString = object.toString();
                if(object.getClass().equals(Node_Literal.class)){
                    Node_Literal objectLiteral = (Node_Literal) object;
                    objectString = objectLiteral.getLiteralLexicalForm() +" ("+
                            objectLiteral.getLiteralDatatype().toString() + ")";
                }
                else{
                    try{
                        if(object.getLocalName() != null)
                            objectString = object.getLocalName();
                    }catch(Exception e){
                        log.info(e);
                    }
                }
                ArrayList<String> valueOfKey = new ArrayList<String>();
                if(key2values.containsKey(predicateString))
                    valueOfKey = key2values.get(predicateString);
                valueOfKey.add(objectString);
                key2values.put(predicateString, valueOfKey);
            }
            map.put(resString, key2values);
            subjectsList.add(resString);
        }
        
        
        //model.
        for (String key : map.keySet()) {
            HashMap<String,ArrayList<String>> value = map.get(key);
            if(value.containsKey("type")){
                String type = value.get("type").get(0);
                if(type.equals("FunctionalProperty") || type.equals("InverseFunctionalProperty") || 
                        type.equals("DatatypeProperty") || type.equals("ObjectProperty")){
                    if(value.containsKey("domain")){
                        String domain = value.get("domain").get(0);
                        HashMap<String,ArrayList<String>> campiclasse = new HashMap<String,ArrayList<String>>();
                        if(classes.containsKey(domain))
                            campiclasse = classes.get(domain);
                        else
                            classes.put(domain, campiclasse);

                        if(value.containsKey("range")){
                            String range = value.get("range").get(0);
                            if(value.get("type").contains("ObjectProperty") &&
                                    !(value.get("type").contains("FunctionalProperty") ||
                                    value.get("type").contains("InverseFunctionalProperty")))
                                range = range+"[]";
                            ArrayList<String> valore = new ArrayList<String>();
                            valore.add(key);
                            campiclasse.put(range, valore);
                        }
                    }
                    else{
                        if(value.containsKey("range")){
                            String range = value.get("range").get(0);
                            if(type.equals("ObjectProperty"))
                                range = range+"[]";
                            HashMap<String,ArrayList<String>> campiclasse = new HashMap<String,ArrayList<String>>();
                            campiclasse.put(key, null);
                            classes.put(range, campiclasse);
                        }
                    }
                }
                else if(type.equals("Class")){
                    HashMap<String,ArrayList<String>> campiclasse = new HashMap<String,ArrayList<String>>();
                    if(classes.containsKey(key))
                        campiclasse = classes.get(key);
                    else
                        classes.put(key, campiclasse);

                    if(value.containsKey("subClassOf")){
                        ArrayList<String> subClassOf = value.get("subClassOf");
                        campiclasse.put("subClassOf", subClassOf);
                    }
                }
            }
        }
        
        return classes;
    }
    
}

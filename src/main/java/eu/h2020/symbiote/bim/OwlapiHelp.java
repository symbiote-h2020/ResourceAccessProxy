/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.bim;

//import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import com.google.common.collect.Multimap;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.springframework.context.annotation.Configuration;
import uk.ac.manchester.cs.owl.owlapi.OWLDataExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectHasValueImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Configuration
public class OwlapiHelp {

    private static final Log log = LogFactory.getLog(OwlapiHelp.class);

    private static final String CIM_FILE = "/core-v1.0.owl";
    private static final String BIM_FILE = "/bim.owl";
    private static final String PIM_FILE = "/pim.owl";
    private static final String PIM_PARTIAL_FILE = "/pim_partial.owl";
    
    private HashMap<String, HashMap<String, String>> map;
    private HashMap<String, HashMap<String, String>> classes;
    private HashSet<String> classesStart = new HashSet<String>();
    private HashSet <String> classesRead = new HashSet<String>();
    private List<OWLOntologyID> allOntology;
    public String filePath;
    
    
    //@Bean
    public OwlapiHelp() throws Exception{
        URL url = OwlapiHelp.class.getResource(PIM_FILE);
        if(url == null)
            url = OwlapiHelp.class.getResource(BIM_FILE);
        if(url == null)
            throw new Exception("Not found file pim.owl or bim.owl");
        this.filePath = url.getPath();
        fromOwlToClasses();
    }
    
    
    public OwlapiHelp(Boolean test) throws Exception{
        URL url = OwlapiHelp.class.getResource(PIM_FILE);
            if(url == null)
                url = OwlapiHelp.class.getResource(BIM_FILE);
            if(url == null)
                throw new Exception("Not found file pim.owl or bim.owl");
            this.filePath = url.getPath();
        if(test)
            fromOwlToClassesTest();
        else 
            fromOwlToClasses();
    }
    
    
    public HashMap<String, HashMap<String, String>> getClasses(){
        return this.classes;
    }
    
    public HashMap<String, HashMap<String, String>> fromOwlToClassesTest(){        
        map = createMapClass2PropAndSuperclassTest();
        classes = new HashMap<String, HashMap<String, String>>();
        //this populate this.classes
        for(String key: map.keySet()){
            HashMap<String,String> attribute2type = fromOwlToClassesPrivate(key,map.get(key),map);
        }
        return classes;
    }
    
    public HashMap<String, HashMap<String, String>> fromOwlToClasses(){     
        map = createMapClass2PropAndSuperclass();
        classes = new HashMap<String, HashMap<String, String>>();
        //this populate this.classes
        for(String key: map.keySet()){
            HashMap<String,String> attribute2type = fromOwlToClassesPrivate(key,map.get(key),map);
        }
        return classes;
    }
    
    public HashSet<String> getSubClassesOfClass(String classStart){
        HashSet<String> subClasses = new HashSet<String>();
            for(String keyClass : map.keySet()){
                HashMap<String, String> mapClass = map.get(keyClass);
                String superClassArrayString = mapClass.get("Superclass");
                String[] superClassArray = superClassArrayString.split(",");
                for(String superClass : superClassArray)
                    if(classStart.equals(superClass)){
                        subClasses.add(keyClass);
                        subClasses.addAll(getSubClassesOfClass(keyClass));
                    }
            }
        return subClasses;
    }
    
    
    private HashMap<String,String> fromOwlToClassesPrivate(String className, HashMap<String, String> key2value, HashMap<String, HashMap<String, String>> map){
        HashMap<String,String> attribute2type = new HashMap<String,String>();
        try{
        for(String key: key2value.keySet()){
            //prendere attributi delle superclass
            String value = key2value.get(key);
            if(key.equals("Superclass")){
                String[] superClassArray = value.split(",");
                for(String superClass: superClassArray){
                    if(!superClass.equals(className) && map.containsKey(superClass)){
                        HashMap<String,String> attribute2typeNew;
                        if(classes.containsKey(superClass))
                            attribute2typeNew = classes.get(superClass);
                        else
                            attribute2typeNew = fromOwlToClassesPrivate(superClass,map.get(superClass),map);
                        
                        attribute2type.putAll(attribute2typeNew);
                    }
                }
            }
            //aggiungere attributi e tipo ma rimuovere Specifiche OWL non utilizzate (es. DISJOINTCLASSES)
            else if(! StringUtils.isAllUpperCase(key)){
                attribute2type.put(key, value);
            }
        }
        }catch(Exception e){
            System.err.println(e);
        }
        classes.put(className, attribute2type);
        return attribute2type;
    }
    
    private OWLOntology addOntologyImport(OWLOntology ontology){
        Stream<OWLOntology> owlOntologyStream = ontology.imports();
        Iterator<OWLOntology> owlOntologyiterator = owlOntologyStream.iterator();
        while(owlOntologyiterator.hasNext()){
            OWLOntology ontologyImport = owlOntologyiterator.next();
            OWLOntologyID ontologyId = ontologyImport.getOntologyID();
            if(!allOntology.contains(ontologyId)){
                log.info("Add ontology: "+ontologyId);
                Stream<OWLAxiom> axiomStream = ontologyImport.axioms();
                Iterator<OWLAxiom> axiomIterator = axiomStream.iterator();
                while (axiomIterator.hasNext()) {                        
                    OWLAxiom axiom = axiomIterator.next();
                    ontology.addAxiom(axiom);
                }
                allOntology.add(ontologyImport.getOntologyID());
                addOntologyImport(ontologyImport);
            }
        }
        return ontology;
    }

    
    private HashMap<String, HashMap<String, String>> createMapClass2PropAndSuperclass() {
        HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
        File file = new File(filePath);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        allOntology = new ArrayList<>();
        try {
            
            ontology = manager.loadOntologyFromOntologyDocument(file);              
            ontology = addOntologyImport(ontology);          

            //ADD PROPERTY rdf:type owl:ObjectProperty ;
            HashMap<IRI, HashMap<String, String>> property2domain_range = new HashMap<IRI, HashMap<String, String>>();
            HashMap<String, HashMap<String, String>> domain2property2range = new HashMap<String, HashMap<String, String>>();

            Stream<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN);
            Iterator<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainIterator = owlObjectPropertyDomainStream.iterator();
            while (owlObjectPropertyDomainIterator.hasNext()) {
                OWLObjectPropertyDomainAxiom op = owlObjectPropertyDomainIterator.next();
                log.info(op);
                HashMap<String, String> mapDomain = new HashMap<String, String>();
                String domainName = op.getDomain().toString();
                if (op.getDomain().isOWLClass()) {
                    IRI iriDomain = op.getDomain().asOWLClass().getIRI();
                    domainName = iriDomain.toString();
//                    if(domainName.contains(startOntologyId))
//                        domainName = iriDomain.getShortForm();
                }
                mapDomain.put("Domain", domainName);
                property2domain_range.put(op.getProperty().getNamedProperty().getIRI(), mapDomain);

                HashMap<String, String> mapProperty = new HashMap<String, String>();
                if (domain2property2range.containsKey(domainName)) {
                    mapProperty = domain2property2range.get(domainName);
                }
                mapProperty.put(op.getProperty().getNamedProperty().getIRI().getShortForm(), "");
                domain2property2range.put(domainName, mapProperty);
            }

            Stream<OWLObjectPropertyRangeAxiom> owlObjectPropertyRangeStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_RANGE);
            Iterator<OWLObjectPropertyRangeAxiom> owlObjectPropertyRangeIterator = owlObjectPropertyRangeStream.iterator();
            while (owlObjectPropertyRangeIterator.hasNext()) {
                OWLObjectPropertyRangeAxiom op = owlObjectPropertyRangeIterator.next();
                log.info(op);

                if (property2domain_range.containsKey(op.getProperty().getNamedProperty().getIRI())) {
                    HashMap<String, String> mapRange = property2domain_range.get(op.getProperty().getNamedProperty().getIRI());

                    String rangeName = op.getRange().toString();
                    if (op.getRange().isOWLClass()) {
                        IRI iriRange = op.getRange().asOWLClass().getIRI();
                        rangeName = iriRange.toString();
//                        if(rangeName.contains(startOntologyId))
//                            rangeName = iriRange.getShortForm();
                        rangeName += "[]";
                    }
                    mapRange.put("Range", rangeName);

                    String domainName = mapRange.get("Domain");
                    HashMap<String, String> mapProperty = domain2property2range.get(domainName);
                    //mapProperty.get(op.getProperty().getNamedProperty().getIRI().getShortForm());
                    mapProperty.put(op.getProperty().getNamedProperty().getIRI().getShortForm(), rangeName);
                }
            }

            Stream<OWLClass> classesStream = ontology.classesInSignature();
            Iterator<OWLClass> classesIterator = classesStream.iterator();
            while (classesIterator.hasNext()) {
                OWLClass c = classesIterator.next();
                String className = c.getIRI().toString();
                HashMap<String, String> prop2type = new HashMap<String, String>();
                String superclass = "";
                
                log.info(c.getIRI().getShortForm());


                Stream<OWLClassAxiom> owlClassAxiomStream = ontology.axioms(c);
                Iterator<OWLClassAxiom> owlClassAxiomIterator = owlClassAxiomStream.iterator();
                while (owlClassAxiomIterator.hasNext()) {
                    OWLClassAxiom owlClassAxiom = owlClassAxiomIterator.next();

                    log.info("\t" + owlClassAxiom.toString());
                    if (owlClassAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
                        OWLSubClassOfAxiomImpl owlSubClassAxiom = (OWLSubClassOfAxiomImpl) owlClassAxiom;
                        
                        String typeClass = "";
                        String namePro = "";

                        Stream<OWLDataProperty> owlDataPropertyStream = owlClassAxiom.dataPropertiesInSignature();
                        Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                        if (owlDataPropertyOptional.isPresent()) {
                            namePro = owlDataPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLObjectProperty> owlObjectPropertyStream = owlClassAxiom.objectPropertiesInSignature();
                        Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                        if (owlObjectPropertyOptional.isPresent()) {
                            namePro = owlObjectPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLDatatype> owlDatatypeStream = owlClassAxiom.datatypesInSignature();
                        Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                        if (owlDatatypeOptional.isPresent()) {
                            typeClass = owlDatatypeOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLClass> owlClassStream = owlClassAxiom.classesInSignature();
                        Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                        while (owlClassIterator.hasNext()) {
                            OWLClass owlClass = owlClassIterator.next();
                            if (!owlClass.getIRI().equals(c.getIRI())) {
                                typeClass = owlClass.getIRI().toString();
                                break;
                            }
                        }   
                        
                        Boolean isArray = true;
                        OWLClassExpression superClassExpression = owlSubClassAxiom.getSuperClass();
                        if(superClassExpression != null){
                            String typeOfSuperClass = superClassExpression.getClass().getName();
                            if(typeOfSuperClass.equals(OWLDataExactCardinalityImpl.class.getName())){
                                OWLDataExactCardinalityImpl exactCardinality = (OWLDataExactCardinalityImpl) superClassExpression;
                                if(exactCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLDataMaxCardinalityImpl.class.getName())){
                                OWLDataMaxCardinalityImpl maxCardinality = (OWLDataMaxCardinalityImpl) superClassExpression;
                                if(maxCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLObjectExactCardinalityImpl.class.getName())){
                                OWLObjectExactCardinalityImpl exactCardinality = (OWLObjectExactCardinalityImpl) superClassExpression;
                                if(exactCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLObjectMaxCardinalityImpl.class.getName())){
                                OWLObjectMaxCardinalityImpl maxCardinality = (OWLObjectMaxCardinalityImpl) superClassExpression;
                                if(maxCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            //TAKE owl:Restriction 
                            else if(typeOfSuperClass.equals(OWLObjectHasValueImpl.class.getName())){
                                OWLObjectHasValueImpl owlObjectHasValueImpl = (OWLObjectHasValueImpl) superClassExpression;
                                OWLIndividual individual = owlObjectHasValueImpl.getFiller();
                                
                                Multimap<OWLDataPropertyExpression,OWLLiteral> exprLiteral = EntitySearcher.getDataPropertyValues(individual, ontology);
                                for(OWLDataPropertyExpression exp : exprLiteral.keys()){
                                    IRI iriDataPropertyImpl = null;
                                    try{
                                        OWLDataPropertyImpl dataPropertyImpl = (OWLDataPropertyImpl) exp;
                                        iriDataPropertyImpl = dataPropertyImpl.getIRI();
                                    }catch(Exception e){}
                                    
                                    if(iriDataPropertyImpl != null && iriDataPropertyImpl.getShortForm().contains("name") ){
                                        Collection<OWLLiteral> literalC = exprLiteral.get(exp);
                                        for(OWLLiteral literal : literalC){
                                            namePro = literal.getLiteral();
                                        }
                                    }
                                }
                                
                                Multimap<OWLObjectPropertyExpression,OWLIndividual> exprIndividual = EntitySearcher.getObjectPropertyValues(individual, ontology);
                                for(OWLObjectPropertyExpression exp : exprIndividual.keys()){
                                    IRI iriObjectPropertyImpl = null;
                                    try{
                                        OWLObjectPropertyImpl objectPropertyImpl = (OWLObjectPropertyImpl) exp;
                                        iriObjectPropertyImpl = objectPropertyImpl.getIRI();
                                    }catch(Exception e){}                                   
                                    
                                    if(iriObjectPropertyImpl != null && iriObjectPropertyImpl.getShortForm().contains("hasDatatype") && !namePro.isEmpty()){
                                        Collection<OWLIndividual> individualC = exprIndividual.get(exp);
                                        for(OWLIndividual individual0 : individualC){
                                            try{
                                                OWLNamedIndividualImpl namedIndividualImpl = (OWLNamedIndividualImpl) individual0;
                                                typeClass = namedIndividualImpl.getIRI().getShortForm();
                                                isArray = false;
                                            }catch(Exception e){}
                                        }
                                    }
                                }
                            }
                        }                        
                        
                        if(!typeClass.isEmpty()){
                            if (namePro.isEmpty()) {
                                if(superclass.isEmpty())
                                    superclass = typeClass;
                                else
                                    superclass += ","+typeClass;
                            } else {
                                classesRead.add(typeClass);
                                if(isArray)
                                    typeClass += "[]";
                                prop2type.put(namePro, typeClass);
                            }
                            log.info("\t\t +: " + typeClass + " " + namePro);
                        }
                    } else {
                        String typeClass = "";
                        Stream<OWLClass> owlClassStream = owlClassAxiom.classesInSignature();
                        Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                        while (owlClassIterator.hasNext()) {
                            OWLClass owlClass = owlClassIterator.next();
                            if (!owlClass.getIRI().equals(c.getIRI())) {
                                typeClass = owlClass.getIRI().toString();
//                                if(typeClass.contains(startOntologyId))
//                                    typeClass = owlClass.getIRI().getShortForm();
                                break;
                            }
                        }
                        prop2type.put(owlClassAxiom.getAxiomType().getName().toUpperCase(), typeClass);
                    }
                }

                if (domain2property2range.containsKey(className)) {
                    HashMap<String, String> property2range = domain2property2range.get(className);
                    for (String property : property2range.keySet()) {
                        if (!prop2type.containsKey(property)) {
                            String range = property2range.get(property);
                            prop2type.put(property, range);
                            log.info("\t\t +: " + range + " " + property);
                            classesRead.add(range.replace("[]", ""));
                        }
                    }

                }
                if(superclass.isEmpty())
                    classesStart.add(className);
                prop2type.put("Superclass", superclass);
                map.put(className, prop2type);
            }
        } catch (OWLOntologyCreationException ex) {
            log.error(ex);
        }
        catch (UnloadableImportException ie){
            log.error(ie);
            URL url = OwlapiHelp.class.getResource(PIM_PARTIAL_FILE);
            String filePathPartial = url.getPath();
            if(ie.getMessage().contains("<http://purl.org/dc/terms/>") && !this.filePath.equals(filePathPartial)){
                this.filePath = filePathPartial;
                map = createMapClass2PropAndSuperclass();
                log.info("Load pim partial ");
            }
        }
        return map;
    }
    
    
    
    public HashMap<String, HashMap<String, String>> createMapClass2PropAndSuperclassTest() {
        HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
        String result = "";
        File file = new File(filePath);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        allOntology = new ArrayList<>();
        OWLDataFactory dataFactory;
        try {
            
            ontology = manager.loadOntologyFromOntologyDocument(file);              
            ontology = addOntologyImport(ontology);
            
            OWL2DLProfile profile = new OWL2DLProfile();
            OWLProfileReport report = profile.checkOntology(ontology);
            for(OWLProfileViolation v:report.getViolations()) {
                System.out.println(v);
            }
            


            
            dataFactory = manager.getOWLDataFactory();
            //ADD PROPERTY rdf:type owl:ObjectProperty ;
            HashMap<IRI, HashMap<String, String>> property2domain_range = new HashMap<IRI, HashMap<String, String>>();
            HashMap<String, HashMap<String, String>> domain2property2range = new HashMap<String, HashMap<String, String>>();

            Stream<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN);
            Iterator<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainIterator = owlObjectPropertyDomainStream.iterator();
            while (owlObjectPropertyDomainIterator.hasNext()) {
                OWLObjectPropertyDomainAxiom op = owlObjectPropertyDomainIterator.next();
                log.info(op);
                HashMap<String, String> mapDomain = new HashMap<String, String>();
                String domainName = op.getDomain().toString();
                if (op.getDomain().isOWLClass()) {
                    IRI iriDomain = op.getDomain().asOWLClass().getIRI();
                    domainName = iriDomain.toString();
//                    if(domainName.contains(startOntologyId))
//                        domainName = iriDomain.getShortForm();
                }
                mapDomain.put("Domain", domainName);
                property2domain_range.put(op.getProperty().getNamedProperty().getIRI(), mapDomain);

                HashMap<String, String> mapProperty = new HashMap<String, String>();
                if (domain2property2range.containsKey(domainName)) {
                    mapProperty = domain2property2range.get(domainName);
                }
                mapProperty.put(op.getProperty().getNamedProperty().getIRI().getShortForm(), "");
                domain2property2range.put(domainName, mapProperty);
            }

            Stream<OWLObjectPropertyRangeAxiom> owlObjectPropertyRangeStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_RANGE);
            Iterator<OWLObjectPropertyRangeAxiom> owlObjectPropertyRangeIterator = owlObjectPropertyRangeStream.iterator();
            while (owlObjectPropertyRangeIterator.hasNext()) {
                OWLObjectPropertyRangeAxiom op = owlObjectPropertyRangeIterator.next();
                log.info(op);

                if (property2domain_range.containsKey(op.getProperty().getNamedProperty().getIRI())) {
                    HashMap<String, String> mapRange = property2domain_range.get(op.getProperty().getNamedProperty().getIRI());

                    String rangeName = op.getRange().toString();
                    if (op.getRange().isOWLClass()) {
                        IRI iriRange = op.getRange().asOWLClass().getIRI();
                        rangeName = iriRange.toString();
//                        if(rangeName.contains(startOntologyId))
//                            rangeName = iriRange.getShortForm();
                        rangeName += "[]";
                    }
                    mapRange.put("Range", rangeName);

                    String domainName = mapRange.get("Domain");
                    HashMap<String, String> mapProperty = domain2property2range.get(domainName);
                    //mapProperty.get(op.getProperty().getNamedProperty().getIRI().getShortForm());
                    mapProperty.put(op.getProperty().getNamedProperty().getIRI().getShortForm(), rangeName);
                }
            }

            Stream<OWLClass> classesStream = ontology.classesInSignature();
            Iterator<OWLClass> classesIterator = classesStream.iterator();
            while (classesIterator.hasNext()) {
                OWLClass c = classesIterator.next();
                String className = c.getIRI().toString();
//                if(className.contains(startOntologyId))
//                    className = c.getIRI().getShortForm();
                HashMap<String, String> prop2type = new HashMap<String, String>();
                String superclass = "";
                

                log.info(c.getIRI().getShortForm());
                
                String breckpoint = null;
                if(c.getIRI().getShortForm().equals("RGBCapability") || c.getIRI().getShortForm().equals("DimmerCapability"))
                    breckpoint = "TREU";


                Stream<OWLClassAxiom> owlClassAxiomStream = ontology.axioms(c);
                Iterator<OWLClassAxiom> owlClassAxiomIterator = owlClassAxiomStream.iterator();
                while (owlClassAxiomIterator.hasNext()) {
                    OWLClassAxiom owlClassAxiom = owlClassAxiomIterator.next();
                    
                    Stream<OWLClassExpression> classExpressionStream = owlClassAxiom.nestedClassExpressions();
                    Iterator<OWLClassExpression> classExpressionIterator = classExpressionStream.iterator();
                    while (classExpressionIterator.hasNext()) {
                        OWLClassExpression classExpression = classExpressionIterator.next();
                        ClassExpressionType classExpressionType = classExpression.getClassExpressionType();
                        String z = classExpressionType.name();
                        String a = ClassExpressionType.DATA_HAS_VALUE.getClass().toString();
                        String b = classExpression.getClass().toString();
                        if(classExpressionType.name().equals(ClassExpressionType.DATA_HAS_VALUE.name())){
                            OWLDataHasValue hasValue = (OWLDataHasValue) classExpression;
                            String s = "t";
                        }
                        else if(classExpressionType.name().equals(ClassExpressionType.OBJECT_HAS_VALUE.name())){
                            OWLObjectHasValue hasValue = (OWLObjectHasValue) classExpression;
                           
                            OWLObjectPropertyExpression objproexp = hasValue.getProperty();
                            
                            OWLObjectPropertyRangeAxiom ciaocio = dataFactory.getOWLObjectPropertyRangeAxiom(objproexp.getNamedProperty(), classExpression);
                            
                            OWLClassExpression classExpression2 = hasValue.asSomeValuesFrom();
                            Stream<?> objectStream0 = hasValue.components();
                            if(objectStream0 != null){
                                Iterator<?> objectIterator0 = objectStream0.iterator();
                                while(objectIterator0.hasNext()){
                                    Object ciao = objectIterator0.next();
                                    String azsd = "";
                                }
                            }
                            OWLIndividual individual0 = hasValue.getValue();
                            
                            
                            OWLClassExpression claEx = hasValue.getObjectComplementOf();
                            
                            
                            OWLIndividual individual = hasValue.getFiller();
                            
                            Stream<?> objectStream = individual.components();
                            Iterator<?> objectIterator = objectStream.iterator();
                            while(objectIterator.hasNext()){
                                Object ciao = objectIterator.next();
                            }
                            
                            
                            if(individual.isAnonymous()){
                                OWLAnonymousIndividual indi = (OWLAnonymousIndividual) individual;
                                
                                NodeID nodeId = indi.getID();
                                Multimap<OWLDataPropertyExpression,OWLLiteral> exprLiteral = EntitySearcher.getDataPropertyValues(individual, ontology);
                                for(OWLDataPropertyExpression exp : exprLiteral.keys()){
                                    Collection<OWLLiteral> literalC = exprLiteral.get(exp);
                                    for(OWLLiteral literal : literalC){
                                        String sese = literal.toString();
                                    }
                                }
                                
                                Multimap<OWLObjectPropertyExpression,OWLIndividual> exprIndividual = EntitySearcher.getObjectPropertyValues(individual, ontology);
                                for(OWLObjectPropertyExpression exp : exprIndividual.keys()){
                                    Collection<OWLIndividual> individualC = exprIndividual.get(exp);
                                    for(OWLIndividual individuale : individualC){
                                        String sese = individuale.toString();
                                    }
                                }
                                
                                
                                Stream<OWLDataProperty> owlDataPropertyStream = indi.dataPropertiesInSignature();
                                Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                                if (owlDataPropertyOptional.isPresent()) {
                                    String ao = owlDataPropertyOptional.get().getIRI().getShortForm();
                                }

                                Stream<OWLObjectProperty> owlObjectPropertyStream = indi.objectPropertiesInSignature();
                                Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                                if (owlObjectPropertyOptional.isPresent()) {
                                    String ao = owlObjectPropertyOptional.get().getIRI().getShortForm();
                                }

                                Stream<OWLDatatype> owlDatatypeStream = indi.datatypesInSignature();
                                Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                                if (owlDatatypeOptional.isPresent()) {
                                    String ao = owlDatatypeOptional.get().getIRI().getShortForm();
                                }
                                
                                Stream<OWLClass> owlClassStream = indi.classesInSignature();
                                Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                                while (owlClassIterator.hasNext()) {
                                    OWLClass owlClass = owlClassIterator.next();
                                    if (!owlClass.getIRI().equals(c.getIRI())) {
                                        String ao = owlClass.getIRI().toString();
                                        break;
                                    }
                                }
                                
                            }
                            
                            //OWLObjectPropertyExpression objproexp = hasValue.getProperty();
                            String s = "t";
                            
                            
                            
                            String zz = "t";
                        }
                    }
                    
                    
                    
                    log.info("\t" + owlClassAxiom.toString());
                    if (owlClassAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
                        OWLSubClassOfAxiomImpl owlSubClassAxiom = (OWLSubClassOfAxiomImpl) owlClassAxiom;
                        
                        String typeClass = "";
                        String namePro = "";

                        Stream<OWLDataProperty> owlDataPropertyStream = owlClassAxiom.dataPropertiesInSignature();
                        Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                        if (owlDataPropertyOptional.isPresent()) {
                            namePro = owlDataPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLObjectProperty> owlObjectPropertyStream = owlClassAxiom.objectPropertiesInSignature();
                        Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                        if (owlObjectPropertyOptional.isPresent()) {
                            namePro = owlObjectPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLDatatype> owlDatatypeStream = owlClassAxiom.datatypesInSignature();
                        Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                        if (owlDatatypeOptional.isPresent()) {
                            typeClass = owlDatatypeOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLClass> owlClassStream = owlClassAxiom.classesInSignature();
                        Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                        while (owlClassIterator.hasNext()) {
                            OWLClass owlClass = owlClassIterator.next();
                            if (!owlClass.getIRI().equals(c.getIRI())) {
                                typeClass = owlClass.getIRI().toString();
//                                if(typeClass.contains(startOntologyId))
//                                    typeClass = owlClass.getIRI().getShortForm();
                                break;
                            }
                        }
                        
                        
                        
                        Stream<OWLAnonymousIndividual> anonymousStream = owlClassAxiom.anonymousIndividuals();
                        Iterator<OWLAnonymousIndividual> anonymousIterator = anonymousStream.iterator();
                        while(anonymousIterator.hasNext()){
                            OWLAnonymousIndividual anonymous = anonymousIterator.next();
                            NodeID nodeId = anonymous.getID();
                            
                            Stream<OWLClass> owlClassStreamAnonymous = anonymous.classesInSignature();
                            Iterator<OWLClass> owlClassIteratorAnonymous = owlClassStreamAnonymous.iterator();
                            while (owlClassIteratorAnonymous.hasNext()) {
                                OWLClass owlClass = owlClassIteratorAnonymous.next();
                                if (!owlClass.getIRI().equals(c.getIRI())) {
                                    typeClass = owlClass.getIRI().toString();
    //                                if(typeClass.contains(startOntologyId))
    //                                    typeClass = owlClass.getIRI().getShortForm();
                                }
                            }
                        }
                        
                        
                        
                        Boolean isArray = true;
                        OWLClassExpression superClassExpression = owlSubClassAxiom.getSuperClass();
                        if(superClassExpression != null){
                            String typeOfSuperClass = superClassExpression.getClass().getName();
                            if(typeOfSuperClass.equals(OWLDataExactCardinalityImpl.class.getName())){
                                OWLDataExactCardinalityImpl exactCardinality = (OWLDataExactCardinalityImpl) superClassExpression;
                                if(exactCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLDataMaxCardinalityImpl.class.getName())){
                                OWLDataMaxCardinalityImpl maxCardinality = (OWLDataMaxCardinalityImpl) superClassExpression;
                                if(maxCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLObjectExactCardinalityImpl.class.getName())){
                                OWLObjectExactCardinalityImpl exactCardinality = (OWLObjectExactCardinalityImpl) superClassExpression;
                                if(exactCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            else if(typeOfSuperClass.equals(OWLObjectMaxCardinalityImpl.class.getName())){
                                OWLObjectMaxCardinalityImpl maxCardinality = (OWLObjectMaxCardinalityImpl) superClassExpression;
                                if(maxCardinality.getCardinality() == 1)
                                    isArray = false;
                            }
                            
                            else if(typeOfSuperClass.equals(OWLObjectHasValueImpl.class.getName())){
                                OWLObjectHasValueImpl owlObjectHasValueImpl = (OWLObjectHasValueImpl) superClassExpression;
                                OWLObjectPropertyExpression proExp = owlObjectHasValueImpl.getProperty();
                                OWLObjectProperty property= proExp.getNamedProperty();
                                        
                                Stream<OWLClass> owlClassStreamAnonymous = owlObjectHasValueImpl.classesInSignature();
                                Iterator<OWLClass> owlClassIteratorAnonymous = owlClassStreamAnonymous.iterator();
                                while (owlClassIteratorAnonymous.hasNext()) {
                                    OWLClass owlClass = owlClassIteratorAnonymous.next();
                                    if (!owlClass.getIRI().equals(c.getIRI())) {
                                        typeClass = owlClass.getIRI().toString();
        //                                if(typeClass.contains(startOntologyId))
        //                                    typeClass = owlClass.getIRI().getShortForm();
                                    }
                                }
                                
                                Stream<OWLDataProperty> owlDataPropertyStreamA = owlObjectHasValueImpl.dataPropertiesInSignature();
                                Optional<OWLDataProperty> owlDataPropertyOptionalA = owlDataPropertyStreamA.findFirst();
                                if (owlDataPropertyOptionalA.isPresent()) {
                                    namePro = owlDataPropertyOptionalA.get().getIRI().getShortForm();
                                }

                                Stream<OWLObjectProperty> owlObjectPropertyStreamA = owlObjectHasValueImpl.objectPropertiesInSignature();
                                Optional<OWLObjectProperty> owlObjectPropertyOptionalA = owlObjectPropertyStreamA.findFirst();
                                if (owlObjectPropertyOptionalA.isPresent()) {
                                    namePro = owlObjectPropertyOptionalA.get().getIRI().getShortForm();
                                }

                                Stream<OWLDatatype> owlDatatypeStreamA = owlObjectHasValueImpl.datatypesInSignature();
                                Optional<OWLDatatype> owlDatatypeOptionalA = owlDatatypeStreamA.findFirst();
                                if (owlDatatypeOptionalA.isPresent()) {
                                    typeClass = owlDatatypeOptionalA.get().getIRI().getShortForm();
                                }
                                String a = "ff";
                            }
                        }
                        
                        
                        
                        if (!typeClass.isEmpty() && namePro.isEmpty()) {
                            if(superclass.isEmpty())
                                superclass = typeClass;
                            else
                                superclass += ","+typeClass;
                        } else {
                            classesRead.add(typeClass);
                            if(!typeClass.isEmpty() && isArray)
                                typeClass += "[]";
                            prop2type.put(namePro, typeClass);
                        }
                        String breackpoint = null;
                        if(typeClass.isEmpty())
                            breackpoint = "TREU";
                        log.info("\t\t +: " + typeClass + " " + namePro);
                    } else {
                        String typeClass = "";
                        Stream<OWLClass> owlClassStream = owlClassAxiom.classesInSignature();
                        Iterator<OWLClass> owlClassIterator = owlClassStream.iterator();
                        while (owlClassIterator.hasNext()) {
                            OWLClass owlClass = owlClassIterator.next();
                            if (!owlClass.getIRI().equals(c.getIRI())) {
                                typeClass = owlClass.getIRI().toString();
//                                if(typeClass.contains(startOntologyId))
//                                    typeClass = owlClass.getIRI().getShortForm();
                                break;
                            }
                        }
                        prop2type.put(owlClassAxiom.getAxiomType().getName().toUpperCase(), typeClass);
                    }
                }

                if (domain2property2range.containsKey(className)) {
                    HashMap<String, String> property2range = domain2property2range.get(className);
                    for (String property : property2range.keySet()) {
                        if (!prop2type.containsKey(property)) {
                            String range = property2range.get(property);
                            prop2type.put(property, range);
                            log.info("\t\t +: " + range + " " + property);
                            classesRead.add(range.replace("[]", ""));
                        }
                    }

                }
                if(superclass.isEmpty())
                    classesStart.add(className);
                prop2type.put("Superclass", superclass);
                map.put(className, prop2type);
            }
        } catch (OWLOntologyCreationException ex) {
            log.error(ex);
        }
        return map;
    }
    
    
}

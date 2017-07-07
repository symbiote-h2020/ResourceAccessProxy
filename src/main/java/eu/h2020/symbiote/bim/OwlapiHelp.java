/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.bim;

//import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.apache.jena.assembler.Assembler.reasonerFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.owlapi.OWLDataExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectExactCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectMaxCardinalityImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class OwlapiHelp {

    private static final Log log = LogFactory.getLog(OwlapiHelp.class);

    private static final String CIM_FILE_OLD = "/core-v0.6.owl";
    private static final String CIM_FILE = "/core-v1.0.owl";
    private static final String BIM_FILE = "/bim-0.3.owl";
    private static final String TIME_FILE = "/time.owl";
    
    private HashMap<String, HashMap<String, String>> classes;
    private HashSet<String> classesStart = new HashSet<String>();
    private HashSet <String> classesRead = new HashSet<String>();
    private List<OWLOntologyID> allOntology;
    
    public HashMap<String, HashMap<String, String>> fromOwlToClasses(){        
        HashMap<String, HashMap<String, String>> map = test2();
        classes = new HashMap<String, HashMap<String, String>>();
        for(String key: map.keySet()){
            HashMap<String,String> attribute2type = fromOwlToClassesPrivate(key,map.get(key),map);
        }
        for(String removeClass: classesRead){
            classesStart.remove(removeClass);
        }
        return classes;
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
    
    private OWLOntology addOntology(OWLOntology ontology){
        Stream<OWLOntology> owlOntologyStream = ontology.imports();
        Iterator<OWLOntology> owlOntologyiterator = owlOntologyStream.iterator();
        while(owlOntologyiterator.hasNext()){
            OWLOntology ontologyImport = owlOntologyiterator.next();
            if(!allOntology.contains(ontologyImport.getOntologyID())){
                Stream<OWLAxiom> axiomStream = ontologyImport.axioms();
                Iterator<OWLAxiom> axiomIterator = axiomStream.iterator();
                while (axiomIterator.hasNext()) {                        
                    OWLAxiom axiom = axiomIterator.next();
                    ontology.addAxiom(axiom);
                }
                allOntology.add(ontologyImport.getOntologyID());
                addOntology(ontology);
            }
        }
        return ontology;
    }

    
    public HashMap<String, HashMap<String, String>> test2() {
        HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
        String result = "";
        String name = OwlapiHelp.class.getResource(CIM_FILE).getPath();
        File file = new File(name);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        allOntology = new ArrayList<>();
        try {
            
            ontology = manager.loadOntologyFromOntologyDocument(file);  
            OWLOntologyID owlOntologyId = ontology.getOntologyID();
            allOntology.add(owlOntologyId);
            String startOntologyId = owlOntologyId.toString();
            if(owlOntologyId.getOntologyIRI().isPresent())
                startOntologyId = owlOntologyId.getOntologyIRI().get().toString();

            
            Stream<OWLOntology> owlOntologyStream = ontology.importsClosure();
            Iterator<OWLOntology> owlOntologyiterator = owlOntologyStream.iterator();
            while(owlOntologyiterator.hasNext()){
                OWLOntology ontologyImport = owlOntologyiterator.next();
                System.out.println(ontologyImport.getOntologyID());
            }
            
            
            ontology = addOntology(ontology);
            
            
            
            Stream<OWLOntology> owlOntologyStream0 = manager.ontologies();
            Iterator<OWLOntology> owlOntologyiterator0 = owlOntologyStream0.iterator();
            while(owlOntologyiterator0.hasNext()){
                OWLOntology ontologyImport = owlOntologyiterator0.next();
                System.out.println(ontologyImport.getOntologyID());
            }
            
            //ADD PROPERTY rdf:type owl:ObjectProperty ;
            HashMap<IRI, HashMap<String, String>> property2domain_range = new HashMap<IRI, HashMap<String, String>>();
            HashMap<String, HashMap<String, String>> domain2property2range = new HashMap<String, HashMap<String, String>>();

            Stream<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainStream = ontology.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN);
            Iterator<OWLObjectPropertyDomainAxiom> owlObjectPropertyDomainIterator = owlObjectPropertyDomainStream.iterator();
            while (owlObjectPropertyDomainIterator.hasNext()) {
                OWLObjectPropertyDomainAxiom op = owlObjectPropertyDomainIterator.next();
                System.out.println(op);
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
                System.out.println(op);

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
                

                System.out.println(c.getIRI().getShortForm());

                String breakPP = "";
                if (c.getIRI().toString().equals("http://www.symbiote-h2020.eu/ontology/core#FeatureOfInterest")) {
                    breakPP = "TRUE";
                }

                Stream<OWLClassAxiom> owlClassAxiomStream = ontology.axioms(c);
                Iterator<OWLClassAxiom> owlClassAxiomIterator = owlClassAxiomStream.iterator();
                while (owlClassAxiomIterator.hasNext()) {
                    OWLClassAxiom owlClassAxiom = owlClassAxiomIterator.next();
                    System.out.println("\t" + owlClassAxiom.toString());
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
                        System.out.println("\t\t +: " + typeClass + " " + namePro);
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
                            System.out.println("\t\t +: " + range + " " + property);
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
    
    

    public HashMap<String, HashMap<String, ArrayList<String>>> test() {
        HashMap<String, HashMap<String, ArrayList<String>>> map = new HashMap<String, HashMap<String, ArrayList<String>>>();
        String result = "";
        String name = OwlapiHelp.class.getResource(CIM_FILE).getPath();
        File file = new File(name);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;
        OWLDataFactory dataFactory;
        OWLReasoner reasoner;

        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);
            //reasoner = reasonerFactory.createReasoner(ontology);
            dataFactory = manager.getOWLDataFactory();

            Set<OWLClass> classes;
            Set<OWLObjectProperty> prop;
            Set<OWLDataProperty> dataProp;
            Set<OWLNamedIndividual> individuals;

            classes = ontology.getClassesInSignature();
            prop = ontology.getObjectPropertiesInSignature();
            dataProp = ontology.getDataPropertiesInSignature();
            individuals = ontology.getIndividualsInSignature();
            //configurator = new OWLAPIOntologyConfigurator(this);    

            for (OWLClass c : classes) {
                OWLDataProperty owlDataProperty = dataFactory.getOWLDataProperty(c.getIRI());
                //System.out.println(owlDataProperty.getIRI());

                OWLObjectProperty owlObjectProperty = dataFactory.getOWLObjectProperty(c.getIRI());
                //System.out.println(owlObjectProperty.getIRI());

                OWLDatatype owlDatatype = dataFactory.getOWLDatatype(c.getIRI());
                System.out.println(owlDatatype.getIRI());

                String breakPP = "";
                if (owlDatatype.getIRI().toString().equals("http://www.symbiote-h2020.eu/ontology/core#InputParameter")) {
                    breakPP = "TRUE";
                }

                Stream<OWLClassAxiom> owlClassAxiomStream = ontology.axioms(c);
                Iterator<OWLClassAxiom> owlClassAxiomIterator = owlClassAxiomStream.iterator();
                while (owlClassAxiomIterator.hasNext()) {
                    OWLClassAxiom owlClassAxiom = owlClassAxiomIterator.next();

                    System.out.println(owlClassAxiom.toString());
                    Stream<OWLDataProperty> owlDataPropertyStream = owlClassAxiom.dataPropertiesInSignature();
                    Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                    if (owlDataPropertyOptional.isPresent()) {
                        String namePro = owlDataPropertyOptional.get().getIRI().getShortForm();
                    }
                }

                System.out.println("\t +PROPRIETA': ");
                for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSubClass(c)) {
                    Set<OWLClass> associatedTerms = new HashSet<OWLClass>();
                    OWLClassExpression expression = axiom.getSuperClass();

                    if (expression.isAnonymous()) {
                        String typeClass = "";
                        String namePro = "";

                        Stream<OWLNamedIndividual> owlNamedIndividualStream = expression.individualsInSignature();
                        Optional<OWLNamedIndividual> owlNamedIndividualOptional = owlNamedIndividualStream.findFirst();
                        if (owlNamedIndividualOptional.isPresent()) {
                            String nulla = owlNamedIndividualOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLDataProperty> owlDataPropertyStream = expression.dataPropertiesInSignature();
                        Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                        if (owlDataPropertyOptional.isPresent()) {
                            namePro = owlDataPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLObjectProperty> owlObjectPropertyStream = expression.objectPropertiesInSignature();
                        Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                        if (owlObjectPropertyOptional.isPresent()) {
                            namePro = owlObjectPropertyOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLDatatype> owlDatatypeStream = expression.datatypesInSignature();
                        Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                        if (owlDatatypeOptional.isPresent()) {
                            typeClass = owlDatatypeOptional.get().getIRI().getShortForm();
                        }

                        Stream<OWLClass> owlClassStream = expression.classesInSignature();
                        Optional<OWLClass> owlClassOptional = owlClassStream.findFirst();
                        if (owlClassOptional.isPresent()) {
                            typeClass = owlClassOptional.get().getIRI().getShortForm();
                        }

                        System.out.println("\t\t +: " + typeClass + " " + namePro);
                    }
                }

                Iterator<OWLSubClassOfAxiom> axiomIterator = ontology.subClassAxiomsForSuperClass(c).iterator();
                while (axiomIterator.hasNext()) {
                    OWLSubClassOfAxiom axiom = axiomIterator.next();
                    Set<OWLClass> associatedTerms = new HashSet<OWLClass>();
                    OWLClassExpression expression = axiom.getSuperClass();
                    System.out.println("\t +PROPRIETA': ");
                    if (expression.isAnonymous()) {
                        String type = "";
                        String namePro = "";
                        for (OWLObjectProperty property : expression.getObjectPropertiesInSignature()) {
                            type = property.toStringID();
                        }
                        for (OWLClass associatedClass : expression.getClassesInSignature()) {
                            namePro = associatedClass.toStringID();
                        }
                        System.out.println("\t\t +: " + type + " " + namePro);
                    }
                }
            }
            /*
                for(OWLNamedIndividual i: reasoner.getInstances(c, true).getFlattened()){
                    System.out.println(labelFor(i, ontology) +":"+ labelFor(c, ontology));
                    
                    
                    for (OWLObjectProperty op: ontology.getObjectPropertiesInSignature()) {
                        NodeSet<OWLNamedIndividual> petValuesNodeSet = reasoner.getObjectPropertyValues(i, op);
                        for (OWLNamedIndividual value : petValuesNodeSet.getFlattened())
                            System.out.println(labelFor(i, ontology) + " " + 
                                    labelFor(op, ontology) + " " + labelFor(value, ontology));
                    }
                }
            }*/

            System.out.println("Classes");
            System.out.println("--------------------------------");
            for (OWLClass cls : classes) {
                HashMap<String, ArrayList<String>> key2values = new HashMap<String, ArrayList<String>>();
                System.out.println("+: " + cls.getIRI().getShortForm());
                result += "+: " + cls.getIRI().getShortForm();

                ArrayList<String> subClassList = new ArrayList<String>();
                System.out.println(" \tSubClass");
                for (OWLSubClassOfAxiom subClass : ontology.getSubClassAxiomsForSubClass(cls)) {
                    OWLClassExpression levelOne = subClass.getSuperClass();
                    if (levelOne.isOWLClass()) {
                        OWLClass owl_class = levelOne.asOWLClass();
                        System.out.println("\t\t +: " + owl_class.getIRI().getShortForm());
                        result += "\t\t +: " + owl_class.getIRI().getShortForm();
                        subClassList.add(owl_class.getIRI().getShortForm());
                    }
                }
                key2values.put("SUPERCLASS", subClassList);

                System.out.println(" \tObject Property");
                Iterator<OWLObjectProperty> objProIterator = cls.objectPropertiesInSignature().iterator();
                while (objProIterator.hasNext()) {
                    OWLObjectProperty objPro = objProIterator.next();
                    String z = "\t\t +: " + objPro.getIRI().getShortForm();
                }

                ArrayList<String> objectPropertyDomainList = new ArrayList<String>();
                System.out.println(" \tObject Property Domain");
                for (OWLObjectPropertyDomainAxiom op : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
                    if (op.getDomain().equals(cls)) {

                        AxiomType axiomType = op.getAxiomType();
                        OWLAxiom axiom = op.getNNF();
                        OWLObjectPropertyExpression o = op.getProperty();
                        OWLObjectPropertyExpression oooo = o.getSimplified();
                        OWLDatatype owlDatatypeeee = dataFactory.getOWLDatatype(o.getNamedProperty().getIRI());
                        /*for (OWLClass associatedClass : op.getClassesInSignature())
                        {
                            System.out.println("\t\t +: " + associatedClass.getIRI().getShortForm());
                        }
                        for (OWLClassExpression associatedClass : op.getNestedClassExpressions())
                        {
                            System.out.println("\t\t +: " + associatedClass.getClassExpressionType() . getIRI().getShortForm());
                            OWLClassExpression express = associatedClass.getObjectComplementOf();
                            System.out.println("\t\t +: " +  express.toString());
                            System.out.println("\t\t +: " + associatedClass.getNNF().toString());
                        }
                        for (OWLDataProperty associatedClass : op.getDataPropertiesInSignature())
                        {
                            System.out.println("\t\t +: " + associatedClass.getIRI().getShortForm());
                        }
                        for (OWLNamedIndividual associatedClass : op.getIndividualsInSignature())
                        {
                            System.out.println("\t\t +: " + associatedClass.getIRI().getShortForm());
                        }
                        for (OWLAnnotation associatedClass : op.getAnnotations())
                        {
                            System.out.println("\t\t +: " + associatedClass.getProperty().toStringID() + associatedClass.getValue().toString());
                        }*/

                        for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {

                            /*Stream<OWLDatatype> owlDatatypeStream = oop.datatypesInSignature();
                            Iterator<OWLDatatype> owlDatatypeIterator = owlDatatypeStream.iterator();
                            while(owlDatatypeIterator.hasNext()){
                                OWLDatatype oWLDatatype = owlDatatypeIterator.next();
                                System.out.println("\t\t +: " + oWLDatatype.toStringID() + oWLDatatype.toString());
                            }*/
                            OWLDatatype owlDatatype = dataFactory.getOWLDatatype(oop.getIRI());
                            //System.out.println(owlDatatype.getIRI());

                            System.out.println("\t\t +: " + oop.getIRI().getShortForm());
                            result += "\t\t +: " + oop.getIRI().getShortForm();
                            objectPropertyDomainList.add(oop.getIRI().getShortForm());

                            OWLObjectPropertyImpl imp = (OWLObjectPropertyImpl) oop;
                            EntityType eee = imp.getEntityType();
                            String a = eee.getIRI().getShortForm();
                        }
                        //System.out.println("\t\t +: " + op.getProperty().getNamedProperty().getIRI().getShortForm());
                    }
                }
                key2values.put("OBJECT_PROPERTY_DOMAIN", objectPropertyDomainList);

                ArrayList<String> dataPropertyDomainList = new ArrayList<String>();
                System.out.println(" \tData Property Domain");
                for (OWLDataPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
                    if (dp.getDomain().equals(cls)) {

                        for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
                            System.out.println("\t\t +: " + odp.getIRI().getShortForm());
                            result += "\t\t +: " + odp.getIRI().getShortForm();
                            dataPropertyDomainList.add(odp.getIRI().getShortForm());

                            /*String breakPoint = "";
                            if(odp.getIRI().getShortForm().equals("isArray"))
                                breakPoint = "TREY";
                            OWLDatatype owlDatatype = dataFactory.getOWLDatatype(odp.getIRI());
                            System.out.println(owlDatatype.getIRI());
                            System.out.println(owlDatatype.isBoolean());*/
                        }
                        //System.out.println("\t\t +:" + dp.getProperty());
                    }
                }
                key2values.put("DATA_PROPERTY_DOMAIN", dataPropertyDomainList);

                ArrayList<String> annotationoPropertyDomainList = new ArrayList<String>();
                System.out.println(" \tAnnotation Property Domain");
                for (OWLAnnotationPropertyDomainAxiom ap : ontology.getAxioms(AxiomType.ANNOTATION_PROPERTY_DOMAIN)) {
                    if (ap.getDomain().equals(cls)) {

                        for (OWLDataProperty odp : ap.getDataPropertiesInSignature()) {
                            System.out.println("\t\t +: " + odp.getIRI().getShortForm());
                            result += "\t\t +: " + odp.getIRI().getShortForm();
                            annotationoPropertyDomainList.add(odp.getIRI().getShortForm());
                        }
                        //System.out.println("\t\t +:" + dp.getProperty());
                    }
                }
                key2values.put("ANNOTATION_PROPERTY_DOMAIN", annotationoPropertyDomainList);

                System.out.println(" \tFunctional Data Property");
                for (OWLFunctionalDataPropertyAxiom dp : ontology.getAxioms(AxiomType.FUNCTIONAL_DATA_PROPERTY)) {
                    OWLDataPropertyExpression ex = dp.getProperty();
                    String a = ex.toString();
                }

                map.put(cls.getIRI().getShortForm(), key2values);
            }

            /*OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            //OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
            for (OWLNamedIndividual i : ontology.getIndividualsInSignature()) {
                for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
                    NodeSet<OWLNamedIndividual> individualValues = reasoner.getObjectPropertyValues(i, p);
                    Set<OWLNamedIndividual> values = individualValues.getFlattened();
                    System.out.println("The property values for "+p+" for individual "+i+" are: ");
                    for (OWLNamedIndividual ind : values) {
                        System.out.println(" " + ind);
                    }
                }
            }*/
        } catch (OWLOntologyCreationException ex) {
            log.error(ex);
        }
        return map;
    }

}

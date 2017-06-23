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
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class OwlapiHelp {

    private static final Log log = LogFactory.getLog(OwlapiHelp.class);

    private static final String CIM_FILE = "/core-v0.6.owl";
    private static final String BIM_FILE = "/bim-0.3.owl";

    public HashMap<String,HashMap<String,ArrayList<String>>> test() {
        HashMap<String,HashMap<String,ArrayList<String>>> map = new HashMap<String,HashMap<String,ArrayList<String>>>();
        String result = "";
        String name = OwlapiHelp.class.getResource(CIM_FILE).getPath();
        File file = new File(name);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology;

        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);

            Set<OWLClass> classes;
            Set<OWLObjectProperty> prop;
            Set<OWLDataProperty> dataProp;
            Set<OWLNamedIndividual> individuals;

            classes = ontology.getClassesInSignature();
            prop = ontology.getObjectPropertiesInSignature();            
            dataProp = ontology.getDataPropertiesInSignature();
            individuals = ontology.getIndividualsInSignature();
            //configurator = new OWLAPIOntologyConfigurator(this);    
        

            System.out.println("Classes");
            System.out.println("--------------------------------");
            for (OWLClass cls : classes) {
                HashMap<String,ArrayList<String>> key2values = new HashMap<String,ArrayList<String>>();
                System.out.println("+: " + cls.getIRI().getShortForm());
                result += "+: " + cls.getIRI().getShortForm();
                             
                
                ArrayList<String> subClassList = new ArrayList<String>();
                System.out.println(" \tSubClass");
                for(OWLSubClassOfAxiom subClass: ontology.getSubClassAxiomsForSubClass(cls)){
                    OWLClassExpression levelOne = subClass.getSuperClass();
                    if(levelOne.isOWLClass()){
                        OWLClass owl_class = levelOne.asOWLClass();
                        System.out.println("\t\t +: " + owl_class.getIRI().getShortForm());
                        result += "\t\t +: " + owl_class.getIRI().getShortForm();
                        subClassList.add(owl_class.getIRI().getShortForm());
                    }
                }
                key2values.put("SUBCLASS", subClassList);
                
                
                System.out.println(" \tObject Property");
                Iterator<OWLObjectProperty> objProIterator =cls.objectPropertiesInSignature().iterator();
                while(objProIterator.hasNext()){
                    OWLObjectProperty objPro = objProIterator.next();
                    System.out.println("\t\t +: " + objPro.getIRI().getShortForm());
                }
                
                
                ArrayList<String> objectPropertyDomainList = new ArrayList<String>();
                System.out.println(" \tObject Property Domain");
                for (OWLObjectPropertyDomainAxiom op : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
                    if (op.getDomain().equals(cls)) {
                        for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
                            System.out.println("\t\t +: " + oop.getIRI().getShortForm());
                            result += "\t\t +: " + oop.getIRI().getShortForm();
                            objectPropertyDomainList.add(oop.getIRI().getShortForm());
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
                        }
                        //System.out.println("\t\t +:" + dp.getProperty());
                    }
                }
                key2values.put("DATA_PROPERTY_DOMAIN", dataPropertyDomainList);
                
                
                ArrayList<String> annotationoPropertyDomainList = new ArrayList<String>();                
                System.out.println(" \tAnnotation Property Domain");
                for (OWLAnnotationPropertyDomainAxiom dp : ontology.getAxioms(AxiomType.ANNOTATION_PROPERTY_DOMAIN)) {
                    if (dp.getDomain().equals(cls)) {
                        for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
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
            
            
            
            
            
            OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
            for (OWLNamedIndividual i : ontology.getIndividualsInSignature()) {
                for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
                    NodeSet<OWLNamedIndividual> individualValues = reasoner.getObjectPropertyValues(i, p);
                    Set<OWLNamedIndividual> values = individualValues.getFlattened();
                    System.out.println("The property values for "+p+" for individual "+i+" are: ");
                    for (OWLNamedIndividual ind : values) {
                        System.out.println(" " + ind);
                    }
                }
            }

        } catch (OWLOntologyCreationException ex) {
            log.error(ex);
        }
        return map;
    }
}

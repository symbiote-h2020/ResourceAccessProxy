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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.apache.jena.assembler.Assembler.reasonerFactory;
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
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

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
                if(owlDatatype.getIRI().toString().equals("http://www.symbiote-h2020.eu/ontology/core#InputParameter"))
                    breakPP = "TRUE";
                
                
                System.out.println("\t +PROPRIETA': " );
                for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSubClass(c))
                {
                        Set<OWLClass> associatedTerms = new HashSet<OWLClass>();
                        OWLClassExpression expression = axiom.getSuperClass();
                        
                        if (expression.isAnonymous())
                        {
                            String typeClass ="";
                            String namePro="";
                                    
                            Stream<OWLDataProperty> owlDataPropertyStream = expression.dataPropertiesInSignature();
                            Optional<OWLDataProperty> owlDataPropertyOptional = owlDataPropertyStream.findFirst();
                            if(owlDataPropertyOptional.isPresent())
                                namePro = owlDataPropertyOptional.get().getIRI().getShortForm();      
                            
                            Stream<OWLObjectProperty> owlObjectPropertyStream = expression.objectPropertiesInSignature();
                            Optional<OWLObjectProperty> owlObjectPropertyOptional = owlObjectPropertyStream.findFirst();
                            if(owlObjectPropertyOptional.isPresent())
                                namePro = owlObjectPropertyOptional.get().getIRI().getShortForm();                                  
                                    
                            
                            
                            Stream<OWLDatatype> owlDatatypeStream = expression.datatypesInSignature();
                            Optional<OWLDatatype> owlDatatypeOptional = owlDatatypeStream.findFirst();
                            if(owlDatatypeOptional.isPresent())
                                typeClass = owlDatatypeOptional.get().getIRI().getShortForm();
                            
                            Stream<OWLClass> owlClassStream = expression.classesInSignature();
                            Optional<OWLClass> owlClassOptional = owlClassStream.findFirst();
                            if(owlClassOptional.isPresent())
                                typeClass = owlClassOptional.get().getIRI().getShortForm();
                            
                            
                            System.out.println("\t\t +: " + typeClass + " " + namePro);
                        }
                }
                
                Iterator<OWLSubClassOfAxiom> axiomIterator = ontology.subClassAxiomsForSuperClass(c).iterator();
                while (axiomIterator.hasNext())
                {
                    OWLSubClassOfAxiom axiom = axiomIterator.next();
                        Set<OWLClass> associatedTerms = new HashSet<OWLClass>();
                        OWLClassExpression expression = axiom.getSuperClass();
                        System.out.println("\t +PROPRIETA': " );
                        if (expression.isAnonymous())
                        {
                            String type ="";
                            String namePro="";
                            for (OWLObjectProperty property : expression.getObjectPropertiesInSignature())
                            {
                                type = property.toStringID();
                            }
                            for (OWLClass associatedClass : expression.getClassesInSignature()){
                                namePro = associatedClass.toStringID();
                            }
                            System.out.println("\t\t +: " + type + " "+namePro);
                        }
                        alternativeDefinitions.add(associatedTerms);
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
                key2values.put("SUPERCLASS", subClassList);
                
                
                System.out.println(" \tObject Property");
                Iterator<OWLObjectProperty> objProIterator =cls.objectPropertiesInSignature().iterator();
                while(objProIterator.hasNext()){
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

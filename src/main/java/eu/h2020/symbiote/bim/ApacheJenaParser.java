/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.bim;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class ApacheJenaParser {
    private static final Log log = LogFactory.getLog(ApacheJenaParser.class);

    private static final String CIM_FILE_OLD = "/core-v0.6.owl";
    private static final String CIM_FILE = "/core-v1.0.owl";
    private static final String BIM_FILE = "/bim-0.3.owl";
    private static final String TIME_FILE = "/time.owl";
    
    public HashMap<String,HashMap<String,String>> test1() throws FileNotFoundException{
        HashMap<String,HashMap<String,String>> map = new HashMap<String,HashMap<String,String>>();
        String pathName = OwlapiHelp.class.getResource(CIM_FILE).getPath();
        File file = new File(pathName);
        FileReader reader = new FileReader(file);
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream(pathName),null,RDFFormat.Turtle.toString());
        
        StmtIterator iter = model.listStatements();

        // print out the predicate, subject and object of each statement
        while (iter.hasNext()) {
            Statement stmt      = iter.nextStatement();  // get next statement
            Resource  subject   = stmt.getSubject();     // get the subject
            Property  predicate = stmt.getPredicate();   // get the predicate
            RDFNode   object    = stmt.getObject();      // get the object

            System.out.print(subject.toString());
            System.out.print(" " + predicate.toString() + " ");
            if (object instanceof Resource) {
               System.out.print(object.toString());
            } else {
                // object is a literal
                System.out.print(" \"" + object.toString() + "\"");
            }

            System.out.println(" .");
        } 
        
        System.out.println("******");
        
        OntModel ontModel = ModelFactory.createOntologyModel();
        ontModel.read(new FileInputStream(pathName),null,RDFFormat.Turtle.toString());
        
        ExtendedIterator<OntClass> classesIterator = ontModel.listClasses();
        while(classesIterator.hasNext()){
            OntClass ontClass = classesIterator.next();
            System.out.println(ontClass.getLocalName());
            
            ExtendedIterator<OntProperty> ontPropertyIterator = ontClass.listDeclaredProperties();
            System.out.println("\t OntProperty:");
            while(ontPropertyIterator.hasNext()){
                
                OntProperty ontProperty = ontPropertyIterator.next();
                
                OntResource ontresource = ontProperty.getDomain();
                Resource res = ontProperty.getRDFType();
                OntResource ontresource2 = ontProperty.getRange();
                
                System.out.println("\t\t " + ontProperty.toString());
            }
            
            StmtIterator stmtPropertyIterator = ontClass.listProperties();
            System.out.println("\t StmtIterator:");
            while(stmtPropertyIterator.hasNext()){
                Statement statementPro = stmtPropertyIterator.next();
                System.out.println("\t\t " + statementPro.toString());
            }
            
        }
        
        return map;
    }
}

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
import org.apache.jena.query.spatial.EntityDefinition;
import org.apache.jena.query.spatial.SpatialDatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateRequest;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    //For tests only - in memory
    public TripleStore() {
        currentDirectory = null;

        EntityDefinition entDef = new EntityDefinition("entityField", "geoField");

        Dataset baseDataset = DatasetFactory.create();

        Directory ramDir = new RAMDirectory();

        dataset = null;
        //dataset = SpatialDatasetFactory.createLucene(baseDataset, ramDir, entDef);
        try {
            /*
            String cim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(CIM_FILE));
            insertGraph("", cim_data, RDFFormat.Turtle);
            */
            
            String bim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(BIM_FILE));
            insertGraph("", bim_data, RDFFormat.Turtle);
            
            /*
            String mim_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(MIM_FILE));
            insertGraph("", mim_data, RDFFormat.Turtle);

            String qureq20_data = IOUtils.toString(TripleStore.class
                    .getResourceAsStream(QU_FILE));
            insertGraph("", qureq20_data, RDFFormat.RDFXML);
            */

        } catch (IOException e) {
            log.fatal("Could not load CIM file: " + e.getMessage(), e);
        }
    }


    /*
    public TripleStore( String directory ) {
        boolean newRepo = false;
        currentDirectory = directory;

        EntityDefinition entDef = new EntityDefinition("entityField", "geoField");

        File dir = new File( directory + (directory.endsWith("/")?"":"/") + SPATIAL_REPO );

        if( !dir.exists() ) {
            dir.mkdirs();
        }

        String baseRepoLocation = directory + (directory.endsWith("/") ? "" : "/") + BASE_REPO;
        File baseDir = new File( baseRepoLocation );
        if( !baseDir.exists() ) {
            baseDir.mkdirs();

            newRepo = true;
        }
        Dataset baseDataset= TDBFactory.createDataset(baseRepoLocation);
        Directory realDir = null;
        try {
            realDir = FSDirectory.open(dir);
        } catch (IOException e) {
            log.error(e);
        }

        dataset = SpatialDatasetFactory.createLucene(baseDataset, realDir, entDef);
        if( newRepo ) {
            try {
                String cim_data = IOUtils.toString(TripleStore.class
                        .getResourceAsStream(CIM_FILE));
                insertGraph("", cim_data, RDFFormat.Turtle);

                String bim_data = IOUtils.toString(TripleStore.class
                        .getResourceAsStream(BIM_FILE));
                insertGraph("", bim_data, RDFFormat.Turtle);

                String mim_data = IOUtils.toString(TripleStore.class
                        .getResourceAsStream(MIM_FILE));
                insertGraph("", mim_data, RDFFormat.Turtle);

                String qureq20_data = IOUtils.toString(TripleStore.class
                        .getResourceAsStream(QU_FILE));
                insertGraph("", qureq20_data, RDFFormat.RDFXML);

//                printDataset();
            } catch (IOException e) {
                log.fatal("Could not load CIM file: " + e.getMessage(),e);
            }
        }
    }
    */
    
    private static void deleteOldFiles(File indexDir) {
        if (indexDir.exists())
            emptyAndDeleteDirectory(indexDir);
    }

    private static void emptyAndDeleteDirectory(File dir) {
        File[] contents = dir.listFiles() ;
        if (contents != null) {
            for (File content : contents) {
                if (content.isDirectory()) {
                    emptyAndDeleteDirectory(content) ;
                } else {
                    content.delete() ;
                }
            }
        }
        dir.delete() ;
    }

    public void executeQueryPP( String query ) {

        log.debug("QUERY: ---------------");
        log.debug(query);
        log.debug("----------------------");

        Query q = QueryFactory.create(query);
        QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
        QueryExecUtils.executeQuery(q, qexec);

    }


    public void insertGraph(String uri, String rdf, RDFFormat format) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(rdf.getBytes()), null, format.toString());
        insertGraph(uri, model);
    }

    public void insertGraph(String uri, Model model) {
//        dataset.begin(ReadWrite.WRITE);
//        if (!dataset.containsNamedModel(uri)) {
//            dataset.addNamedModel(uri, ModelFactory.createDefaultModel());
//        }
//        dataset.getNamedModel(uri).add(model);
        dataset.begin(ReadWrite.WRITE);
        dataset.getDefaultModel().add(model);
        dataset.commit();
        dataset.end();
    }

    public ResultSet executeQuery(String queryString) {
        return executeQueryOnGraph(queryString, "urn:x-arq:UnionGraph");
    }

    public ResultSet executeQuery(Query query) {
        return executeQueryOnGraph(query, "urn:x-arq:UnionGraph");
    }

    public ResultSet executeQueryOnGraph(String queryString, String graphUri) {
        return executeQueryOnGraph(QueryFactory.create(queryString, Syntax.syntaxARQ), graphUri);
    }

    public void executeUpdate( UpdateRequest request ) {
        dataset.begin(ReadWrite.WRITE);
        UpdateAction.execute(request,dataset);
        dataset.commit();
        dataset.end();
    }

    public ResultSet executeQueryOnGraph(Query query, String graphUri) {
        dataset.begin(ReadWrite.READ);
//        Model model = dataset.containsNamedModel(graphUri)
//                ? dataset.getNamedModel(graphUri)
//                : dataset.getDefaultModel();
//        Model model = dataset.getDefaultModel();
        ResultSet result = null;
        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset) ) {
            result = ResultSetFactory.copyResults(qe.execSelect());
            dataset.end();
        }

        return result;
    }

    public Model getGraph(String graph) {
        dataset.begin(ReadWrite.READ);
        Model result = dataset.getNamedModel(graph);
        dataset.end();
        return result;
    }

    public Model getDefaultGraph() {
        dataset.begin(ReadWrite.READ);
        Model result = dataset.getDefaultModel();
        dataset.end();
        return result;
    }

    public String getGraphAsString(String graph, String syntax) {
        StringWriter out = new StringWriter();
        getGraph(graph).write(out, syntax);
        return out.toString();
    }

    public List<String> loadDataFromDataset() {
        List<String> data = new ArrayList<>();
        dataset.begin(ReadWrite.READ);
        Iterator<String> stringIterator = dataset.listNames();
        while (stringIterator.hasNext() ) {
            String s = stringIterator.next();
            log.debug( "Loaded dataset: " + s);
            data.add(s);
        }
        dataset.end();
        return data;
    }

    public void printDataset() {
        if( SHOULD_PRINT_DATASET ) {
            dataset.begin(ReadWrite.READ);
            Model result = dataset.getDefaultModel();
            result.write(System.out, "TURTLE");
            dataset.end();
        }
    }

    public String getGraphAsString(String graph) {
        return getGraphAsString(graph, "RDF/XML-ABBREV");
    }
}

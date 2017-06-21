/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.bim;
import org.apache.commons.io.FilenameUtils;
/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public enum RDFFormat {
    Turtle("TURTLE"),
    NTriples("NTRIPLES"),
    RDFXML("RDFXML"),
    N3("N3"),
    JSONLD("JSONLD");

    private final String name;

    RDFFormat(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static RDFFormat fromFilename(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        switch (ext) {
            case "ttl":
                return RDFFormat.Turtle;
            case "nt":
                return RDFFormat.NTriples;
            case "rdf":
            case "xml":
                return RDFFormat.RDFXML;
            case "n3":
                return RDFFormat.N3;
            case "jsonld":
                return RDFFormat.JSONLD;
            default:
                throw new IllegalArgumentException("unknown file extension '" + ext + "'");
        }
    }
}

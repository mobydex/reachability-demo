package org.aksw.mobydex.demo.domain;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class MobyDexVocab {
    public static final Property project = ResourceFactory.createProperty(MobyDexTerms.project);
    public static final Property projectId = ResourceFactory.createProperty(MobyDexTerms.projectId);
}

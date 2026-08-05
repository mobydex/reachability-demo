package org.aksw.mobydex.demo.domain;

import java.util.Collection;

import org.aksw.jenax.annotation.reprogen.Inverse;
import org.aksw.jenax.annotation.reprogen.Iri;
import org.apache.jena.rdf.model.Resource;

public interface Project
    extends Resource
{
    @Iri(MobyDexTerms.projectId)
    Integer getProjectId();

    @Iri(MobyDexTerms.project)
    @Inverse
    Collection<GridCell> getCells();
}

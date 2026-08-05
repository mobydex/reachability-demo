package org.aksw.mobydex.demo.domain;

import org.aksw.jenax.annotation.reprogen.Iri;
import org.aksw.jenax.model.geosparql.HasGeometry;

public interface GridCell
    extends HasGeometry
{
    @Iri(MobyDexTerms.project)
    Project getProject();

    @Iri(MobyDexTerms.cellId)
    Integer getCellId();
}

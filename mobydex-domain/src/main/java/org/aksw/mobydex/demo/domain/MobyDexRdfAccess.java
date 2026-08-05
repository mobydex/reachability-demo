package org.aksw.mobydex.demo.domain;

import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.rdf.model.Model;

public class MobyDexRdfAccess {
    public static Project getProject(Model projectModel) {
        return Iter.asStream(projectModel.listResourcesWithProperty(MobyDexVocab.projectId)
                .mapWith(r -> r.as(Project.class))).findFirst().orElse(null);
    }

    public static Stream<GridCell> streamGridCells(Model projectModel) {
        return Iter.asStream(projectModel.listResourcesWithProperty(MobyDexVocab.project)
                .mapWith(r -> r.as(GridCell.class)));
    }
}

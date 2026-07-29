package org.aksw.mobydex.demo.example;

import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.mobydex.demo.ConfigMobyDexDemo;
import org.aksw.mobydex.demo.MainPlaygroundMobyDex;
import org.aksw.mobydex.demo.backend.ComputationDao;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.JenaPluginMobyDexModel;
import org.aksw.mobydex.demo.domain.MobyDexRdfAccess;
import org.aksw.mobydex.demo.domain.Project;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sys.JenaSystem;

public class MainMobyDexDemoExamples {

    public static void main(String[] args) {
        JenaSystem.init();
        JenaPluginMobyDexModel.init();
        // testProjectLoad();
        // testComputationLoad();

        // testComputationLoad();
        Project project = testProjectLoad();
        testCompute(project);
    }

    public static Project testProjectLoad() {
        MobyDexRdfApi api = MobyDexRdfApi.get();
        Model projectModel = api.loadProjectGrid(2);

        Project project = MobyDexRdfAccess.getProject(projectModel);
        System.out.println(project + " --- cells: " + project.getCells().size());

//        List<GridCell> cells = MobyDexRdfAccess.streamGridCells(projectModel).toList();
//        for (GridCell cell : cells) {
//            System.out.println(String.format("project: %s, cellId: %s, wkt: %s",
//                    cell.getProject(), cell.getCellId(),
//                    cell.getOneGeometry().map(Geometry::getAsWKT).map(Object::toString).orElse("no wkt")));
//        }

        //RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
        return project;
    }

    public static void testComputationLoad() {
        MobyDexRdfApi api = MobyDexRdfApi.get();
        Resource r = api.loadComputation(2, 70, 271);
        RDFDataMgr.write(System.out, r.getModel(), RDFFormat.TURTLE_PRETTY);
    }


    public static void testCompute(Project project) {
        MobyDexRdfApi mobyDexApi = MobyDexRdfApi.get();

        // int projectId = 2;
        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
        Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(project, tagsFragment);

        ComputationDao dao = new ComputationDao(ConfigMobyDexDemo.newRestTemplate(), ConfigMobyDexDemo.baseUrl);
        System.out.println("Got projectId: " + dao.getProjectId(70));

        Resource originCell = mobyDexApi.loadComputation(2, 70, 271);

        int i = 0;
        for (GridCell projectGridCell : project.getCells()) {
            if (i > 5) {
                break;
            }
            ++i;

            System.out.println("Table for " + projectGridCell.toString());
            Table table = OsmRdfApi.createQueryPoiTypeInRange(projectGridCell, poiTypeHistogramModel, tagsFragment, 1, MainPlaygroundMobyDex.durationProperty);
            RowSetOps.out(System.out, table.toRowSet());
        }
    }
}

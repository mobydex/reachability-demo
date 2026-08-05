package org.aksw.mobydex.demo.example;

import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.mobydex.demo.ConfigMobyDexDemo;
import org.aksw.mobydex.demo.MainPlaygroundMobyDex;
import org.aksw.mobydex.demo.backend.ComputationDao;
import org.aksw.mobydex.demo.backend.FileCache;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.aksw.mobydex.demo.backend.ProjectDao;
import org.aksw.mobydex.demo.backend.loader.GridComputationLoadTask;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.MobyDexRdfAccess;
import org.aksw.mobydex.demo.domain.Project;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.exec.RowSetOps;

public class MainMobyDexDemoExamples {

    public static void main(String[] args) {
        // JenaSystem.init();
        // JenaPluginMobyDexModel.init();
        // testProjectLoad();
        // testComputationLoad();

        // testComputationLoad();
        Project project = testProjectLoad();
        // testCompute(project);
        testAsyncLoad(project);
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


    public static void testAsyncLoad(Project project) {
        MobyDexRdfApi mobyDexApi = MobyDexRdfApi.get();
        FileCache fileCache = mobyDexApi.getFileCache();

        //MobyDexRdfApi mobyDexApi, Model poiTypeHistogramModel, Fragment2 tagsFragment
        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
        Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(project, tagsFragment);
        GridComputationLoadTask task = new GridComputationLoadTask(fileCache, 1, project, 70, mobyDexApi, poiTypeHistogramModel, tagsFragment);

        task.startBackgroundLoading();
    }

    public static void testCompute(Project project) {
        MobyDexRdfApi mobyDexApi = MobyDexRdfApi.get();

        // int projectId = 2;
        // int projectId = project.getProjectId();
        long computationId = 70;
        ProjectDao projectDao = new ProjectDao(ConfigMobyDexDemo.newRestTemplate(), ConfigMobyDexDemo.baseUrl);
        // long projectId = projectDao.fetchItem(computationId).id();

        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
        Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(project, tagsFragment);

        ComputationDao computationDao = new ComputationDao(ConfigMobyDexDemo.newRestTemplate(), ConfigMobyDexDemo.baseUrl);
        long projectId = computationDao.getProjectId(computationId);
        System.out.println("Got projectId: " + projectId);

        // Resource originCell = mobyDexApi.loadComputation(2, 70, 271);

        // PriorityExecutor priorityExecutor = new PriorityExecutor(0);
        // CompletionService<Long> service = new ExecutorCompletionService<>(priorityExecutor);


        int i = 0;

        for (GridCell projectGridCell : project.getCells()) {
            if (i > 5) {
                break;
            }
            ++i;

            //int projectId = projectGridCell.getProject().getProjectId();
            /// projectGridCell.get
            Resource originCell = mobyDexApi.loadComputation(projectId, 70, projectGridCell.getCellId());

            System.out.println("Table for " + projectGridCell.toString());
            Table table = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, MainPlaygroundMobyDex.durationProperty);
            RowSetOps.out(System.out, table.toRowSet());

            Table tags = tagsFragment.rename("cp", "co").toTable();

            // Table tags = evaluator.project(tagsFragment.extractTable(), List.of(Var.alloc("key"), Var.alloc("value")));

            float ratio = OsmRdfApi.getReachableWithinThresholdRatio(table, tags, 1800);
            System.out.println("Ratio for cell: " + originCell + " " + ratio);
        }
    }
}

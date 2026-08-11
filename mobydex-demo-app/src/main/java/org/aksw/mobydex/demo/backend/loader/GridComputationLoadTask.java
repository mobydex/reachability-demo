package org.aksw.mobydex.demo.backend.loader;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.mobydex.demo.backend.FileCache;
import org.aksw.mobydex.demo.backend.FileCaches;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.MobyDexRdfApiRaw;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.Project;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.algebra.Table;

import io.reactivex.rxjava3.core.Flowable;

public class GridComputationLoadTask
    // implements Runnable
{
    private FileCache fileCache;
    //private Project project;
    // private long projectId;
    private long computationId;

    private Model poiTypeHistogramModel;
    private Fragment2 tagsFragment;

    private Project project;
    private MobyDexRdfApi mobyDexApi;

    private BackgroundLoadingMap<Node, Table> map = null;

    private Map<Node, Integer> cellNodeToId;
    private Map<Node, GridCell> cellNodeToRes;

    private List<GridCell> gridCells;

    public GridComputationLoadTask(FileCache fileCache, int nThreads, Project project, List<GridCell> gridCells, long computationId, MobyDexRdfApi mobyDexApi, Model poiTypeHistogramModel, Fragment2 tagsFragment) {
        this.fileCache = fileCache;
        this.project = project;
        this.computationId = computationId;
        this.mobyDexApi = mobyDexApi;
        this.poiTypeHistogramModel = poiTypeHistogramModel;
        this.tagsFragment = tagsFragment;

        // List<GridCell> gridCells = new ArrayList<>(project.getCells());
        this.gridCells = gridCells;
        // List<Node> gridCells = new ArrayList<>(project.getCells());
        this.cellNodeToId = gridCells.stream()
                .collect(Collectors.toMap(GridCell::asNode, GridCell::getCellId));

        this.cellNodeToRes = gridCells.stream()
                .collect(Collectors.toMap(GridCell::asNode, x -> x));

        this.map = new BackgroundLoadingMap<>(nThreads, cellNodeToId.keySet(), cellNode -> {
            // Node cellNode = gridCell.asNode();
            int cellId = cellNodeToId.get(cellNode);
            return fetchCell(cellId);
        });
    }

    public BackgroundLoadingMap<Node, Table> getMap() {
        return map;
    }

    public long getTotalTasks() {
        // return project.getCells().size();
        return gridCells.size();
    }

    public long getCompletedTasks() {
        return map.getCache().asMap().size();
    }

    public Table getCell(Node cellNode) {
        return map.getIfPresent(cellNode);
    }

    public static List<String> createKeyPoiTable(long projectId, long computationId, long originCellId) {
        List<String> cellKey = MobyDexRdfApi.getCellsKey(projectId, computationId)
            .add("cell" + originCellId + ".reachablePois.srj.bz2")
            .buildList();
        return cellKey;
    }

    protected Table fetchCell(long cellId) {
        long projectId = project.getProjectId();
        List<String> poiTableKey = createKeyPoiTable(projectId, computationId, cellId);
        return FileCaches.loadTable(fileCache, poiTableKey, () -> {
            Resource originCell = mobyDexApi.loadComputation(projectId, computationId, cellId);
            Table table = loadTableX(originCell);
            return table;
        });
    }

    public Table loadTableX(Resource originCell) {
        // System.out.println("Table for " + projectGridCell.toString());
        Table table = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, MobyDexRdfApiRaw.durationProperty);
        // RowSetOps.out(System.out, table.toRowSet());
        return table;
//      if (Thread.currentThread().isInterrupted()) {
////    throw new InterruptedException();
//    throw new RuntimeException("interrupted");
//}

// Project project = gridCell.getProject();
// long projectId = project.getProjectId();
//int projectId = projectGridCell.getProject().getProjectId();
/// projectGridCell.get
    }

    public GridComputationLoadTask load(FileCache fileCache, Project project, List<GridCell> gridCells, long computationId, Model poiTypeHistogram, Fragment2 tagsFragment) {
        MobyDexRdfApi mobyDexApi = MobyDexRdfApi.get();
        int nThreads = Runtime.getRuntime().availableProcessors();
        GridComputationLoadTask task = new GridComputationLoadTask(fileCache, nThreads, project, gridCells, computationId, mobyDexApi, poiTypeHistogram, tagsFragment);
        return task;
    }

    public Flowable<Entry<GridCell, Table>> flow() {
        return map.flow().map(e -> Map.entry(cellNodeToRes.get(e.getKey()), e.getValue()));
    }
//
//    public void run(Emitter<GridCell> emitter) {
//        if (taskState == null) {
//            Consumer<GridCell> loader = projectGridCell -> {
//                Node cellNode = projectGridCell.asNode();
//                long cellId = projectGridCell.getCellId();
//                PrioritizedFutureTask<Table> task = priorityExecutor.submit(cellNode, PRIO_BACKGROUND,
//                        () -> {
//                            try {
//                                return loadCell(cellId, cellNode).get();
//                            } catch (Exception e) {
//                                throw new RuntimeException(e);
//                            }
//                        });
//                return task.asCompletableFuture();
////                task.asCompletableFuture().whenComplete((table, ex) -> {
////                    try {
////                        // emitter.onNext(Map.entry(projectGridCell, table));
////                        emitter.onNext(projectGridCell);
////                    } finally {
////                        System.out.println("Loading complete: " + projectGridCell);
////                        long count = counter.decrementAndGet();
////                        if (count == 0) {
////                            emitter.onComplete();
////                        }
////                    }
////                });
//            };
//
//
//            Stream<GridCell> taskStream = project.getCells().stream();
//            taskStream = taskStream.limit(50);
//            // taskIterator = new ArrayList<>((Iterable<GridCell>)() -> taskIterator).subList(0, 50).iterator();
//            taskState = new BackgroundLoadingMap<>(taskStream.iterator(), loader);
//        }
//
//        int i = 0;
//
//        Iterator<GridCell> taskIterator = project.getCells().iterator();
//
//        AtomicLong counter = new AtomicLong();
//        counter.incrementAndGet();
//        try {
//            for (GridCell projectGridCell : project.getCells()) {
//                if (i > 50) {
//                    break;
//                }
//                ++i;
//
//                counter.incrementAndGet();
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//                Node cellNode = projectGridCell.asNode();
//                long cellId = projectGridCell.getCellId();
//
//
//                // PrioritizedFutureTask<Table> task = priorityExecutor.submit(cellId, PRIO_BACKGROUND, () -> loadCell(cellId));
//            }
//        } finally {
//            long count = counter.decrementAndGet();
//            if (count == 0) {
//                emitter.onComplete();
//            }
//        }
//        priorityExecutor.getExecutor().shutdown();
//        try {
//            priorityExecutor.getExecutor().awaitTermination(10, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
////            //int projectId = projectGridCell.getProject().getProjectId();
////            /// projectGridCell.get
////            Resource originCell = mobyDexApi.loadComputation(projectId, 70, projectGridCell.getCellId());
////
////            System.out.println("Table for " + projectGridCell.toString());
////            Table table = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, MainPlaygroundMobyDex.durationProperty);
////            RowSetOps.out(System.out, table.toRowSet());
////
////            Table tags = tagsFragment.rename("cp", "co").toTable();
////
////            // Table tags = evaluator.project(tagsFragment.extractTable(), List.of(Var.alloc("key"), Var.alloc("value")));
////
////            float ratio = OsmRdfApi.getReachableWithinThresholdRatio(table, tags, 1800);
////            System.out.println("Ratio for cell: " + originCell + " " + ratio);
//
//    }
}

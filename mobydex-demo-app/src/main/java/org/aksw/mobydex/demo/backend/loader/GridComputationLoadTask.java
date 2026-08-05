package org.aksw.mobydex.demo.backend.loader;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.mobydex.demo.MainPlaygroundMobyDex;
import org.aksw.mobydex.demo.backend.FileCache;
import org.aksw.mobydex.demo.backend.FileCaches;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.aksw.mobydex.demo.backend.loader.PriorityExecutor.PrioritizedFutureTask;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.Project;
import org.aksw.shellgebra.exec.ListBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.algebra.Table;

public class GridComputationLoadTask
    implements Runnable
{
    public static final int PRIO_BACKGROUND = 0;
    public static final int PRIO_FOREGROUND = 1;

    // Perhaps include copmutation in key? so cell + computation -> value
    // private List<?> items;
    // private Caffeine<Node, Table> cellCache;
    private AsyncCache<Node, Table> cache;

    private FileCache fileCache;

    private PriorityExecutor<Node> priorityExecutor;

    //private Project project;
    // private long projectId;
    private long computationId;

    private Model poiTypeHistogramModel;
    private Fragment2 tagsFragment;

    private Project project;
    private MobyDexRdfApi mobyDexApi;

    public GridComputationLoadTask(FileCache fileCache ,int nThreads, Project project, long computationId, MobyDexRdfApi mobyDexApi, Model poiTypeHistogramModel, Fragment2 tagsFragment) {
        this.priorityExecutor = new PriorityExecutor<>(nThreads);

        this.fileCache = fileCache;
        cache = Caffeine.newBuilder().executor(priorityExecutor.getExecutor()).buildAsync();

        this.project = project;
        this.computationId = computationId;
        this.mobyDexApi = mobyDexApi;
        this.poiTypeHistogramModel = poiTypeHistogramModel;
        this.tagsFragment = tagsFragment;
    }

    public long getTotalTasks() {
        return project.getCells().size();
    }

    public long getCompletedTasks() {
        return cache.asMap().size();
    }

    protected CompletableFuture<Table> loadCell(long cellId, Node cellNode) {
        // Try to update the priority of an already queued task
        priorityExecutor.updatePriority(cellNode, PRIO_FOREGROUND);
        return loadCell(cellId, cellNode, PRIO_FOREGROUND);
    }

    protected CompletableFuture<Table> loadCell(long cellId, Node cellNode, int prio) {
        CompletableFuture<Table> result = cache.get(cellNode, (cn, executor) -> {
            PrioritizedFutureTask<Table> task = priorityExecutor.submit(cellNode, prio, () -> fetchCell(cellId));
            return task.asCompletableFuture();
        });
        return result;
    }

    public static List<String> createKeyPoiTable(long projectId, long computationId, long originCellId) {
        List<String> cellKey = ListBuilder.ofStrings(MobyDexRdfApi.getProjectGridKey(projectId))
            .add("computation" + computationId)
            .add("cell" + originCellId)
            .add("reachablePois.srj.bz2")
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
        Table table = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, MainPlaygroundMobyDex.durationProperty);
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

    public GridComputationLoadTask load(FileCache fileCache, Project project, long computationId, Model poiTypeHistogram, Fragment2 tagsFragment) {
        MobyDexRdfApi mobyDexApi = MobyDexRdfApi.get();
        int nThreads = Runtime.getRuntime().availableProcessors();
        GridComputationLoadTask task = new GridComputationLoadTask(fileCache, nThreads, project, computationId, mobyDexApi, poiTypeHistogram, tagsFragment);
        return task;
    }

    private Thread backgroundLoadThread = null;

    public synchronized void startBackgroundLoading() {
        if (backgroundLoadThread != null) {
            throw new IllegalStateException("thread already started");
        }
        Thread backgroundLoadThread = new Thread(this::run);
        backgroundLoadThread.start();
//        Executor executor = priorityExecutor.getExecutor();
//        CompletableFuture<?> backgroundLoader = CompletableFuture.runAsync(this::run, executor);
    }
    public synchronized void stopBackgroundLoading() {
        backgroundLoadThread.interrupt();
        try {
            backgroundLoadThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        backgroundLoadThread = null;
    }

    @Override
    public void run() {
        int i = 0;
        for (GridCell projectGridCell : project.getCells()) {
            if (i > 50) {
                break;
            }
            ++i;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Node cellNode = projectGridCell.asNode();
            long cellId = projectGridCell.getCellId();
            priorityExecutor.submit(cellNode, PRIO_BACKGROUND, () -> loadCell(cellId, cellNode));
            // PrioritizedFutureTask<Table> task = priorityExecutor.submit(cellId, PRIO_BACKGROUND, () -> loadCell(cellId));
        }
        priorityExecutor.getExecutor().shutdown();
        try {
            priorityExecutor.getExecutor().awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

//            //int projectId = projectGridCell.getProject().getProjectId();
//            /// projectGridCell.get
//            Resource originCell = mobyDexApi.loadComputation(projectId, 70, projectGridCell.getCellId());
//
//            System.out.println("Table for " + projectGridCell.toString());
//            Table table = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, MainPlaygroundMobyDex.durationProperty);
//            RowSetOps.out(System.out, table.toRowSet());
//
//            Table tags = tagsFragment.rename("cp", "co").toTable();
//
//            // Table tags = evaluator.project(tagsFragment.extractTable(), List.of(Var.alloc("key"), Var.alloc("value")));
//
//            float ratio = OsmRdfApi.getReachableWithinThresholdRatio(table, tags, 1800);
//            System.out.println("Ratio for cell: " + originCell + " " + ratio);

    }
}

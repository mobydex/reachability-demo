package org.aksw.mobydex.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.aksw.commons.io.util.PathUtils;
import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.shellgebra.exec.ListBuilder;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.graph.GraphReadOnly;

public class MobyDexRdfApi
{
    private Cache<List<String>, Object> cache;
    private Path basePath;

    public MobyDexRdfApi() throws IOException {
        basePath = Path.of(System.getProperty("user.home")).resolve(".cache/mobydex");
        Files.createDirectories(basePath);

        cache = Caffeine.newBuilder().maximumSize(10000).build();
    }

    public static List<String> getProjectGridKey(long projectId) {
        return List.of("projects", Long.toString(projectId));
    }

    public Model loadProjectGrid(long projectId) {
        List<String> projectKey = ListBuilder.ofStrings(getProjectGridKey(projectId)).add("grid.ttl").buildList();
        Model result = loadModel(projectKey, () -> MobyDexRdfApiRaw.loadProjectGrid(projectId));
        return result;
    }

    /** Generic cached model loading. */
    public Model loadModel(List<String> key, Callable<Model> creator) {
        Model result = (Model)cache.get(key, k -> {
            Model r;
            Path path = PathUtils.resolve(basePath, key);
            if (Files.exists(path)) {
                r = RDFDataMgr.loadModel(path.toString());
            } else {
                Path tmpFile;
                try {
                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }
                    tmpFile = Files.createTempFile("model-", ".ttl");
                    r = creator.call();
                    IOX.safeWriteOrCopy(path, tmpFile, out -> RDFDataMgr.write(out, r, RDFFormat.TURTLE_BLOCKS));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return readOnly(r);
        });
        return result;
    }

    public static Model readOnly(Model model) {
        Graph graph = model.getGraph();
        Model result = graph instanceof GraphReadOnly
            ? model
            : ModelFactory.createModelForGraph(new GraphReadOnly(graph));
        return result;
    }

    public Model loadPoiHistogramModel(long projectId, Fragment2 tagsFragment) {
        List<String> poiKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("pois.ttl").buildList();

        Model r = loadModel(poiKey, () -> {
            Model projectGridModel = loadProjectGrid(projectId);

            Table cellIdAndGeoms = QueryExec.graph(projectGridModel.getGraph())
                    .query("""
                        PREFIX geo: <http://www.opengis.net/ont/geosparql#>
                        SELECT ?s ?wkt { ?s geo:hasGeometry/geo:asWKT ?wkt }
                    """).table();
            Fragment2 cellFragment = Fragment.of(cellIdAndGeoms).toFragment2();

            // Resource originCell = loadComputation(projectId, computationId, originCellId);

            // Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
            Query poiTypeHistogramQuery = OsmRdfApi.createQueryExportPoiHistogram(cellFragment, tagsFragment);
            // System.out.println(q3);

            Model poiTypeHistogramModel = QueryExecutionHTTP.service("https://data.aksw.org/mobydex")
                .query(poiTypeHistogramQuery)
                .construct();
            return poiTypeHistogramModel;
        });
        return r;
    }


    public Resource loadComputation(long projectId, long computationId, long originCellId) {
        List<String> computationKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("computation" + computationId)
                .add("cell" + originCellId + ".ttl").buildList();
        Resource result = (Resource)cache.get(computationKey, k -> {
            Resource r;
            Path path = PathUtils.resolve(basePath, computationKey);
            if (Files.exists(path)) {
                Model m = RDFDataMgr.loadModel(path.toString());
                String id = MobyDexRdfApiRaw.createOriginCellId(projectId, computationId, originCellId);
                r = m.createResource(id);
            } else {
                Path tmpFile;
                try {
                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }
                    tmpFile = Files.createTempFile("computation" + computationId, ".ttl");
                    r = MobyDexRdfApiRaw.loadComputation(projectId, computationId, originCellId);
                    IOX.safeWriteOrCopy(path, tmpFile, out -> RDFDataMgr.write(out, r.getModel(), RDFFormat.TURTLE_BLOCKS));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return r;
        });
        return result;
    }
}

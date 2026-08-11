package org.aksw.mobydex.demo.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.frontendtools.internal.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import org.aksw.commons.io.util.PathUtils;
import org.aksw.jenax.dataaccess.sparql.creator.FileSets;
import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.mobydex.demo.domain.Computation;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.MobyDexRdfAccess;
import org.aksw.mobydex.demo.domain.Project;
import org.aksw.shellgebra.exec.ListBuilder;
import org.aksw.vaadin.jena.geo.leafletflow.JtsUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/** Caching wrapper (in-memory + disk). */
public class MobyDexRdfApi {
    // Need separate caches to prevent recursive updates!
    private FileCache projectCache;
    private FileCache computationCache;
    private FileCache cellCache;

    protected MobyDexRdfApi(Path cachePath) {
        super();
        this.projectCache = new FileCache(cachePath);
        this.computationCache = new FileCache(cachePath);
        this.cellCache = new FileCache(cachePath);
    }

    public static MobyDexRdfApi of(Path cachePath) {
        return new MobyDexRdfApi(cachePath);
    }

    public FileCache getProjectCache() {
        return projectCache;
    }

    public FileCache getComputationCache() {
        return projectCache;
    }

    public FileCache getCellCache() {
        return cellCache;
    }

    @Deprecated
    public static MobyDexRdfApi get() {
        Path cachePath = Path.of(System.getProperty("user.home")).resolve(".cache/mobydex");
        return of(cachePath);
    }

    public static ListBuilder<String> getProjectGridKey(long projectId) {
        return ListBuilder.ofStrings("projects", Long.toString(projectId));
    }

    public static ListBuilder<String> getComputationKey(long projectId, long computationId) {
        return getProjectGridKey(projectId).add("computations").add(Long.toString(computationId));
    }

//    public static ListBuilder<String> getCellKey(long projectId, long computationId, long cellId) {
//        return getComputationKey(projectId, computationId).add("cells").add(Long.toString(cellId));
//    }

    // Only returns the "/cells" folder for (project, computationId)
    public static ListBuilder<String> getCellsKey(long projectId, long computationId) {
        return getComputationKey(projectId, computationId).add("cells");
    }

    List<String> createKeyProjectGridFile(long projectId) {
        List<String> gridFileKey = getProjectGridKey(projectId).add("grid.ttl").buildList();
        return gridFileKey;
    }

    public Project loadProject(long projectId) {
        Model projectModel = loadProjectGrid(projectId);
        Project project = MobyDexRdfAccess.getProject(projectModel);
        return project;
    }

    public Model loadProjectGrid(long projectId) {
        List<String> projectKey = createKeyProjectGridFile(projectId);
        Model result = projectCache.loadModel(projectKey, () -> MobyDexRdfApiRaw.loadProjectGrid(projectId));
        return result;
    }

    public boolean isProjectGridLoaded(long projectId) {
        List<String> projectGridFileKey = createKeyProjectGridFile(projectId);
        Path path = PathUtils.resolve(projectCache.getCacheBasePath(), projectGridFileKey);
        return Files.exists(path);
    }


    public List<String> createKeyPoisFile(long projectId) {
        List<String> poiKey = getProjectGridKey(projectId).add("pois.ttl").buildList();
        return poiKey;
    }

    // HACK - this should be configurable per poi type.
    public boolean isProjectPoisLoaded(long projectId) {
        List<String> key = createKeyPoisFile(projectId);
        Path path = PathUtils.resolve(projectCache.getCacheBasePath(), key);
        return Files.exists(path);
    }

    /**
     * For a given project, annotate each cell with how many POIs
     * are contained in it.
     *
     * @param projectId
     * @param tagsFragment
     * @return
     */
    @Deprecated
    public Model loadAndCachePoiHistogramModel(long projectId, Fragment2 tagsFragment) {
        List<String> poiKey = getProjectGridKey(projectId)
                .add("pois.ttl").buildList();

        Model r = projectCache.loadModel(poiKey, () -> {
            Model projectGridModel = loadProjectGrid(projectId);
            Model poiTypeHistogramModel = loadPoiHistogramModel(projectGridModel, tagsFragment);
            return poiTypeHistogramModel;
        });
        return r;
    }

    public Model loadAndCachePoiHistogramModel(Project project, Fragment2 tagsFragment) {
        int projectId = Objects.requireNonNull(project.getProjectId());
        Model projectGridModel = project.getModel();

        List<String> poiKey = getProjectGridKey(projectId)
                .add("pois.ttl").buildList();

        Model r = projectCache.loadModel(poiKey, () -> {
            Model poiTypeHistogramModel = loadPoiHistogramModel(projectGridModel, tagsFragment);
            return poiTypeHistogramModel;
        });
        return r;
//        List<String> poiKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
//                .add("pois.ttl").buildList();
//
//        Model r = loadModel(poiKey, () -> {
//            Model projectGridModel = loadProjectGrid(projectId);
//            Model poiTypeHistogramModel = loadPoiHistogramModel(projectGridModel, tagsFragment);
//            return poiTypeHistogramModel;
//        });
//        return r;
    }

    public Model loadPoiHistogramModel(Model projectGridModel, Fragment2 tagsFragment) {
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

        System.out.println(poiTypeHistogramQuery);

        Model poiTypeHistogramModel = QueryExecutionHTTP.service("https://data.aksw.org/mobydex")
            .query(poiTypeHistogramQuery)
            .construct();
        return poiTypeHistogramModel;
    }


    public long getComputedCells(long projectId, long computationId) {
        List<String> computationKey = getCellsKey(projectId, computationId).buildList();
                // .add("cell" + originCellId + ".ttl")

        Path path = PathUtils.resolve(computationCache.getCacheBasePath(), computationKey);
        long result = FileSets.countFlat(path, "cell[0-9][0-9]*.data.json.bz2");
        return result;
    }

    public Computation loadComputationNew(long projectId, long computationId, long originCellId) throws IOException, InterruptedException {
        List<String> cellsKey = getCellsKey(projectId, computationId).buildList();
                // .add("cell" + originCellId).buildList();

        List<String> dataJsonBz2 = ListBuilder.ofStrings(cellsKey).add("cell" + originCellId + ".data.json.bz2").buildList();
        List<String> dataTrigBz2 = ListBuilder.ofStrings(cellsKey).add("cell" + originCellId + ".data.trig.bz2").buildList();

        String urlStr = MobyDexRdfApiRaw.buildComputationUrl(computationId, originCellId);
        URI uri = URI.create(urlStr);

        Path rdfPath = PathUtils.resolve(projectCache.getCacheBasePath(), dataTrigBz2);
        if (!Files.exists(rdfPath)) {
            Path rdfPathTmp = rdfPath.resolveSibling(rdfPath.getFileName() + ".tmp");
            Path jsonPath = PathUtils.resolve(projectCache.getCacheBasePath(), dataJsonBz2);
            Path jsonPathTmp = jsonPath.resolveSibling(jsonPath.getFileName() + ".tmp");
            // System.out.println("Writing" + filePath);

            if (!Files.exists(jsonPath)) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                try (HttpClient client = HttpClient.newHttpClient()) {
                    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    try (InputStream in = response.body()) {
                        JsonNode json = mapper.readTree(in);
                        try (OutputStream out = new BZip2CompressorOutputStream(Files.newOutputStream(jsonPathTmp))) {
                            mapper.writeValue(out, json);
                            out.flush();
                        }
                        IOX.moveAllowCopy(jsonPathTmp, jsonPath);
                    }
                }
            }

            String jsonStr;
            try (InputStream in = new BZip2CompressorInputStream(Files.newInputStream(jsonPath), true)) {
                jsonStr = IOUtils.toString(in, StandardCharsets.UTF_8);
            }

            Resource r = MobyDexRdfApiRaw.rdfizeComputationJson(projectId, computationId, originCellId, jsonStr);
            if (!r.isURIResource()) {
                throw new RuntimeException("Expected a URI resource (graph names in quad serializations must be URIs). Got: " + r);
            }
            DatasetGraph dsg = FileCaches.resourceToDataset(r);
            try (OutputStream out = new BZip2CompressorOutputStream(Files.newOutputStream(rdfPathTmp))) {
                RDFDataMgr.write(out, dsg, RDFFormat.TRIG_PRETTY);
                out.flush();
            }
            IOX.moveAllowCopy(rdfPathTmp, rdfPath);
        }

        // Jena handles bz2
        DatasetGraph dd = RDFDataMgr.loadDatasetGraph(rdfPath.toString());
        Resource resource = FileCaches.datasetToResource(dd);
        Computation result = resource.as(Computation.class);
        return result;
    }

    public static List<String> createCellKey(long projectId, long computationId, long originCellId) {
        List<String> cellKey = getProjectGridKey(projectId)
                .add("computation" + computationId)
                .add("cell" + originCellId + ".ttl").buildList();
        return cellKey;
    }

    public Resource loadComputation(long projectId, long computationId, long originCellId) {
        List<String> cellKey = createCellKey(projectId, computationId, originCellId);
        Resource result = (Resource)computationCache.getCache().get(cellKey, k -> {
            Resource r;
            try {
                r = loadComputationNew(projectId, computationId, originCellId);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            return r;
        });
        return result;

//        Resource result = (Resource)fileCache.getCache().get(computationKey, k -> {
//            Resource r;
//            Path path = PathUtils.resolve(fileCache.getCacheBasePath(), computationKey);
//            if (Files.exists(path)) {
//                Model m = RDFDataMgr.loadModel(path.toString());
//                String id = MobyDexRdfApiRaw.createOriginCellId(projectId, computationId, originCellId);
//                r = m.createResource(id);
//            } else {
//                Path tmpFile;
//                try {
//                    if (path.getParent() != null) {
//                        Files.createDirectories(path.getParent());
//                    }
//                    tmpFile = Files.createTempFile("computation" + computationId, ".ttl");
//                    r = loadComputationNew(projectId, computationId, originCellId);
//                    IOX.safeWriteOrCopy(path, tmpFile, out -> RDFDataMgr.write(out, r.getModel(), RDFFormat.TURTLE_BLOCKS));
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            return r;
//        });
//        return result;
    }

    public static Stream<Geometry> getCellGeometry(GridCell cell) {
        return Stream.of(cell)
                .map(GridCell::getHasGeometry)
                .flatMap(Collection::stream)
                .map(org.aksw.jenax.model.geosparql.Geometry::getAsGeometryWrapper)
                .filter(Objects::nonNull)
                .map(GeometryWrapper::getParsingGeometry)
                .filter(Objects::nonNull);
    }

    public static Geometry getUnionGeom(Project project) {
        Geometry result = project.getCells().stream()
                .flatMap(MobyDexRdfApi::getCellGeometry)
                .collect(JtsUtils.union());
            return result;
    }

    public static Envelope getEnvelope(Project project) {
        Envelope result = project.getCells().stream()
            .map(GridCell::getHasGeometry)
            .flatMap(Collection::stream)
            .map(org.aksw.jenax.model.geosparql.Geometry::getAsGeometryWrapper)
            .filter(Objects::nonNull)
            .map(GeometryWrapper::getParsingGeometry)
            .filter(Objects::nonNull)
            .collect(JtsUtils.unionEnvelopeGeometry());
        return result;
    }

}

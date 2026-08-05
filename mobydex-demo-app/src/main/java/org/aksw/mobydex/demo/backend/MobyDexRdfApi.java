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
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.frontendtools.internal.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import org.aksw.commons.io.util.PathUtils;
import org.aksw.jenax.dataaccess.sparql.creator.FileSets;
import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.mobydex.demo.domain.Computation;
import org.aksw.mobydex.demo.domain.Project;
import org.aksw.shellgebra.exec.ListBuilder;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

/** Caching wrapper (in-memory + disk). */
public class MobyDexRdfApi {
    private FileCache fileCache;

    public MobyDexRdfApi() {
        super();
        Path cachePath = Path.of(System.getProperty("user.home")).resolve(".cache/mobydex");
        this.fileCache = new FileCache(cachePath);
    }

    public FileCache getFileCache() {
        return fileCache;
    }

    public static MobyDexRdfApi get() {
        return new MobyDexRdfApi();
    }

    public static List<String> getProjectGridKey(long projectId) {
        return List.of("projects", Long.toString(projectId));
    }

    List<String> createKeyProjectGridFile(long projectId) {
        List<String> gridFileKey = ListBuilder.ofStrings(getProjectGridKey(projectId)).add("grid.ttl").buildList();
        return gridFileKey;
    }

    public Model loadProjectGrid(long projectId) {
        List<String> projectKey = createKeyProjectGridFile(projectId);
        Model result = fileCache.loadModel(projectKey, () -> MobyDexRdfApiRaw.loadProjectGrid(projectId));
        return result;
    }

    public boolean isProjectGridLoaded(long projectId) {
        List<String> projectGridFileKey = createKeyProjectGridFile(projectId);
        Path path = PathUtils.resolve(fileCache.getCacheBasePath(), projectGridFileKey);
        return Files.exists(path);
    }


    public List<String> createKeyPoisFile(long projectId) {
        List<String> poiKey = ListBuilder.ofStrings(getProjectGridKey(projectId)).add("pois.ttl").buildList();
        return poiKey;
    }

    // HACK - this should be configurable per poi type.
    public boolean isProjectPoisLoaded(long projectId) {
        List<String> key = createKeyPoisFile(projectId);
        Path path = PathUtils.resolve(fileCache.getCacheBasePath(), key);
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
        List<String> poiKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("pois.ttl").buildList();

        Model r = fileCache.loadModel(poiKey, () -> {
            Model projectGridModel = loadProjectGrid(projectId);
            Model poiTypeHistogramModel = loadPoiHistogramModel(projectGridModel, tagsFragment);
            return poiTypeHistogramModel;
        });
        return r;
    }

    public Model loadAndCachePoiHistogramModel(Project project, Fragment2 tagsFragment) {
        int projectId = Objects.requireNonNull(project.getProjectId());
        Model projectGridModel = project.getModel();

        List<String> poiKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("pois.ttl").buildList();

        Model r = fileCache.loadModel(poiKey, () -> {
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
        List<String> computationKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("computation" + computationId).buildList();
                // .add("cell" + originCellId + ".ttl")

        Path path = PathUtils.resolve(fileCache.getCacheBasePath(), computationKey);
        long result = FileSets.countFlat(path, "cell[0-9][0-9]*.ttl");
        return result;
    }

    public Computation loadComputationNew(long projectId, long computationId, long originCellId) throws IOException, InterruptedException {
        List<String> computationKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("computation" + computationId).buildList();
                // .add("cell" + originCellId).buildList();

        List<String> dataJsonBz2 = ListBuilder.ofStrings(computationKey).add("cell" + originCellId + ".data.json.bz2").buildList();
        List<String> dataTrigBz2 = ListBuilder.ofStrings(computationKey).add("cell" + originCellId + ".data.trig.bz2").buildList();

        String urlStr = MobyDexRdfApiRaw.buildComputationUrl(computationId, originCellId);
        URI uri = URI.create(urlStr);

        Path rdfPath = PathUtils.resolve(fileCache.getCacheBasePath(), dataTrigBz2);
        if (!Files.exists(rdfPath)) {
            Path rdfPathTmp = rdfPath.resolveSibling(rdfPath.getFileName() + ".tmp");
            Path jsonPath = PathUtils.resolve(fileCache.getCacheBasePath(), dataJsonBz2);
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
        List<String> cellKey = ListBuilder.ofStrings(getProjectGridKey(projectId))
                .add("computation" + computationId)
                .add("cell" + originCellId + ".ttl").buildList();
        return cellKey;
    }

    public Resource loadComputation(long projectId, long computationId, long originCellId) {
        List<String> cellKey = createCellKey(projectId, computationId, originCellId);
        Resource result = (Resource)fileCache.getCache().get(cellKey, k -> {
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
}

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
import org.aksw.commons.io.util.UriToPathUtils;
import org.aksw.jena_sparql_api.sparql.ext.json.RDFDatatypeJson;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphMapLink;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.vocabulary.RDF;

public class MobyDexRdfApiRaw {

    public static final Node durationProperty = NodeFactory.createURI("http://www.example.org/durationMin");

    public static Model loadProjectGrid(long projectId) {
        String queryStr = """
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX eg: <http://www.example.org/>
            PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX json: <https://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf#>
            PREFIX norse: <https://w3id.org/aksw/norse#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX url: <http://jsa.aksw.org/fn/url/>

            CONSTRUCT {
              ?cell eg:cellId ?cellId .
              # ?cell eg:projectId ?projectId .
              ?cell eg:project ?project .
              ?cell geo:hasGeometry ?cellGeom .
              ?cellGeom geo:asWKT ?polygon .

              ?project  a            eg:Project .
              ?project  eg:projectId ?projectId .
            }
            #SELECT ?cellId ?polygon ?polygonTooltip WHERE
            {
              BIND($PROJECT_ID AS ?projectId)
              BIND('WGS84' AS ?coordinateSystem)
              BIND(5 AS ?coordinatePrecision)
              # Lateral join - essentialy a for each loop.
              LATERAL { SERVICE <cache:> {
                BIND("https://mobydex.locoslab.com/controller-service/projects/" + STR(?projectId) + "/cells?coordinateSystem=" + STR(?coordinateSystem) + "&coordinatePrecision=" + STR(?coordinatePrecision) + "&pageOffset=0&pageSize=10000" AS ?url)
                BIND(STRDT(url:text(?url), norse:json) AS ?json)
              } }
              BIND(norse:json.get(?json, "elements") AS ?elements)
              ?elements norse:json.unnest ?cellJson .
              BIND(norse:json.get(?cellJson, "id") AS ?cellId)
              BIND("https://mobydex.locoslab.com/controller-service/projects/" + STR(?projectId) AS ?projectStr)
              BIND(IRI(?projectStr) AS ?project)
              BIND(IRI(concat(?projectStr + "#cell" + STR(?cellId))) AS ?cell)
              BIND(IRI(concat(?projectStr + "#cellGeom" + STR(?cellId))) AS ?cellGeom)
              BIND(norse:json.path(?cellJson, "$.bounds.coordinates") AS ?rawCoords)
              BIND(geof:parsePolyline(?rawCoords) AS ?linestring)
              # Note: fixStructure implies a WKT literal
              BIND(STRDT(norse:geo.wkt.fixStructure(REPLACE(STR(?linestring), "LINESTRING", "POLYGON (") + ")"), datatype(?linestring)) AS ?polygon)
              # BIND(STRDT(REPLACE(STR(?linestring), "LINESTRING", "POLYGON (") + ")", datatype(?linestring)) AS ?polygon)

              # For each routing cell, find the census cells

              # BIND(geof:buffer(?linestring, 1, uom:metre) AS ?polygon)
              BIND(STR(?cellId) AS ?polygonTooltip)
            }
            # LIMIT 10000
        """
            .replace("$PROJECT_ID", Long.toString(projectId))
        ;
        Model model = QueryExecution
            .dataset(DatasetFactory.empty())
            .query(queryStr)
            .construct();

        return model;
    }

    @Deprecated(forRemoval = true)
    public static Model loadProjectGridOld(long projectId) {
        String queryStr = """
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX eg: <http://www.example.org/>
            PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX json: <https://www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf#>
            PREFIX norse: <https://w3id.org/aksw/norse#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX url: <http://jsa.aksw.org/fn/url/>

            CONSTRUCT {
              ?cell eg:cellId ?cellId .
              ?cell eg:projectId ?projectId .
              ?cell geo:hasGeometry ?cellGeom .
              ?cellGeom geo:asWKT ?polygon
            }
            #SELECT ?cellId ?polygon ?polygonTooltip WHERE
            {
              BIND($PROJECT_ID AS ?projectId)
              BIND('WGS84' AS ?coordinateSystem)
              BIND(5 AS ?coordinatePrecision)
              # Lateral join - essentialy a for each loop.
              LATERAL { SERVICE <cache:> {
                BIND("https://mobydex.locoslab.com/controller-service/projects/" + STR(?projectId) + "/cells?coordinateSystem=" + STR(?coordinateSystem) + "&coordinatePrecision=" + STR(?coordinatePrecision) + "&pageOffset=0&pageSize=10000" AS ?url)
                BIND(STRDT(url:text(?url), norse:json) AS ?json)
              } }
              BIND(norse:json.get(?json, "elements") AS ?elements)
              ?elements norse:json.unnest ?cellJson .
              BIND(norse:json.get(?cellJson, "id") AS ?cellId)
              BIND("https://mobydex.locoslab.com/controller-service/projects/" + STR(?projectId) AS ?projectStr)
              BIND(IRI(concat(?projectStr + "#cell" + STR(?cellId))) AS ?cell)
              BIND(IRI(concat(?projectStr + "#cellGeom" + STR(?cellId))) AS ?cellGeom)
              BIND(norse:json.path(?cellJson, "$.bounds.coordinates") AS ?rawCoords)
              BIND(geof:parsePolyline(?rawCoords) AS ?linestring)
              # Note: fixStructure implies a WKT literal
              BIND(STRDT(norse:geo.wkt.fixStructure(REPLACE(STR(?linestring), "LINESTRING", "POLYGON (") + ")"), datatype(?linestring)) AS ?polygon)

              # For each routing cell, find the census cells

              # BIND(geof:buffer(?linestring, 1, uom:metre) AS ?polygon)
              BIND(STR(?cellId) AS ?polygonTooltip)
            }
            # LIMIT 10000
        """
            .replace("$PROJECT_ID", Long.toString(projectId))
        ;
        Model model = QueryExecution
            .dataset(DatasetFactory.empty())
            .query(queryStr)
            .construct();

        return model;
    }

    public static String buildComputationUrl(long computationId, long originCellId) {
        String baseUrl = "https://mobydex.locoslab.com/controller-service";
        return baseUrl + "/computations/" + computationId + "/directions?origins=" + originCellId + "&steps=false&routes=false";
    }


//    public InputStream streamFile(Path path, Supplier<InputStream> inSupplier) {
//
//    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int projectId = -1;
        int computationId = 70;
        int originCellId = 271;
        String urlStr = buildComputationUrl(computationId, originCellId);
        URI uri = URI.create(urlStr);

        String userHome = Objects.requireNonNull(System.getProperty("user.home"));
        Path cacheBase = Path.of(userHome).resolve(".cache").resolve("mobydex");
        Files.createDirectories(cacheBase);

        String[] segments = UriToPathUtils.toPathSegments(urlStr);
        Path fileFolder = PathUtils.resolve(cacheBase, segments);
        Files.createDirectories(fileFolder);

        Path rdfPath = fileFolder.resolve("data.trig.bz2");
        Path rdfPathTmp = rdfPath.resolveSibling(rdfPath.getFileName() + ".tmp");

        if (!Files.exists(rdfPath)) {
            Path jsonPath = fileFolder.resolve("data.json.bz2");
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

            Resource r = rdfizeComputationJson(0, 0, 0, jsonStr);
            if (!r.isURIResource()) {
                throw new RuntimeException("Expected a URI resource (graph names in quad serializations must be URIs). Got: " + r);
            }
            DatasetGraphMapLink dsg = new DatasetGraphMapLink(GraphFactory.createDefaultGraph());
            dsg.addGraph(r.asNode(), r.getModel().getGraph());

            try (OutputStream out = new BZip2CompressorOutputStream(Files.newOutputStream(rdfPathTmp))) {
                RDFDataMgr.write(out, dsg, RDFFormat.TRIG_PRETTY);
                out.flush();
            }
            IOX.moveAllowCopy(rdfPathTmp, rdfPath);
        }


        // Read the resource.
        System.out.println("Reading: " + rdfPath.toString());

        // TODO Should specificy explicit dataset graph during load.
        DatasetGraph dsgIn = RDFDataMgr.loadDatasetGraph(rdfPath.toString(), Lang.TRIG);
        List<Node> list = Iter.toList(dsgIn.listGraphNodes());
        // TODO There does not seem to be a closing "expectExactlyOneItem" util method.
        if (list.size() != 1) {
            throw new RuntimeException("Exactly 1 named graph expected");
        }
        Node g = list.getFirst();
        Graph graph = dsgIn.getGraph(g);
        Model m = ModelFactory.createModelForGraph(graph);
        Resource outR = m.asRDFNode(g).asResource();

        System.out.println(outR);
    }

    /**
     * Project is needed because it is part of the cell IRIs.
     *
     * Model example:
     * <pre>{@code
     * <https://mobydex.locoslab.com/controller-service/computations/70/directions?origins=271&steps=false&routes=false#dest=510>
     *   eg:computationId    70;
     *   eg:destinationCell  <https://mobydex.locoslab.com/controller-service/projects/2#cell510>;
     *   eg:durationAvg      7060.0;
     *   eg:durationMax      8390;
     *   eg:durationMedian   6966.0;
     *   eg:durationMin      6470;
     *   eg:originCell       <https://mobydex.locoslab.com/controller-service/projects/2#cell271>;
     *   eg:project          <https://mobydex.locoslab.com/controller-service/projects/2>;
     *   eg:url              <https://mobydex.locoslab.com/controller-service/> .
     * }</pre>
     *
     * @param projectId
     * @param computationId
     * @param originCellId
     * @return The resource that corresponds to the origin cell.
     */
    public static Resource rdfizeComputationJson(long projectId, long computationId, long originCellId, String jsonStr) {
        Node jsonRdfNode = NodeFactory.createLiteralDT(jsonStr, RDFDatatypeJson.get());

        String queryStr = """
            PREFIX eg: <http://www.example.org/>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX url: <http://jsa.aksw.org/fn/url/>
            PREFIX norse: <https://w3id.org/aksw/norse#>

            CONSTRUCT {
              ?s
                eg:url ?apiBaseUrl ;
                eg:computationId ?computationId ;
                # eg:projectId     ?projectId     ;
                eg:project         ?project ;
                # eg:originCellId ?originCellId ;
                # eg:destinationCellId ?destinationCellId ;
                eg:originCell ?originCell ;
                eg:destinationCell ?destinationCell ;
                eg:durationMin ?durationMin ;
                eg:durationMax ?durationMax ;
                eg:durationAvg ?durationAvg ;
                eg:durationMedian ?durationMedian ;
                .
            }
            # SELECT ?apiBaseUrl ?projectId ?project ?computationId ?originCellId ?origin ?originCell ?inputJson ?s ?destinationCellId ?destinationCell ?durationMin ?durationMax ?durationAvg ?durationMedian {
            {
              # BIND(?origin AS ?originCellId)
              BIND(STR(?apiBaseUrl) + "computations/" + STR(?computationId)
                + "/directions?origins=" + STR(?origin)
                # "&destinations=" + STR(?destinationCellId)
                + "&steps=false&routes=false" AS ?url)
              # BIND("https://mobydex.locoslab.com/controller-service/computations/" + STR(?route_computation_id) + "/computations?pageOffset=0&pageSize=10000" AS ?url)
              LATERAL {
                { SELECT ?inputJson ?originCellId ?destinationCellId (MIN(?duration) AS ?durationMin) (MAX(?duration) AS ?durationMax) (AVG(?duration) AS ?durationAvg) (MEDIAN(?duration) AS ?durationMedian) {
                    ?inputJson norse:json.unnest ?entry .
                    BIND(norse:json.get(?entry, "originCell") AS ?originCellId)
                    BIND(norse:json.get(?entry, "destinationCell") AS ?destinationCellId)
                    BIND(norse:json.get(?entry, "routes") AS ?routes)
                    ?routes norse:json.unnest ?route .
                    BIND(norse:json.get(?route, "duration") AS ?duration)
                  }
                  GROUP BY ?inputJson ?originCellId ?destinationCellId
                }
              }
              BIND(IRI(CONCAT(STR(?url), '#dest=', STR(?destinationCellId))) AS ?s)

              BIND(STR(?apiBaseUrl) + "projects/" + STR($projectId) AS ?projectStr)
              BIND(IRI(?projectStr) AS ?project)
              BIND(IRI(concat(?projectStr, "#cell", STR(?originCellId))) AS ?originCell)
              BIND(IRI(concat(?projectStr, "#cell", STR(?destinationCellId))) AS ?destinationCell)
            }
            ORDER BY ?durationMin # Ordered for aesthetics.
        """;

        String id = createOriginCellId(projectId, computationId, originCellId);
        Graph g = QueryExec.dataset(DatasetGraphFactory.empty()).query(queryStr)
                .substitution("apiBaseUrl", NodeFactory.createURI("https://mobydex.locoslab.com/controller-service/"))
                .substitution("computationId", NodeValue.makeInteger(computationId).asNode())
                .substitution("origin", NodeValue.makeInteger(originCellId).asNode())
                .substitution("projectId", NodeValue.makeInteger(projectId).asNode())
                .substitution("inputJson", jsonRdfNode)
                .construct();
        Model m = ModelFactory.createModelForGraph(g);

        // RDFDataMgr.write(System.out, m, RDFFormat.TURTLE_PRETTY);
        Resource result = m.getResource(id);
        // The model may be empty if the cell has no connections.
        // Add a type triple to ensure that the model is non-empty.
        Resource CELL = ResourceFactory.createResource("http://www.example.org/Cell");
        result.addProperty(RDF.type, CELL);
        return result;
        // return m.getResource(id);
    }


    /**
     * Project is needed because it is part of the cell IRIs.
     *
     * @param projectId
     * @param computationId
     * @param originCellId
     * @return The resource that corresponds to the origin cell.
     */
    @Deprecated(forRemoval = true)
    public static Resource loadComputationOld(long projectId, long computationId, long originCellId) {
        String queryStr = """
            PREFIX eg: <http://www.example.org/>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX url: <http://jsa.aksw.org/fn/url/>
            PREFIX norse: <https://w3id.org/aksw/norse#>

            CONSTRUCT {
              ?s
                eg:url ?apiBaseUrl ;
                eg:computationId ?computationId ;
                # eg:originCellId ?originCellId ;
                # eg:destinationCellId ?destinationCellId ;
                eg:originCell ?originCell ;
                eg:destinationCell ?destinationCell ;
                eg:durationMin ?durationMin ;
                eg:durationMax ?durationMax ;
                eg:durationAvg ?durationAvg ;
                eg:durationMedian ?durationMedian ;
                .
            }
            # SELECT ?apiBaseUrl ?s ?projectId ?originCellId ?destinationCellId ?durationMin ?durationMax ?durationAvg ?durationMedian {
            {
              # BIND(2 AS ?projectId)
              #BIND('WGS84' AS ?coordiante_system)
              #BIND(5 AS ?coordinate_precision)
              BIND($COMPUTATION_ID AS ?computationId)
              BIND($ORIGIN_CELL_ID AS ?origin)
              BIND(<https://mobydex.locoslab.com/controller-service> AS ?apiBaseUrl)
              BIND(STR(?apiBaseUrl) + "/computations/" + STR(?computationId)
                + "/directions?origins=" + STR(?origin)
                # "&destinations=" + STR(?destination)
                + "&steps=false&routes=false" AS ?url)
              # BIND("https://mobydex.locoslab.com/controller-service/computations/" + STR(?route_computation_id) + "/computations?pageOffset=0&pageSize=10000" AS ?url)
              LATERAL {
                { SELECT ?url ?originCellId ?destinationCellId (MIN(?duration) AS ?durationMin) (MAX(?duration) AS ?durationMax) (AVG(?duration) AS ?durationAvg) (MEDIAN(?duration) AS ?durationMedian) {
                    SERVICE <loop:cache:> { SELECT ?url ?json { # Explicit projection for ?url needed (lhs of BIND is not detected as "injectable")
                      BIND(STRDT(url:text(?url), norse:json) AS ?json)
                    } }
                    ?json norse:json.unnest ?entry .
                    BIND(norse:json.get(?entry, "originCell") AS ?originCellId)
                    BIND(norse:json.get(?entry, "destinationCell") AS ?destinationCellId)
                    BIND(norse:json.get(?entry, "routes") AS ?routes)
                    ?routes norse:json.unnest ?route .
                    BIND(norse:json.get(?route, "duration") AS ?duration)
                  }
                  GROUP BY ?url ?originCellId ?destinationCellId
                }
              }
              BIND(IRI(CONCAT(STR(?url), '#dest=', STR(?destinationCellId))) AS ?s)

              BIND("https://mobydex.locoslab.com/controller-service/projects/" + STR($PROJECT_ID) AS ?projectStr)
              BIND(IRI(concat(?projectStr + "#cell" + STR(?originCellId))) AS ?originCell)
              BIND(IRI(concat(?projectStr + "#cell" + STR(?destinationCellId))) AS ?destinationCell)
            }
            ORDER BY ?durationMin # Ordered for aesthetics.
        """
            .replace("$ORIGIN_CELL_ID", Long.toString(originCellId))
            .replace("$COMPUTATION_ID", Long.toString(computationId))
            .replace("$PROJECT_ID", Long.toString(projectId))
            ;

        String id = createOriginCellId(projectId, computationId, originCellId);
        Model model = QueryExecution.dataset(DatasetFactory.empty()).query(queryStr).construct();
        Resource result = model.getResource(id);
        // The model may be empty if the cell has no connections.
        // Add a type triple to ensure that the model is non-empty.
        Resource CELL = ResourceFactory.createResource("http://www.example.org/Cell");
        result.addProperty(RDF.type, CELL);
        return result;
    }

    public static String createOriginCellId(long projectId, long computationId, long originCellId) {
        String result = "https://mobydex.locoslab.com/controller-service/projects/" + projectId + "#cell" + originCellId;
        return result;
    }
}

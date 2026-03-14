package org.aksw.mobydex.demo;

import java.io.IOException;

import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.jenax.sparql.fragment.impl.FragmentUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSetOps;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.graph.GraphFactory;

public class MainPlaygroundMobyDex {
    public static void main(String[] args) throws IOException {

        MobyDexRdfApi mobyDexApi = new MobyDexRdfApi();

        long projectId = 2;
        long computationId = 70;
        long originCellId = 271;
        Model projectGridModel = mobyDexApi.loadProjectGrid(projectId);
        Resource originCell = mobyDexApi.loadComputation(projectId, computationId, originCellId);

        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();

        Model poiTypeHistogramModel = mobyDexApi.loadPoiHistogramModel(projectId, tagsFragment);


//
//
//
//        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
//        Query poiTypeHistogramQuery = OsmRdfApi.createQueryExportPoiHistogram(cellFragment, tagsFragment);
//        // System.out.println(q3);
//
//        Model poiTypeHistogramModel = QueryExecutionHTTP.service("https://data.aksw.org/mobydex")
//            .query(poiTypeHistogramQuery)
//            .construct();

        Node durationProperty = NodeFactory.createURI("http://www.example.org/durationMin");
        Table poiTypeToCells = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, durationProperty);
        if (true) {
            RowSetOps.out(System.out, poiTypeToCells.toRowSet());
            // RDFDataMgr.write(System.out, originCell.getModel(), RDFFormat.TURTLE_BLOCKS);
            return;
        }

        Table table1 = QueryExec.graph(GraphFactory.emptyGraph()).query("""
                PREFIX geo: <http://www.opengis.net/ont/geosparql#>
                SELECT * {
                  VALUES (?cid ?geom) {
                    (<urn:myCell> "POLYGON ((6.615850000000001 51.32242, 6.630190000000001 51.32271, 6.62973 51.33169, 6.615380000000001 51.3314, 6.615850000000001 51.32242))"^^geo:wktLiteral)
                  }
                }
                """).table();

        Table table2 = QueryExec.graph(GraphFactory.emptyGraph()).query("""
                SELECT * {
                  VALUES (?x ?y) {
                    (<https://www.openstreetmap.org/wiki/Key:amenity> "parking")
                  }
                }
                """).table();

        Query q = OsmRdfApi.createQueryExportPois(Fragment.of(table1).project(0).toFragment1(), Fragment.of(table2).toFragment2());
        System.out.println(q);


//        Table table = QueryExecHTTP.service("https://data.aksw.org/mobydex")
//            .query(q)
//            .table();
//        System.out.println(table);


        Query q2 = OsmRdfApi.createQueryExportPois(FragmentUtils.fromQuery("SELECT * { SELECT DISTINCT ?s { ?s a ?o } }").toFragment1(), Fragment.of(table2).toFragment2());
        System.out.println(q2);


        Fragment osmPoiTable = Fragment.of(OsmRdfApi.getPoiCategories()).toFragment3();

        Table cellIdAndGeoms = QueryExec.graph(projectGridModel.getGraph())
                .query("""
                    PREFIX geo: <http://www.opengis.net/ont/geosparql#>
                    SELECT ?s ?wkt { ?s geo:hasGeometry/geo:asWKT ?wkt }
                    LIMIT 100
                """).table();
            Fragment2 cellFragment = Fragment.of(cellIdAndGeoms).toFragment2();
        Query q3 = OsmRdfApi.createQueryExportPoiHistogram(Fragment.of(cellIdAndGeoms).toFragment2(), Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2());
        System.out.println(q3);

        Model m = QueryExecutionHTTP.service("https://data.aksw.org/mobydex")
            .query(q3)
            .construct();
        RDFDataMgr.write(System.out, m, RDFFormat.TURTLE_BLOCKS);


//        Model model = MobyDexRdfApi.loadComputation(70, 271);
//        RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
    }
}

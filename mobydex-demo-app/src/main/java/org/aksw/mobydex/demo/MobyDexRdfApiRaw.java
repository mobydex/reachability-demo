package org.aksw.mobydex.demo;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public class MobyDexRdfApiRaw {

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
              BIND(STRDT(REPLACE(STR(?linestring), "LINESTRING", "POLYGON (") + ")", datatype(?linestring)) AS ?polygon)

              # For each routing cell, find the census cells

              # BIND(geof:buffer(?linestring, 1, uom:metre) AS ?polygon)
              BIND(STR(?cellId) AS ?polygonTooltip)
            } LIMIT 10000
        """
            .replace("$PROJECT_ID", Long.toString(projectId))
        ;
        Model model = QueryExecution
            .dataset(DatasetFactory.empty())
            .query(queryStr)
            .construct();

        return model;
    }

    /**
     * Project is needed because it is part of the cell IRIs.
     *
     * @param projectId
     * @param computationId
     * @param originCellId
     * @return The resource that corresponds to the origin cell.
     */
    public static Resource loadComputation(long projectId, long computationId, long originCellId) {
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
        Model result = QueryExecution.dataset(DatasetFactory.empty()).query(queryStr).construct();
        return result.getResource(id);
    }

    public static String createOriginCellId(long projectId, long computationId, long originCellId) {
        String result = "https://mobydex.locoslab.com/controller-service/projects/" + projectId + "#cell" + originCellId;
        return result;
    }
}

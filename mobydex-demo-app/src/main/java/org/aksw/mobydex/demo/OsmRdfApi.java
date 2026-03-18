package org.aksw.mobydex.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.aksw.jenax.sparql.fragment.api.Fragment1;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.syntaxtransform.ElementTransformCopyBase;
import org.apache.jena.sparql.syntax.syntaxtransform.ElementTransformSubst;
import org.apache.jena.sparql.syntax.syntaxtransform.ElementTransformer;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;

public class OsmRdfApi {

    /**
     * For each poi type, return the first n nearest cells that contain them.
     * @param projectGridCell
     * @param poiTypeHistogram
     * @param n
     * @param durationProperty
     * @return
     */
    public static Table createQueryPoiTypeInRange(Resource projectGridCell,  Model poiTypeHistogram, Fragment2 tags, long n, Node durationProperty) {

        Model unionModel = ModelFactory.createUnion(projectGridCell.getModel(), poiTypeHistogram);

        Query baseQuery = QueryFactory.create("""
            PREFIX eg: <http://www.example.org/>
            PREFIX  geo:  <http://www.opengis.net/ont/geosparql#>
            PREFIX  spatial: <http://jena.apache.org/spatial#>
            PREFIX  geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

            SELECT * {
              BIND($ORIGIN_CELL AS ?originCell)
              SERVICE <elt:tags> {} # For each poi type
              LATERAL {
                { SELECT * { # Limit per poi type
                  SERVICE <loop:cache:> { # Cache the order of the dest cells
                    SELECT * {
                        {
                          BIND(?originCell AS ?destCell)
                          BIND(0.0 AS ?duration)
                        }
                      UNION
                        {
                          ?computation
                            eg:originCell ?originCell ;
                            eg:destinationCell ?destCell ;
                            $DURATION_PROPERTY ?duration ;
                            .
                        }
                    } ORDER BY ASC(?duration)
                  }
                  LATERAL {
                    ?destCell
                      eg:hasPoiHistogram ?cellTypeHist ;
                      .

                    ?cellTypeHist
                      eg:cp ?cp ;
                      eg:co ?co ;
                      # eg:count ?count ;
                      .
                  }
                } LIMIT $N }
              }
            } ORDER BY ?duration
        """
            .replace("$N", Long.toString(n))
            .replace("$ORIGIN_CELL", "<" + projectGridCell.getURI() + ">")
            .replace("$DURATION_PROPERTY", "<" + durationProperty.getURI() + ">")
        );

        Element tagsElt = tags.rename("cp", "co").getElement();

        Map<String, Element> map = new HashMap<>();
        map.put("elt:tags", tagsElt);

        Query query = QueryTransformOps.transform(baseQuery, new ElementTransformInjectNamedElement(map));
        System.out.println(query);
        FileUtils.write("/tmp/union.ttl", out -> RDFDataMgr.write(out, unionModel, RDFFormat.TURTLE_BLOCKS));
        return QueryExec.graph(unionModel.getGraph()).query(query).table();
        // QueryExecution.model(unionModel).query(query).

        // return result;
    }

    public static Query createQueryExportPoiHistogram(Fragment2 geoms, Fragment2 tags) {
        Query baseQuery = QueryFactory.create("""
            PREFIX eg: <http://www.example.org/>
            PREFIX  geo:  <http://www.opengis.net/ont/geosparql#>
            PREFIX  spatial: <http://jena.apache.org/spatial#>
            PREFIX  geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            CONSTRUCT {
              ?cell
                eg:hasPoiHistogram ?cellTypeHist .

              ?cellTypeHist
                eg:cp ?cp ;
                eg:co ?co ;
                eg:count ?count
            }
            # SELECT * {
            {
              { SELECT ?cell ?cp ?co (COUNT(*) AS ?count)

              {
                SERVICE <elt:geoms> { }
                SERVICE <elt:tags> { }
                SERVICE <loop:cache:>
                  { ?s    spatial:intersectBoxGeom  ( ?cellGeom ) .
                    ?s geo:hasGeometry/geo:asWKT ?wkt
                    FILTER geof:sfIntersects(?cellGeom, ?wkt)
                  }
                FILTER EXISTS { # match criteria
                  ?s ?cp ?co .
                }
              }
            GROUP BY ?cell ?cp ?co }

            BIND(IRI(concat(str(?cell), '-poiHistogram-', ENCODE_FOR_URI(concat(str(?cp) + '|' + str(?co))))) AS ?cellTypeHist)
            }
        """);

        Element geomsElt = geoms.rename("cell", "cellGeom").getElement();
        Element tagsElt = tags.rename("cp", "co").getElement();

        Map<String, Element> map = new HashMap<>();
        map.put("elt:geoms", geomsElt);
        map.put("elt:tags", tagsElt);

        Query result = QueryTransformOps.transform(baseQuery, new ElementTransformInjectNamedElement(map));
        return result;
    }

    public static Query createQueryExportPoiIds(Fragment1 geoms, Fragment2 tags) {
        Query baseQuery = QueryFactory.create("""
            PREFIX spatial: <http://jena.apache.org/spatial#>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

            SELECT DISTINCT ?s {
              SERVICE <elt:geoms> { }
              SERVICE <elt:tags> { }
              SERVICE <loop:cache:> { # cache repeated spatial index lookups with the same query geom.
                ?s spatial:intersectBoxGeom(?queryGeom) .
                ?s geo:hasGeometry/geo:asWKT ?wkt
                FILTER(geof:sfIntersects(?queryGeom, ?wkt))
              }
              FILTER EXISTS { # match criteria
                ?s ?cp ?co .
              }
            }
            """);

        Element geomsElt = geoms.rename("queryGeom").getElement();
        Element tagsElt = tags.rename("cp", "co").getElement();

        Map<String, Element> map = new HashMap<>();
        map.put("elt:geoms", geomsElt);
        map.put("elt:tags", tagsElt);

        Query result = QueryTransformOps.transform(baseQuery, new ElementTransformInjectNamedElement(map));
        return result;
    }

    public static Query createQueryExportPois(Fragment1 geoms, Fragment2 tags) {
        // TODO Make graph name(s) configurable to boost spatial lookups
        Query baseQuery = QueryFactory.create("""
            PREFIX spatial: <http://jena.apache.org/spatial#>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

            SELECT ?s ?cp ?co ?wkt {
              SERVICE <elt:geoms> { }
              SERVICE <elt:tags> { }
              SERVICE <loop:cache:> { # cache repeated spatial index lookups with the same query geom.
                #GRAPH <https://data.mobydex.org/osm/20250903/15mincity/> {
                  ?s spatial:intersectBoxGeom(?queryGeom) .
                  ?s geo:hasGeometry/geo:asWKT ?wkt
                  FILTER(geof:sfIntersects(?queryGeom, ?wkt))
                #}
              }
              FILTER EXISTS { # match criteria
                ?s ?cp ?co .
              }
            }
            """);

        Element geomsElt = geoms.rename("queryGeom").getElement();
        Element tagsElt = tags.rename("cp", "co").getElement();

        Map<String, Element> map = new HashMap<>();
        map.put("elt:geoms", geomsElt);
        map.put("elt:tags", tagsElt);

        Query result = QueryTransformOps.transform(baseQuery, new ElementTransformInjectNamedElement(map));
        return result;
    }

    public static class NamedElementRegistry {
        private Map<String, Element> map;

        public void putQuery(String name, String queryStr) {
            Query query = QueryFactory.create(queryStr);
            putQuery(name, query);
        }

        public void putQuery(String name, Query query) {
            Element elt = new ElementSubQuery(query);
            putElement(name, elt);
        }

        public void putElement(String name, Element elt) {
            map.put(name, elt);
        }

        public Optional<Element> getElement(String name) {
            return Optional.ofNullable(map.get(name));
        }

        public Map<String, Element> getMap() {
            return map;
        }
    }

    /**
     * Inject named elements.
     *
     * Basic usage:
     * <pre>
     * SERVICE &lt;NAME&gt; { }
     * </pre>
     *
     * Variables of the named element can be substituted prior to injection:
     * <pre>
     * SERVICE &lt;NAME&gt; {
     *   [] &lt;urn:x-arq:var:x&gt; ?y
     * }
     * </pre>
     *
     */
    public static class ElementTransformInjectNamedElement extends ElementTransformCopyBase {
        protected Map<String, Element> map;

        public static final String prefix = "urn:x-arq:var:";

        public ElementTransformInjectNamedElement(Map<String, Element> map) {
            super();
            this.map = Objects.requireNonNull(map);
        }

        @Override
        public Element transform(ElementService el, Node service, Element elt1) {
            if (service.isURI()) {
                String serviceName = service.getURI();
                Element subst = map.get(serviceName);
                if (subst != null) {
                    // Apply any substitution
                    if (elt1 instanceof ElementTriplesBlock block) {
                        Map<Var, Var> relabel = new HashMap<>();
                        BasicPattern bgp = block.getPattern();
                        for (Triple t : bgp) {
                            Node p = t.getPredicate();
                            if (p.isURI()) {
                                String pStr = p.getURI();
                                if (pStr.startsWith(serviceName)) {
                                    String varName = pStr.substring(prefix.length());
                                    Node o = t.getObject();
                                    if (!o.isVariable()) {
                                        throw new IllegalStateException("Expected a varible in the object position. Instead got: " + t);
                                    }
                                    relabel.put(Var.alloc(varName), (Var)o);
                                }
                            }

                            subst = ElementTransformer.transform(subst, new ElementTransformSubst(relabel));
                        }
                    }
                    return subst;
                }
            }
            return super.transform(el, service, elt1);
        }
    }

    public static Table getPoiCategoriesRaw() {
        String valuesBlock = """
        VALUES (?key ?value ?category) {
              ("building"        "residential"   "residential")
              ("building"        "apartments"    "residential")
              ("building"        "house"         "residential")
              ("building"        "detached"      "residential")
              ("building"        "terrace"       "residential")

              ("shop"            "supermarket"   "shopping")
              ("shop"            "convenience"   "shopping")
              ("shop"            "greengrocer"   "shopping")
              ("shop"            "bakery"        "shopping")
              ("shop"            "butcher"       "shopping")
              ("shop"            "dairy"         "shopping")
              ("shop"            "chemist"       "shopping")
              ("shop"            "cosmetics"     "shopping")
              ("amenity"         "pharmacy"      "shopping")

              ("amenity"         "doctors"       "health_care")
              ("amenity"         "dentist"       "health_care")
              ("amenity"         "hospital"      "health_care")
              ("healthcare"      "hospital"      "health_care")
              ("social_facility" "nursing_home"  "health_care")
              ("amenity"         "social_facility" "health_care")

              ("amenity"         "kindergarten"  "education")
              ("amenity"         "childcare"     "education")
              ("amenity"         "school"        "education")
              ("amenity"         "college"       "education")
              ("amenity"         "university"    "education")
              ("amenity"         "library"       "education")

              ("amenity"         "childcare"     "child_care")

              ("amenity"         "coworking_space" "work")

              ("leisure"         "park"          "leisure")
              ("amenity"         "cinema"        "leisure")
              ("amenity"         "theatre"       "leisure")
              ("amenity"         "restaurant"    "leisure")
              ("amenity"         "cafe"          "leisure")
              ("amenity"         "bar"           "leisure")
              ("amenity"         "fast_food"     "leisure")
              ("amenity"         "pub"           "leisure")

              ("highway"         "bus_stop"      "public_transport")
              ("public_transport" "platform"     "public_transport")
              ("railway"         "station"       "public_transport")
              ("public_transport" "stop_position" "public_transport")
              ("amenity"         "bus_station"   "public_transport")
              ("railway"         "halt"          "public_transport")
              ("light_rail"      "station"       "public_transport")
              ("subway"          "station"       "public_transport")
              ("tram"            "station"       "public_transport")
            }
            """;
        String queryStr = "SELECT * {" + valuesBlock + "}";
        return QueryExec.graph(GraphFactory.emptyGraph()).query(queryStr).table();
    }

    public static Table getPoiCategories() {
        String valuesBlock = """
            VALUES (?key ?value ?category) {
              ( <https://www.openstreetmap.org/wiki/Key:building>        "residential"   "residential" )
              ( <https://www.openstreetmap.org/wiki/Key:building>        "apartments"    "residential" )
              ( <https://www.openstreetmap.org/wiki/Key:building>        "house"         "residential" )
              ( <https://www.openstreetmap.org/wiki/Key:building>        "detached"      "residential" )
              ( <https://www.openstreetmap.org/wiki/Key:building>        "terrace"       "residential" )

              ( <https://www.openstreetmap.org/wiki/Key:shop>            "supermarket"   "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "convenience"   "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "greengrocer"   "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "bakery"        "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "butcher"       "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "dairy"         "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "chemist"       "shopping" )
              ( <https://www.openstreetmap.org/wiki/Key:shop>            "cosmetics"     "shopping" )

              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "pharmacy"      "shopping" )

              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "doctors"       "health_care" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "dentist"       "health_care" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "hospital"      "health_care" )
              ( <https://www.openstreetmap.org/wiki/Key:healthcare>      "hospital"      "health_care" )
              ( <https://www.openstreetmap.org/wiki/Key:social_facility> "nursing_home"  "health_care" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "social_facility" "health_care" )

              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "kindergarten"  "education" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "childcare"     "education" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "school"        "education" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "college"       "education" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "university"    "education" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "library"       "education" )

              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "childcare"     "child_care" )

              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "coworking_space" "work" )

              ( <https://www.openstreetmap.org/wiki/Key:leisure>         "park"          "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "cinema"        "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "theatre"       "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "restaurant"    "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "cafe"          "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "bar"           "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "fast_food"     "leisure" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "pub"           "leisure" )

              ( <https://www.openstreetmap.org/wiki/Key:highway>         "bus_stop"      "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:public_transport> "platform"     "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:railway>         "station"       "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:public_transport> "stop_position" "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:amenity>         "bus_station"   "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:railway>         "halt"          "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:light_rail>      "station"       "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:subway>          "station"       "public_transport" )
              ( <https://www.openstreetmap.org/wiki/Key:tram>            "station"       "public_transport" )
            }
                        """;
        String queryStr = "SELECT * {" + valuesBlock + "}";
        return QueryExec.graph(GraphFactory.emptyGraph()).query(queryStr).table();
    }
}

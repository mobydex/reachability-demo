package org.aksw.mobydex.demo;

import java.util.List;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.vocabulary.RDFS;

@Route(value = "geosparql", layout = MainLayout.class)
@PageTitle("Sparql Browser")
public class GeoSparqlView
    extends VerticalLayout
{
    private static final long serialVersionUID = 1L;

    private GeoSparqlBrowser browser;

    public static final Property queryString = ResourceFactory.createProperty("http://www.example.org/queryString");

    public GeoSparqlView() {
        Resource r0 = ModelFactory.createDefaultModel().createResource();
        r0.addLiteral(RDFS.label, "(clear)");
        r0.addLiteral(queryString, "");

        Resource r1 = ModelFactory.createDefaultModel().createResource();
        r1.addLiteral(RDFS.label, "RegioStarR");
        r1.addLiteral(queryString, """
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX spatialF: <http://jena.apache.org/function/spatial#>
            PREFIX spatial: <http://jena.apache.org/spatial#>

            SELECT *
            # SELECT (COUNT(*) AS ?c)
            # FROM <https://mobydex.org/resource/regiostar/>
            {
              GRAPH <https://mobydex.org/resource/regiostar/> {
                ?s <https://mobydex.org/resource/regiostar/RS7> 75 .
                ?s geo:hasGeometry ?g .
                ?g geo:asWKT ?wkt .
              }
            }
            LIMIT 10
            """);

        Resource r2 = ModelFactory.createDefaultModel().createResource();
        r2.addLiteral(RDFS.label, "Zensus");
        r2.addLiteral(queryString, """
            PREFIX eg: <http://www.example.org/>
            PREFIX qb: <http://purl.org/linked-data/cube#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            SELECT *
            WHERE {
              GRAPH <https://data.aksw.org/zensus/2022/> {
                ?obs a qb:Observation .
                ?obs eg:cell ?cell .
                ?cell geo:hasGeometry ?cellGeom .
                ?cellGeom geo:asWKT ?cellWkt .
                ?obs eg:inhabitants ?inhabitants .
                ?obs eg:averageAge ?avgAge .
                # ?obs ?p ?o
              }
              #FILTER (?inhabitants > 1000 && ?inhabitants < 10000)
              #FILTER(?avgAge > 30 && ?avgAge < 50)
            }
            LIMIT 10
            """);

        ComboBox<Resource> comboBox = new ComboBox<>();
        comboBox.setLabel("Examples");
        comboBox.setItems(List.of(r0, r1, r2));
        comboBox.setItemLabelGenerator(r -> r.getProperty(RDFS.label).getString());
        comboBox.addValueChangeListener(ev -> {
            browser.getYasqe().setValue(ev.getValue().getProperty(queryString).getString());
        });
        add(comboBox);
        // comboBox.setRenderer(r);

        QueryExecutionFactoryQuery qef = query -> QueryExecutionHTTP.service("https://data.aksw.org/mobydex").query(query).build();

        browser = new GeoSparqlBrowser(qef);

        add(browser);
    }
}

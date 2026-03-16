package org.aksw.mobydex.demo;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

@Route(value = "geosparql", layout = MainLayout.class)
@PageTitle("Sparql Browser")
public class GeoSparqlView
    extends HorizontalLayout
{
    private static final long serialVersionUID = 1L;

    private GeoSparqlBrowser browser;

    public GeoSparqlView() {
        QueryExecutionFactoryQuery qef = query -> QueryExecutionHTTP.service("https://data.aksw.org/mobydex").query(query).build();

        browser = new GeoSparqlBrowser(qef);

        add(browser);
    }
}

package org.aksw.mobydex.demo.view;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.aksw.jena_sparql_api.sparql.ext.geosparql.GeometryWrapperUtils;
import org.aksw.jenax.arq.util.query.QueryTransform;
import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.aksw.mobydex.demo.CellStyles;
import org.aksw.mobydex.demo.MainLayout;
import org.aksw.mobydex.demo.appstate.AppState;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.component.GeoSparqlBrowser;
import org.aksw.mobydex.demo.domain.Project;
import org.aksw.vaadin.jena.geo.leafletflow.JtsToLMapConverter;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.geosparql.implementation.GeometryWrapper;
import org.apache.jena.geosparql.implementation.datatype.WKTDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;
import org.apache.jena.vocabulary.RDFS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import io.reactivex.rxjava3.schedulers.Schedulers;
import software.xdev.vaadin.maps.leaflet.MapContainer;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLngBounds;
import software.xdev.vaadin.maps.leaflet.layer.LLayer;
import software.xdev.vaadin.maps.leaflet.layer.LLayerGroup;
import software.xdev.vaadin.maps.leaflet.layer.vector.LPath;
import software.xdev.vaadin.maps.leaflet.map.LMap;
import software.xdev.vaadin.maps.leaflet.map.LMapZoomPanOptions;
import software.xdev.vaadin.maps.leaflet.registry.LComponentManagementRegistry;

@Route(value = "geosparql", layout = MainLayout.class)
@PageTitle("Sparql Browser")
public class GeoSparqlView
    extends VerticalLayout
{
    private static final long serialVersionUID = 1L;

    private Button clearSelectionBtn;
    private Button applySelectionBtn;

    private GeoSparqlBrowser browser;

    private MapContainer mapContainer;
    private LMap lMap;

    private LLayerGroup projectGridLayer;

    JtsToLMapConverter jtsToLMapConverter;

    private LLatLngBounds bounds;
    private Geometry projectGeometry;

    private UI ui;

    public static final Property queryString = ResourceFactory.createProperty("http://www.example.org/queryString");

    public GeoSparqlView(AppState appState) {
        Resource r0 = ModelFactory.createDefaultModel().createResource();
        r0.addLiteral(RDFS.label, "(clear)");
        r0.addLiteral(queryString, "");

        Resource r1 = ModelFactory.createDefaultModel().createResource();
        r1.addLiteral(RDFS.label, "RegioStaR - All");
        r1.addLiteral(queryString, """
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX spatial: <http://jena.apache.org/spatial#>
            PREFIX rro: <https://schema.aksw.org/regiostar/>

            SELECT *
            {
              GRAPH <https://mobydex.org/resource/regiostar/> {
                ?s spatial:intersectBoxGeom(?GRID_WKT) .
                ?s geo:hasGeometry ?g .
                ?g geo:asWKT ?wkt .
                FILTER(geof:sfIntersects(?wkt, ?GRID_WKT))
                #?s rro:type ?regioStaRType .
              }
            }
            """);

        Resource r2 = ModelFactory.createDefaultModel().createResource();
        r2.addLiteral(RDFS.label, "RegioStaR - Metropolitane Stadregion - Metropole RS17/111");
        r2.addLiteral(queryString, """
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX spatial: <http://jena.apache.org/spatial#>
            PREFIX rro: <https://schema.aksw.org/regiostar/>

            SELECT *
            {
              GRAPH <https://mobydex.org/resource/regiostar/> {
                ?s spatial:intersectBoxGeom(?GRID_WKT) .
                ?s geo:hasGeometry ?g .
                ?g geo:asWKT ?wkt .
                FILTER(geof:sfIntersects(?wkt, ?GRID_WKT))
                ?s <https://mobydex.org/resource/regiostar/RS17> 111
                #?s rro:type ?regioStaRType .
              }
            }
            """);

        Resource r3 = ModelFactory.createDefaultModel().createResource();
        r3.addLiteral(RDFS.label, "Zensus");
        r3.addLiteral(queryString, """
            PREFIX eg: <http://www.example.org/>
            PREFIX qb: <http://purl.org/linked-data/cube#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX geo: <http://www.opengis.net/ont/geosparql#>
            PREFIX spatial: <http://jena.apache.org/spatial#>
            PREFIX geof: <http://www.opengis.net/def/function/geosparql/>

            SELECT *
            WHERE {
              GRAPH <https://data.aksw.org/zensus/2022/> {
                ?cell spatial:intersectBoxGeom(?GRID_WKT) .
                ?cell geo:hasGeometry ?cellGeom .
                ?cellGeom geo:asWKT ?cellWkt .
                ?obs eg:cell ?cell .
                ?obs a qb:Observation .
                ?obs eg:inhabitants ?inhabitants .
                ?obs eg:averageAge ?avgAge .
                # ?obs ?p ?o
              }
              FILTER(geof:sfIntersects(?cellWkt, ?GRID_WKT))
              FILTER (?inhabitants > 200)
              #FILTER(?avgAge > 30 && ?avgAge < 50)
            }
            #LIMIT 10
            """);

        HorizontalLayout controlBar = new HorizontalLayout();
        controlBar.setAlignItems(FlexComponent.Alignment.END);

        ComboBox<Resource> comboBox = new ComboBox<>();
        comboBox.setLabel("Examples");
        comboBox.setItems(List.of(r0, r1, r2, r3));
        comboBox.setItemLabelGenerator(r -> r.getProperty(RDFS.label).getString());
        comboBox.addValueChangeListener(ev -> {
            browser.getYasqe().setValue(ev.getValue().getProperty(queryString).getString());
        });
        controlBar.add(comboBox);


        Button recenterMapBtn = new Button("Recenter Map");
        recenterMapBtn.addClickListener(ev -> {
            recenterMap();
        });
        controlBar.add(recenterMapBtn);

        applySelectionBtn = new Button("Apply Selection");
        applySelectionBtn.addClickListener(ev -> {
            Table table = browser.getSelection();
            Set<Binding> geomBindings = Iter.toSet(table.rows());
            appState.setSelectedGeomBindings(Optional.of(geomBindings));
            ui.navigate(ReachabilityView.class);
        });
        controlBar.add(applySelectionBtn);


        clearSelectionBtn = new Button("Clear Selection");
        clearSelectionBtn.addClickListener(ev -> {
            appState.setSelectedGeomBindings(Optional.empty());
            ui.navigate(ReachabilityView.class);
        });


        //controlBar.add(clearSelectionBtn);
        add(controlBar);


        // comboBox.setRenderer(r);

        // TODO If no project has been selected, use a world wide polygon?
        //      Or show an error that a project needs to be selected first?
        QueryExecutionFactoryQuery qef = query -> QueryExecutionHTTP.service("https://data.aksw.org/mobydex").query(query).build();
        String gridStr = "POLYGON((6.673210000000001 51.323570000000004, 6.65887 51.32329000000001, 6.6445300000000005 51.32300000000001, 6.630190000000001 51.32271, 6.615850000000001 51.32242, 6.615380000000001 51.3314, 6.614920000000001 51.340390000000006, 6.614450000000001 51.34937000000001, 6.613980000000001 51.35835, 6.61352 51.367340000000006, 6.61305 51.37632000000001, 6.61258 51.385310000000004, 6.61211 51.394290000000005, 6.611650000000001 51.403270000000006, 6.611180000000001 51.41226, 6.610710000000001 51.421240000000004, 6.61024 51.43023, 6.60977 51.43921, 6.6093 51.448190000000004, 6.60883 51.45718, 6.60836 51.46616, 6.60789 51.475150000000006, 6.60742 51.48413000000001, 6.60695 51.49311, 6.60648 51.502100000000006, 6.60601 51.51108000000001, 6.60554 51.520070000000004, 6.60507 51.529050000000005, 6.6046000000000005 51.538030000000006, 6.604120000000001 51.54702, 6.603650000000001 51.556000000000004, 6.603180000000001 51.564980000000006, 6.602710000000001 51.57397, 6.6022300000000005 51.582950000000004, 6.6017600000000005 51.59194, 6.616180000000001 51.59223, 6.630610000000001 51.59252000000001, 6.64503 51.59281000000001, 6.6594500000000005 51.59310000000001, 6.6738800000000005 51.59339000000001, 6.688300000000001 51.59367, 6.702730000000001 51.59396, 6.71715 51.594240000000006, 6.73158 51.59452, 6.746 51.594800000000006, 6.76043 51.59507000000001, 6.774850000000001 51.59535, 6.789280000000001 51.595620000000004, 6.803700000000001 51.595890000000004, 6.818130000000001 51.596160000000005, 6.832560000000001 51.596430000000005, 6.84698 51.59669, 6.86141 51.59696, 6.87584 51.59722000000001, 6.890270000000001 51.597480000000004, 6.90469 51.59774, 6.91912 51.59799, 6.93355 51.59825000000001, 6.94798 51.5985, 6.96241 51.59875, 6.97684 51.599000000000004, 6.977240000000001 51.59002, 6.97763 51.581030000000005, 6.97803 51.57204, 6.97843 51.56306000000001, 6.97883 51.55407, 6.97923 51.54509, 6.97963 51.536100000000005, 6.980020000000001 51.52711000000001, 6.9804200000000005 51.518130000000006, 6.9808200000000005 51.50914, 6.98122 51.50016, 6.981610000000001 51.491170000000004, 6.982010000000001 51.48218000000001, 6.982410000000001 51.473200000000006, 6.982800000000001 51.46421, 6.983200000000001 51.45523000000001, 6.983600000000001 51.44624, 6.98399 51.437250000000006, 6.98439 51.428270000000005, 6.984780000000001 51.41928000000001, 6.985180000000001 51.41029, 6.985570000000001 51.40131, 6.985970000000001 51.392320000000005, 6.98636 51.38333, 6.98676 51.37435000000001, 6.987150000000001 51.36536, 6.987550000000001 51.35638, 6.987940000000001 51.347390000000004, 6.98833 51.33840000000001, 6.98873 51.329420000000006, 6.974380000000001 51.329170000000005, 6.96004 51.328920000000004, 6.945690000000001 51.32867, 6.931350000000001 51.32842, 6.91701 51.328160000000004, 6.9026700000000005 51.32791, 6.88832 51.327650000000006, 6.87398 51.32739, 6.859640000000001 51.327130000000004, 6.84529 51.32686, 6.8309500000000005 51.326600000000006, 6.816610000000001 51.326330000000006, 6.802270000000001 51.32607, 6.78793 51.3258, 6.77359 51.325520000000004, 6.759250000000001 51.325250000000004, 6.744910000000001 51.32497000000001, 6.73057 51.32470000000001, 6.716220000000001 51.32442, 6.701890000000001 51.32414000000001, 6.687550000000001 51.32386, 6.673210000000001 51.323570000000004))";
        Node gridNode = NodeFactory.createLiteralDT(gridStr, WKTDatatype.INSTANCE);


        QueryTransform substitutionTransform = query -> {
            Node projectGeomNode = GeometryWrapperUtils.toWrapperWkt(projectGeometry).asNode();
            Map<Var, Node> substitutions = Map.of(Var.alloc("GRID_WKT"), projectGeomNode);
            Query result = QueryTransformOps.replaceVars(query, substitutions);
            return result;
        };

        browser = new GeoSparqlBrowser(qef, substitutionTransform);

        add(browser);

        MobyDexRdfApi mobyDexRdfApi = appState.getMobyDexRdfApi();
        browser.getMapReady().andThen(appState.selectedProject())
            .subscribeOn(Schedulers.io())
            .subscribe(projectId -> {
                Project project = mobyDexRdfApi.loadProject(projectId);
                Geometry unionGeom = MobyDexRdfApi.getUnionGeom(project);
                setProjectGeometry(unionGeom);
            });

        // Add the project grid preview

        mapContainer = browser.getMapContainer();
        lMap = mapContainer.getlMap();
        LComponentManagementRegistry reg = lMap.componentRegistry();
        projectGridLayer = new LLayerGroup(reg);
        lMap.addLayer(projectGridLayer);
        jtsToLMapConverter = new JtsToLMapConverter(reg);

        Geometry geom = GeometryWrapper.extract(gridNode).getParsingGeometry();
        setProjectGeometry(geom);

        browser.getMapReady().subscribe(() -> {
            recenterMap();
        });
    }


    public void setProjectGeometry(Geometry geom) {
        this.projectGeometry = geom;
        projectGridLayer.clearLayers();
        Envelope envelope = geom.getEnvelopeInternal();
        bounds = jtsToLMapConverter.convert(envelope);
        LLayer<?> layer = jtsToLMapConverter.convert(geom);
        LPath<?> path = (LPath<?>)layer;
        path.setStyle(CellStyles.green());
        projectGridLayer.addLayer(path);
    }

    public void recenterMap() {
        if (bounds != null) {
            lMap.flyToBounds(bounds, new LMapZoomPanOptions().withDuration(0.5));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = UI.getCurrent();
    }
}

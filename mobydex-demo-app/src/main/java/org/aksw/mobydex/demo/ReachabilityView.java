package org.aksw.mobydex.demo;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.aksw.commons.index.StorageComposers;
import org.aksw.jenax.arq.util.binding.BindingUtils;
import org.aksw.jenax.arq.util.tuple.adapter.TupleBridgeBinding;
import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.vaadin.jena.geo.leafletflow.JtsToLMapConverter;
import org.aksw.vaadin.jena.geo.leafletflow.JtsUtils;
import org.aksw.vaadin.jena.geo.leafletflow.ResultSetMapRendererL;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.exec.QueryExec;
import org.locationtech.jts.geom.Geometry;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.vaadin.chartjs.ChartContainer;
import software.xdev.vaadin.maps.leaflet.MapContainer;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLng;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLngBounds;
import software.xdev.vaadin.maps.leaflet.layer.LLayer;
import software.xdev.vaadin.maps.leaflet.layer.LLayerGroup;
import software.xdev.vaadin.maps.leaflet.layer.raster.LTileLayer;
import software.xdev.vaadin.maps.leaflet.layer.vector.LPath;
import software.xdev.vaadin.maps.leaflet.layer.vector.LPolygon;
import software.xdev.vaadin.maps.leaflet.layer.vector.LPolylineOptions;
import software.xdev.vaadin.maps.leaflet.map.LMap;
import software.xdev.vaadin.maps.leaflet.map.LMapZoomPanOptions;
import software.xdev.vaadin.maps.leaflet.registry.LComponentManagementRegistry;
import software.xdev.vaadin.maps.leaflet.registry.LDefaultComponentManagementRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Route(value = "reachability", layout = MainLayout.class)
@PageTitle("Demo")
public class ReachabilityView
    extends VerticalLayout
{
    private static final long serialVersionUID = 1L;

    protected LComponentManagementRegistry reg = new LDefaultComponentManagementRegistry(this);
    protected JtsToLMapConverter converter;

    protected MapContainer mapContainer;

    protected LLayerGroup connectionGroup;

    private String ID;

    private Map<String, LPath<?>> cellIdToLayer = new ConcurrentHashMap<>();

    // --- User Settings ---

    private String selectedCell = null;
    private long durationThreshold;

    public static class CellStyles {
        public static LPolylineOptions grey() { return grey(new LPolylineOptions()); }
        public static LPolylineOptions blue() { return blue(new LPolylineOptions()); }

        public static LPolylineOptions green() { return blue(new LPolylineOptions()); }
        public static LPolylineOptions red() { return blue(new LPolylineOptions()); }

        public static LPolylineOptions grey(LPolylineOptions options) {
            return options.withColor("grey").withFillColor("lightgrey").withFillOpacity(0.5);
        }

        public static LPolylineOptions blue(LPolylineOptions options) {
            return options.withColor("blue").withFillColor("lightblue").withFillOpacity(0.5);
        }

        public static LPolylineOptions green(LPolylineOptions options) {
            return options.withColor("green").withFillColor("lightgreen").withFillOpacity(0.5);
        }

        public static LPolylineOptions red(LPolylineOptions options) {
            return options.withColor("red").withFillColor("lightred").withFillOpacity(0.5);
        }

        public static LPolylineOptions selected(LPolylineOptions options) {
            return options.withStroke(true).withColor("orange").withOpacity(0.5);
        }
    }

    public ReachabilityView() {
        setSizeFull();

        ID = "my-map-view-" + System.nanoTime();
        setId(ID);

        // setSizeFull();

        HorizontalLayout controlBar = new HorizontalLayout();

        Button loadGridBtn = new Button("LoadGrid");
        add(loadGridBtn);
        loadGridBtn.addClickListener(ev -> {
            loadProjectGrid();
        });
        controlBar.add(loadGridBtn);


        Select<Long> durationCapSelect = new Select<>();
        durationCapSelect.setLabel("Duration Cap");
        List<Long> values = new ArrayList<>();
        for (long i = 0; i < 12; ++i) {
            long v = (i + 1) * 5 * 60;
            values.add(v);
        }

        durationThreshold = values.get(2);

        //DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat decimalFormat = new DecimalFormat("#0.##"); //, symbols);
        durationCapSelect.setItems(values);
        durationCapSelect.setTextRenderer(durationSeconds -> decimalFormat.format(durationSeconds / 60.0) + "m");
        durationCapSelect.setValue(durationThreshold);
        durationCapSelect.addValueChangeListener(ev -> {
            durationThreshold = ev.getValue();
            refreshStats();
        });
        controlBar.add(durationCapSelect);

        add(controlBar);


        SplitLayout mapAndChartSplit = new SplitLayout(Orientation.VERTICAL);
        add(mapAndChartSplit);
        mapAndChartSplit.setSizeFull();
        mapAndChartSplit.setSplitterPosition(50.0);
        mapAndChartSplit.addSplitterDragEndListener(ev -> {
            mapContainer.getlMap().invalidateSize(false);
        });

        mapContainer = createLMap();
        mapAndChartSplit.addToPrimary(mapContainer);
        converter = new JtsToLMapConverter(reg);

        connectionGroup = new LLayerGroup(reg);
        mapContainer.getlMap().addLayer(connectionGroup);
        // mapContainer.setWidth("400px");
        mapContainer.setWidthFull();
        mapContainer.setHeight("400px");

     // Assumes that this code is in some kind of Vaadin component or view
        ChartContainer chart = new ChartContainer();
        mapAndChartSplit.addToSecondary(chart);

        // chart.setWidth("400px");
        chart.setWidthFull();
        chart.setHeight("400px");

        // this.add(chart);

//        chart.showChart(
//          "{\"data\":{\"labels\":[\"A\",\"B\"],\"datasets\":[{\"data\":[1,2],\"label\":\"X\"}]},\"type\":\"bar\"}");

        // Or utilizing chartjs-java-model
        chart.showChart(new BarChart(new BarData()
          .addLabels("A", "B")
          .addDataset(new BarDataset()
            .setLabel("X")
            .addData(1)
            .addData(2)))
          .toJson());
    }


    // Reset the style of all cells
    public void clearCells() {
        Map<Node, List<Binding>> cellToBinding = computeHistogram(selectedCell);

        for (var e : cellIdToLayer.entrySet()) {
            String cellId = e.getKey();
            LPath<?> cellPath = e.getValue();

            LPolylineOptions style = new LPolylineOptions();
            CellStyles.grey(style); // default color

            Node cellNode = NodeFactory.createURI(cellId);
            List<Binding> bindings = cellToBinding.get(cellNode);
            if (bindings != null) {
                for (Binding b : bindings) {
                    Long duration = BindingUtils.tryGetNumber(b, "duration").map(Number::longValue).orElse(null);
                    if (duration != null) {
                        if (duration < durationThreshold) {
                            CellStyles.green(style);
                        } else {
                            CellStyles.red(style);
                        }
                    }
                }
            }
            if (Objects.equals(cellId, selectedCell)) {
                CellStyles.selected(style);
            }
            cellPath.setStyle(style);
        }
    }

    public Map<Node, List<Binding>> computeHistogram(String originCellIdStr) {
        if (selectedCell == null) {
            return Collections.emptyMap();
        }

        // FIXME Get the ID via the project model!
        String tmp = originCellIdStr.substring("https://mobydex.locoslab.com/controller-service/projects/2#cell".length());
        long originCellId = Long.parseLong(tmp);

        MobyDexRdfApi mobyDexApi;
        try {
            mobyDexApi = new MobyDexRdfApi();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long projectId = 2;
        long computationId = 70;
        // long originCellId = 271;

        Fragment osmPoiTable = Fragment.of(OsmRdfApi.getPoiCategories()).toFragment3();

        Model projectGridModel = mobyDexApi.loadProjectGrid(projectId);
        Resource originCell = mobyDexApi.loadComputation(projectId, computationId, originCellId);

        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();

        Model poiTypeHistogramModel = mobyDexApi.loadPoiHistogramModel(projectId, tagsFragment);

        Node durationProperty = NodeFactory.createURI("http://www.example.org/durationMin");
        Table poiTypeToCells = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1, durationProperty);

        TupleBridgeBinding bridge = TupleBridgeBinding.ofVarNames("destCell");

        var aggregator = StorageComposers.innerMap(0, HashMap::new,
                             StorageComposers.leafList(ArrayList::new, bridge));
        Map<Node, List<Binding>> cellToBinding = aggregator.newStore();
        poiTypeToCells.rows().forEachRemaining(b -> aggregator.add(cellToBinding, b));

        return cellToBinding;
    }

    public void refreshStats() {
        clearCells();
    }

    public void loadProjectGrid() {
        Model model = MobyDexRdfApiRaw.loadProjectGrid(2);

        Table bindings = QueryExec.graph(model.getGraph())
            .query("""
                PREFIX geo: <http://www.opengis.net/ont/geosparql#>

                SELECT * {
                  ?s geo:hasGeometry/geo:asWKT ?wkt
                }
            """).table();


        Set<Geometry> detectedGeometries = ResultSetMapRendererL.addBindingsToLayer(converter, connectionGroup, bindings.iterator(null),
                (layer, b, v) -> {
                    Node s = b.get("s");
                    String str = s.toString();
                    cellIdToLayer.put(str, (LPath<?>)layer);
                    layer.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked('" + str + "')");
//                    layer.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.target.options)");
                });
        if (!detectedGeometries.isEmpty()) {
            LLatLngBounds bounds = converter.convert(JtsUtils.envelope(detectedGeometries));
            mapContainer.getlMap().flyToBounds(bounds, new LMapZoomPanOptions().withDuration(0.5));
            // Setting options is currently broken:
            //   https://github.com/xdev-software/vaadin-maps-leaflet-flow/issues/330
//            LMapZoomPanOptions opts = new LMapZoomPanOptions();
//            opts.setDuration(1.0);
//            map.flyToBounds(bounds, opts);
        }


    }

    private MapContainer createLMap() {
        // Create and add the MapContainer (which contains the map) to the UI
        MapContainer mapContainer = new MapContainer(
                reg, map ->
                // This needs to be done after the map was initially resized
                // otherwise the view is calculated incorrectly
                map.fitWorld()
            );
        mapContainer.setSizeFull();
        mapContainer.getlMap().fixInvalidSizeAfterCreation(ID);
        this.add(mapContainer);

        LMap map = mapContainer.getlMap();

        // Add a (default) TileLayer so that we can see something on the map
        map.addLayer(LTileLayer.createDefaultForOpenStreetMapTileServer(reg));

        // Set what part of the world should be shown
        map.setView(new LLatLng(reg, 49.6751, 12.1607), 17);


//        LLatLng locationSchnitzel = new LLatLng(this.reg, 49.673800, 12.160113);
//        LMarker markerSchnitzel = new LMarker(this.reg, locationSchnitzel)
            // .bindPopup("Schnitzel")
//            ;

        List<LLatLng> points = List.of(
                new LLatLng(reg, 51.51, -0.12),
                new LLatLng(reg, 51.49, -0.08),
                new LLatLng(reg, 51.50, -0.06),
                new LLatLng(reg, 51.51, -0.12)  // close the ring
            );

        LPolylineOptions options = new LPolylineOptions();
        options.setColor("blue");
        options.setFillColor("lightblue");
        options.setFillOpacity(0.5);
        // options.set... other style props, popup, tooltip etc.

        LPolygon polygon = new LPolygon(reg, points, options);

        // Optional: attach popup / tooltip directly (shows on click/hover)
        polygon.bindPopup("This is polygon X!<br>ID: 123");
        polygon.bindTooltip("Polygon X");
        polygon.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.target.options)");
        // polygon.on("click", "e => console.log(e)");

        // Add to map (or to a LayerGroup / FeatureGroup first if managing many)
        map.addLayer(polygon);

        // See also https://vaadin.com/docs/latest/create-ui/element-api/client-server-rpc
        map.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.latlng)");
        // locationSchnitzel.list



        // Create a new marker
//        new LMarker(reg, new LLatLng(reg, 49.6756, 12.1610))
//            // Bind a popup which is displayed when clicking the marker
//            .bindPopup("XDEV Software")
//            // Add it to the map
//            .addTo(map);
        return mapContainer;
    }

    LLayer<?> getSelectedLayer() {
        LLayer<?> layer = cellIdToLayer.get(selectedCell);
        return layer;
    }

    // This server side method will be called when the map is clicked
    @ClientCallable
    public void mapClicked(JsonNode input)
    {
        if (input.isString()) {
            selectedCell = input.asString();
        }

        LLayer<?> layer = getSelectedLayer();

        if (layer instanceof LPath<?> p) {
            LPolylineOptions options = new LPolylineOptions();
            options.setColor("red");
            p.setStyle(options);
            refreshStats();
        }

        System.out.println("GOT CLICK: " + input);
        if(!(input instanceof final ObjectNode obj))
        {
            return;
        }

        // System.out.println("Map clicked - lat: {}, lng: {}" + obj.get("lat").asDouble() + " " + obj.get("lng").asDouble());
    }

}

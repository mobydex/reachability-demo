package org.aksw.mobydex.demo.view;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.primitives.Ints;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

import org.aksw.commons.index.StorageComposers;
import org.aksw.jena_sparql_api.vaadin.util.GridLike;
import org.aksw.jena_sparql_api.vaadin.util.GridWrapperBase;
import org.aksw.jena_sparql_api.vaadin.util.VaadinSparqlUtils;
import org.aksw.jenax.arq.util.binding.BindingUtils;
import org.aksw.jenax.arq.util.tuple.adapter.TupleBridgeBinding;
import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.aksw.jenax.sparql.fragment.api.Fragment;
import org.aksw.jenax.sparql.fragment.api.Fragment2;
import org.aksw.jenax.vaadin.component.grid.sparql.GridSparqlBinding;
import org.aksw.mobydex.demo.CellStyles;
import org.aksw.mobydex.demo.MainLayout;
import org.aksw.mobydex.demo.appstate.AppState;
import org.aksw.mobydex.demo.backend.FileCache;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.MobyDexRdfApiRaw;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.aksw.mobydex.demo.backend.OsmRdfApi.ElementTransformInjectNamedElement;
import org.aksw.mobydex.demo.backend.ProjectDao;
import org.aksw.mobydex.demo.backend.loader.GridComputationLoadTask;
import org.aksw.mobydex.demo.component.TabSheet;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.MobyDexRdfAccess;
import org.aksw.mobydex.demo.domain.Project;
import org.aksw.mobydex.demo.interpolation.Scale;
import org.aksw.vaadin.jena.geo.leafletflow.JtsToLMapConverter;
import org.aksw.vaadin.jena.geo.leafletflow.JtsUtils;
import org.aksw.vaadin.jena.geo.leafletflow.ResultSetMapRendererL;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.http.QueryExecHTTP;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableAsync;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.Scales.ScaleAxis;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;
import software.xdev.vaadin.chartjs.ChartContainer;
import software.xdev.vaadin.maps.leaflet.MapContainer;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLng;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLngBounds;
import software.xdev.vaadin.maps.leaflet.layer.LLayer;
import software.xdev.vaadin.maps.leaflet.layer.LLayerGroup;
import software.xdev.vaadin.maps.leaflet.layer.raster.LTileLayer;
import software.xdev.vaadin.maps.leaflet.layer.vector.LPath;
import software.xdev.vaadin.maps.leaflet.layer.vector.LPolylineOptions;
import software.xdev.vaadin.maps.leaflet.map.LMap;
import software.xdev.vaadin.maps.leaflet.map.LMapZoomPanOptions;
import software.xdev.vaadin.maps.leaflet.registry.LComponentManagementRegistry;
import software.xdev.vaadin.maps.leaflet.registry.LDefaultComponentManagementRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Route(value = "reachability", layout = MainLayout.class)
@PageTitle("Demo")
@RouteScope
@RouteScopeOwner(MainLayout.class)
// @PreserveOnRefresh
// @Push(PushMode.AUTOMATIC)
@EnableAsync
public class ReachabilityView extends VerticalLayout
    implements BeforeEnterObserver, AfterNavigationListener, BeforeLeaveObserver
{
    private static final Logger logger = LoggerFactory.getLogger(ReachabilityView.class);

    private static final long serialVersionUID = 1L;

    protected LComponentManagementRegistry reg = new LDefaultComponentManagementRegistry(this);
    protected JtsToLMapConverter converter;

    protected MapContainer mapContainer;
    protected ChartContainer reachabilityChart = new ChartContainer();

    protected ChartContainer cellDetailsChart = new ChartContainer();

    protected LLayerGroup overviewGridLayerGroup;

    protected LLayerGroup gridLayerGroup;
    private LLayerGroup markerLayerGroup;

    private String ID;

    private volatile UI ui;

    protected CellSelectionMode cellSelectionMode = CellSelectionMode.FOCUS;

    protected GridComputationLoadTask gridComputationLoadTask = null;

    private final CompletableSubject mapReady = CompletableSubject.create();


//    protected Timer gridUpdateTimer = new Timer();
//    protected volatile TimerTask gridUpdateTask = null;
//    protected BlockingQueue<GridCell> overviewGridQueue = new LinkedBlockingDeque<>();

//    protected void processOverviewGridQueue() {
//        System.out.println("PROCESSING QUEUE");
//        List<GridCell> tasks = new ArrayList<>();
//        overviewGridQueue.drainTo(tasks);
//
//        for (GridCell cell : tasks) {
//            String cellId = cell.getURI();
//            LPath<?> cellPath = cellIdToLayer.get(cellId);
//            System.out.println("Geom for " + cellId + " -> " + cellPath);
//            LPolylineOptions style = new LPolylineOptions();
//            CellStyles.purple(style);
//            cellPath.setStyle(style);
//        }
//    }

    protected void paintGridCell(GridCell gridCell) {
        // get the tags
        Set<Binding> tagSet = new HashSet<>();
        Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toTable().rows().forEachRemaining(tagSet::add);
        int tagSetSize = tagSet.size();

        Table table = gridComputationLoadTask.getMap().getIfPresent(gridCell.asNode());
        long match = 0;
        for (Binding b : (Iterable<Binding>)() -> table.rows()) {
//            Binding tag = BindingUtils.project(b, "co", "cop");
//            if (tagSet.contains(tag)) {
//                ++match;
//            }

            Number duration = BindingUtils.getNumberNullable(b, "duration");
            if (duration != null && duration.floatValue() < durationThreshold) {
                ++match;
            }
        }

        float ratioThreshold = 0.5f;
        float ratio = match / (float)tagSetSize;

        float[] color = Scale.ofFloat(3)
            .put(0.0f, new float[] {1.0f, 0.0f, 0.0f})
            .put(0.4f, new float[] {0.9f, 0.7f, 0.1f})
            .put(0.6f, new float[] {0.0f, 0.5f, 0.0f})
            .put(0.8f, new float[] {0.0f, 1.0f, 0.0f})
            .interpolate(ratio);

        String colorStr = toColorHexString(color);

        String cellId = gridCell.getURI();
        LPath<?> cellPath = cellIdToLayer.get(cellId);
        System.out.println("Geom for " + cellId + " -> " + cellPath);
        LPolylineOptions style = new LPolylineOptions()
                .withColor(colorStr).withOpacity(0.8).withFillColor(colorStr).withFillOpacity(0.8);

        // CellStyles.purple(style);
        cellPath.setStyle(style);
    }

    public static String toColorHexString(float[] color) {
        if (color == null || color.length != 3) {
            throw new IllegalArgumentException(
                    "Color must be a 3-component RGB array");
        }

        int red = Math.round(clamp01(color[0]) * 255.0f);
        int green = Math.round(clamp01(color[1]) * 255.0f);
        int blue = Math.round(clamp01(color[2]) * 255.0f);

        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(
                    "Color components must not be NaN");
        }

        return Math.max(0.0f, Math.min(1.0f, value));
    }

//    Object myLock = new Object();
//
//    public void enqueGridCell(GridCell cell) {
//        System.out.println("Enqueing: " + cell.getURI());
//
//        overviewGridQueue.add(cell);
//
//        if (gridUpdateTask == null) {
//            // synchronized (myLock) {
//                if (gridUpdateTask == null) {
//                    // if (task != null)
//                    gridUpdateTask = new TimerTask() {
//                        @Override
//                        public void run() {
//                            System.out.println("Refreshing UI");
//                            gridUpdateTask = null;
//                            ui.accessLater(() -> {
//                                // synchronized (myLock) {
//                                    processOverviewGridQueue();
//                                // }
//                            }, null);
//                        }
//                    };
//                    gridUpdateTimer.schedule(gridUpdateTask, 1000);
//                }
//            // }
//        }
//    }

    public enum CellSelectionMode {
        FOCUS("Focus"), // Focus on the selected cell, making it the origin of reachability computations.
        INFO("Info");  // Selecting a cell shows detailed info about the cell. It does not change the reachability origin.

        private String label;

        private CellSelectionMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return getLabel();
        }
    }

    // --- Formatting ---
    protected static final DecimalFormat decimalFormat = new DecimalFormat("#0.#"); // , symbols);

    // --- State / User Settings ---

    long projectId = 2;
    long computationId = 70;

    MobyDexRdfApi mobyDexApi;

    private Map<String, LPath<?>> cellIdToLayer = new ConcurrentHashMap<>();

    private String focusCell = null;
    private String infoCell = null;
    private long durationThreshold;
    private Binding selectedPoiType;


    private TabSheet tabSheet = new TabSheet();
    private Tab reachabilityTab;
    private Tab cellDetailsTab;
    private Tab poisTab;

    private GridSparqlBinding poiReachabilityGrid = new GridSparqlBinding();

    private AppState appState;
    private ProjectDao projectDao;

    public static String fmtDurationS2M(Long durationSeconds) {
        return (durationSeconds == null || durationSeconds.equals(Long.MAX_VALUE))
            ? "-"
            : decimalFormat.format(durationSeconds / 60.0) + "min";
    }

    public ReachabilityView(
            @RouteScopeOwner(MainLayout.class) AppState appState,
            ProjectDao projectDao) {
        this.appState = appState;
        this.projectDao = projectDao;

        this.mobyDexApi = appState.getMobyDexRdfApi();

        setSizeFull();

        ID = "my-map-view-" + System.nanoTime();
        setId(ID);

        // setSizeFull();

        HorizontalLayout controlBar = new HorizontalLayout();

        Button loadGridBtn = new Button("LoadGrid");
        add(loadGridBtn);
        loadGridBtn.addClickListener(ev -> {
            loadProjectGrid();
            refreshStats();
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

        // DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        // DecimalFormat decimalFormat = new DecimalFormat("#0.##"); // , symbols);
        durationCapSelect.setItems(values);
        durationCapSelect.setTextRenderer(ReachabilityView::fmtDurationS2M);
        durationCapSelect.setValue(durationThreshold);
        durationCapSelect.addValueChangeListener(ev -> {
            durationThreshold = ev.getValue();
            refreshStats();
        });
        controlBar.add(durationCapSelect);
        add(controlBar);

        RadioButtonGroup<CellSelectionMode> radioGroup = new RadioButtonGroup<>();
        radioGroup.addThemeVariants(RadioGroupVariant.AURA_HORIZONTAL);
        radioGroup.setLabel("Cell Selection Mode");
        radioGroup.setItems(CellSelectionMode.FOCUS, CellSelectionMode.INFO);
        radioGroup.setValue(cellSelectionMode);
        // radioGroup.setItemLabelGenerator(item -> StringUtils.toUpperCamelCase(item.toString()));
        radioGroup.addValueChangeListener(ev -> {
            cellSelectionMode = ev.getValue();
        });
        // radioGroup.setReadOnly(true);
        add(radioGroup);

        SplitLayout mapAndChartSplit = new SplitLayout(Orientation.VERTICAL);
        add(mapAndChartSplit);
        mapAndChartSplit.setSizeFull();
        mapAndChartSplit.setSplitterPosition(50.0);
        mapAndChartSplit.addSplitterDragEndListener(ev -> {
            mapContainer.getlMap().invalidateSize(false);
        });

        mapContainer = createLMap(mapReady);
        mapAndChartSplit.addToPrimary(mapContainer);
        converter = new JtsToLMapConverter(reg);

        markerLayerGroup = new LLayerGroup(reg);
        mapContainer.getlMap().addLayer(markerLayerGroup);


        overviewGridLayerGroup = new LLayerGroup(reg);
        mapContainer.getlMap().addLayer(overviewGridLayerGroup);
        // mapContainer.getlMap().rem


        gridLayerGroup = new LLayerGroup(reg);
        mapContainer.getlMap().addLayer(gridLayerGroup);



        // mapContainer.setWidth("400px");
        mapContainer.setWidthFull();
        mapContainer.setHeight("400px");

        reachabilityTab = tabSheet.add("Reachability", reachabilityChart);
        poisTab = tabSheet.add("Pois", poiReachabilityGrid);
        cellDetailsTab = tabSheet.add("Cell Details", cellDetailsChart);

        // Assumes that this code is in some kind of Vaadin component or view
        mapAndChartSplit.addToSecondary(tabSheet);

        // chart.setWidth("400px");
        reachabilityChart.setWidthFull();
        reachabilityChart.setMaxHeight("300px");


        poiReachabilityGrid.addSelectionListener(ev -> {
            Binding b = ev.getFirstSelectedItem().orElse(null);
            markerLayerGroup.clearLayers();

            if (b == null) {
                return;
            }

            Node destCell = b.get("destCell");

            Model projectGridModel = MobyDexRdfApiRaw.loadProjectGrid(projectId);

            Table geoms = QueryExec.graph(projectGridModel.getGraph())
                .query("""
                    PREFIX geo: <http://www.opengis.net/ont/geosparql#>
                    SELECT ?wkt { ?s geo:hasGeometry/geo:asWKT ?wkt }
                """)
                .substitution("s", destCell)
                .table();

            Table tags = TableFactory.builder().addRowAndVars(BindingUtils.project(b, "cp", "co")).build();
            Query query = OsmRdfApi.createQueryExportPois(Fragment.of(geoms).toFragment1(), Fragment.of(tags).toFragment2());

            Table poiTable = QueryExecHTTP.service("https://data.aksw.org/mobydex")
                .query(query).table();

            Set<Geometry> detectedGeometries = ResultSetMapRendererL.addBindingsToLayer(converter, markerLayerGroup,
                    poiTable.rows(), (layer, binding, v) -> {
//                        Node s = b.get("s");
//                        String str = s.toString();
//                        cellIdToLayer.put(str, (LPath<?>) layer);
                        // layer.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked('" + str + "')");
//                        layer.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.target.options)");
                    });

//            Node geoNode = projectGridModel.wrapAsResource(destCell)
//            		.getProperty(Geo.HAS_GEOMETRY_PROP)

        });
    }

    /**
     * Table structure:
     *
     * <pre>
     *   originCell | poiType (cp co) | destCell | duration
     * </pre>
     *
     * @param poiTypeToCells
     */
    public void setPoiDurationTable(Table poiTypeToCells) {
        // TupleBridgeBinding bridge = TupleBridgeBinding.ofVarNames("originCell", "cp", "co", "destCell", "duration");

        List<Binding> list = Iter.toList(poiTypeToCells.rows());
        // Sort the bindings by the duration
        Comparator<Binding> durationComparator = Comparator.nullsLast((x, y) -> Ints.saturatedCast(
                BindingUtils.getNumber(y, OsmRdfApi.DURATION_COL).longValue() - BindingUtils.getNumber(x, OsmRdfApi.DURATION_COL).longValue()));

        Collections.sort(list, durationComparator);

        Function<Binding, Binding> projectPoiType = b -> BindingUtils.project(b, "cp", "co");

        // For each duration: how many poi types have been covered.
        Set<Binding> allPoiTypes = list.stream().map(projectPoiType).collect(Collectors.toSet());


        NavigableMap<Long, List<Binding>> durationToPoiTypes = list.stream().collect(
                Collectors.groupingBy(b -> BindingUtils.tryGetNumber(b, OsmRdfApi.DURATION_COL).map(Number::longValue).orElse(Long.MAX_VALUE),
                        TreeMap::new,
                        Collectors.mapping(projectPoiType, Collectors.toList())));

//        List<Long> sortedDurations = durationToPoiTypes.keySet().stream().collect(Collectors.toList());
//        Collections.sort(sortedDurations, Comparator.nullsLast(Comparator.naturalOrder()));

        //      chart.showChart(new BarChart(
//      new BarData().addLabels("A", "B").addDataset(new BarDataset().setLabel("X").addData(1).addData(2)))
//      .toJson());

        BarDataset successDataset = new BarDataset()
            .setLabel("Success")
            .setBackgroundColor("#BAFFC9") // Soft pastel green
            .setBorderColor("#77DD77")     // Slightly darker border for definition
            .setBorderWidth(1)
            .setStack("stack1"); // Assign to a stack group

        // 3. Create Failure Dataset (Red)
        BarDataset failureDataset = new BarDataset()
            .setLabel("Failure")
            .setBackgroundColor("#FFB3BA") // Soft pastel red/pink
            .setBorderColor("#FF6961")     // Slightly darker border
            .setBorderWidth(1)
            .setStack("stack1"); // Assign to the SAME stack group

        // BarDataset barDataset = new BarDataset();
        successDataset.setLabel("On-Time POI type ratio");
        failureDataset.setLabel("Late-Arrival POI type ratio");
        Set<Binding> successPoiTypes = new HashSet<>();
        Set<Binding> failurePoiTypes = new HashSet<>();

        BarData barData = new BarData();
        for (var e : durationToPoiTypes.entrySet()) {
            Long duration = e.getKey();
            barData.addLabel(fmtDurationS2M(duration));

            if (duration != null && duration < durationThreshold) {
                successPoiTypes.addAll(e.getValue());
            } else {
                failurePoiTypes.addAll(e.getValue());
            }
            double successRatio = successPoiTypes.size() / (double)allPoiTypes.size();
            double failureRatio = failurePoiTypes.size() / (double)allPoiTypes.size();
            successDataset.addData(successRatio);
            failureDataset.addData(failureRatio);
        }

        // barData.addDataset(barDataset);
        barData.addDataset(successDataset);
        barData.addDataset(failureDataset);

        BarOptions options = new BarOptions()
                .setResponsive(true)
                .setScales(new Scales()
                    // .addScale(ScaleAxis.X, new LinearScaleOptions().setStacked(true))
                    .addScale(ScaleAxis.Y, new LinearScaleOptions().setStacked(true)));

        BarChart barChart = new BarChart(barData).setOptions(options);
        reachabilityChart.showChart(barChart.toJson());

//        mapContainer.addAttachListener(ev -> {
//            loadProjectGrid();
//            clearCells();
//        });
    }

    // TODO Misleading name - it colors the focus cell; it does not paint the overview grid
    // Reset the style of all cells - and colors the focus cell!
    public void clearCells() {
        Map<Node, List<Binding>> cellToBinding = computePoiDurations(focusCell);
        for (String cellId : cellIdToLayer.keySet()) {
            colorizeReachableCells(cellToBinding, cellId);
        }
    }

    private void colorizeReachableCells(Map<Node, List<Binding>> cellToBinding, String cellId) {
        LPath<?> cellPath = cellIdToLayer.get(cellId);

        LPolylineOptions style = new LPolylineOptions();
        CellStyles.grey(style); // default color

        Node cellNode = NodeFactory.createURI(cellId);
        List<Binding> bindings = cellToBinding.getOrDefault(cellNode, List.of());
        Long poiDuration = null;
        for (Binding b : bindings) {
            Long duration = BindingUtils.tryGetNumber(b, "duration").map(Number::longValue).orElse(null);
            poiDuration = duration;
            if (duration != null) {
                if (duration < durationThreshold) {
                    CellStyles.green(style);
                } else {
                    CellStyles.red(style);
                }
            }
        }
        if (poiDuration != null) {
            String popupStr = "Reachable in " + fmtDurationS2M(poiDuration) + "<br />"
                              + bindings.stream().map(b -> toOsmLabel(b, "cp", "co")).collect(Collectors.joining("<br />"));
            cellPath.bindPopup(popupStr);
        }

        if (Objects.equals(cellId, focusCell)) {
            CellStyles.purple(style);
            // CellStyles.selected(style);
        }
        cellPath.setStyle(style);
    }

    public Table computePoiDurationsTable(String originCellIdStr) {
        if (originCellIdStr == null) {
            originCellIdStr = "urn:x-mobydex:absent-cell";
        }

        // FIXME Get the ID via the project model!
        String prefix = "https://mobydex.locoslab.com/controller-service/projects/2#cell";
        if (!originCellIdStr.startsWith(prefix)) {
            return TableFactory.create();
        }

        String tmp = originCellIdStr.substring(prefix.length());
        long originCellId = Long.parseLong(tmp);

        // long originCellId = 271;

        // Fragment osmPoiTable =
        // Fragment.of(OsmRdfApi.getPoiCategories()).toFragment3();


        // TODO Need to migrate to
        // GridComputationLoadTask

        // Project project = projectDao.fetchItem(originCellId);
        Model projectModel = mobyDexApi.loadProjectGrid(projectId);
        Project project = MobyDexRdfAccess.getProject(projectModel);

        // Model projectGridModel = mobyDexApi.loadProjectGrid(projectId);
        Resource originCell = mobyDexApi.loadComputation(projectId, computationId, originCellId);

        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();

        // Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(projectId, tagsFragment);
        // Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
        Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(project, tagsFragment);

        // Node durationProperty = NodeFactory.createURI("http://www.example.org/durationMin");
        Table poiTypeToCells = OsmRdfApi.createQueryPoiTypeInRange(originCell, poiTypeHistogramModel, tagsFragment, 1,
                MobyDexRdfApiRaw.durationProperty);
        return poiTypeToCells;
    }

    public Map<Node, List<Binding>> computePoiDurations(String originCellIdStr) {
        Table poiTypeToCells = computePoiDurationsTable(originCellIdStr);
        Query query = Fragment.of(poiTypeToCells).projectVarNames("destCell", "cp", "co", "duration").toQuery();

        // Query query = QueryUtils.tableToQuery(poiTypeToCells);
        QueryExecutionFactoryQuery qef = q -> QueryExecution.dataset(DatasetFactory.empty()).query(q).build();

        GridLike<Binding> wrappedGrid = GridWrapperBase.wrap(poiReachabilityGrid);
        VaadinSparqlUtils.setQueryForGridBinding(wrappedGrid, poiReachabilityGrid.getHeaderRow(), qef, query);
        VaadinSparqlUtils.configureGridFilter(wrappedGrid, poiReachabilityGrid.getFilterRow(), query.getProjectVars(),
                var -> str -> VaadinSparqlUtils.createFilterExpr(var, str).orElse(null));


        // FIXME Updating the view does not belong here
        setPoiDurationTable(poiTypeToCells);

        TupleBridgeBinding bridge = TupleBridgeBinding.ofVarNames("destCell");

        var aggregator = StorageComposers.innerMap(0, HashMap::new, StorageComposers.leafList(ArrayList::new, bridge));
        Map<Node, List<Binding>> cellToBinding = aggregator.newStore();
        poiTypeToCells.rows().forEachRemaining(b -> aggregator.add(cellToBinding, b));

        return cellToBinding;
    }

    public void refreshStats() {
        clearCells();
    }

    public void refreshCellDetails() {
        if (infoCell == null) {
            return;
        }

        Node cellNode = NodeFactory.createURI(infoCell);
        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
        Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(projectId, tagsFragment);

        Query baseQuery = QueryFactory.create("""
            PREFIX eg: <http://www.example.org/>

            SELECT ?cell ?cp ?co ?count {
              SERVICE <elt:tags> { }
              LATERAL {
                OPTIONAL {
                  ?cell
                    eg:hasPoiHistogram ?cellTypeHist .
                  ?cellTypeHist
                    eg:cp ?cp ;
                    eg:co ?co ;
                    eg:count ?count
                }
                # BIND(IF(bound(?cnt), ?cnt, 0) AS ?count)
              }
            } ORDER BY ?co ?cp
            """);
        Element tagsElt = tagsFragment.rename("cp", "co").getElement();

        Query query = QueryTransformOps.transform(baseQuery,
            ElementTransformInjectNamedElement.of(Map.of("elt:tags", tagsElt)));

        Table table = QueryExec.graph(poiTypeHistogramModel.getGraph()).query(query)
            .substitution("cell", cellNode)
            .table();

        List<Binding> bindings = Iter.toList(table.rows());
//        Function<Binding, Binding> projectPoiType = b -> BindingUtils.project(b, "cp", "co");
//
//        String COL = "count";
//        NavigableMap<Long, List<Binding>> countToPoiTypes = bindings.stream().collect(
//            Collectors.groupingBy(b -> BindingUtils.tryGetNumber(b, COL).map(Number::longValue).orElse(0l),
//                () -> new TreeMap<>(Comparator.reverseOrder()),
//                Collectors.mapping(projectPoiType, Collectors.toList())));


        // TODO: Color pois based on relation to the focus cell:
        // 4 categories: poi within best reachable cell, poi within duration limit, poi outside of duration limit, poi not present.
//        BarDataset successDataset = new BarDataset()
//            .setLabel("Success")
//            .setBackgroundColor("#BAFFC9") // Soft pastel green
//            .setBorderColor("#77DD77")     // Slightly darker border for definition
//            .setBorderWidth(1)
//            .setStack("stack1"); // Assign to a stack group

        // 3. Create Failure Dataset (Red)
        BarDataset dataset = new BarDataset()
            .setLabel("Failure")
            // .setBackgroundColor("#FFB3BA") // Soft pastel red/pink
            // .setBorderColor("#FF6961")     // Slightly darker border
            .setBorderWidth(1);
            //.setStack("stack1"); // Assign to the SAME stack group

        // BarDataset barDataset = new BarDataset();
        // successDataset.setLabel("In-Reach POI Type Coverage by Travel Time");
        dataset.setLabel("Quantity");
        Set<Binding> successPoiTypes = new HashSet<>();

        BarData barData = new BarData();
        for (var b : bindings) {
//            String poiTypeLabel = ("" + b.get("cp")).replace("https://www.openstreetmap.org/wiki/Key:", "")
//                    + " " + b.get("co");
            Node node = b.get("co");
            String poiTypeLabel = node.isLiteral() ? node.getLiteralLexicalForm() : "" + node;
            barData.addLabel(poiTypeLabel);

            Number count = BindingUtils.getNumberNullable(b, "count");
            dataset.addData(count);
        }

        // barData.addDataset(barDataset);
        barData.addDataset(dataset);

        BarOptions options = new BarOptions()
            .setResponsive(true);
            // .setScales(new Scales()
                // .addScale(ScaleAxis.X, new LinearScaleOptions().setStacked(true))
               // .addScale(ScaleAxis.Y, new LinearScaleOptions().setStacked(true)));

        BarChart barChart = new BarChart(barData).setOptions(options);
        cellDetailsChart.showChart(barChart.toJson());
    }

    public static String toOsmLabel(Binding b, String cp, String co) {
        String poiTypeLabel = ("" + b.get(cp)).replace("https://www.openstreetmap.org/wiki/Key:", "")
        + " " + b.get(co);
        return poiTypeLabel;

    }

    public void loadProjectGrid() {
        // MobyDexRdfApi mobyDexRdfApi = MobyDexRdfApi.get();
        Project project = mobyDexApi.loadProject(2);
        Model projectModel = project.getModel();

        Table bindings = QueryExec.graph(projectModel.getGraph()).query("""
                PREFIX geo: <http://www.opengis.net/ont/geosparql#>

                SELECT * {
                  ?s geo:hasGeometry/geo:asWKT ?wkt
                }
            """).table();

        Set<Geometry> detectedGeometries = ResultSetMapRendererL.addBindingsToLayer(converter, gridLayerGroup,
                bindings.rows(), (layer, b, v) -> {
                    Node s = b.get("s");
                    String str = s.toString();
                    cellIdToLayer.put(str, (LPath<?>) layer);
                    layer.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked('" + str + "')");
//                    layer.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.target.options)");
                });
        if (!detectedGeometries.isEmpty()) {
            LLatLngBounds bounds = converter.convert(JtsUtils.envelope(detectedGeometries));
            mapContainer.getlMap().flyToBounds(bounds, new LMapZoomPanOptions().withDuration(0.5));
            // Setting options is currently broken:
            // https://github.com/xdev-software/vaadin-maps-leaflet-flow/issues/330
//            LMapZoomPanOptions opts = new LMapZoomPanOptions();
//            opts.setDuration(1.0);
//            map.flyToBounds(bounds, opts);
        }

        FileCache fileCache = mobyDexApi.getProjectCache();
        Fragment2 tagsFragment = Fragment.of(OsmRdfApi.getPoiCategories()).project(0, 1).toFragment2();
        Model poiTypeHistogramModel = mobyDexApi.loadAndCachePoiHistogramModel(project, tagsFragment);
        gridComputationLoadTask = new GridComputationLoadTask(fileCache, 1, project, 70, mobyDexApi, poiTypeHistogramModel, tagsFragment);

//        System.out.println("STARTING LOAD");

        gridComputationLoadTask.flow()
            .subscribeOn(Schedulers.io(), false)
            .buffer(1000, TimeUnit.MILLISECONDS, 50)
            .filter(buffer -> !buffer.isEmpty())
            .forEach(gridCells -> {
                System.out.println(String.format("Received batch of %d cells", gridCells.size()));
                ui.access(() -> {
                    for (GridCell cell : gridCells) {
                        paintGridCell(cell);
                    }
                }); //, null);
            // logger.info("Loaded: " + gridCell.getURI());
            //enqueGridCell(gridCell);
            // gridComputationLoadTask.loadCell(computationId, null)
            });

//        System.out.println("DONE LOAD");

        // gridComputationLoadTask.startBackgroundLoading();
    }

    private MapContainer createLMap(CompletableSubject mapReady) {
        // Create and add the MapContainer (which contains the map) to the UI
        MapContainer mapContainer = new MapContainer(reg, map -> {
            // This needs to be done after the map was initially resized
            // otherwise the view is calculated incorrectly
            map.fitWorld();
            mapReady.onComplete();
            });

        mapContainer.setSizeFull();
        mapContainer.getlMap().fixInvalidSizeAfterCreation(ID);
        this.add(mapContainer);

        LMap map = mapContainer.getlMap();

        // Add a (default) TileLayer so that we can see something on the map
        map.addLayer(LTileLayer.createDefaultForOpenStreetMapTileServer(reg));

        // Set what part of the world should be shown
        map.setView(new LLatLng(reg, 49.6751, 12.1607), 17);

        // See also
        // https://vaadin.com/docs/latest/create-ui/element-api/client-server-rpc
        map.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.latlng)");
        // locationSchnitzel.list

        return mapContainer;
    }

    LLayer<?> getFocusLayer() {
        LLayer<?> layer = cellIdToLayer.get(focusCell);
        return layer;
    }

    LLayer<?> getDetailsLayer() {
        LLayer<?> layer = cellIdToLayer.get(infoCell);
        return layer;
    }

    /**
     * This server side method will be called when the map is clicked.
     */
    @ClientCallable
    public void mapClicked(JsonNode input) {
        if (input.isString()) {
            String str = input.asString();
            switch (cellSelectionMode) {
            case FOCUS:
                focusCell = str;
                LLayer<?> layer = getFocusLayer();
                if (layer instanceof LPath<?> p) {
                    tabSheet.setSelectedTab(reachabilityTab);
                    refreshStats();
                }
                break;
            case INFO:
                infoCell = str;
                LLayer<?> layer2 = getDetailsLayer();
                if (layer2 instanceof LPath<?> p) {
                    tabSheet.setSelectedTab(cellDetailsTab);
                    refreshCellDetails();
                }
                break;
            default:
                throw new RuntimeException("Unsupported select mode: " + cellSelectionMode);
            }
        }
//        LLayer<?> layer = getSelectedLayer();
//        if (str != null && layer instanceof LPath<?> p) {
//            LPolylineOptions options = new LPolylineOptions();
//            options.setColor("red");
//            p.setStyle(options);
//            refreshStats();
//        }

        System.out.println("GOT CLICK: " + input);
        if (!(input instanceof final ObjectNode obj)) {
            return;
        }

        // System.out.println("Map clicked - lat: {}, lng: {}" +
        // obj.get("lat").asDouble() + " " + obj.get("lng").asDouble());
    }


    private Disposable projectStateDisposable;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // ui = event.getUI();
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        ui = event.getUI();
        projectStateDisposable = mapReady.andThen(appState.projectState()).subscribe(ev -> {
            loadProjectGrid();
            // refreshStats();
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (projectStateDisposable != null) {
            projectStateDisposable.dispose();
        }
        super.onDetach(detachEvent);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
    }
}



// Create a new marker
//new LMarker(reg, new LLatLng(reg, 49.6756, 12.1610))
//    // Bind a popup which is displayed when clicking the marker
//    .bindPopup("XDEV Software")
//    // Add it to the map
//    .addTo(map);

//LLatLng locationSchnitzel = new LLatLng(this.reg, 49.673800, 12.160113);
//LMarker markerSchnitzel = new LMarker(this.reg, locationSchnitzel)
// .bindPopup("Schnitzel")
//  ;

//List<LLatLng> points = List.of(new LLatLng(reg, 51.51, -0.12), new LLatLng(reg, 51.49, -0.08),
//      new LLatLng(reg, 51.50, -0.06), new LLatLng(reg, 51.51, -0.12) // close the ring
//);

//LPolylineOptions options = new LPolylineOptions();
//options.setColor("blue");
//options.setFillColor("lightblue");
//options.setFillOpacity(0.5);
// options.set... other style props, popup, tooltip etc.

//LPolygon polygon = new LPolygon(reg, points, options);
//
//// Optional: attach popup / tooltip directly (shows on click/hover)
//polygon.bindPopup("This is polygon X!<br>ID: 123");
//polygon.bindTooltip("Polygon X");
//polygon.on("click", "e => document.getElementById('" + ID + "').$server.mapClicked(e.target.options)");
// polygon.on("click", "e => console.log(e)");

// Add to map (or to a LayerGroup / FeatureGroup first if managing many)
//map.addLayer(polygon);

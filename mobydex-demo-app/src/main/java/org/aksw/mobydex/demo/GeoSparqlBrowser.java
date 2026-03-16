package org.aksw.mobydex.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import com.google.common.util.concurrent.MoreExecutors;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import org.aksw.jena_sparql_api.vaadin.util.GridWrapper;
import org.aksw.jena_sparql_api.vaadin.util.GridWrapperBase;
import org.aksw.jena_sparql_api.vaadin.util.VaadinSparqlUtils;
import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.aksw.jenax.stmt.util.QueryParseExceptionUtils;
import org.aksw.jenax.vaadin.component.grid.sparql.GridSparqlBinding;
import org.aksw.vaadin.jena.geo.leafletflow.ResultSetMapRendererL;
import org.aksw.vaadin.yasqe.Yasqe;
import org.aksw.vaadin.yasqe.YasqeConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

import software.xdev.vaadin.maps.leaflet.MapContainer;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLng;
import software.xdev.vaadin.maps.leaflet.layer.LLayerGroup;
import software.xdev.vaadin.maps.leaflet.layer.raster.LTileLayer;
import software.xdev.vaadin.maps.leaflet.map.LMap;
import software.xdev.vaadin.maps.leaflet.registry.LComponentManagementRegistry;
import software.xdev.vaadin.maps.leaflet.registry.LDefaultComponentManagementRegistry;

class Futures {
    public static <T> CompletableFuture<T> adapt(Consumer<CompletableFuture<T>> resolve) {
        CompletableFuture<T> result = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> resolve.accept(result));

        return result;
    }
}


/**
 * A generic SPARQL viewer based on Vaadin that is somewhat similar to yasgui.
 * Map visualition: done
 * TODO Pivot table needs further integration
 *
 * @author raven
 *
 */
// @Route("sparql")
// @Push(PushMode.AUTOMATIC)
public class GeoSparqlBrowser extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    protected Executor executor;

    protected QueryExecutionFactoryQuery qef;

    protected LComponentManagementRegistry reg = new LDefaultComponentManagementRegistry(this);


    // protected TextArea textArea;
    // protected AceEditor textArea;
    protected Yasqe yasqe;

//    protected Button runBtn;
    protected GridSparqlBinding resultSetGrid;
    protected MapContainer mapContainer;

//    protected TileLayer lightLayer = new TileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png");
//    protected TileLayer darkLayer = new TileLayer("http://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}.png");

    // protected LayerGroup connectionGroup = new FeatureGroup();
    protected LLayerGroup connectionGroup;

    private String ID;

    // @Autowired
    public GeoSparqlBrowser(QueryExecutionFactoryQuery qef) {
        this.qef = qef;

        ID = "geosparql-browser-" + System.nanoTime();
        setId(ID);

        this.executor = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor)Executors.newCachedThreadPool());

        setSizeFull();

        // connections table and map
        HorizontalLayout inputRow = new HorizontalLayout();

        HorizontalLayout availableVars = new HorizontalLayout();
        HorizontalLayout pivotColumnVars = new HorizontalLayout();

//        SortableLayout pivotColumnVarsSortable = new SortableLayout(pivotColumnVars);
//        pivotColumnVarsSortable.addSortableComponentReorderListener(ev -> {
//            System.out.println("Children:");
//            pivotColumnVarsSortable.getChildren().forEach(x -> System.out.println("Child: " + x));
//        });

        pivotColumnVars.add("PIVOT");
        DropTarget<?> tgt = DropTarget.create(pivotColumnVars);
        tgt.addDropListener(ev -> ev.getDragSourceComponent().ifPresent(pivotColumnVars::add));


        inputRow.setWidthFull();
        inputRow.setMinHeight("50%");
        // textArea = new TextArea();

        YasqeConfig config = new YasqeConfig();
        config.setPersistenceId("yasqe-persistenceid-1");

        yasqe = new Yasqe(config);
        yasqe.setWidthFull();

//        textArea = new AceEditor();
//        textArea.setWidthFull();
//        textArea.setMode(AceMode.sparql);
//        textArea.setTheme(AceTheme.chrome);
//        textArea.setFontSize(18);
//
//        textArea.addValueChangeListener(ev -> {
//            String value = ev.getValue();
//            System.out.println("Update: " + value);
//            WebStorage.setItem("activeQuery", value);
//        });
//
//        WebStorage.getItem("activeQuery", v -> {
//            textArea.setValue(v);
//        });



//        Button autoFixBtn = new Button(new Icon(VaadinIcon.CHECK_CIRCLE));
//        autoFixBtn.getElement().setAttribute("title", "Validate Input");
//
//        autoFixBtn.addClickListener(ev -> {
//            String value = textArea.getValue();
//            try {
//                Query query = QueryFactory.create(value);
//                System.out.println(query);
//            } catch (QueryParseException e) {
//                int pos[] = QueryParseExceptionUtils.parseLineAndCol(e);
//                if (pos != null) {
//                    textArea.addMarker(new AceMarker(pos[0], pos[1], pos[0], pos[1] + 5, AceMarkerColor.red));
//                    // textArea.setHighlightActiveLine(false);
//                    // textArea.setHighlightSelectedWord(false);
//                }
//                System.out.println(Arrays.toString(pos));
//            }

//        	textArea.sync();
//        	textArea.addSyncCompletedListener(() -> {
//
//        	});
//        });

        // runBtn = new Button(new Icon(VaadinIcon.PLAY));
        // runBtn.addClickListener(ev -> {
            ///textArea.removeAllMarkers();

        yasqe.addQueryButtonListener(ev -> {
            String queryString = ev.getValue();
            Query query;
            try {
                query = QueryFactory.create(queryString);
            } catch (Exception e) {
                String msg = ExceptionUtils.getRootCauseMessage(e);

                if (e instanceof QueryParseException) {
                    QueryParseException qpe = (QueryParseException)e;
                    int[] lineAndCol = QueryParseExceptionUtils.parseLineAndCol(qpe);
                    if (lineAndCol != null) {
                        // TODO The Vaadin AceEditor wrapper does not seem to support annotations
                        // Markers do not seem to be a replacement
                        // AceMarker marker = new AceMarker(lineAndCol[0] - 1, lineAndCol[1] - 1, lineAndCol[0], 0, AceMarkerColor.red);
                        // textArea.addMarker(marker);
                    }
                }

                Notification n = new Notification(msg, 10000);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.open();
                return;
            }

            GridWrapper<Binding> wrappedGrid = GridWrapperBase.wrap(resultSetGrid);
            VaadinSparqlUtils.setQueryForGridBinding(wrappedGrid, resultSetGrid.getHeaderRow(), qef, query);
            VaadinSparqlUtils.configureGridFilter(wrappedGrid, resultSetGrid.getFilterRow(), query.getProjectVars(),
                    var -> str -> VaadinSparqlUtils.createFilterExpr(var, str).orElse(null));

            resultSetGrid.getDataCommunicator().enablePushUpdates(executor);
            resultSetGrid.getLazyDataView().setItemCountEstimate(1000);
            resultSetGrid.getLazyDataView().setItemCountEstimateIncrease(1000);

            // Currently needed to reflect the changes by VaadinSparqlUtils
            // resultSetGrid.updateDataProviderListenerRegistration();

            availableVars.removeAll();
            for (Var var : query.getProjectVars()) {
                Span span = new Span(var.getName());
                availableVars.add(span);

                DragSource<?> src = DragSource.create(span);
                src.setDragData(var);
            }
        });


//        map = createConnectionsMap();
//        map.setWidthFull();
//        LayerGroup group = new FeatureGroup();
//        group.addTo(map);
        mapContainer = createLMap();
        mapContainer.setSizeUndefined();
        mapContainer.setWidthFull();

        connectionGroup = new LLayerGroup(reg);
        mapContainer.getlMap().addLayer(connectionGroup);

        // Grid<Binding> resultSetGrid = new Grid<>();
        resultSetGrid = new GridSparqlBinding();
        resultSetGrid.setMultiSort(true);
        resultSetGrid.setSelectionMode(SelectionMode.MULTI);
        resultSetGrid.getSelectionModel().addSelectionListener(ResultSetMapRendererL.createGridListener(mapContainer.getlMap(), connectionGroup));
        resultSetGrid.setPageSize(100);

        resultSetGrid.setEmptyStateText("No data to display");
        resultSetGrid.setSizeFull();

        inputRow.add(yasqe);
        inputRow.setFlexGrow(1, yasqe);

//        inputRow.add(runBtn);
//        inputRow.add(autoFixBtn);

        inputRow.add(mapContainer);
        inputRow.setFlexGrow(1, mapContainer);


        add(inputRow);

        add(availableVars);
        // add(pivotColumnVarsSortable);

        add(resultSetGrid);
    }

    private MapContainer createLMap() {
        // Create and add the MapContainer (which contains the map) to the UI
        MapContainer mapContainer = new MapContainer(reg);
        //this.add(mapContainer);

        LMap map = mapContainer.getlMap();

        // Add a (default) TileLayer so that we can see something on the map
        map.addLayer(LTileLayer.createDefaultForOpenStreetMapTileServer(reg));

        // Set what part of the world should be shown
        map.setView(new LLatLng(reg, 49.6751, 12.1607), 17);
        map.fixInvalidSizeAfterCreation(ID);

        // Create a new marker
//        new LMarker(reg, new LLatLng(reg, 49.6756, 12.1610))
//            // Bind a popup which is displayed when clicking the marker
//            .bindPopup("XDEV Software")
//            // Add it to the map
//            .addTo(map);
        return mapContainer;
    }

//    private LeafletMap createConnectionsMap() {
//        MapOptions options = new DefaultMapOptions();
//        options.setCenter(new LatLng(47.070121823, 19.2041015625));
//        options.setZoom(7);
//        LeafletMap map = new LeafletMap(options);
//        map.setSizeFull();
//        map.addLayer(lightLayer);
//
//        return map;
//    }
}




//textArea.getElement().addEventListener("keypress", ev -> {
//  System.out.println(ev.getEventData());
//   AceCursorPosition cursorPos = textArea.getCursorPosition();
//   String key = ev.getEventData().getString("event.key");
//   if (":".equals(key)) {
//       textArea.sync();
//        String value = textArea.getValue();
//        int pos = cursorPos.getIndex();
//        System.out.println(value);
//        System.out.println("pos: " + pos);
//        if (pos >= 1) {
//            //char c = value.charAt(pos - 1);
//            //if (':' == c) {
//                textArea.addTextAtCurrentPosition("hello!");
//                textArea.sync();
//            //}
//        }
//
//   }
//
//
//  // System.out.println("got event: " + ev.getEventData());
//  // System.out.println(ev.getType());
//  // System.out.println(ev.g);
//}).addEventData("event.key").addEventData("event.ctrlKey");

//textArea.setHighlightActiveLine(false);
//textArea.setHighlightSelectedWord(false);
//textArea.addAceChangedListener(ev -> {
//  String value = ev.getValue();
//  System.out.println("Update: " + value);
//
//  AceCursorPosition cursorPos = ev.getSource().getCursorPosition();
//  System.out.println("Pos: " + cursorPos);
//  int pos = cursorPos.getIndex();
//  if (pos >= 1) {
//      char c = value.charAt(pos -1);
//      if (':' == c) {
//          System.out.println("Found :");
//      }
//  }
//
//});


//textArea.addBlurListener(ev -> {
//  String value = ev.getSource().getValue();
//  System.out.println("Update: " + value);
//  WebStorage.setItem("activeQuery", value);
//
//  AceCursorPosition cursorPos = ev.getSource().getCursorPosition();
//  System.out.println("Pos: " + cursorPos);
//  int pos = cursorPos.getIndex();
//  if (pos >= 1) {
//      char c = value.charAt(pos -1);
//      if (':' == c) {
//          System.out.println("Found :");
//      }
//  }
//});


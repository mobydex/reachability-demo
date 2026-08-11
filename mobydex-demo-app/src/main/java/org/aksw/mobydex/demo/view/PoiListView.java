package org.aksw.mobydex.demo.view;

import java.util.Set;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridMultiSelectionModel.SelectAllCheckboxVisibility;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

import org.aksw.jena_sparql_api.vaadin.util.GridWrapper;
import org.aksw.jena_sparql_api.vaadin.util.GridWrapperBase;
import org.aksw.jena_sparql_api.vaadin.util.VaadinSparqlUtils;
import org.aksw.jenax.arq.util.syntax.QueryUtils;
import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.aksw.mobydex.demo.MainLayout;
import org.aksw.mobydex.demo.appstate.AppState;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "pois", layout = MainLayout.class)
@PageTitle("Poi Types")
public class PoiListView
    extends VerticalLayout
    implements BeforeEnterObserver
{
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(PoiListView.class);

    protected Button applySelectionBtn;
    // protected Button resetSelectionBtn;

    protected Grid<Binding> resultSetGrid;
    protected HeaderRow resultSetGridHeaderRow;
    protected HeaderRow resultSetGridFilterRow;

    protected AppState appState;
    protected UI ui;

    public PoiListView(
        @RouteScopeOwner(MainLayout.class) AppState appState
    ) {
        this.appState = appState;
//        ExecutorService executorService = Executors.newCachedThreadPool();
//        executor = executorService;

        applySelectionBtn = new Button("Apply Selection");
        applySelectionBtn.addClickListener(ev -> {
            Set<Binding> selectionSet = resultSetGrid.getSelectedItems();
            Table selectionTable = TableFactory.builder().addRowsAndVars(selectionSet).build();
            appState.setSelectedTags(selectionTable);

            ui.navigate(ReachabilityView.class);

//        	DataProvider<Binding, ?> dataProvider = resultSetGrid.getDataProvider();
//            resultSetGrid.getDataProvider()
//            	.fetch(new com.vaadin.flow.data.provider.Query<>())
//            	.toList();
        });

        add(applySelectionBtn);

        QueryExecutionFactoryQuery qef = q -> QueryExecution.create().dataset(DatasetFactory.empty()).query(q).build();

        Table poiTable = OsmRdfApi.getPoiCategories();
        Query query = QueryUtils.tableToQuery(poiTable);

        resultSetGrid = new Grid<>();
        resultSetGrid.setSelectionMode(SelectionMode.MULTI);
        // resultSetGrid.getSelectionModel().addSelectionListener(ResultSetMapRendererL.createGridListener(map.getlMap(), connectionGroup));

        GridMultiSelectionModel<?> selectionModel = (GridMultiSelectionModel<?>)resultSetGrid.getSelectionModel();
        selectionModel.setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility.VISIBLE);

        resultSetGrid.setMultiSort(true);
        resultSetGrid.setPageSize(100);
        resultSetGrid.setAllRowsVisible(true);
        resultSetGridHeaderRow = resultSetGrid.appendHeaderRow();
        resultSetGridFilterRow = resultSetGrid.appendHeaderRow();

        resultSetGrid.setEmptyStateText("No data to display");
        resultSetGrid.setSizeFull();

        GridWrapper<Binding> wrappedGrid = GridWrapperBase.wrap(resultSetGrid);
        VaadinSparqlUtils.setQueryForGridBinding(wrappedGrid, resultSetGridHeaderRow, qef, query);
        VaadinSparqlUtils.configureGridFilter(wrappedGrid, resultSetGridFilterRow, query.getProjectVars(),
                var -> str -> VaadinSparqlUtils.createFilterExpr(var, str).orElse(null));

//        resultSetGrid.getDataCommunicator().enablePushUpdates(executor);
//        resultSetGrid.getLazyDataView().setItemCountEstimate(10000);
//        resultSetGrid.getLazyDataView().setItemCountEstimateIncrease(1000);

        // resultSetGrid.getDataProvider().refreshAll();

        add(resultSetGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = UI.getCurrent();
    }

    void refreshSelection() {
        Table table = appState.selectedTags().getValue();
        // GridMultiSelectionModel<?> selectionModel = (GridMultiSelectionModel<?>)resultSetGrid.getSelectionModel();
        // selectionModel.setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility.VISIBLE);
        //selectionModel.getSelectedItems();

        table.rows().forEachRemaining(resultSetGrid::select);

        // logger.info("Refreshing selection: " + projectSelectionState);
//        Long projectId;
//        if ((projectId = projectSelectionState.getProjectId()) != null) {
//            Project selectedProject = projectDao.fetchItem(projectId);
//            projectSelector.setValue(selectedProject);
//        }
//
//        Long computationId;
//        if ((computationId = projectSelectionState.getComputationId()) != null) {
//            Computation computation = computationDao.fetchItem(computationId);
//            computationSelector.setValue(computation);
//        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        refreshSelection();
    }
}

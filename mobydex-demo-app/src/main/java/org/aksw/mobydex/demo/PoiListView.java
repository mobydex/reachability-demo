package org.aksw.mobydex.demo;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridMultiSelectionModel.SelectAllCheckboxVisibility;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.aksw.jena_sparql_api.vaadin.util.GridWrapper;
import org.aksw.jena_sparql_api.vaadin.util.GridWrapperBase;
import org.aksw.jena_sparql_api.vaadin.util.VaadinSparqlUtils;
import org.aksw.jenax.arq.util.syntax.QueryUtils;
import org.aksw.jenax.dataaccess.sparql.factory.execution.query.QueryExecutionFactoryQuery;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.engine.binding.Binding;

@Route(value = "pois", layout = MainLayout.class)
@PageTitle("Poi Types")
public class PoiListView
    extends VerticalLayout
{
    private static final long serialVersionUID = 1L;

//    protected Executor executor;

    protected Grid<Binding> resultSetGrid;
    protected HeaderRow resultSetGridHeaderRow;
    protected HeaderRow resultSetGridFilterRow;

    public PoiListView() {
//        ExecutorService executorService = Executors.newCachedThreadPool();
//        executor = executorService;

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
}

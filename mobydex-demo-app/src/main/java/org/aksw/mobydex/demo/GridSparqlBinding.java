package org.aksw.mobydex.demo;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;

import org.apache.jena.sparql.engine.binding.Binding;

public class GridSparqlBinding extends Grid<Binding> {
    private static final long serialVersionUID = 1L;

    protected HeaderRow headerRow;
    protected HeaderRow filterRow;

    public GridSparqlBinding() {
        super();
        headerRow = appendHeaderRow();
        filterRow = appendHeaderRow();
    }

    public HeaderRow getHeaderRow() {
        return headerRow;
    }

    public HeaderRow getFilterRow() {
        return filterRow;
    }
}

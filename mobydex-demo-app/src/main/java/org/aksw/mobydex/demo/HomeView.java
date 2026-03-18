package org.aksw.mobydex.demo;

import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "home", layout = MainLayout.class)
@PageTitle("Demo")
public class HomeView
    extends VerticalLayout
{
    private static final long serialVersionUID = 1L;

    public HomeView() {
        add(new Markdown("""
        ## MobyDex X-Minute City Demo

        * Project: Select a project and computation: Projects cover a certain area with a spatial grid, where the result of a computation is a set of relations between the grid cells.
        * GeoSPARQL: Create overlays of polygons from a SPARQL data source.
        * POIs: Select the POI types you are interested in.
        * Dashboard: Shows the grid of the selected project. Clicking the cell shows the closest POIs of each type.
        """));
    }
}

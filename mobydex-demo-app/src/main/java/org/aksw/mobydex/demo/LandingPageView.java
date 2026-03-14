package org.aksw.mobydex.demo;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "home", layout = MainLayout.class)
@PageTitle("Demo")
public class LandingPageView
    extends VerticalLayout
{
    private static final long serialVersionUID = 1L;

    public LandingPageView() {
        add(new H1("Welcome"));
    }
}

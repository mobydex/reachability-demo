package org.aksw.mobydex.demo;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;

/** Source:https://cookbook.vaadin.com/tabs-with-routes/a */
public class RouteTabs extends Tabs implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

	private final Map<RouterLink, Tab> routerLinkTabMap = new HashMap<>();

    public void add(RouterLink ...routerLinks) {
        for (RouterLink routerLink : routerLinks) {
            routerLink.setHighlightCondition(HighlightConditions.sameLocation());
            routerLink.setHighlightAction(
                (link, shouldHighlight) -> {
                    if (shouldHighlight) setSelectedTab(routerLinkTabMap.get(routerLink));
                }
            );
            routerLinkTabMap.put(routerLink, new Tab(routerLink));
            add(routerLinkTabMap.get(routerLink));
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // In case no tabs will match
        setSelectedTab(null);
    }

    /** Convenience method to create a new tab */
    public static RouterLink newTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> routeClass) {
        Icon icon = viewIcon.create();
        icon.getStyle()
                .set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setTabIndex(-1);
        link.setRoute(routeClass);

        // return new Tab(link);
        return link;
      }

}

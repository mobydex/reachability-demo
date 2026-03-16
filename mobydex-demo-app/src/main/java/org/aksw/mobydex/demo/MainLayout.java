package org.aksw.mobydex.demo;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.ColorScheme.Value;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.Lumo;

@Route("")
public class MainLayout
    extends AppLayout
    implements AfterNavigationListener
{
    private static final long serialVersionUID = 1L;

    protected DrawerToggle drawerToggle;
    protected MenuBar menuBar;

    protected Button mainViewBtn;
    protected Button newDataProjectBtn;
    protected Button connectionMgmtBtn;

    private final SideNav nav = new SideNav();
    // @Autowired
    // protected LabelServiceSwitchable<Node, String> labelService;

    public MainLayout () {
        drawerToggle = new DrawerToggle();
        H1 title = new H1("MobyDex Reachability Demo");

        // Global level
//        SideNavItem allProjects = new SideNavItem("All Projects", ReachabilityView.class);
//        nav.addItem(allProjects);

        // Dynamically populated project sections (or pre-build if projects are few/static)
        // For real apps → populate in constructor or via service + listener
        refreshProjectItems();  // see below

        HorizontalLayout navbarLayout = new HorizontalLayout();
        menuBar = new MenuBar();

        navbarLayout.setWidthFull();
        navbarLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button appSettingsBtn = new Button(new Icon(VaadinIcon.COG));
        navbarLayout.add(appSettingsBtn);

        Button colorSchemeToggleButton = new Button(new Icon(VaadinIcon.LIGHTBULB), click -> {
            Page page = UI.getCurrent().getPage();
            Value currentColorScheme = page.getColorScheme();
            Value nextColorScheme = ColorScheme.Value.DARK.equals(currentColorScheme)
                ? ColorScheme.Value.LIGHT
                : ColorScheme.Value.DARK;
            page.setColorScheme(nextColorScheme);
        });
        navbarLayout.add(colorSchemeToggleButton);

        navbarLayout.add(title);
        // refreshMenuBar();

        // setAlignItems(Alignment.CENTER);
        navbarLayout.add(menuBar);

        addToNavbar(drawerToggle, navbarLayout);

        setPrimarySection(Section.DRAWER);
        addToDrawer(new Scroller(nav));  // Scroller for overflow

        SideNavItem landingPage = new SideNavItem("Home", LandingPageView.class);
        nav.addItem(landingPage);
    }

    private void refreshProjectItems() {
        // Clear old project items (if dynamic)
        // nav.getChildren().filter(...) or keep reference to project container

        // Example: assume you have current/active projects from somewhere
        List<Project> projects = List.of(new Project("P1", "p1")); //, new Project("P2", "p2"));

        for (Project p : projects) {
            SideNavItem projectItem = new SideNavItem(p.getName(), ReachabilityView.class, p.getId());

            // Sub-items for this project (settings etc.)
            projectItem.addItem(new SideNavItem("Project", ProjectSelectorView.class, p.getId()));
            projectItem.addItem(new SideNavItem("Dashboard", ReachabilityView.class, p.getId()));
            projectItem.addItem(new SideNavItem("GeoSparql", GeoSparqlView.class, p.getId()));
            projectItem.addItem(new SideNavItem("POIs", PoiListView.class, p.getId()));
//            projectItem.addItem(new SideNavItem("Settings", LandingPageView.class, p.getId()));
//            projectItem.addItem(new SideNavItem("Members", LandingPageView.class, p.getId()));
//            projectItem.addItem(new SideNavItem("Tasks", LandingPageView.class, p.getId()));

            nav.addItem(projectItem);
        }
    }

    public record Project(String name, String id) {
        public String getName() {
            return name;
        }

        public RouteParameters getId() {
            return new RouteParameters(Map.of());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Optional: force expand the relevant project section if deep in a sub-view
        // SideNav does this automatically in most cases with matchNested
//        nav.getItems().forEach(item -> {
//            if (item.getPath() != null && event.getLocation().getPath().startsWith(item.getPath())) {
//                item.setExpanded(true);
//            }
//        });
    }

    private void setupNavbar() {
        HorizontalLayout navbarLayout = new HorizontalLayout();
        navbarLayout.setWidthFull();
        navbarLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button themeToggleButton = new Button(new Icon(VaadinIcon.LIGHTBULB), click -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if (themeList.contains(Lumo.DARK)) {
              themeList.remove(Lumo.DARK);
            } else {
              themeList.add(Lumo.DARK);
            }
        });
        navbarLayout.add(themeToggleButton);

        Button resetLabelsBtn = new Button(new Icon(VaadinIcon.TEXT_LABEL), ev -> {
            //LookupService<Node, String> ls = labelMgr.getLookupService() == ls1 ? ls2 : ls1;
//            labelService.next();
//            labelService.refreshAll();
        });
        navbarLayout.add(resetLabelsBtn);

        addToNavbar(navbarLayout);

    }

    private Tabs getTabs() {

        RouteTabs tabs = new RouteTabs();
        tabs.add(
                RouteTabs.newTab(VaadinIcon.HOME, "Home", ReachabilityView.class)
                // RouteTabs.newTab(VaadinIcon.FOLDER_ADD, "New Data Project", ResourceEditorView.class),
                // RouteTabs.newTab(VaadinIcon.EYE, "Labels", LabelView.class)
                // RouteTabs.newTab(VaadinIcon.TABLE, "TableMapper", TableMapperView.class),
                // RouteTabs.newTab(VaadinIcon.LINK, "ShaclGrid", ShaclGridView.class)
//                createTab(VaadinIcon.EYE, "Browse", BrowseRepoView.class),
//                createTab(VaadinIcon.CONNECT, "Connections", ConnectionMgmtView.class),
//                createTab(VaadinIcon.DATABASE, "Catalogs", CatalogMgmtView.class)
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
    //  Tabs tabs = new Tabs();
    //  tabs.add(
    //    createTab(VaadinIcon.HOME, "Home", DmanLandingPageView.class),
    //    createTab(VaadinIcon.FOLDER_ADD, "New Data Project", NewDataProjectView.class),
    //    createTab(VaadinIcon.EYE, "Browse", BrowseRepoView.class),
    //    createTab(VaadinIcon.CONNECT, "Connections", ConnectionMgmtView.class)
    //  );
    //  tabs.setOrientation(Tabs.Orientation.VERTICAL);
      return tabs;
    }
}

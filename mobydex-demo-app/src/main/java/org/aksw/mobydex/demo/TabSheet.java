package org.aksw.mobydex.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.copilot.shaded.guava.collect.BiMap;
import com.vaadin.copilot.shaded.guava.collect.HashBiMap;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

/** Note: Such a class is part of V23.3 */
public class TabSheet
    extends Div
{
    private static final long serialVersionUID = 1L;

    protected Component container;
    protected Tabs tabs = new Tabs();
    protected Div pages = new Div();

    protected BiMap<String, Tab> idToTab = HashBiMap.create();
    protected Map<Tab, ManagedComponent> tabsToPages = new LinkedHashMap<>();

    protected int nextTabId = 0;

    public TabSheet() {
        this(new VerticalLayout());
    }

    public TabSheet(Tabs.Orientation orientation) {
        this(Tabs.Orientation.VERTICAL.equals(orientation) ? new HorizontalLayout() : new VerticalLayout());
        getTabsComponent().setOrientation(orientation);
    }

    // TODO The supplier is not a view-model; there should be some
    // DataProvider that provides the components
    public TabSheet(Component container) {
        this.container = container;

        setSizeFull();
        pages.setSizeFull();
//        this.setWidthFull();
//        this.setHeightFull();
//        pages.setWidthFull();
//        pages.setHeightFull();

//        HorizontalLayout container = new HorizontalLayout();

        tabs.addSelectedChangeListener(ev -> {
            Tab selectedTab = tabs.getSelectedTab();
            setSelectedTab(selectedTab);
        });

        ((HasSize)container).setSizeFull();;
        ((HasComponents)container).add(tabs, pages);
        add(container);
    }

    public Tabs getTabs() {
        return tabs;
    }

    public Div getPages() {
        return pages;
    }

    public Component getContainer() {
        return container;
    }

    public Tab add(String name, Component content) {
        String generatedId = "tab" + nextTabId++;
        return newTab(generatedId, name, ManagedComponentSimple.wrap(content));
    }

    public Tab add(Component headerComponent, Component content) {
        String generatedId = "tab" + nextTabId++;
        return newTab(generatedId, headerComponent, ManagedComponentSimple.wrap(content));
    }


    public Tab newTab(String id, String name, Component content) {
        return newTab(id, name, ManagedComponentSimple.wrap(content));
    }

    public Tab newTab(String id, String name, ManagedComponent content) {
        return newTab(id, new Text(name), content);
    }

    /**
     * Append a new tab instance to the tabs component
     * This method is also invoked when the 'new tab' button is clicked
     *
     */
    public Tab newTab(String id, Component headerComponent, ManagedComponent content) {
        Tab priorTab = idToTab.get(id);
        if (priorTab != null) {
            destroyTab(priorTab);
        }

        Tab newTab = new Tab(headerComponent); //new Tab(new Icon(VaadinIcon.PLUS));
        newTab.setClassName("compact");


        Component contentComponent = content.getComponent();

        Tab selectedTab = tabs.getSelectedTab();

        idToTab.put(id, newTab);
        tabsToPages.put(newTab, content);
        tabs.add(newTab);
        pages.add(contentComponent);

        contentComponent.setVisible(false);

        if (selectedTab == null || newTab == selectedTab) {
            setSelectedTab(newTab);
        }

        return newTab;
    }

    public int getTabCount() {
        return tabsToPages.size();
    }

    public String getSelectedTabId() {
        Tab tab = tabs.getSelectedTab();
        String id = idToTab.inverse().get(tab);
        return id;
    }

    public void setSelectedTabId(String id) {
        Tab tab = idToTab.get(id);
        setSelectedTab(tab);
    }

    public void setSelectedTab(Tab selectedTab) {
        tabsToPages.values().forEach(page -> page.getComponent().setVisible(false));
        if (selectedTab != null) {
            Component selectedPage = tabsToPages.get(selectedTab).getComponent();
            selectedPage.setVisible(true);
        }
        tabs.setSelectedTab(selectedTab);
    }

    public Tabs getTabsComponent() {
        return tabs;

    }
    public Collection<Tab> getAvailableTabs() {
        return tabsToPages.keySet();
    }

    public void destroyTab(String id) {
        Tab tab = idToTab.get(id);
        if (tab != null) {
            destroyTab(tab);
        }
    }

    protected void destroyTab(Tab tab) {
        ManagedComponent page = tabsToPages.get(tab);
        pages.remove(page.getComponent());
        tabs.remove(tab);
        tabsToPages.remove(tab);
        idToTab.inverse().remove(tab);
        page.close();
    }

    public void removeAllTabs() {
        tabs.setSelectedTab(null);

        List<Tab> tabList = new ArrayList<>(tabsToPages.keySet());
        Collections.reverse(tabList);

        for (Tab tab : tabList) {
            destroyTab(tab);
        }
    }
}

package org.aksw.mobydex.demo;

import com.vaadin.flow.component.Component;

/**
 * A wrapper for a Component that adds a refresh and close method. Experimental.
 */
public interface ManagedComponent
    extends AutoCloseable
{
    Component getComponent();
    void refresh();

    @Override
    void close();
}

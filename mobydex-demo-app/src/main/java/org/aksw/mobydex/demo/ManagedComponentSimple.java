package org.aksw.mobydex.demo;

import com.vaadin.flow.component.Component;

public class ManagedComponentSimple
    implements ManagedComponent
{
    protected Component component;

    public ManagedComponentSimple(Component component) {
        super();
        this.component = component;
    }

    public static ManagedComponentSimple wrap(Component component) {
        return new ManagedComponentSimple(component);
    }


    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void close() {
    }
}

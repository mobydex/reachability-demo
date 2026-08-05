package org.aksw.mobydex.demo.plugin;

import org.aksw.jenax.reprogen.core.JenaPluginUtils;
import org.aksw.mobydex.demo.domain.Computation;
import org.aksw.mobydex.demo.domain.GridCell;
import org.aksw.mobydex.demo.domain.Project;
import org.apache.jena.sys.JenaSubsystemLifecycle;

public class JenaPluginMobyDexDemo
    implements JenaSubsystemLifecycle
{
    @Override
    public void start() {
        init();
    }

    @Override
    public void stop() {
    }

    public static void init() {
        JenaPluginUtils.registerResourceClasses(
                Project.class, GridCell.class, Computation.class);
    }
}

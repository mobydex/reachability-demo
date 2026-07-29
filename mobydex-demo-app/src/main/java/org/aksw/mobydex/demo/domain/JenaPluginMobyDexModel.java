package org.aksw.mobydex.demo.domain;

import org.aksw.jenax.reprogen.core.JenaPluginUtils;

public class JenaPluginMobyDexModel {
    public static void init() {
        JenaPluginUtils.registerResourceClasses(
            Project.class, GridCell.class, Computation.class);
    }

}

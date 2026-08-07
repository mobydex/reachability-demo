package org.aksw.mobydex.demo.appstate;

import java.io.Serializable;

import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;

import org.aksw.mobydex.demo.MainLayout;

@SpringComponent
// @UIScope
@RouteScopeOwner(MainLayout.class)
public class ProjectSelectionState implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long projectId;
    private Long computationId;
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getComputationId() { return computationId; }
    public void setComputationId(Long computationId) { this.computationId = computationId; }
}

package org.aksw.mobydex.demo.view;

import java.util.Optional;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

import org.aksw.mobydex.demo.MainLayout;
import org.aksw.mobydex.demo.appstate.AppState;
import org.aksw.mobydex.demo.appstate.ProjectSelectionState;
import org.aksw.mobydex.demo.backend.ComputationDao;
import org.aksw.mobydex.demo.backend.ComputationDao.Computation;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.ProjectDao;
import org.aksw.mobydex.demo.backend.ProjectDao.Project;
import org.aksw.mobydex.demo.component.ComputationComboBox;
import org.aksw.mobydex.demo.component.ProjectComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "project", layout = MainLayout.class)
@PageTitle("Project Selection")
// @PreserveOnRefresh
public class ProjectSelectorView
    extends VerticalLayout
    implements BeforeEnterObserver
{
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ProjectSelectorView.class);

    // private ComboBox<Project> projectSelector;
    private ProjectComboBox projectSelector;
    private ComputationComboBox computationSelector;
    private Text projectStatusBox = new Text("Project Status.");
    private Text computationStatusBox = new Text("Computation Status.");

    private ProjectDao projectDao;
    private ComputationDao computationDao;

    private Button loadProjectBtn = new Button("Load Project");

    // private Long currentProjectId;
    private AppState appState;
    private ProjectSelectionState projectSelectionState;

    private MobyDexRdfApi mobyDexRdfApi;

    public ProjectSelectorView(
            @RouteScopeOwner(MainLayout.class) AppState appState,
            @RouteScopeOwner(MainLayout.class) ProjectSelectionState projectSelectionState,
            // MobyDexRdfApi mobyDexRdfApi,
            ProjectDao projectDao,
            ComputationDao computationDao
            ) {
        super();
        this.appState = appState;
        this.projectSelectionState = projectSelectionState;

        this.projectDao = projectDao;
        this.computationDao = computationDao;

        this.mobyDexRdfApi = appState.getMobyDexRdfApi();

        // MobyDexRdfApi mobyDexRdfApi = MobyDexRdfApi.get(); // TODO Inject

        projectSelector = new ProjectComboBox("Project", projectDao);
        computationSelector = new ComputationComboBox("Computation", computationDao);

        projectSelector.addValueChangeListener(ev -> {
            Project project = ev.getValue();
            Long projectId = project != null ? project.id() : null;
            projectSelectionState.setProjectId(projectId);
            if (projectId != null) {
                boolean isLoadedGrid = mobyDexRdfApi.isProjectGridLoaded(projectId);
                boolean isLoadedPois = mobyDexRdfApi.isProjectPoisLoaded(projectId);
                projectStatusBox.setText("Grid loaded: " + isLoadedGrid + ", POIs loaded: " + isLoadedPois);

                computationSelector.clear();
                computationSelector.setProjectId(projectId);
            }
        });

        computationSelector.addValueChangeListener(ev -> {
            Long currentProjectId = projectSelectionState.getProjectId();

            Computation computation = ev.getValue();
            Long computationId = Optional.ofNullable(computation).map(Computation::id).orElse(null);
            projectSelectionState.setComputationId(computationId);

            long count = 0;
            if (computationId != null) {
                count = mobyDexRdfApi.getComputedCells(currentProjectId, computationId);
            }
            computationStatusBox.setText("Cached cells: " + count);
        });

        loadProjectBtn.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SUCCESS);
        loadProjectBtn.addClickListener(ev -> {
            Long projectId = projectSelectionState.getProjectId();
            Long computationId = projectSelectionState.getComputationId();

            appState.selectProject(projectId);
            appState.selectComputation(computationId);

            UI.getCurrent().navigate(ReachabilityView.class);
        });

        add(projectSelector);
        add(projectStatusBox);
        add(computationSelector);
        add(computationStatusBox);
        add(loadProjectBtn);
    }

    void refreshSelection() {
        logger.info("Refreshing selection: " + projectSelectionState);
        Long projectId;
        if ((projectId = projectSelectionState.getProjectId()) != null) {
            Project selectedProject = projectDao.fetchItem(projectId);
            projectSelector.setValue(selectedProject);
        }

        Long computationId;
        if ((computationId = projectSelectionState.getComputationId()) != null) {
            Computation computation = computationDao.fetchItem(computationId);
            computationSelector.setValue(computation);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        refreshSelection();
    }
}

package org.aksw.mobydex.demo;

import java.util.Optional;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.aksw.mobydex.demo.ComputationComboBox.Computation;
import org.aksw.mobydex.demo.ProjectComboBox.Project;
import org.springframework.web.client.RestTemplate;

@Route(value = "project", layout = MainLayout.class)
@PageTitle("Project Selection")
//@PreserveOnRefresh
public class ProjectSelectorView
    extends VerticalLayout
{


    private static final long serialVersionUID = 1L;

    private ComboBox<Project> projectSelector;
    private ComputationComboBox computationSelector;
    private Text projectStatusBox = new Text("Project Status.");
    private Text computationStatusBox = new Text("Computation Status.");

    private Long currentProjectId;

    public ProjectSelectorView() {
        super();

        MobyDexRdfApi mobyDexRdfApi = MobyDexRdfApi.get(); // TODO Inject
        RestTemplate restTemplate = new RestTemplate();
//        .setConnectTimeout(Duration.ofSeconds(5))
//        .setReadTimeout(Duration.ofSeconds(10))
//        // Optional: add interceptors, error handler, custom message converters...
//        // .additionalInterceptors(new LoggingInterceptor())
//        // .errorHandler(new CustomResponseErrorHandler())
//        .build();

        projectSelector = new ProjectComboBox("Project", restTemplate);
        computationSelector = new ComputationComboBox("Computation", restTemplate);

        projectSelector.addValueChangeListener(ev -> {
            Project project = ev.getValue();
            Long id = Optional.ofNullable(project).map(Project::id).orElse(null);
            currentProjectId = id;
            if (id != null) {
                boolean isLoadedGrid = mobyDexRdfApi.isProjectGridLoaded(id);
                boolean isLoadedPois = mobyDexRdfApi.isProjectPoisLoaded(id);
                projectStatusBox.setText("Grid loaded: " + isLoadedGrid + ", POIs loaded: " + isLoadedPois);

                computationSelector.clear();
                computationSelector.setProjectId(id);
            }
        });

        computationSelector.addValueChangeListener(ev -> {
            Computation computation = ev.getValue();
            Long id = Optional.ofNullable(computation).map(Computation::id).orElse(null);
            long count = 0;
            if (id != null) {
                count = mobyDexRdfApi.getComputedCells(currentProjectId, id);
            }
            computationStatusBox.setText("Cached cells: " + count);
        });

        add(projectSelector);
        add(projectStatusBox);
        add(computationSelector);
        add(computationStatusBox);
    }
}

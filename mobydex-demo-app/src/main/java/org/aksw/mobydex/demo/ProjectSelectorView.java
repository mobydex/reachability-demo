package org.aksw.mobydex.demo;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

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

    public ProjectSelectorView() {
        super();
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
            Long id = project != null ? project.id() : null;
            computationSelector.clear();
            computationSelector.setProjectId(id);
        });

        add(projectSelector);
        add(computationSelector);
    }
}

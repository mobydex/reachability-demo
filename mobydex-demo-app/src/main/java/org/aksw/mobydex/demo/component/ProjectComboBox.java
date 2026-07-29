package org.aksw.mobydex.demo.component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import org.aksw.mobydex.demo.backend.ProjectDao;
import org.aksw.mobydex.demo.backend.ProjectDao.Project;

public class ProjectComboBox extends ComboBox<Project> {

    private static final int PAGE_SIZE = 30;   // good balance for ComboBox
    // private final RestTemplate restTemplate;   // or use WebClient / Feign / your http client
    private ProjectDao projectDao;

    public ProjectComboBox(String label, ProjectDao projectDao) {
        super(label);
        this.projectDao = projectDao;

        setWidth("400px");
        setPageSize(PAGE_SIZE);
        setItemLabelGenerator(project -> project.name() + " (" + project.id() + ")");           // main display text
        // setItemLabelGenerator(Project::key, "key");     // optional: secondary label (shown smaller)
        // setItemLabelGenerator(p -> p.key() + " – " + p.name());  // alternative: combined

        // Optional: show more info in dropdown
        // setRenderer(new ComponentRenderer<>(this::createProjectItem));

        // Enable filtering (searches on both key and name)
        setAllowCustomValue(false);
        setAutoOpen(true);

        setRenderer(createProjectRenderer());
        // The most important part: lazy backend binding
        DataProvider<Project, String> dataProvider = createProjectDataProvider();
        setItems(dataProvider);
    }


    public static long getProjectId(Project project) {
        return project.id();
    }

    private DataProvider<Project, String> createProjectDataProvider() {
        return DataProvider.fromFilteringCallbacks(
            // fetch items
            query -> {
                String filter = query.getFilter().orElse("");
                int offset = query.getOffset();
                int limit  = query.getLimit();
                return projectDao.fetchItems(filter, offset, limit);
            },

            // count total (needed for scrollbar / "showing x of y")
            query -> {
                String filter = query.getFilter().orElse("");
                return projectDao.countItems(filter);
            }
        );
    }

    private Card createProjectCard(Project project) {
        Card card = new Card();
        // Optional: subtle elevation / outline look (works in Aura & Lumo)
        // card.addThemeVariants(CardVariant.LUMO_ELEVATED);   // or LUMO_OUTLINED
        // Aura tends to look good even without extra variants

        // Title = project name (uses built-in card title styling)
        card.setTitle(project.name() + " (" + project.id() + ")");

        // Subtitle line: ID + dates (small secondary text)
        String created  = formatTimestamp(project.creationTime());
        String modified = formatTimestamp(project.modificationTime());

        Span subtitle = new Span(
            "ID: " + project.id() +
            "  •  Created: " + created +
            "  •  Modified: " + modified
        );
        subtitle.getElement().getThemeList().add("secondary");   // many themes style this smaller/lighter
        // or: subtitle.addClassName("text-secondary");          // Aura-friendly class in newer versions

        // Description
        Div description = new Div(
            new Span(project.description().isBlank() ? "No description provided." : project.description())
        );
        description.getStyle()
            .set("white-space", "pre-wrap")           // minimal – respect newlines
            .set("margin-top", "var(--vaadin-spacing-xs)");  // ← base variable, works in Aura & Lumo

        // Optional subtle separator before dates if description exists
        // Hr separator = new Hr();
        // separator.getStyle().set("margin", "var(--vaadin-spacing-m) 0 var(--vaadin-spacing-s)");

        // Assemble content (card body is just added children)
        card.add(description, subtitle);

//        if (!project.description().isBlank()) {
//            card.add(separator);
//        }

        // Dates can stay in subtitle, or move here if you prefer them at the bottom
        // card.add(new Span("… dates here …").with { small & secondary });

        return card;
    }

    private ComponentRenderer<Card, Project> createProjectRenderer() {
        return new ComponentRenderer<>(this::createProjectCard);
    }
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.getDefault());  // ← or fixed e.g. Locale.GERMAN

    private String formatTimestamp(long millis) {
        if (millis <= 0) return "—";
        Instant instant = Instant.ofEpochMilli(millis);
        return DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }
}

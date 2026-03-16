package org.aksw.mobydex.demo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import org.aksw.mobydex.demo.ProjectComboBox.Project;
import org.springframework.web.client.RestTemplate;

public class ProjectComboBox extends ComboBox<Project> {

    // Assuming you have this record / DTO (adjust fields as needed)
    public record Project(
        Long id,
        String key,
        String name,
        String description,
        Long creationTime,
        Long modificationTime
    ) {}

    private static final int PAGE_SIZE = 30;   // good balance for ComboBox
    private final RestTemplate restTemplate;   // or use WebClient / Feign / your http client

    public ProjectComboBox(String label, RestTemplate restTemplate) {
        super(label);
        this.restTemplate = restTemplate;

        setWidth("400px");
        setPageSize(PAGE_SIZE);
        setItemLabelGenerator(project -> project.name + " (" + project.id() + ")");           // main display text
        // setItemLabelGenerator(Project::key, "key");     // optional: secondary label (shown smaller)
        // setItemLabelGenerator(p -> p.key() + " – " + p.name());  // alternative: combined

        // Optional: show more info in dropdown
        // setRenderer(new ComponentRenderer<>(this::createProjectItem));

        // Enable filtering (searches on both key and name)
        setAllowCustomValue(false);
        setAutoOpen(true);

        setRenderer(createProjectRenderer());
        // The most important part: lazy backend binding
        setItems(createProjectDataProvider());
    }

    private DataProvider<Project, String> createProjectDataProvider() {
        return DataProvider.fromFilteringCallbacks(
            // fetch items
            query -> {
                String filter = query.getFilter().orElse("");
                int offset = query.getOffset();
                int limit  = query.getLimit();

                // Build your real URL (adjust query params to match your API)
                String url = "https://mobydex.locoslab.com/controller-service/projects"
                    + "?pageOffset=" + offset
                    + "&pageSize=" + limit;
                    // + (filter.isBlank() ? "" : "&search=" + StringUtils.urlEncode(filter));   // ← adapt filter param!

                try {
                    ProjectPage page = restTemplate.getForObject(url, ProjectPage.class);
                    if (page == null || page.elements() == null) {
                        return Stream.empty();
                    }
                    return page.elements().stream();
                } catch (Exception e) {
                    // log error – in real app use Notification + logger
                    return Stream.empty();
                }
            },

            // count total (needed for scrollbar / "showing x of y")
            query -> {
                String filter = query.getFilter().orElse("");
                String url = "https://mobydex.locoslab.com/controller-service/projects"
                    + "?pageSize=1"  // many APIs return total even with size=0 - this one doesn't!
                    ;
                    // + (filter.isBlank() ? "" : "&search=" + StringUtils.urlEncode(filter));

                try {
                    ProjectPage page = restTemplate.getForObject(url, ProjectPage.class);
                    return page != null ? page.total() : 0;
                } catch (Exception e) {
                    return 0;
                }
            }
        );
    }

    // Helper record matching your JSON structure
    private record ProjectPage(
        int size,
        int offset,
        int total,
        List<Project> elements
    ) {}



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

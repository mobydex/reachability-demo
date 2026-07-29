package org.aksw.mobydex.demo.backend;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.web.client.RestTemplate;

public class ProjectDao {

    // Helper record matching your JSON structure
    private record ProjectPage(
        int size,
        int offset,
        int total,
        List<Project> elements
    ) {}

    // Assuming you have this record / DTO (adjust fields as needed)
    public record Project(
        Long id,
        String key,
        String name,
        String description,
        Long creationTime,
        Long modificationTime
    ) {}

    private String baseUrl;
    private RestTemplate restTemplate;

    public ProjectDao(RestTemplate restTemplate, String baseUrl) {
        super();
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Project fetchItem(long id) {
        // Build your real URL (adjust query params to match your API)
        String url = baseUrl + "projects/" + id;
        Project project = restTemplate.getForObject(url, Project.class);
        return project;
//        try {
//        } catch (Exception e) {
//            // log error – in real app use Notification + logger
//            // return Stream.empty();
//        	thro
//        }
    }

    public Stream<Project> fetchItems(String filterStr, int offset, int limit) {
        // Build your real URL (adjust query params to match your API)
        String url = baseUrl + "projects"
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
    }

    public int countItems(String filter) {
        String url = baseUrl + "projects"
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
}

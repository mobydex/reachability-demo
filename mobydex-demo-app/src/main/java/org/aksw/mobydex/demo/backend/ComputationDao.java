package org.aksw.mobydex.demo.backend;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.web.client.RestTemplate;

public class ComputationDao {
    // Helper record matching your JSON structure
    private record ComputationPage(
        int size,
        int offset,
        int total,
        List<Computation> elements
    ) {}

    // Assuming you have this record / DTO (adjust fields as needed)
    public record Computation(
        Long id,
        Long projectId,
        String type,
        String state,
//        String key,
//        String name,
//        String description,
        Long creationTime,
        Long modificationTime
    ) {}

    private String baseUrl;
    private RestTemplate restTemplate;

    public ComputationDao(RestTemplate restTemplate, String baseUrl) {
        super();
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Long getProjectId(long computationId) {
        org.aksw.mobydex.demo.backend.ComputationDao.Computation computationRec = fetchItem(computationId);
        long projectId = computationRec.id();
        return projectId;
    }

    public Computation fetchItem(long computationId) {
        // Build your real URL (adjust query params to match your API)
        String url = baseUrl + "computations/" + computationId;
        Computation computation = restTemplate.getForObject(url, Computation.class);
        return computation;
    }

    public Stream<Computation> fetchItems(Long projectId, String filter, long offset, long limit) {
        if (projectId == null) {
            return Stream.of();
        }
        // Build your real URL (adjust query params to match your API)
        String url = baseUrl + "projects/" + projectId + "/computations"
           + "?pageOffset=" + offset
            + "&pageSize=" + limit;
            // + (filter.isBlank() ? "" : "&search=" + StringUtils.urlEncode(filter));   // ← adapt filter param!

        try {
            ComputationPage page = restTemplate.getForObject(url, ComputationPage.class);
            if (page == null || page.elements() == null) {
                return Stream.empty();
            }
            return page.elements().stream();
        } catch (Exception e) {
            // log error – in real app use Notification + logger
            return Stream.empty();
        }
    }

    public int countItems(Long projectId, String filter) {
        if (projectId == null) {
            return 0;
        }

        String url = baseUrl + "projects/" + projectId + "/computations"
            + "?pageSize=1"  // many APIs return total even with size=0 - this one doesn't!
            ;
            // + (filter.isBlank() ? "" : "&search=" + StringUtils.urlEncode(filter));

        try {
            ComputationPage page = restTemplate.getForObject(url, ComputationPage.class);
            return page != null ? page.total() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

}

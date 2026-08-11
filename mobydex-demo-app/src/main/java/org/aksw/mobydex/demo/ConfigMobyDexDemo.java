package org.aksw.mobydex.demo;

import java.nio.file.Path;
import java.util.Optional;

import org.aksw.mobydex.demo.appstate.AppState;
import org.aksw.mobydex.demo.backend.ComputationDao;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.OsmRdfApi;
import org.aksw.mobydex.demo.backend.ProjectDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConfigMobyDexDemo {

    @Value("${mobydex.api.base-url:https://mobydex.locoslab.com/controller-service/}")
    private String mobyDexApiBaseUrl;

    @Bean
    public MobyDexRdfApi mobyDexRdfApi() {
        Path cachePath = Path.of(System.getProperty("user.home")).resolve(".cache/mobydex");
        return MobyDexRdfApi.of(cachePath);
    }

    @Bean
    public RestTemplate restTemplate() {
        return newRestTemplate();
    }

    @Bean
    public ProjectDao projectDao(RestTemplate restTemplate) {
        return new ProjectDao(restTemplate, mobyDexApiBaseUrl);
    }

    @Bean
    public ComputationDao computationDao(RestTemplate restTemplate) {
        return new ComputationDao(restTemplate, mobyDexApiBaseUrl);
    }

    @Bean
    public AppState appState(MobyDexRdfApi mobyDexRdfApi) {
        AppState result = new AppState(mobyDexRdfApi);
        result.setSelectedTags(OsmRdfApi.getPoiCategories());
        result.setAvailableTags(OsmRdfApi.getPoiCategories());
        result.setSelectedGeomBindings(Optional.empty());
        return result;
    }

    public static RestTemplate newRestTemplate() {
        return new RestTemplate();
    }
}

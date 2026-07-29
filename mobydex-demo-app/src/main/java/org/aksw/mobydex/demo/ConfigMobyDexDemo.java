package org.aksw.mobydex.demo;

import org.aksw.mobydex.demo.backend.ComputationDao;
import org.aksw.mobydex.demo.backend.MobyDexRdfApi;
import org.aksw.mobydex.demo.backend.ProjectDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConfigMobyDexDemo {

    public static String baseUrl = "https://mobydex.locoslab.com/controller-service/";

    @Bean
    public MobyDexRdfApi mobyDexRdfApi() {
        // cacheBasePath = Path.of(System.getProperty("user.home")).resolve(".cache/mobydex");

        return MobyDexRdfApi.get();
    }

    @Bean
    public RestTemplate restTemplate() {
        return newRestTemplate();
    }

    @Bean
    public ProjectDao projectDao(RestTemplate restTemplate) {
        return new ProjectDao(restTemplate, baseUrl);
    }

    @Bean
    public ComputationDao computationDao(RestTemplate restTemplate) {
        return new ComputationDao(restTemplate, baseUrl);
    }


    public static RestTemplate newRestTemplate() {
        return new RestTemplate();
    }
}

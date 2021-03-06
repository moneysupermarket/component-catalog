package com.moneysupermarket.componentcatalog.service.scanners.sonarqube.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import com.moneysupermarket.componentcatalog.sdk.models.sonarqube.SonarQubeMeasure;
import com.moneysupermarket.componentcatalog.sdk.models.sonarqube.SummarySonarQubeMetric;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.config.SonarQubeConfig;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.constants.ApiPaths;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.constants.MetricKeys;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.models.Project;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.models.api.Component;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.models.api.ComponentQualifier;
import com.moneysupermarket.componentcatalog.service.scanners.sonarqube.models.api.GetComponentMeasuresResponse;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Client;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Client
@RequiredArgsConstructor
public class SonarQubeClient {

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private final WebClient webClient;
    private final SonarQubeConfig config;
    private final ObjectMapper objectMapper;

    public List<SummarySonarQubeMetric> getMetrics() {
        return getAllResourcePages(uriVariables -> webClient.get().uri(config.getBaseUrl() + ApiPaths.SEARCH_METRICS + "?p={pageNumber}", uriVariables),
                "Search Metrics", "metrics", new HashMap<>(), SummarySonarQubeMetric.class);
    }

    public List<Project> getProjects() {
        return getAllResourcePages(uriVariables -> webClient.get().uri(
                config.getBaseUrl() + ApiPaths.SEARCH_COMPONENTS + "?qualifiers={qualifiers}&p={pageNumber}", uriVariables),
                "Search Components", "components", createProjectsUriVariables(), Component.class)
                .stream()
                .map(component -> new Project(component.getKey(), component.getName()))
                .collect(Collectors.toList());
    }

    public List<SonarQubeMeasure> getProjectMeasures(String projectKey, List<SummarySonarQubeMetric> metrics) {
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = webClient
                .get()
                .uri(config.getBaseUrl() + ApiPaths.GET_COMPONENT_MEASURES + "?component={component}&metricKeys={metricKeys}",
                        projectKey,
                        getMetricKeys(metrics));

        Response response = makeRequest(requestHeadersSpec);
        checkResponseStatus(response, HttpStatus.OK, "Get Component Measures");

        try {
            return objectMapper.readValue(response.getBody(), GetComponentMeasuresResponse.class)
                    .getComponent()
                    .getMeasures();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> createProjectsUriVariables() {
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("qualifiers", ComponentQualifier.TRK.toString());
        return uriVariables;
    }

    private <T> List<T> getAllResourcePages(Function<Map<String, String>, WebClient.RequestHeadersSpec<?>> requestHeadersSpecSupplier, String endpointName,
            String itemsFieldName, Map<String, String> uriVariables, Class<T> type) {
        int pageNumber = 1;
        List<T> allResources = new ArrayList<>();

        while (true) {
            List<T> page = getResourcePage(requestHeadersSpecSupplier, endpointName, itemsFieldName, uriVariables, pageNumber, type);

            if (page.isEmpty()) {
                break;
            }

            allResources.addAll(page);
            pageNumber++;
        }

        return allResources;
    }

    private <T> List<T> getResourcePage(Function<Map<String, String>, WebClient.RequestHeadersSpec<?>> requestHeadersSpecSupplier, String endpointName, String itemsFieldName,
            Map<String, String> uriVariables, int pageNumber, Class<T> type) {
        uriVariables.put("pageNumber", Integer.toString(pageNumber));
        Response response = makeRequest(requestHeadersSpecSupplier.apply(uriVariables));
        checkResponseStatus(response, HttpStatus.OK, endpointName);

        try {
            ObjectNode responseBody = (ObjectNode) objectMapper.readTree(response.getBody());
            ArrayNode itemJsons = (ArrayNode) responseBody.get(itemsFieldName);
            return Streams.stream(itemJsons.elements())
                    .map(itemJson -> objectMapper.convertValue(itemJson, type))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Response makeRequest(WebClient.RequestHeadersSpec<?> requestHeadersSpec) {
        Mono<ResponseEntity<String>> responseEntityMono = requestHeadersSpec
                .retrieve()
                .toEntity(String.class);
        try {
            ResponseEntity<String> responseEntity = responseEntityMono.block(TIMEOUT);
            return new Response(responseEntity.getStatusCode(), responseEntity.getBody());
        } catch (WebClientResponseException e) {
            return new Response(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }

    private void checkResponseStatus(Response response, HttpStatus expectedStatusCode, String endpointName) {
        if (response.getStatusCode() != expectedStatusCode) {
            throw new SonarQubeClientException(endpointName, response.getStatusCode().value(), response.getBody());
        }
    }

    private String getMetricKeys(List<SummarySonarQubeMetric> metrics) {
        return metrics.stream()
                .map(SummarySonarQubeMetric::getKey)
                .filter(this::metricKeyIsNotAffectedBySonarQubeBug)
                .collect(Collectors.joining(","));
    }

    /**
     * This can be removed once we upgrade to SonarQube 8.1 or higher.
     * See https://jira.sonarsource.com/browse/SONAR-12728 for more information.
     *
     * @param metricKey
     * @return
     */
    private boolean metricKeyIsNotAffectedBySonarQubeBug(String metricKey) {
        return !MetricKeys.AFFECTED_BY_SONARQUBE_BUG.contains(metricKey);
    }

    @Value
    private static class Response {

        HttpStatus statusCode;
        String body;
    }
}

package com.moneysupermarket.componentcatalog.service.scanners.zipkin.client;

import com.moneysupermarket.componentcatalog.sdk.models.zipkin.ZipkinDependency;
import com.moneysupermarket.componentcatalog.service.constants.Resilience4JInstanceNames;
import com.moneysupermarket.componentcatalog.service.scanners.zipkin.config.ZipkinConfig;
import com.moneysupermarket.componentcatalog.service.scanners.zipkin.constants.ZipkinApiPaths;
import com.moneysupermarket.componentcatalog.service.scanners.zipkin.models.api.Span;
import com.moneysupermarket.componentcatalog.service.services.UriVariablesBuilder;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Client;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Client
@RequiredArgsConstructor
@Slf4j
public class ZipkinClient {

    public static final Duration LOOKBACK = Duration.ofDays(1);
    private final WebClient webClient;
    private final ZipkinConfig config;
    private final Clock clock;

    @Retry(name = Resilience4JInstanceNames.ZIPKIN_CLIENT)
    public List<ZipkinDependency> getDependencies() {
        String uriTemplate = config.getBaseUrl() + ZipkinApiPaths.DEPENDENCIES + "?endTs={endTs}&lookback={lookback}";
        Map<String, String> uriVariables = UriVariablesBuilder.builder()
                .addUriVariable("endTs", clock.millis())
                .addUriVariable("lookback", LOOKBACK.toMillis())
                .build();

        ClientResponse clientResponse = makeRequest(webClient.get().uri(uriTemplate, uriVariables));
        checkResponseStatus(clientResponse, uriTemplate, uriVariables);
        return clientResponse.bodyToMono(new ParameterizedTypeReference<List<ZipkinDependency>>() { })
                .block(config.getTimeout());
    }

    @Retry(name = Resilience4JInstanceNames.ZIPKIN_CLIENT)
    public List<String> getServiceNames() {
        String uriTemplate = config.getBaseUrl() + ZipkinApiPaths.SERVICES;
        Map<String, String> uriVariables = UriVariablesBuilder.builder().build();

        ClientResponse clientResponse = makeRequest(webClient.get().uri(uriTemplate));
        checkResponseStatus(clientResponse, uriTemplate, uriVariables);
        return clientResponse.bodyToMono(new ParameterizedTypeReference<List<String>>() { }).block(config.getTimeout());
    }

    @Retry(name = Resilience4JInstanceNames.ZIPKIN_CLIENT)
    public List<String> getSpanNames(String serviceName) {
        String uriTemplate = config.getBaseUrl() + ZipkinApiPaths.SPANS + "?serviceName={serviceName}";
        Map<String, String> uriVariables = UriVariablesBuilder.builder()
                .addUriVariable("serviceName", serviceName)
                .build();

        ClientResponse clientResponse = makeRequest(webClient.get().uri(uriTemplate, uriVariables));
        checkResponseStatus(clientResponse, uriTemplate, uriVariables);
        return clientResponse.bodyToMono(new ParameterizedTypeReference<List<String>>() { }).block(config.getTimeout());
    }

    @Retry(name = Resilience4JInstanceNames.ZIPKIN_CLIENT)
    public List<List<Span>> getTraces(String serviceName, String spanName, int limit) {
        String uriTemplate = config.getBaseUrl() + ZipkinApiPaths.TRACES + "?serviceName={serviceName}&spanName={spanName}&endTs={endTs}&lookback={lookback}"
                + "&limit={limit}";
        Map<String, String> uriVariables = UriVariablesBuilder.builder()
                .addUriVariable("serviceName", serviceName)
                .addUriVariable("spanName", spanName)
                .addUriVariable("endTs", clock.millis())
                .addUriVariable("lookback", LOOKBACK.toMillis())
                .addUriVariable("limit", limit)
                .build();

        ClientResponse clientResponse = makeRequest(webClient.get().uri(uriTemplate, uriVariables));
        checkResponseStatus(clientResponse, uriTemplate, uriVariables);
        return clientResponse.bodyToMono(new ParameterizedTypeReference<List<List<Span>>>() { }).block(config.getTimeout());
    }

    private ClientResponse makeRequest(WebClient.RequestHeadersSpec<?> requestHeadersSpec) {
        String cookieValue = getCookieHeaderValue();

        if (nonNull(cookieValue)) {
            requestHeadersSpec = requestHeadersSpec.header(HttpHeaders.COOKIE, cookieValue);
        }

        return requestHeadersSpec
                .exchange()
                .block(config.getTimeout());
    }

    private void checkResponseStatus(ClientResponse clientResponse, String uriTemplate, Map<String, String> uriVariables) {
        if (clientResponse.statusCode() != HttpStatus.OK) {
            String responseBody = clientResponse.bodyToMono(String.class).block(config.getTimeout());

            ZipkinClientException exception = new ZipkinClientException(expandUriTemplate(uriTemplate, uriVariables),
                    clientResponse.rawStatusCode(), responseBody);
            log.warn(exception.getMessage());
            throw exception;
        }
    }

    private String expandUriTemplate(String uriTemplate, Map<String, String> uriVariables) {
        return new UriTemplate(uriTemplate).expand(uriVariables).toString();
    }

    private String getCookieHeaderValue() {
        String cookieName = config.getCookieName();
        String cookieValue = config.getCookieValue();

        if (nonNull(cookieName) && nonNull(cookieValue)) {
            try {
                return URLEncoder.encode(cookieName, StandardCharsets.UTF_8.name()) + "=" + URLEncoder.encode(cookieValue, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}

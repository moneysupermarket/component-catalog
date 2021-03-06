package com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.client;

import com.moneysupermarket.componentcatalog.service.models.ApiRepo;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.config.BitbucketServerConfig;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.config.BitbucketServerHostConfig;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.constants.BitbucketServerApiPaths;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.models.api.BrowseResponse;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.models.api.Link;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.models.api.PageResponse;
import com.moneysupermarket.componentcatalog.service.repoproviders.bitbucketserver.models.api.Repo;
import com.moneysupermarket.componentcatalog.service.services.UriVariablesBuilder;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Client;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * See https://docs.atlassian.com/bitbucket-server/rest/7.11.1/bitbucket-rest.html
 * for a description of the API endpoints for Bitbucket Server.  Bitbucket Service
 * is a distinct product from Bitbucket Cloud.
 */
@Client
@RequiredArgsConstructor
@Slf4j
public class BitbucketServerClient {

    private static final List<HttpStatus> GET_REPOS_EXPECTED_STATUS_CODES = List.of(HttpStatus.OK);
    private static final List<HttpStatus> BROWSE_EXPECTED_STATUS_CODES = List.of(HttpStatus.OK, HttpStatus.NOT_FOUND);
    private static final Comparator<RepoAndApiRepo> REPO_AND_API_REPO_COMPARATOR = Comparator.comparing(repoAndApiRepo -> repoAndApiRepo.getApiRepo().getUrl());

    private final WebClient webClient;
    private final BitbucketServerConfig config;

    public List<ApiRepo> getNormalRepos() {
        return config.getHosts().stream()
                .map(this::getNormalRepos)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<ApiRepo> getNormalRepos(BitbucketServerHostConfig host) {
        List<Repo> normalRepos = new ArrayList<>();
        Optional<Integer> start = Optional.empty();

        while (true) {
            PageResponse<Repo> page = getReposPage(host, start);

            normalRepos.addAll(getNormalReposFromPage(page));

            if (page.getIsLastPage()) {
                break;
            }

            start = Optional.ofNullable(page.getNextPageStart());
        }

        return normalRepos.stream()
                .map(this::mapRepoToApiRepo)
                .sorted(REPO_AND_API_REPO_COMPARATOR)
                .map(addHasComponentMetadataFileToApiRepo(host))
                .collect(Collectors.toList());
    }

    private PageResponse<Repo> getReposPage(BitbucketServerHostConfig host, Optional<Integer> start) {
        String uriTemplate = host.getBaseUrl() + BitbucketServerApiPaths.REPOS;
        UriVariablesBuilder uriVariablesBuilder = UriVariablesBuilder.builder();
        if (start.isPresent()) {
            uriTemplate += "?start={start}";
            uriVariablesBuilder.addUriVariable("start", start.get());
        }
        Map<String, String> uriVariables = uriVariablesBuilder.build();

        logWebCall(uriTemplate, uriVariables);

        ClientResponse clientResponse = makeRequest(host, webClient.get().uri(uriTemplate, uriVariables));
        checkResponseStatus(clientResponse, GET_REPOS_EXPECTED_STATUS_CODES, uriTemplate, uriVariables);
        return clientResponse.bodyToMono(new ParameterizedTypeReference<PageResponse<Repo>>() { })
                .block(config.getTimeout());
    }

    private List<Repo> getNormalReposFromPage(PageResponse<Repo> page) {
        return page.getValues().stream()
                .filter(this::isNormalRepo)
                .collect(Collectors.toList());
    }

    private boolean isNormalRepo(Repo repo) {
        return repo.getScmId().equals("git")
                && repo.getState().equals("AVAILABLE")
                && repo.getProject().getType().equals("NORMAL");
    }

    private RepoAndApiRepo mapRepoToApiRepo(Repo repo) {
        return new RepoAndApiRepo(repo, new ApiRepo(getHttpCloneLink(repo).getHref(), null));
    }

    private Link getHttpCloneLink(Repo repo) {
        return repo.getLinks().getClone().stream()
                .filter(this::isHttpLink)
                .findFirst().get();
    }

    private boolean isHttpLink(Link link) {
        return link.getName().equals("http");
    }

    private Function<RepoAndApiRepo, ApiRepo> addHasComponentMetadataFileToApiRepo(BitbucketServerHostConfig host) {
        return repoAndApiRepo -> repoAndApiRepo.getApiRepo().withHasComponentMetadataFile(hasComponentMetadataFile(host, repoAndApiRepo.getRepo()));
    }

    private boolean hasComponentMetadataFile(BitbucketServerHostConfig host, Repo repo) {
        String uriTemplate = host.getBaseUrl() + BitbucketServerApiPaths.BROWSE + "/component-metadata.yaml?type=true";
        Map<String, String> uriVariables = UriVariablesBuilder.builder()
                .addUriVariable("projectKey", repo.getProject().getKey())
                .addUriVariable("repositorySlug", repo.getSlug())
                .build();

        logWebCall(uriTemplate, uriVariables);

        ClientResponse clientResponse = makeRequest(host, webClient.get().uri(uriTemplate, uriVariables));
        checkResponseStatus(clientResponse, BROWSE_EXPECTED_STATUS_CODES, uriTemplate, uriVariables);
        BrowseResponse browseResponse = clientResponse.bodyToMono(BrowseResponse.class)
                .block(config.getTimeout());
        return Optional.ofNullable(browseResponse)
                .map(BrowseResponse::getType)
                .map(type -> type.equals("FILE"))
                .orElse(false);
    }

    private ClientResponse makeRequest(BitbucketServerHostConfig host, WebClient.RequestHeadersSpec<?> requestHeadersSpec) {
        return requestHeadersSpec
                .headers(headers -> headers.setBasicAuth(host.getUsername(), host.getPassword()))
                .exchange()
                .block(config.getTimeout());
    }

    private void checkResponseStatus(ClientResponse clientResponse, List<HttpStatus> expectedStatusCodes, String uriTemplate, Map<String, String> uriVariables) {
        if (!expectedStatusCodes.contains(clientResponse.statusCode())) {
            String responseBody = clientResponse.bodyToMono(String.class).block(config.getTimeout());

            BitbucketServerClientException exception = new BitbucketServerClientException(expandUriTemplate(uriTemplate, uriVariables),
                    clientResponse.rawStatusCode(), responseBody);
            log.warn(exception.getMessage());
            throw exception;
        }
    }

    private void logWebCall(String uriTemplate, Map<String, String> uriVariables) {
        if (log.isInfoEnabled()) {
            log.info("Calling {}", expandUriTemplate(uriTemplate, uriVariables));
        }
    }

    private String expandUriTemplate(String uriTemplate, Map<String, String> uriVariables) {
        return new UriTemplate(uriTemplate).expand(uriVariables).toString();
    }

    @Value
    private static class RepoAndApiRepo {

        Repo repo;
        ApiRepo apiRepo;
    }
}

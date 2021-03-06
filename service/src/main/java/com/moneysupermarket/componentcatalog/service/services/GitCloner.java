package com.moneysupermarket.componentcatalog.service.services;

import com.moneysupermarket.componentcatalog.service.config.GitConfig;
import com.moneysupermarket.componentcatalog.service.config.GitHost;
import com.moneysupermarket.componentcatalog.service.models.RepoDirAndGit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static com.moneysupermarket.componentcatalog.common.utils.StringEscapeUtils.escapeString;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitCloner {

    private static final String HEAD_REF_NAME = "HEAD";
    private static final String TAG_REF_NAME_PREFIX = "refs/tags/";
    private static final String BRANCH_REF_NAME_PREFIX = "refs/heads/";

    private final GitConfig config;
    private Path reposDir;

    @PostConstruct
    public void initialize() throws IOException {
        reposDir = Path.of(config.getReposDir());
        Files.createDirectories(reposDir);
    }

    public RepoDirAndGit cloneOrPullRepo(String repoUrl) throws GitAPIException, URISyntaxException, IOException {
        return cloneOrPullRepo(repoUrl, null);
    }

    public RepoDirAndGit cloneOrPullRepo(String repoUrl, String repoRef) throws GitAPIException, URISyntaxException, IOException {
        Path repoDir = getRepoDir(repoUrl);
        return new RepoDirAndGit(repoDir, cloneOrPullRepo(repoUrl, repoRef, repoDir));
    }

    private Path getRepoDir(String repoUrl) {
        return reposDir.resolve(convertRepoUrlToDirName(repoUrl));
    }

    private String convertRepoUrlToDirName(String repoUrl) {
        try {
            return URLEncoder.encode(repoUrl, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Git cloneOrPullRepo(String repoUrl, String repoRef, Path repoDir) throws GitAPIException, URISyntaxException, IOException {
        CredentialsProvider credentialsProvider = getCredentialsProvider(repoUrl);
        Git git = cloneOrFetchGit(repoUrl, repoDir, credentialsProvider);
        checkoutRef(git, repoUrl, repoRef, repoDir, credentialsProvider);
        return git;
    }

    private CredentialsProvider getCredentialsProvider(String repoUrl) throws URISyntaxException {
        if (isNull(config.getHosts())) {
            return null;
        }

        String host = new URI(repoUrl).getHost();
        Optional<GitHost> optionalHost = config.getHosts().stream().filter(item -> item.getHost().equals(host)).findFirst();
        return optionalHost.map(hostEntry -> new UsernamePasswordCredentialsProvider(hostEntry.getUsername(), hostEntry.getPassword())).orElse(null);
    }

    private Git cloneOrFetchGit(String repoUrl, Path repoDir, CredentialsProvider credentialsProvider) throws IOException, GitAPIException {
        if (Files.exists(repoDir)) {
            Git git = null;

            try {
                git = getGit(repoDir);
                fetch(git, repoUrl, repoDir, credentialsProvider);
                return git;
            } catch (Exception e) {
                log.error("Fetch failed for {} so deleting and cloning again", createRepoDescription(repoUrl, repoDir), e);
                if (nonNull(git)) {
                    git.close();
                }
            }

            FileSystemUtils.deleteRecursively(repoDir);
        }

        return clone(repoUrl, repoDir, credentialsProvider);
    }

    private Git getGit(Path repoDir) throws IOException {
        return Git.open(repoDir.toFile());
    }

    private Git clone(String repoUrl, Path repoDir, CredentialsProvider credentialsProvider) throws GitAPIException {
        log.info("Cloning {}", createRepoDescription(repoUrl, repoDir));
        return Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir.toFile())
                .setCredentialsProvider(credentialsProvider)
                .call();
    }

    private void fetch(Git git, String repoUrl, Path repoDir, CredentialsProvider credentialsProvider) throws GitAPIException {
        log.info("Fetching {}", createRepoDescription(repoUrl, repoDir));
        git.fetch()
                .setCredentialsProvider(credentialsProvider)
                .call();
    }

    private void checkoutRef(Git git, String repoUrl, String repoRef, Path repoDir, CredentialsProvider credentialsProvider) throws GitAPIException {
        Collection<Ref> remoteRefs = getRemoteRefs(repoUrl, credentialsProvider);
        Optional<Ref> refMatch;

        if (isNull(repoRef)) {
            refMatch = getHeadRef(remoteRefs);
        } else {
            refMatch = getRefBySimpleName(remoteRefs, repoRef);
        }

        if (refMatch.isEmpty()) {
            throw new RuntimeException(String.format("Could not find ref \"%s\" on remote %s", escapeString(repoRef), createRepoDescription(repoUrl, repoDir)));
        }

        log.info("Checking out ref \"{}\" in {}", escapeString(refMatch.get().getName()), createRepoDescription(repoUrl, repoDir));
        git.checkout()
                .setName(refMatch.get().getObjectId().getName())
                .call();
    }

    private Collection<Ref> getRemoteRefs(String repoUrl, CredentialsProvider credentialsProvider) throws GitAPIException {
        return Git.lsRemoteRepository()
                    .setRemote(repoUrl)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
    }

    private Optional<Ref> getHeadRef(Collection<Ref> refs) {
        return refs.stream()
                .filter(remoteRef -> Objects.equals(remoteRef.getName(), HEAD_REF_NAME))
                .findFirst();
    }

    private Optional<Ref> getRefBySimpleName(Collection<Ref> refs, String simpleName) {
        return refs.stream()
                .filter(doesRefMatchSimpleName(simpleName))
                .findFirst();
    }

    private Predicate<Ref> doesRefMatchSimpleName(String simpleName) {
        return ref -> Objects.equals(ref.getName(), BRANCH_REF_NAME_PREFIX + simpleName)
                || Objects.equals(ref.getName(), TAG_REF_NAME_PREFIX + simpleName);
    }

    private String createRepoDescription(String repoUrl, Path repoDir) {
        return String.format("repo \"%s\" in dir \"%s\"", escapeString(repoUrl), escapeString(repoDir.toString()));
    }
}
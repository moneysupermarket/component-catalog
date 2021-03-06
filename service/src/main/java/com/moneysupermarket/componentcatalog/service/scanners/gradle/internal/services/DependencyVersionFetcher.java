package com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services;

import com.moneysupermarket.componentcatalog.sdk.models.Software;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareDependencyType;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareRepository;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareType;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.MavenPackagings;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.models.Pom;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.models.PomOutcome;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.utils.ArtifactUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class DependencyVersionFetcher {

    private final PomFetcher pomFetcher;
    private final ArtifactUtils artifactUtils;

    public void findDependencyVersions(String scannerId, String pomArtifactCoordinates, Set<SoftwareRepository> softwareRepositories,
            Map<String, Set<String>> dependencyVersions, Set<Software> software) {
        addProjectObjectModelSoftware(scannerId, pomArtifactCoordinates, SoftwareDependencyType.DIRECT, software);
        PomOutcome pomOutcome = pomFetcher.fetchPom(pomArtifactCoordinates, softwareRepositories);
        if (!pomOutcome.isJarOnly()) {
            Pom pom = pomOutcome.getPom();
            pom.getTransitiveArtifactCoordinates().forEach(artifact -> addProjectObjectModelSoftware(scannerId, artifact, SoftwareDependencyType.TRANSITIVE, software));
            if (nonNull(pom.getDependencyManagementDependencies())) {
                pom.getDependencyManagementDependencies().forEach(item -> {
                    Set<String> versions = dependencyVersions.get(item.getName());

                    if (isNull(versions)) {
                        versions = new HashSet<>();
                        dependencyVersions.put(item.getName(), versions);
                    }

                    versions.add(item.getVersion());
                });
            }
        }
    }

    private void addProjectObjectModelSoftware(String scannerId, String pomArtifactCoordinates, SoftwareDependencyType dependencyType,
            Set<Software> software) {
        ArtifactUtils.ArtifactParts parts = artifactUtils.getArtifactParts(pomArtifactCoordinates);
        software.add(new Software(scannerId, SoftwareType.JVM, dependencyType, parts.getName(), parts.getVersion(), null, MavenPackagings.BOM, null));
    }
}

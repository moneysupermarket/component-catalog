package com.moneysupermarket.componentcatalog.service.scanners.gradle;

import com.moneysupermarket.componentcatalog.sdk.models.ScannerError;
import com.moneysupermarket.componentcatalog.sdk.models.Software;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareDependencyType;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareRepository;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareRepositoryScope;
import com.moneysupermarket.componentcatalog.sdk.models.SoftwareType;
import com.moneysupermarket.componentcatalog.sdk.models.gradle.Gradle;
import com.moneysupermarket.componentcatalog.service.constants.Comparators;
import com.moneysupermarket.componentcatalog.service.mappers.ThrowableToScannerErrorMapper;
import com.moneysupermarket.componentcatalog.service.scanners.CodebaseScanner;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.SoftwareRepositoryUrls;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.BaseVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.BuildGradleVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.ProcessPhase;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.ProjectMode;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.SettingsGradleVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.VisitorState;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.models.Import;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.BuildFileLoader;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.BuildFileProcessor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.DependencyVersionFetcher;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.PluginProcessor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.SoftwareRepositoryFactory;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.utils.ArtifactUtils;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.utils.InheritingHashMap;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.utils.InheritingHashSet;
import com.moneysupermarket.componentcatalog.service.scanners.models.Codebase;
import com.moneysupermarket.componentcatalog.service.scanners.models.Output;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Scanner;
import com.moneysupermarket.componentcatalog.service.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.ASTNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.moneysupermarket.componentcatalog.common.utils.StringEscapeUtils.escapeString;
import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.GradleFileNames.BUILD_GRADLE;
import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.GradleFileNames.GRADLE_PROPERTIES;
import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.GradleFileNames.GRADLE_WRAPPER_PROPERTIES;
import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.GradleFileNames.SETTINGS_GRADLE;
import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.GradleWrapperPropertyNames.DISTRIBUTION_URL;
import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.constants.ToolNames.GRADLE_WRAPPER;
import static java.util.Objects.requireNonNull;

@Scanner
@RequiredArgsConstructor
@Slf4j
public class GradleScanner extends CodebaseScanner {

    private static final Pattern GRADLE_WRAPPER_VERSION_EXTRACTION_PATTERN = Pattern.compile("/gradle-([0-9]+\\.[0-9]+(\\.[0-9]+)?)-");
    private static final List<ProcessPhase> PROCESS_PHASES = List.of(
            ProcessPhase.INITIALIZE,
            ProcessPhase.PROPERTIES,
            ProcessPhase.PLUGINS,
            ProcessPhase.BUILDSCRIPT_REPOSITORIES,
            ProcessPhase.BUILDSCRIPT_DEPENDENCIES,
            ProcessPhase.APPLY_PLUGINS,
            ProcessPhase.REPOSITORIES,
            ProcessPhase.DEPENDENCY_MANAGEMENT,
            ProcessPhase.DEPENDENCIES,
            ProcessPhase.FINALIZE);
    private final SettingsGradleVisitor settingsGradleVisitor;
    private final BuildGradleVisitor buildGradleVisitor;
    private final BuildFileLoader buildFileLoader;
    private final DependencyVersionFetcher dependencyVersionFetcher;
    private final ArtifactUtils artifactUtils;
    private final PluginProcessor pluginProcessor;
    private final SoftwareRepositoryFactory softwareRepositoryFactory;
    private final BuildFileProcessor buildFileProcessor;
    private final ThrowableToScannerErrorMapper throwableToScannerErrorMapper;
    private final FileUtils fileUtils;

    @Override
    public String id() {
        return "gradle";
    }

    @Override
    public String description() {
        return "Scans a component's codebase for any Gradle build scripts.  Collections information like Gradle version and software used";
    }

    @Override
    public String notes() {
        return "If the scanner finds Gradle build scripts, it will:\n"
                + "\n"
                + "* Find the version of Gradle wrapper used\n"
                + "* Find the names and versions of any Gradle plugins used"
                + "* Find the names and versions of any Java libraries used";
    }

    @Override
    public Output<Void> scan(Codebase input) {
        log.info("Starting Gradle scan of codebase \"" + escapeString(input.getDir().toString()) + "\"");

        Gradle gradle;
        Set<SoftwareRepository> allSoftwareRepositories = new HashSet<>();
        Set<Software> allSoftware = new HashSet<>();

        try {
            Path gradleWrapperPropertiesFile = input.getDir().resolve("gradle").resolve("wrapper").resolve(GRADLE_WRAPPER_PROPERTIES);

            if (Files.exists(gradleWrapperPropertiesFile)) {
                Properties gradleWrapperProperties = fileUtils.loadProperties(gradleWrapperPropertiesFile);
                String distributionUrl = requireNonNull(gradleWrapperProperties.getProperty(DISTRIBUTION_URL),
                        "distributionUrl in " + GRADLE_WRAPPER_PROPERTIES + " file does not contain \"" + DISTRIBUTION_URL +  "\" property");
                allSoftware.add(Software.builder()
                        .scannerId(id())
                        .type(SoftwareType.TOOL)
                        .dependencyType(SoftwareDependencyType.DIRECT)
                        .name(GRADLE_WRAPPER)
                        .version(extractGradleWrapperVersionFromDistributionUrl(distributionUrl))
                        .build());
            }

            HashMap<String, String> rootProperties = new HashMap<>();
            rootProperties.put("rootDir", input.getDir().toString());
            rootProperties.put("rootProject.projectDir", input.getDir().toString());
            rootProperties.put("rootProject.name", input.getDir().getFileName().toString());

            long buildFileCount = fileUtils.findFiles(input.getDir(), this::matchBuildFile)
                .peek(buildFile -> {
                    log.debug("Found build file \"" + escapeString(buildFile.toString()) + "\"");
                    List<Path> buildFileChain = getBuildFileChain(input.getDir(), buildFile);
                    List<InheritingHashMap<String, String>> properties = new ArrayList<>();
                    List<InheritingHashSet<SoftwareRepository>> buildscriptSoftwareRepositories = new ArrayList<>();
                    List<InheritingHashSet<SoftwareRepository>> softwareRepositories = new ArrayList<>();
                    List<InheritingHashSet<Software>> software = new ArrayList<>();
                    List<InheritingHashMap<String, Set<String>>> dependencyVersions = new ArrayList<>();

                    PROCESS_PHASES.forEach(processPhase -> {
                        log.debug("Beginning {} phase", processPhase);
                        for (int index = 0, count = buildFileChain.size(); index < count; index++) {
                            ProjectMode projectMode = getProjectMode(buildFileChain, index);

                            InheritingHashMap<String, String> currentProperties = getInheritingItem(properties, index, processPhase,
                                    () -> cloneValues(rootProperties), InheritingHashMap::new);
                            InheritingHashSet<SoftwareRepository> currentBuildscriptSoftwareRepositories = getInheritingItem(buildscriptSoftwareRepositories,
                                    index, processPhase, InheritingHashSet::new, InheritingHashSet::new);
                            InheritingHashSet<SoftwareRepository> currentSoftwareRepositories = getInheritingItem(softwareRepositories, index, processPhase,
                                    InheritingHashSet::new, InheritingHashSet::new);
                            InheritingHashSet<Software> currentSoftware = getInheritingItem(software, index, processPhase,
                                    InheritingHashSet::new, InheritingHashSet::new);
                            InheritingHashMap<String, Set<String>> currentDependencyVersions = getInheritingItem(dependencyVersions, index, processPhase,
                                    InheritingHashMap::new, InheritingHashMap::new);

                            Path currentBuildFile = buildFileChain.get(index);
                            log.debug("Processing build file \"" + escapeString(currentBuildFile.toString()) + "\"");

                            if (processPhase == ProcessPhase.INITIALIZE) {
                                if (projectMode != ProjectMode.SETTINGS) {
                                    Path projectDir = currentBuildFile.getParent();
                                    currentProperties.put("project.name", projectDir.getFileName().toString());
                                    String propertyName = "projectDir";

                                    while (projectDir.startsWith(input.getDir())) {
                                        currentProperties.put(propertyName, projectDir.toString());

                                        projectDir = projectDir.getParent();
                                        propertyName += ".parent";
                                    }
                                }

                                Path gradlePropertiesFile = currentBuildFile.getParent().resolve(GRADLE_PROPERTIES);
                                if (Files.exists(gradlePropertiesFile)) {
                                    Properties gradleProperties = fileUtils.loadProperties(gradlePropertiesFile);
                                    addPropertiesToPropertyMap(gradleProperties, currentProperties);
                                }
                            } else if (processPhase == ProcessPhase.FINALIZE) {
                                if (isLastBuildFileInChain(buildFileChain, index)) {
                                    allSoftwareRepositories.addAll(currentBuildscriptSoftwareRepositories);
                                    allSoftwareRepositories.addAll(currentSoftwareRepositories);
                                    allSoftware.addAll(currentSoftware);
                                }
                            } else {
                                if (processPhase == ProcessPhase.DEPENDENCIES && projectMode != ProjectMode.SETTINGS) {
                                    Optional<Software> optionalSpringBootPlugin = pluginProcessor.getSpringBootPlugin(currentSoftware);

                                    optionalSpringBootPlugin.ifPresent(springBootPlugin ->
                                        dependencyVersionFetcher.findDependencyVersions(
                                                id(),
                                                artifactUtils.createArtifactFromNameAndVersion(
                                                        "org.springframework.boot:spring-boot-dependencies",
                                                        springBootPlugin.getVersion()),
                                                currentSoftwareRepositories,
                                                currentDependencyVersions,
                                                currentSoftware));
                                }

                                if (Files.exists(currentBuildFile)) {
                                    List<ASTNode> nodes = buildFileLoader.loadBuildFile(currentBuildFile, input.getDir());

                                    try {
                                        Set<Import> imports = buildFileProcessor.getImports(nodes);

                                        VisitorState visitorState = new VisitorState(id(), processPhase, projectMode, input.getDir(), currentBuildFile,
                                                null, imports, currentBuildscriptSoftwareRepositories, currentSoftwareRepositories,
                                                currentSoftware, currentProperties, currentDependencyVersions);
                                        BaseVisitor visitor = (projectMode == ProjectMode.SETTINGS) ? settingsGradleVisitor : buildGradleVisitor;
                                        visitor.setVisitorState(visitorState, visitorState.getProperties());

                                        buildFileProcessor.visitNodes(nodes, visitor);
                                    } catch (Exception e) {
                                        throw new RuntimeException(String.format("Failed to process build file \"%s\" for %s project mode and %s process phase",
                                                escapeString(currentBuildFile.toString()), projectMode, processPhase), e);
                                    }
                                }

                                if (processPhase == ProcessPhase.BUILDSCRIPT_REPOSITORIES && projectMode != ProjectMode.SETTINGS) {
                                    if (pluginProcessor.getPluginCount(currentSoftware) > 0) {
                                        if (currentBuildscriptSoftwareRepositories.isEmpty()) {
                                            currentBuildscriptSoftwareRepositories.add(softwareRepositoryFactory.createSoftwareRepository(id(), SoftwareRepositoryUrls.GRADLE_PLUGIN_PORTAL,
                                                    SoftwareRepositoryScope.BUILDSCRIPT));
                                        }
                                    }
                                }
                            }
                        }
                    });
                })
                .count();

            gradle = new Gradle(buildFileCount > 0);
        } catch (Exception e) {
            return Output.of(new ScannerError(id(), "Failed to scan codebase", throwableToScannerErrorMapper.map(id(), e)));
        }

        List<SoftwareRepository> allSoftwareRepositoriesList = allSoftwareRepositories.stream()
                .sorted(Comparators.SOFTWARE_REPOSITORIES)
                .collect(Collectors.toList());
        List<Software> allSoftwareList = allSoftware.stream()
                .sorted(Comparators.SOFTWARE)
                .collect(Collectors.toList());
        return Output.of(component -> component.withGradle(gradle)
                .withSoftwareRepositories(replaceScannerItemsInList(component.getSoftwareRepositories(), allSoftwareRepositoriesList))
                .withSoftware(replaceScannerItemsInList(component.getSoftware(), allSoftwareList)));
    }

    private ProjectMode getProjectMode(List<Path> buildFileChain, int index) {
        ProjectMode projectMode;

        if (index == 0 && isSettingsGradleFile(buildFileChain.get(index))) {
            projectMode = ProjectMode.SETTINGS;
        } else {
            projectMode = isLastBuildFileInChain(buildFileChain, index)
                    ? ProjectMode.THIS_PROJECT
                    : ProjectMode.SUBPROJECT;
        }
        return projectMode;
    }

    private boolean isSettingsGradleFile(Path file) {
        return Objects.equals(file.getFileName().toString(), SETTINGS_GRADLE);
    }

    private <T> T getInheritingItem(List<T> list, int index, ProcessPhase processPhase, Supplier<T> initializeRoot, UnaryOperator<T> initializeChild) {
        if (processPhase == ProcessPhase.INITIALIZE) {
            T item = (index == 0) ? initializeRoot.get() : initializeChild.apply(getParentItem(list, index));
            list.add(item);
            return item;
        } else {
            return list.get(index);
        }
    }

    private <T> T getParentItem(List<T> list, int index) {
        return list.get(index - 1);
    }

    protected InheritingHashMap<String, String> cloneValues(HashMap<String, String> values) {
        InheritingHashMap<String, String> newValues = new InheritingHashMap<>();
        newValues.putAll(values);
        return newValues;
    }

    private boolean isLastBuildFileInChain(List<Path> buildFileChain, int index) {
        return index == buildFileChain.size() - 1;
    }

    private List<Path> getBuildFileChain(Path codebaseDir, Path buildFile) {
        List<Path> buildFileChain = new ArrayList<>();
        Path currentBuildFile = buildFile;

        do {
            buildFileChain.add(0, currentBuildFile);
            currentBuildFile = currentBuildFile.getParent().getParent().resolve(BUILD_GRADLE);
        } while (currentBuildFile.startsWith(codebaseDir));

        currentBuildFile = codebaseDir.resolve(SETTINGS_GRADLE);
        buildFileChain.add(0, currentBuildFile);

        return buildFileChain;
    }

    private void addPropertiesToPropertyMap(Properties gradleProperties, HashMap<String, String> properties) {
        gradleProperties.forEach((name, value) -> properties.put((String) name, (String) value));
    }

    private String extractGradleWrapperVersionFromDistributionUrl(String distributionUrl) {
        Matcher matcher = GRADLE_WRAPPER_VERSION_EXTRACTION_PATTERN.matcher(distributionUrl);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not extract Gradle Wrapper version from distribution URL \"" + escapeString(distributionUrl) + "\"");
        }

        return matcher.group(1);
    }

    private boolean matchBuildFile(Path path, BasicFileAttributes basicFileAttributes) {
        return basicFileAttributes.isRegularFile() && Objects.equals(path.getFileName().toString(), BUILD_GRADLE);
    }
}

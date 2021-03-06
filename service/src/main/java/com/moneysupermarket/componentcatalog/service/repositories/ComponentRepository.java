package com.moneysupermarket.componentcatalog.service.repositories;

import com.moneysupermarket.componentcatalog.componentmetadata.models.ComponentMetadata;
import com.moneysupermarket.componentcatalog.sdk.models.Area;
import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.Scanner;
import com.moneysupermarket.componentcatalog.sdk.models.Summary;
import com.moneysupermarket.componentcatalog.sdk.models.SummaryCallGraph;
import com.moneysupermarket.componentcatalog.sdk.models.SummarySubComponentDependencies;
import com.moneysupermarket.componentcatalog.sdk.models.SummarySubComponentDependencyNode;
import com.moneysupermarket.componentcatalog.sdk.models.Team;
import com.moneysupermarket.componentcatalog.sdk.models.Test;
import com.moneysupermarket.componentcatalog.service.services.ComponentMetadataAssembler;
import com.moneysupermarket.componentcatalog.service.services.ComponentMetadataLoader;
import com.moneysupermarket.componentcatalog.service.services.ScanEngine;
import com.moneysupermarket.componentcatalog.service.services.ScannerFinder;
import com.moneysupermarket.componentcatalog.service.services.TestEngine;
import com.moneysupermarket.componentcatalog.service.services.TestFinder;
import com.moneysupermarket.componentcatalog.service.utils.ObjectReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ComponentRepository extends RefreshingRepository {

    private final ComponentMetadataRepository repository;
    private final ComponentMetadataLoader loader;
    private final ComponentMetadataAssembler assembler;
    private final ScanEngine scanEngine;
    private final ScannerFinder scannerFinder;
    private final TestEngine testEngine;
    private final TestFinder testFinder;
    private volatile ConcurrentHashMap<String, Area> areas = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, Team> teams = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, Component> components = new ConcurrentHashMap<>();
    private volatile Summary summary = Summary.EMPTY;

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected void doInitialize() {
    }

    @Scheduled(cron = "0 */15 * * * *", zone = "UTC")
    @Override
    public void refresh() {
        super.refresh();
    }

    @Override
    protected void doRefresh(boolean firstTime) {
        ComponentMetadata componentMetadata = repository.getComponentMetadata();
        ComponentMetadataLoader.Output loaderOutput = loader.loadComponentMetadata(componentMetadata);
        Consumer<Summary> summaryUpdater;
        ObjectReference<Summary> newSummary = new ObjectReference<>();

        if (firstTime) {
            updateState(loaderOutput);
            summaryUpdater = updatedSummary -> summary = updatedSummary;
        } else {
            summaryUpdater = newSummary::set;
        }

        scanEngine.scan(componentMetadata, loaderOutput.getComponents(), summaryUpdater);
        testEngine.test(loaderOutput.getComponents());

        if (!firstTime) {
            updateState(loaderOutput);
            summary = newSummary.get();
        }
    }

    private void updateState(ComponentMetadataLoader.Output loaderOutput) {
        areas = loaderOutput.getAreas();
        teams = loaderOutput.getTeams();
        components = loaderOutput.getComponents();
    }

    public List<Area> getAreas() {
        return assembler.toSortedUnmodifiableAreaList(areas.values().stream(), teams, components);
    }

    public Area getArea(String areaId) {
        return assembler.addNestedItemsToArea(areas.get(areaId), teams, components);
    }

    public List<Team> getTeams() {
        return assembler.toSortedUnmodifiableTeamList(teams.values().stream(), components);
    }

    public Team getTeam(String teamId) {
        return assembler.addNestedItemsToTeam(teams.get(teamId), components);
    }

    public List<Component> getComponents() {
        return assembler.toSortedUnmodifiableComponentList(components.values().stream());
    }

    public Component getComponent(String componentId) {
        return components.get(componentId);
    }

    public List<SummarySubComponentDependencyNode> getComponentNodes(String componentId) {
        return summary.getSubComponentDependencies().getNodes().stream()
                .filter(nodeBelongsToComponent(componentId))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<SummaryCallGraph> getComponentCallGraphs(String componentId) {
        return summary.getCallGraphs().stream()
                .filter(callGraphIncludesComponent(componentId))
                .collect(Collectors.toUnmodifiableList());
    }

    public Summary getSummary() {
        return summary;
    }

    public List<Scanner> getScanners() {
        return scannerFinder.getAllScanners().stream()
                .map(this::mapScanner)
                .sorted(Comparator.comparing(Scanner::getId))
                .collect(Collectors.toList());
    }

    public Scanner getScanner(String scannerId) {
        return mapScanner(scannerFinder.getScanner(scannerId));
    }

    public List<Test> getTests() {
        return testFinder.getAllTests().stream()
                .map(this::mapTest)
                .sorted(Comparator.comparing(Test::getId))
                .collect(Collectors.toUnmodifiableList());
    }

    public Test getTest(String testId) {
        return Optional.ofNullable(testFinder.getTest(testId))
                .map(this::mapTest)
                .orElse(null);
    }

    private Scanner mapScanner(com.moneysupermarket.componentcatalog.service.scanners.Scanner<?, ?> scanner) {
        if (isNull(scanner)) {
            return null;
        }

        return Scanner.builder()
                .id(scanner.id())
                .description(scanner.description())
                .notes(scanner.notes())
                .build();
    }

    private Predicate<SummarySubComponentDependencies> callGraphIncludesComponent(String componentId) {
        return callGraph -> callGraph.getNodes().stream().anyMatch(nodeBelongsToComponent(componentId));
    }

    private Predicate<SummarySubComponentDependencyNode> nodeBelongsToComponent(String componentId) {
        return node -> Objects.equals(node.getComponentId(), componentId);
    }

    private Test mapTest(com.moneysupermarket.componentcatalog.service.tests.Test<?> test) {
        return new Test(test.id(), test.description(), test.notes(), test.priority());
    }
}

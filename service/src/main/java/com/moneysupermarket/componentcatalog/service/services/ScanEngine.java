package com.moneysupermarket.componentcatalog.service.services;

import com.moneysupermarket.componentcatalog.componentmetadata.models.ComponentMetadata;
import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.ObjectWithReference;
import com.moneysupermarket.componentcatalog.sdk.models.Repo;
import com.moneysupermarket.componentcatalog.sdk.models.ScannerError;
import com.moneysupermarket.componentcatalog.sdk.models.Summary;
import com.moneysupermarket.componentcatalog.service.exceptions.ValidationException;
import com.moneysupermarket.componentcatalog.service.mappers.ThrowableToScannerErrorMapper;
import com.moneysupermarket.componentcatalog.service.scanners.Scanner;
import com.moneysupermarket.componentcatalog.service.scanners.models.Codebase;
import com.moneysupermarket.componentcatalog.service.scanners.models.ComponentAndCodebase;
import com.moneysupermarket.componentcatalog.service.scanners.models.Output;
import com.moneysupermarket.componentcatalog.service.utils.MapCollectors;
import com.moneysupermarket.componentcatalog.service.utils.ObjectReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.moneysupermarket.componentcatalog.common.utils.StringEscapeUtils.escapeString;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanEngine {

    private final ScannerFinder scannerFinder;
    private final ValidatorService validatorService;
    private final ThrowableToScannerErrorMapper throwableToScannerErrorMapper;

    public void scan(ComponentMetadata componentMetadata, ConcurrentHashMap<String, Component> componentMap, Consumer<Summary> summaryConsumer) {
        ObjectReference<Summary> summary = new ObjectReference<>(Summary.EMPTY);
        Consumer<UnaryOperator<Summary>> summaryTransformerConsumer = summaryTransformer -> {
            Summary transformedSummary = summaryTransformer.apply(summary.get());
            summaryConsumer.accept(transformedSummary);
            summary.set(transformedSummary);
        };

        scannerFinder.getComponentScanners().forEach(scanner -> executeScanner(
                componentMetadata,
                getFreshComponentAndComponentIdMap(componentMap),
                componentMap,
                scanner,
                summaryTransformerConsumer));
        Map<Codebase, List<String>> codebaseAndComponentIdsMap = executeScanner(
                componentMetadata,
                getRepoAndComponentIdsMap(componentMap),
                componentMap,
                scannerFinder.getRepoScanner(),
                summaryTransformerConsumer);
        scannerFinder.getCodebaseScanners().forEach(scanner -> executeScanner(
                componentMetadata,
                codebaseAndComponentIdsMap,
                componentMap,
                scanner,
                summaryTransformerConsumer));
        scannerFinder.getComponentAndCodebaseScanners().forEach(scanner -> executeScanner(
                componentMetadata,
                getFreshComponentAndCodebaseAndComponentIdsMap(componentMap, codebaseAndComponentIdsMap),
                componentMap,
                scanner,
                summaryTransformerConsumer));
        scannerFinder.getLateComponentScanners().forEach(scanner -> executeScanner(
                componentMetadata,
                getFreshComponentAndComponentIdMap(componentMap),
                componentMap,
                scanner,
                summaryTransformerConsumer));
    }

    /**
     * The components are immutable value types using Lombok's @Value annotation.  The components in componentMap get updated (replaced because they
     * are immutable) when they pass through the scanners.  Consequently this method needs to be called each time its return value is needed and not stored
     * in a variable for reuse, otherwise the components in the map would be out-of-date for second and subsequent reuses.
     *
     * @param componentMap  a map with component ids as the keys and their components as the values.
     * @return              a map containing components as keys and each component's id as the value
     */
    private Map<Component, List<String>> getFreshComponentAndComponentIdMap(ConcurrentHashMap<String, Component> componentMap) {
        return componentMap.values().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.mapping(Component::getId, Collectors.toList())));
    }

    private Map<Repo, List<String>> getRepoAndComponentIdsMap(ConcurrentHashMap<String, Component> componentMap) {
        return componentMap.values().stream()
                .collect(Collectors.groupingBy(Component::getRepo, Collectors.mapping(Component::getId, Collectors.toList())));
    }

    /**
     * The components are immutable value types using Lombok's @Value annotation.  The components in componentMap get updated (replaced because they
     * are immutable) when they pass through the scanners.  Consequently this method needs to be called each time its return value is needed and not stored
     * in a variable for reuse, otherwise the components in the map would be out-of-date for second and subsequent reuses.
     *
     * @param componentMap                  a map with component ids as the keys and their components as the values.
     * @param codebaseAndComponentIdsMap    a map with codebases as the keys and their components as the values.  Each codebase can have 1 or more components
     * @return                              a map containing component and codebase pairs as keys and those component's ids as the values
     */
    private Map<ComponentAndCodebase, List<String>> getFreshComponentAndCodebaseAndComponentIdsMap(ConcurrentHashMap<String, Component> componentMap,
            Map<Codebase, List<String>> codebaseAndComponentIdsMap) {
        return codebaseAndComponentIdsMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(componentId -> Map.entry(
                                convertComponentIdAndCodebaseToComponentAndCodebase(componentMap, componentId, entry.getKey()),
                                List.of(componentId))))
                .collect(MapCollectors.toMap());
    }

    private ComponentAndCodebase convertComponentIdAndCodebaseToComponentAndCodebase(ConcurrentHashMap<String, Component> componentMap, String componentId,
            Codebase codebase) {

        return new ComponentAndCodebase(componentMap.get(componentId), codebase);
    }

    private <I extends ObjectWithReference, O> Map<O, List<String>> executeScanner(ComponentMetadata componentMetadata,
            Map<I, List<String>> inputAndComponentIdsMap, ConcurrentHashMap<String, Component> componentMap, Scanner<I, O> scanner,
            Consumer<UnaryOperator<Summary>> summaryTransformerConsumer) {
        try {
            scanner.refresh(componentMetadata);
        } catch (Exception e) {
            log.error("Failed to refresh scanner {}", scanner.id(), e);
            List<ScannerError> newErrors = List.of(new ScannerError(scanner.id(), "Failed to refresh scanner",
                    throwableToScannerErrorMapper.map(scanner.id(), e)));
            inputAndComponentIdsMap.values().forEach(componentIds -> addScannerErrorsToComponents(componentMap, componentIds, scanner, newErrors));
            return Map.of();
        }

        Map<O, List<String>> outputAndComponentIdsMap = inputAndComponentIdsMap
                .entrySet()
                .stream()
                .sorted(getInputEntryComparator())
                .map(entry -> executeScanner(entry.getKey(), entry.getValue(), componentMap, scanner))
                .filter(Objects::nonNull)
                .collect(MapCollectors.toMap());

        summaryTransformerConsumer.accept(scanner::transformSummary);

        return outputAndComponentIdsMap;
    }

    private <I extends ObjectWithReference> Comparator<Map.Entry<I, List<String>>> getInputEntryComparator() {
        return Comparator.comparing(entry -> entry.getKey().reference());
    }

    private void addScannerErrorsToComponents(ConcurrentHashMap<String, Component> componentMap, List<String> componentIds, Scanner<?, ?> scanner,
            List<ScannerError> newErrors) {
        updateComponents(componentMap, componentIds, scanner, component -> addScannerErrorsToComponent(component, newErrors));
    }

    private Component addScannerErrorsToComponent(Component component, List<ScannerError> newErrors) {
        List<ScannerError> errors = new ArrayList<>(component.getScannerErrors());
        errors.addAll(newErrors);
        return component.withScannerErrors(errors);
    }

    private void updateComponents(ConcurrentHashMap<String, Component> componentMap, List<String> componentIds, Scanner<?, ?> scanner,
            UnaryOperator<Component> componentTransformer) {
        componentIds.forEach(componentId -> updateComponent(componentMap, componentId, scanner, componentTransformer));
    }

    private void updateComponent(ConcurrentHashMap<String, Component> componentMap, String componentId, Scanner<?, ?> scanner, UnaryOperator<Component> componentTransformer) {
        Component component = componentTransformer.apply(componentMap.get(componentId));
        try {
            validatorService.validate(component);
        } catch (ValidationException e) {
            ScannerError scannerError = new ScannerError(scanner.id(), "Validation failure for transformed component",
                    throwableToScannerErrorMapper.map(scanner.id(), e));
            component = addScannerErrorsToComponent(component, List.of(scannerError));
        }
        componentMap.put(componentId, component);
    }

    private <I extends ObjectWithReference, O> Map.Entry<O, List<String>> executeScanner(I input, List<String> componentIds,
            ConcurrentHashMap<String, Component> componentMap, Scanner<I, O> scanner) {
        log.info("Executing scanner {} for \"{}\"", scanner.id(), escapeString(input.reference()));
        Output<O> output;
        try {
            output = scanner.scan(input);
        } catch (Exception e) {
            output = Output.of(new ScannerError(
                    scanner.id(), String.format("Failed to scan \"%s\"", escapeString(input.reference())), throwableToScannerErrorMapper.map(scanner.id(), e)));
        }

        if (output.failed()) {
            addScannerErrorsToComponents(componentMap, componentIds, scanner, output.getErrors());
            output.getErrors().forEach(error -> log.error("Failed to scan \"{}\" with scanner {}: {}", escapeString(input.reference()), scanner.id(),
                    error.toString()));
        }

        UnaryOperator<Component> componentTransformer = output.getComponentTransformer();
        if (nonNull(componentTransformer)) {
            updateComponents(componentMap, componentIds, scanner, componentTransformer);
        }

        return nonNull(output.getOutput()) ? Map.entry(output.getOutput(), componentIds) : null;
    }
}

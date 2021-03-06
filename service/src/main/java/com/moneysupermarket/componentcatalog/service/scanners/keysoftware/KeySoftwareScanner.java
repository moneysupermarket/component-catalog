package com.moneysupermarket.componentcatalog.service.scanners.keysoftware;

import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.KeySoftware;
import com.moneysupermarket.componentcatalog.sdk.models.Software;
import com.moneysupermarket.componentcatalog.service.scanners.LateComponentScanner;
import com.moneysupermarket.componentcatalog.service.scanners.keysoftware.config.KeySoftwareConfig;
import com.moneysupermarket.componentcatalog.service.scanners.keysoftware.config.KeySoftwareRule;
import com.moneysupermarket.componentcatalog.service.scanners.models.Output;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Scanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.Version;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Scanner
@RequiredArgsConstructor
@Slf4j
public class KeySoftwareScanner extends LateComponentScanner {

    private final Map<String, Pattern> patternCache = new HashMap<>();
    private final VersionParser versionParser = new VersionParser();
    private final Comparator<Version> versionComparator = new DefaultVersionComparator().asVersionComparator().reversed();
    private final KeySoftwareConfig config;

    @Override
    public String id() {
        return "key-software";
    }

    @Override
    public String description() {
        return "Processes all software found by other scanners and looks for certain configured `key software` to find what version(s) if any of those key "
                + "software a component uses.  Key software is typically things like Gradle and Spring Boot";
    }

    @Override
    public Output<Void> scan(Component input) {
        if (isNull(input.getSoftware())) {
            return Output.of(UnaryOperator.identity());
        }

        List<KeySoftware> keySoftware = getKeySoftware(input);
        return Output.of(component -> component.withKeySoftware(keySoftware));
    }

    private List<KeySoftware> getKeySoftware(Component input) {
        return config.getRules().stream()
                .map(applyRule(input))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Function<KeySoftwareRule, KeySoftware> applyRule(Component component) {
        return (KeySoftwareRule rule) -> {
            List<String> versions = applyRule(component, rule);

            if (versions.isEmpty()) {
                return null;
            }

            return new KeySoftware(rule.getName(), versions);
        };
    }

    private List<String> applyRule(Component component, KeySoftwareRule rule) {
        Pattern softwareNamePattern = getCachedPattern(rule.getSoftwareNamePattern());
        return component.getSoftware().stream()
                .filter(software -> softwareNamePattern.matcher(software.getName()).find())
                .map(Software::getVersion)
                .map(versionParser::transform)
                .sorted(versionComparator)
                .map(Version::getSource)
                .distinct()
                .collect(Collectors.toList());
    }

    private Pattern getCachedPattern(String pattern) {
        Pattern compiledPattern = patternCache.get(pattern);

        if (isNull(compiledPattern)) {
            compiledPattern = Pattern.compile(pattern);
            patternCache.put(pattern, compiledPattern);
        }

        return compiledPattern;
    }
}

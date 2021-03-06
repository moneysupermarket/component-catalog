package com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors;

import com.moneysupermarket.componentcatalog.sdk.models.Software;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor.BuildscriptVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor.DependenciesVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor.DependencyManagementVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor.ExtOuterVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor.RepositoriesVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.BuildFileLoader;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.BuildFileProcessor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.ExpressionEvaluator;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.PluginProcessor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.SoftwareRepositoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.ProjectMode.SUBPROJECT;

@Component
@Slf4j
public class BuildGradleVisitor extends BaseBuildFileVisitor {

    private final BuildscriptVisitor buildscriptVisitor;
    private final DependencyManagementVisitor dependencyManagementVisitor;
    private final DependenciesVisitor dependenciesVisitor;
    private final ExtOuterVisitor extOuterVisitor;
    private final PluginProcessor pluginProcessor;

    public BuildGradleVisitor(BuildFileLoader buildFileLoader, BuildFileProcessor buildFileProcessor, ExpressionEvaluator expressionEvaluator,
            BuildscriptVisitor buildscriptVisitor, PluginsVisitor pluginsVisitor, RepositoriesVisitor repositoriesVisitor,
            DependencyManagementVisitor dependencyManagementVisitor, DependenciesVisitor dependenciesVisitor, ExtOuterVisitor extOuterVisitor,
            PluginProcessor pluginProcessor, SoftwareRepositoryFactory softwareRepositoryFactory) {
        super(buildFileLoader, buildFileProcessor, expressionEvaluator, pluginsVisitor, repositoriesVisitor, softwareRepositoryFactory, pluginProcessor);
        this.buildscriptVisitor = buildscriptVisitor;
        this.dependencyManagementVisitor = dependencyManagementVisitor;
        this.dependenciesVisitor = dependenciesVisitor;
        this.extOuterVisitor = extOuterVisitor;
        this.pluginProcessor = pluginProcessor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("allprojects")) {
            log.debug("Found allprojects");
            return ExpressionVisitOutcome.CONTINUE;
        } else if (call.getMethodAsString().equals("subprojects")) {
            log.debug("Found subprojects");
            return visitorState().getProjectMode() == SUBPROJECT
                    ? ExpressionVisitOutcome.CONTINUE
                    : ExpressionVisitOutcome.IGNORED;
        } else if (call.getMethodAsString().equals("buildscript")) {
            if (visitorState().getProcessPhase() == ProcessPhase.PROPERTIES
                || visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_REPOSITORIES
                || visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_DEPENDENCIES) {
                log.debug("Found buildscript");
                visit(call.getArguments(), buildscriptVisitor);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("ext")) {
            if (visitorState().getProcessPhase() == ProcessPhase.PROPERTIES) {
                log.debug("Found ext");
                int count = visitorState().getProperties().size();
                visit(call, extOuterVisitor);
                log.debug("Found {} project properties", visitorState().getProperties().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("dependencyManagement")) {
            if (visitorState().getProcessPhase() == ProcessPhase.DEPENDENCY_MANAGEMENT) {
                log.debug("Found dependencyManagement");
                visit(call.getArguments(), dependencyManagementVisitor);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("dependencies")) {
            if (visitorState().getProcessPhase() == ProcessPhase.DEPENDENCIES) {
                log.debug("Found dependencies");
                int count = visitorState().getSoftware().size();
                visit(call.getArguments(), dependenciesVisitor);
                log.debug("Found {} dependencies", visitorState().getSoftware().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        }

        return super.processMethodCallExpression(call);
    }

    @Override
    protected void processApplyPlugin(Map<String, String> values) {
        log.debug("Process apply plugin");
        int count = getPluginCount();
        String name = values.get("plugin");

        if (Objects.equals(name, "org.springframework.boot")) {
            Optional<Software> springBootPlugin = pluginProcessor.getSpringBootPlugin(visitorState().getSoftware());

            if (springBootPlugin.isEmpty()) {
                Optional<Software> springBootPluginDependency = pluginProcessor.getSpringBootPluginDependency(visitorState().getSoftware());

                if (springBootPluginDependency.isEmpty()) {
                    throw new RuntimeException("Could not find dependency for Spring Boot Plugin");
                }

                pluginProcessor.processPlugin(visitorState().getScannerId(), name, springBootPluginDependency.get().getVersion(), visitorState().getSoftware());
            }
        } else {
            pluginProcessor.processPlugin(visitorState().getScannerId(), name, null, visitorState().getSoftware());
        }

        log.debug("Found {} plugins", getPluginCount() - count);
    }
}

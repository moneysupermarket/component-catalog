package com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.buildgradlevisitor;

import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.BaseVisitor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.groovyscriptvisitors.ExpressionVisitOutcome;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.BuildFileLoader;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.BuildFileProcessor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.ExpressionEvaluator;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.PluginProcessor;
import com.moneysupermarket.componentcatalog.service.scanners.gradle.internal.services.SoftwareRepositoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

@Component
@Slf4j
public class PluginsVisitor extends BaseVisitor {

    private final PluginProcessor pluginProcessor;

    public PluginsVisitor(BuildFileLoader buildFileLoader, BuildFileProcessor buildFileProcessor, ExpressionEvaluator expressionEvaluator,
            PluginProcessor pluginProcessor, SoftwareRepositoryFactory softwareRepositoryFactory) {
        super(buildFileLoader, buildFileProcessor, expressionEvaluator, softwareRepositoryFactory);
        this.pluginProcessor = pluginProcessor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("id")) {
            processPlugin(call);
            return ExpressionVisitOutcome.PROCESSED;
        } else if (call.getMethodAsString().equals("version")) {
            processPlugin(call);
            return ExpressionVisitOutcome.PROCESSED;
        } else if (call.getMethodAsString().equals("apply")) {
            processPlugin(call);
            return ExpressionVisitOutcome.PROCESSED;
        }

        return ExpressionVisitOutcome.IGNORED;
    }

    private void processPlugin(MethodCallExpression call) {
        Map<String, String> values = getValues(call);
        String name = values.get("id");
        String version = values.get("version");

        pluginProcessor.processPlugin(visitorState().getScannerId(), name, version, visitorState().getSoftware());
    }

    private Map<String, String> getValues(MethodCallExpression call) {
        Map<String, String> values = new HashMap<>();
        do {
            addValue(values, call);
            call = call.getObjectExpression() instanceof MethodCallExpression
                ? (MethodCallExpression) call.getObjectExpression()
                : null;
        } while (nonNull(call));
        return values;
    }

    private void addValue(Map<String, String> values, MethodCallExpression call) {
        String argumentValue = getArgumentValue(call);
        values.put(call.getMethodAsString(), argumentValue);
    }

    private String getArgumentValue(MethodCallExpression call) {
        ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
        if (arguments.getExpressions().size() == 1) {
            return evaluateExpression(arguments.getExpression(0));
        } else {
            throw new RuntimeException(String.format("Method has %d arguments but only 1 argument is supported", arguments.getExpressions().size()));
        }
    }
}

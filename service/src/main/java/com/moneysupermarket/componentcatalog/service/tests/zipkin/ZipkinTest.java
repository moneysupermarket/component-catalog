package com.moneysupermarket.componentcatalog.service.tests.zipkin;

import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.Priority;
import com.moneysupermarket.componentcatalog.sdk.models.TestResult;
import com.moneysupermarket.componentcatalog.sdk.models.zipkin.Zipkin;
import com.moneysupermarket.componentcatalog.service.scanners.zipkin.config.ZipkinConfig;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Test;
import com.moneysupermarket.componentcatalog.service.tests.ComponentTest;
import com.moneysupermarket.componentcatalog.service.tests.models.TestContext;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static java.util.Objects.isNull;

@Test
@RequiredArgsConstructor
public class ZipkinTest extends ComponentTest {

    private final ZipkinConfig config;

    @Override
    public String description() {
        return "Checks whether various types of components are using [Zipkin](https://zipkin.io/) for\n"
                + "[distributed tracing](https://microservices.io/patterns/observability/distributed-tracing.html)";
    }

    @Override
    public Priority priority() {
        return Priority.HIGH;
    }

    @Override
    public TestResult test(Component input, TestContext testContext) {
        if (componentTypeIdNotExpectedToUseZipkin(input.getTypeId())) {
            return createNotApplicableTestResult(String.format("Zipkin is not relevant for component type %s", input.getTypeId()));
        }

        if (isZipkinUsed(input)) {
            return createPassTestResult("Component is already using Zipkin for distributed tracing");
        } else {
            return createFailTestResult("We cannot visualise the dependencies and interactions between this component and other components "
                    + "because the component is not using [Zipkin](https://zipkin.io/) for\n"
                    + "[distributed tracing](https://microservices.io/patterns/observability/distributed-tracing.html)");
        }
    }

    private boolean componentTypeIdNotExpectedToUseZipkin(String componentTypeId) {
        return isNull(config.getExpectedComponentTypeIds()) || !config.getExpectedComponentTypeIds().contains(componentTypeId);
    }

    private Boolean isZipkinUsed(Component input) {
        return Optional.ofNullable(input.getZipkin()).map(Zipkin::getUsed).orElse(false);
    }
}

package com.moneysupermarket.componentcatalog.service.tests.dropwizard;

import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.KeySoftware;
import com.moneysupermarket.componentcatalog.sdk.models.Priority;
import com.moneysupermarket.componentcatalog.sdk.models.TestOutcome;
import com.moneysupermarket.componentcatalog.sdk.models.TestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DropwizardVersionTestTest {

    private final DropwizardVersionTest underTest = new DropwizardVersionTest();

    @Test
    public void descriptionShouldReturnTheDescriptionOfTheTest() {
        // When
        String returnValue = underTest.description();

        // Then
        assertThat(returnValue).isEqualTo("For components using the Dropwizard web framework, checks that they are using a recent and supported version of "
                + "Dropwizard");
    }

    @Test
    public void priorityShouldReturnHigh() {
        // When
        Priority returnValue = underTest.priority();

        // Then
        assertThat(returnValue).isEqualTo(Priority.HIGH);
    }

    @Test
    public void testShouldReturnNotApplicableWhenComponentHasNoKeySoftware() {
        // Given
        Component component = Component.builder()
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.NOT_APPLICABLE)
                        .priority(Priority.HIGH)
                        .build());
    }

    @Test
    public void testShouldReturnNotApplicableWhenKeySoftwareDoesNotIncludeDropwizard() {
        // Given
        Component component = Component.builder()
                .keySoftware(List.of(
                        KeySoftware.builder().name("something-else").build()))
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.NOT_APPLICABLE)
                        .priority(Priority.HIGH)
                        .build());
    }

    @Test
    public void testShouldReturnPassWhenDropwizardVersionIsSameAsMinimumVersion() {
        // Given
        Component component = Component.builder()
                .keySoftware(List.of(
                        KeySoftware.builder()
                                .name("dropwizard")
                                .versions(List.of("2.0.0"))
                                .build()))
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.PASS)
                        .priority(Priority.HIGH)
                        .message("Component is using supported version `2.0.0` of the Dropwizard framework which is equal to or greater than the minimum "
                                + "supported version of `2.0.0`.  ")
                        .build());
    }

    @Test
    public void testShouldReturnPassWhenDropwizardVersionIsHigherThanMinimumVersion() {
        // Given
        Component component = Component.builder()
                .keySoftware(List.of(
                        KeySoftware.builder()
                                .name("dropwizard")
                                .versions(List.of("2.0.1"))
                                .build()))
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.PASS)
                        .priority(Priority.HIGH)
                        .message("Component is using supported version `2.0.1` of the Dropwizard framework which is equal to or greater than the minimum "
                                + "supported version of `2.0.0`.  ")
                        .build());
    }

    @Test
    public void testShouldReturnFailWhenDropwizardVersionIsLowerThanMinimumVersion() {
        // Given
        Component component = Component.builder()
                .keySoftware(List.of(
                        KeySoftware.builder()
                                .name("dropwizard")
                                .versions(List.of("1.3.16"))
                                .build()))
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.FAIL)
                        .priority(Priority.HIGH)
                        .message("Component is using very old and unsupported version `1.3.16` of the Dropwizard framework.  Should be using at least version "
                                + "`2.0.0`.  ")
                        .build());
    }

    @Test
    public void testShouldReturnFailForLowestDropwizardVersionThatIsLowerThanMinimumVersion() {
        // Given
        Component component = Component.builder()
                .keySoftware(List.of(
                        KeySoftware.builder()
                                .name("dropwizard")
                                .versions(List.of("1.3.16", "1.0.0"))
                                .build()))
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.FAIL)
                        .priority(Priority.HIGH)
                        .message("Component is using very old and unsupported version `1.0.0` of the Dropwizard framework.  Should be using at least version "
                                + "`2.0.0`.  ")
                        .build());
    }

    @Test
    public void testShouldReturnFailWhenOneDropwizardVersionIsHigherThanMinimumAndOneDropwizardVersionIsLowerThanMinimum() {
        // Given
        Component component = Component.builder()
                .keySoftware(List.of(
                        KeySoftware.builder()
                                .name("dropwizard")
                                .versions(List.of("2.1.0", "1.3.16"))
                                .build()))
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("dropwizard-version")
                        .outcome(TestOutcome.FAIL)
                        .priority(Priority.HIGH)
                        .message("Component is using very old and unsupported version `1.3.16` of the Dropwizard framework.  Should be using at least version "
                                + "`2.0.0`.  ")
                        .build());
    }
}

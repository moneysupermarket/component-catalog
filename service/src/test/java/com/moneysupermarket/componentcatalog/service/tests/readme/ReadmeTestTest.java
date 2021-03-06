package com.moneysupermarket.componentcatalog.service.tests.readme;

import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.Priority;
import com.moneysupermarket.componentcatalog.sdk.models.TestOutcome;
import com.moneysupermarket.componentcatalog.sdk.models.TestResult;
import com.moneysupermarket.componentcatalog.sdk.models.readme.Readme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReadmeTestTest {

    private final ReadmeTest underTest = new ReadmeTest();

    @Test
    public void descriptionShouldReturnTheDescriptionOfTheTest() {
        // When
        String returnValue = underTest.description();

        // Then
        assertThat(returnValue).isEqualTo("Checks whether a component's repo includes a README file at its root");
    }

    @Test
    public void priorityShouldReturnHigh() {
        // When
        Priority returnValue = underTest.priority();

        // Then
        assertThat(returnValue).isEqualTo(Priority.HIGH);
    }

    @Test
    public void testShouldReturnFailWhenAComponentHasNoReadme() {
        // Given
        Component component = Component.builder().build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("readme")
                        .outcome(TestOutcome.FAIL)
                        .priority(Priority.HIGH)
                        .message("Component has no README file")
                        .build());
    }

    @Test
    public void testShouldReturnFailWhenAComponentHasAnEmptyReadme() {
        // Given
        Component component = Component.builder()
                .readme(Readme.builder()
                        .content("")
                        .build())
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("readme")
                        .outcome(TestOutcome.FAIL)
                        .priority(Priority.HIGH)
                        .message("Component has an empty README file")
                        .build());
    }

    @Test
    public void testShouldReturnPassWhenAComponentHasANonEmptyReadme() {
        // Given
        Component component = Component.builder()
                .readme(Readme.builder()
                        .content("# Read Me")
                        .build())
                .build();

        // When
        TestResult returnValue = underTest.test(component, null);

        // Then
        assertThat(returnValue).isEqualTo(
                TestResult.builder()
                        .testId("readme")
                        .outcome(TestOutcome.PASS)
                        .priority(Priority.HIGH)
                        .message("Component has a README file")
                        .build());
    }
}
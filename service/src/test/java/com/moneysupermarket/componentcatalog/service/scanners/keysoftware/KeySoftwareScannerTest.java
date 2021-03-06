package com.moneysupermarket.componentcatalog.service.scanners.keysoftware;

import com.moneysupermarket.componentcatalog.sdk.models.Component;
import com.moneysupermarket.componentcatalog.sdk.models.KeySoftware;
import com.moneysupermarket.componentcatalog.sdk.models.Software;
import com.moneysupermarket.componentcatalog.service.scanners.BaseScannerTest;
import com.moneysupermarket.componentcatalog.service.scanners.keysoftware.config.KeySoftwareConfig;
import com.moneysupermarket.componentcatalog.service.scanners.keysoftware.config.KeySoftwareRule;
import com.moneysupermarket.componentcatalog.service.scanners.models.Output;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class KeySoftwareScannerTest extends BaseScannerTest {

    @Test
    public void idShouldReturnTheIdOfTheScanner() {
        // Given
        KeySoftwareScanner underTest = new KeySoftwareScanner(new KeySoftwareConfig(List.of()));

        // When
        String returnValue = underTest.id();

        // Then
        assertThat(returnValue).isEqualTo("key-software");
    }

    @Test
    public void descriptionShouldReturnTheDescriptionOfTheScanner() {
        // Given
        KeySoftwareScanner underTest = new KeySoftwareScanner(new KeySoftwareConfig(List.of()));

        // When
        String returnValue = underTest.description();

        // Then
        assertThat(returnValue).isEqualTo("Processes all software found by other scanners and looks for certain configured `key software` to find what "
                + "version(s) if any of those key software a component uses.  Key software is typically things like Gradle and Spring Boot");
    }

    @Test
    public void notesShouldReturnNull() {
        // Given
        KeySoftwareScanner underTest = new KeySoftwareScanner(new KeySoftwareConfig(List.of()));

        // When
        String returnValue = underTest.notes();

        // Then
        assertThat(returnValue).isNull();
    }

    @Test
    public void scanShouldHandleNoSoftware() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(new KeySoftwareRule("test", "test")));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Component component = Component.builder().build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).isEmpty();
    }

    @Test
    public void scanShouldHandleNoRules() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of());
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Component component = Component.builder()
                .software(List.of(Software.builder().build()))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).isEmpty();
    }

    @Test
    public void scanShouldHandleRuleThatDoesNotMatch() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(new KeySoftwareRule("test-software-name", "test-key-software-name")));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Component component = Component.builder()
                .software(List.of(Software.builder().name("other-software-name").build()))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).isEmpty();
    }

    @Test
    public void scanShouldMatchKeySoftwareItem() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(new KeySoftwareRule("test-software-name", "test-key-software-name")));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Software softwareItem = Software.builder()
                .name("test-software-name")
                .version("1.2.3")
                .build();
        Component component = Component.builder()
                .software(List.of(softwareItem))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).hasSize(1);
        KeySoftware keySoftwareItem;
        keySoftwareItem = keySoftware.get(0);
        assertThat(keySoftwareItem.getName()).isEqualTo("test-key-software-name");
        assertThat(keySoftwareItem.getVersions()).containsExactly("1.2.3");
    }

    @Test
    public void scanShouldMatchMultipleKeySoftwareItems() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(new KeySoftwareRule("test-software-name", "test-key-software-name")));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Software softwareItem1 = Software.builder()
                .name("test-software-name")
                .version("4.5.6")
                .build();
        Software softwareItem2 = Software.builder()
                .name("test-software-name")
                .version("1.2.3")
                .build();
        Component component = Component.builder()
                .software(List.of(softwareItem1, softwareItem2))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).hasSize(1);
        KeySoftware keySoftwareItem;
        keySoftwareItem = keySoftware.get(0);
        assertThat(keySoftwareItem.getName()).isEqualTo("test-key-software-name");
        assertThat(keySoftwareItem.getVersions()).containsExactly("4.5.6", "1.2.3");
    }

    @Test
    public void scanShouldMatchMultipleKeySoftwareItemsAndSortThemByVersion() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(new KeySoftwareRule("test-software-name", "test-key-software-name")));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Software softwareItem1 = Software.builder()
                .name("test-software-name")
                .version("1.2.3")
                .build();
        Software softwareItem2 = Software.builder()
                .name("test-software-name")
                .version("4.5.6")
                .build();
        Component component = Component.builder()
                .software(List.of(softwareItem1, softwareItem2))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).hasSize(1);
        KeySoftware keySoftwareItem;
        keySoftwareItem = keySoftware.get(0);
        assertThat(keySoftwareItem.getName()).isEqualTo("test-key-software-name");
        assertThat(keySoftwareItem.getVersions()).containsExactly("4.5.6", "1.2.3");
    }

    @Test
    public void scanShouldMatchMultipleKeySoftwareItemsAndDeduplicateThem() {
        // Given
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(new KeySoftwareRule("test-software-name", "test-key-software-name")));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Software softwareItem1 = Software.builder()
                .name("test-software-name")
                .version("1.2.3")
                .build();
        Software softwareItem2 = Software.builder()
                .name("test-software-name")
                .version("1.2.3")
                .build();
        Component component = Component.builder()
                .software(List.of(softwareItem1, softwareItem2))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).hasSize(1);
        KeySoftware keySoftwareItem;
        keySoftwareItem = keySoftware.get(0);
        assertThat(keySoftwareItem.getName()).isEqualTo("test-key-software-name");
        assertThat(keySoftwareItem.getVersions()).containsExactly("1.2.3");
    }

    @Test
    public void scanShouldMatchMultipleRules() {
        // Given
        KeySoftwareRule rule1 = new KeySoftwareRule("test-software-name-1", "test-key-software-name-1");
        KeySoftwareRule rule2 = new KeySoftwareRule("test-software-name-2", "test-key-software-name-2");
        KeySoftwareConfig config = new KeySoftwareConfig(List.of(rule1, rule2));
        KeySoftwareScanner underTest = new KeySoftwareScanner(config);
        Software softwareItem1 = Software.builder()
                .name("test-software-name-1")
                .version("1.2.3")
                .build();
        Software softwareItem2 = Software.builder()
                .name("test-software-name-2")
                .version("4.5.6")
                .build();
        Component component = Component.builder()
                .software(List.of(softwareItem1, softwareItem2))
                .build();

        // When
        Output<Void> returnValue = underTest.scan(component);

        // Then
        assertThat(returnValue.getErrors()).isEmpty();
        List<KeySoftware> keySoftware = getMutatedComponent(returnValue).getKeySoftware();
        assertThat(keySoftware).hasSize(2);
        KeySoftware keySoftwareItem;
        keySoftwareItem = keySoftware.get(0);
        assertThat(keySoftwareItem.getName()).isEqualTo("test-key-software-name-1");
        assertThat(keySoftwareItem.getVersions()).containsExactly("1.2.3");
        keySoftwareItem = keySoftware.get(1);
        assertThat(keySoftwareItem.getName()).isEqualTo("test-key-software-name-2");
        assertThat(keySoftwareItem.getVersions()).containsExactly("4.5.6");
    }
}

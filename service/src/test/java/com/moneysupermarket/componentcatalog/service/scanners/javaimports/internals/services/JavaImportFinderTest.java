package com.moneysupermarket.componentcatalog.service.scanners.javaimports.internals.services;

import com.moneysupermarket.componentcatalog.sdk.models.Import;
import com.moneysupermarket.componentcatalog.sdk.models.ImportType;
import com.moneysupermarket.componentcatalog.service.scanners.javaimports.internal.services.JavaImportFinder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaImportFinderTest {

    private static final String TEST_SCANNER_NAME = "test_scanner";
    private final JavaImportFinder underTest = new JavaImportFinder();

    @Test
    public void shouldFindImport() {
        // Given
        String input = "import test.Example;";

        // When
        List<Import> toDos = underTest.findImports(TEST_SCANNER_NAME, input);

        // Then
        assertThat(toDos).containsExactly(new Import(TEST_SCANNER_NAME, ImportType.JAVA, "test.Example"));
    }

    @Test
    public void shouldFindImportIncludingUnderscore() {
        // Given
        String input = "import test.Example_Example;";

        // When
        List<Import> toDos = underTest.findImports(TEST_SCANNER_NAME, input);

        // Then
        assertThat(toDos).containsExactly(new Import(TEST_SCANNER_NAME, ImportType.JAVA, "test.Example_Example"));
    }

    @Test
    public void shouldFindImportSurroundedByNewlines() {
        // Given
        String input = "\nimport test.Example;\n";

        // When
        List<Import> toDos = underTest.findImports(TEST_SCANNER_NAME, input);

        // Then
        assertThat(toDos).containsExactly(new Import(TEST_SCANNER_NAME, ImportType.JAVA, "test.Example"));
    }

    @Test
    public void shouldFindImportWithWhitespace() {
        // Given
        String input = "  import  test.Example  ;  ";

        // When
        List<Import> toDos = underTest.findImports(TEST_SCANNER_NAME, input);

        // Then
        assertThat(toDos).containsExactly(new Import(TEST_SCANNER_NAME, ImportType.JAVA, "test.Example"));
    }
}

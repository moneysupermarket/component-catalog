package com.moneysupermarket.componentcatalog.service.scanners.readme;

import com.moneysupermarket.componentcatalog.sdk.models.readme.Readme;
import com.moneysupermarket.componentcatalog.service.scanners.CodebaseScanner;
import com.moneysupermarket.componentcatalog.service.scanners.models.Codebase;
import com.moneysupermarket.componentcatalog.service.scanners.models.Output;
import com.moneysupermarket.componentcatalog.service.scanners.readme.services.ReadmeFileNameChecker;
import com.moneysupermarket.componentcatalog.service.spring.stereotypes.Scanner;
import com.moneysupermarket.componentcatalog.service.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Optional;

@Scanner
@RequiredArgsConstructor
public class ReadmeScanner extends CodebaseScanner {

    private static final int SEARCH_ONLY_ROOT_DIRECTORY = 1;
    private static final Comparator<Path> PATH_FILE_NAME_COMPARATOR = Comparator.comparing(path -> path.getFileName().toString());

    private final FileUtils fileUtils;
    private final ReadmeFileNameChecker readmeFileNameChecker;

    @Override
    public String id() {
        return "readme";
    }

    @Override
    public String description() {
        return "Scans a component's codebase for a README file at the root of the codebase";
    }

    @Override
    public Output<Void> scan(Codebase input) {
        Optional<Path> optionalReadmeFile = fileUtils.findFiles(input.getDir(), SEARCH_ONLY_ROOT_DIRECTORY, this::pathIsReadme)
                .sorted(PATH_FILE_NAME_COMPARATOR)
                .findFirst();

        Readme readme = optionalReadmeFile
                .map(readmeFile -> new Readme(readmeFile.getFileName().toString(), fileUtils.readFileContent(readmeFile)))
                .orElse(null);
        return Output.of(component -> component.withReadme(readme));
    }

    private boolean pathIsReadme(Path path, BasicFileAttributes attributes) {
        return attributes.isRegularFile() && readmeFileNameChecker.fileNameIsReadmeFileName(path);
    }
}

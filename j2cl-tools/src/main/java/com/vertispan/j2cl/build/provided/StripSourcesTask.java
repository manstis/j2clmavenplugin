package com.vertispan.j2cl.build.provided;

import com.google.auto.service.AutoService;
import com.google.j2cl.common.SourceUtils;
import com.vertispan.j2cl.build.task.*;
import com.vertispan.j2cl.tools.GwtIncompatiblePreprocessor;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(TaskFactory.class)
public class StripSourcesTask extends TaskFactory {
    public static final PathMatcher JAVA_SOURCES = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
    public static final PathMatcher NATIVE_JS_SOURCES = FileSystems.getDefault().getPathMatcher("glob:**/*.native.js");

    @Override
    public String getOutputType() {
        return OutputTypes.STRIPPED_SOURCES;
    }

    @Override
    public String getTaskName() {
        return "default";
    }

    @Override
    public String getVersion() {
        return "0";
    }

    @Override
    public Task resolve(Project project, Config config) {
        Input inputSources = input(project, OutputTypes.INPUT_SOURCES).filter(JAVA_SOURCES, NATIVE_JS_SOURCES);
        Input generatedSources = input(project, OutputTypes.GENERATED_SOURCES).filter(JAVA_SOURCES, NATIVE_JS_SOURCES);

        return output -> {
            if (inputSources.getFilesAndHashes().isEmpty()) {
                return;// nothing to do
            }
            GwtIncompatiblePreprocessor preprocessor = new GwtIncompatiblePreprocessor(output.path().toFile());
            preprocessor.preprocess(
                    Stream.concat(
                            inputSources.getFilesAndHashes().stream(),
                            generatedSources.getFilesAndHashes().stream()
                    )
                            .map(p -> SourceUtils.FileInfo.create(p.getAbsolutePath().toString(), p.getSourcePath().toString()))
                            .collect(Collectors.toList())
            );
        };
    }
}

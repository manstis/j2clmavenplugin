package com.vertispan.j2cl.build.provided;

import com.google.auto.service.AutoService;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DependencyOptions;
import com.vertispan.j2cl.build.task.*;
import com.vertispan.j2cl.tools.Closure;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(TaskFactory.class)
public class ClosureTask extends TaskFactory {
    public static final PathMatcher JS_SOURCES = FileSystems.getDefault().getPathMatcher("glob:**/*.js");
    public static final PathMatcher NATIVE_JS_SOURCES = FileSystems.getDefault().getPathMatcher("glob:**/*.native.js");
    public static final PathMatcher PLAIN_JS_SOURCES = new PathMatcher() {
        @Override
        public boolean matches(Path path) {
            return JS_SOURCES.matches(path) && !NATIVE_JS_SOURCES.matches(path);
        }

        @Override
        public String toString() {
            return "Only non-native JS sources";
        }
    };
    @Override
    public String getOutputType() {
        return OutputTypes.OPTIMIZED_JS;
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
        // collect current project JS sources and runtime deps JS sources
        // TODO filter to just JS and sourcemaps? probably not required unless we also get sources
        //      from the actual input source instead of copying it along each step
        List<Input> jsSources = Stream.concat(
                Stream.of(
                        input(project, OutputTypes.TRANSPILED_JS).filter(JS_SOURCES),
                        input(project, OutputTypes.GENERATED_SOURCES).filter(PLAIN_JS_SOURCES),
                        input(project, OutputTypes.INPUT_SOURCES).filter(PLAIN_JS_SOURCES)
                ),
                scope(project.getDependencies(), Dependency.Scope.RUNTIME)
                .stream()
                .flatMap(p -> Stream.of(
                        input(p, OutputTypes.TRANSPILED_JS).filter(JS_SOURCES),
                        input(p, OutputTypes.GENERATED_SOURCES).filter(PLAIN_JS_SOURCES),
                        input(p, OutputTypes.INPUT_SOURCES).filter(PLAIN_JS_SOURCES)
                ))
        ).collect(Collectors.toList());


        // grab configs we plan to use
        String compilationLevelConfig = config.getCompilationLevel();
        String initialScriptFilename = config.getInitialScriptFilename();
        Map<String, String> configDefines = config.getDefines();
        DependencyOptions.DependencyMode dependencyMode = DependencyOptions.DependencyMode.valueOf(config.getDependencyMode());
        List<String> entrypoint = config.getEntrypoint();
        CompilerOptions.LanguageMode languageOut = CompilerOptions.LanguageMode.fromString(config.getLanguageOut());
        //TODO probably kill this, or at least make it work like an import via another task so we detect changes
        Collection<String> externs = config.getExterns();
        boolean checkAssertions = config.getCheckAssertions();
        boolean rewritePolyfills = config.getRewritePolyfills();
        boolean sourcemapsEnabled = config.getSourcemapsEnabled();
        List<File> extraJsZips = config.getExtraJsZips();
        String env = config.getEnv();

        String sourcemapDirectory = "sources";
        return new FinalOutputTask() {
            @Override
            public void execute(TaskOutput output) throws Exception {
                Closure closureCompiler = new Closure();

                File closureOutputDir = output.path().toFile();

                CompilationLevel compilationLevel = CompilationLevel.fromString(compilationLevelConfig);

                // set up a source directory to build from, and to make sourcemaps work
                // TODO move logic to the "post" phase to decide whether or not to copy the sourcemap dir
                String jsOutputDir = new File(closureOutputDir + "/" + initialScriptFilename).getParent();
                File sources;
                if (compilationLevel == CompilationLevel.BUNDLE) {
                    if (!sourcemapsEnabled) {
                        //TODO warn that sourcemaps are there anyway, we can't disable in bundle modes?
                    }
                    sources = new File(jsOutputDir, sourcemapDirectory);
                } else {
                    if (sourcemapsEnabled) {
                        sources = new File(jsOutputDir, sourcemapDirectory);//write to the same place as in bundle mode
                    } else {
                        sources = null;
                    }
                }
                if (sources != null) {
                    Files.createDirectories(Paths.get(closureOutputDir.getAbsolutePath(), initialScriptFilename).getParent());

                    //TODO this is quite dirty, we should make this a configurable input to match
                    //     which files are important
                    for (Input jsSource : jsSources) {
                        for (Path path : jsSource.getParentPaths()) {
                            FileUtils.copyDirectory(path.toFile(), sources);
                        }
                    }
                }

                Map<String, String> defines = new LinkedHashMap<>(configDefines);

                if (compilationLevel == CompilationLevel.BUNDLE) {
                    defines.putIfAbsent("goog.ENABLE_DEBUG_LOADER", "false");//TODO maybe overwrite instead?
                }

                boolean success = closureCompiler.compile(
                        compilationLevel,
                        dependencyMode,
                        languageOut,
                        jsSources,
                        sources,
                        extraJsZips,
                        entrypoint,
                        defines,
                        externs,
                        null,
                        true,//TODO have this be passed in,
                        checkAssertions,
                        rewritePolyfills,
                        sourcemapsEnabled,
                        env,
                        closureOutputDir + "/" + initialScriptFilename
                );

                if (!success) {
                    throw new IllegalStateException("Closure Compiler failed, check log for details");
                }

            }

            @Override
            public void finish(TaskOutput taskOutput) throws IOException {
                Path webappDirectory = config.getWebappDirectory();
                if (!Files.exists(webappDirectory)) {
                    Files.createDirectories(webappDirectory);
                }
                FileUtils.copyDirectory(taskOutput.path().toFile(), webappDirectory.toFile());
            }
        };
    }
}

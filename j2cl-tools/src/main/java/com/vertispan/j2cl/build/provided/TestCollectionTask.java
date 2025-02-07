package com.vertispan.j2cl.build.provided;

import com.google.auto.service.AutoService;
import com.vertispan.j2cl.build.task.*;
import org.apache.commons.io.FileUtils;

import java.nio.file.*;

/**
 * Runs annotation processors and collects the test summary output.
 */
@AutoService(TaskFactory.class)
public class TestCollectionTask extends TaskFactory {
    private static final String TEST_SUMMARY_FILENAME = "test_summary.json";
    private static final PathMatcher TEST_SUMMARY_JSON = FileSystems.getDefault().getPathMatcher("glob:" + TEST_SUMMARY_FILENAME);
    private static final PathMatcher TEST_SUITE = FileSystems.getDefault().getPathMatcher("glob:**/*.testsuite");

    @Override
    public String getOutputType() {
        return "test_summary";
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
        // gather possible inputs so we can get the test summary file
        // we assume here that the user will correctly depend on the junit apt, might revise this later
        Input src = input(project, OutputTypes.INPUT_SOURCES).filter(TEST_SUMMARY_JSON, TEST_SUITE);
        Input apt = input(project, OutputTypes.GENERATED_SOURCES).filter(TEST_SUMMARY_JSON, TEST_SUITE);
        return new FinalOutputTask() {
            @Override
            public void execute(TaskOutput output) throws Exception {
                // TODO If both container a test summary, we should fail, rather than overwrite
                // Or even better, merge?

                for (CachedPath entry : src.getFilesAndHashes()) {
                    Files.createDirectories(output.path().resolve(entry.getSourcePath()).getParent());
                    Files.copy(entry.getAbsolutePath(), output.path().resolve(entry.getSourcePath()));
                }
                for (CachedPath entry : apt.getFilesAndHashes()) {
                    Files.createDirectories(output.path().resolve(entry.getSourcePath()).getParent());
                    Files.copy(entry.getAbsolutePath(), output.path().resolve(entry.getSourcePath()));
                }
            }

            @Override
            public void finish(TaskOutput taskOutput) throws Exception {
                Files.createDirectories(config.getWebappDirectory());
                FileUtils.copyDirectory(taskOutput.path().toFile(), config.getWebappDirectory().toFile());
            }
        };
    }
}

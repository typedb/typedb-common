package com.vaticle.typedb.common.test;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;

class RunnerUtil {

    private static final String TAR_GZ = ".tar.gz";
    private static final String ZIP = ".zip";

    static Path distributionSetup(String name, File archive) throws IOException, TimeoutException, InterruptedException {
        System.out.println("Unarchiving " + name + " distribution.");
        Path runnerDir = createRunnerDir();
        ProcessExecutor executor = new ProcessExecutor()
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        if (getFormat(archive).equals(TAR_GZ)) {
            executor.command("tar", "-xf", archive.toString(),
                    "-C", runnerDir.toString()).execute();
        } else {
            executor.command("unzip", "-q", archive.toString(),
                    "-d", runnerDir.toString()).execute();
        }
        // The TypeDB Cluster archive extracts to a folder inside TYPEDB_TARGET_DIRECTORY named
        // typedb-server-{platform}-{version}. We know it's the only folder, so we can retrieve it using Files.list.
        final Path typeDBPath = Files.list(runnerDir).findFirst().get().toAbsolutePath();
        System.out.println(name + " distribution unarchived.");
        return typeDBPath;
    }

    private static Path createRunnerDir() throws IOException {
        Path runnerDir = Files.createTempDirectory("typedb"); // TODO: rename directory from "typedb" to "runner"
        System.out.println("Checking for existing distribution at " + runnerDir.toAbsolutePath());
        if (runnerDir.toFile().exists()) {
            System.out.println("An existing distribution found. Deleting...");
            FileUtils.deleteDirectory(runnerDir.toFile());
            System.out.println("Existing distribution deleted");
        } else {
            System.out.println("There is no existing distribution");
        }
        return runnerDir;
    }

    private static String getFormat(File archive) {
        if (archive.toString().endsWith(TAR_GZ)) {
            return TAR_GZ;
        } else if (archive.toString().endsWith(ZIP)) {
            return ZIP;
        } else {
            throw new IllegalStateException(String.format("Distribution file format should either be %s or %s", TAR_GZ, ZIP));
        }
    }

    static List<String> distributionBin() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return Collections.singletonList("typedb");
        } else {
            return Arrays.asList("cmd.exe", "/c", "typedb.bat");
        }
    }
}

/*
 * Copyright (C) 2021 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package grakn.common.test.server;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class GraknRunnerBase implements GraknRunner {
    private static final String[] ARGS = System.getProperty("sun.java.command").split(" ");
    private static final File DISTRIBUTION_ARCHIVE = ARGS.length > 1 ? new File(ARGS[1]) : null;
    private static final String TAR = ".tar.gz";
    private static final String ZIP = ".zip";

    private final File distributionArchive;
    private final Path distributionDir;
    private final String distributionArchiveFormat;
    protected final int port;
    protected final Path dataDir;
    protected final boolean debug;

    private ProcessExecutor executor;
    private StartedProcess graknProcess;

    public GraknRunnerBase() throws InterruptedException, TimeoutException, IOException {
        this(DISTRIBUTION_ARCHIVE, false);
    }

    public GraknRunnerBase(boolean debug) throws InterruptedException, TimeoutException, IOException {
        this(DISTRIBUTION_ARCHIVE, debug);
    }

    public GraknRunnerBase(File distributionArchive, boolean debug) throws InterruptedException, TimeoutException, IOException {
        this.port = ThreadLocalRandom.current().nextInt(40000, 60000);
        System.out.println("Constructing a " + name() + " runner");

        if (!distributionArchive.exists()) {
            throw new IllegalArgumentException("Grakn distribution file is missing from " + distributionArchive.getAbsolutePath());
        }

        checkAndDeleteExistingDistribution(distributionArchive);

        this.distributionArchive = distributionArchive;
        distributionArchiveFormat = distributionFormat(distributionArchive);
        distributionDir = distributionTarget(distributionArchive);
        this.debug = debug;
        executor = new ProcessExecutor()
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        Path graknPath = distributionSetup(distributionDir);
        dataDir = graknPath.resolve("server").resolve("data");
        System.out.println(name() + " runner constructed");
    }

    private static String distributionFormat(File distributionFile) {
        if (distributionFile.toString().endsWith(TAR)) {
            return TAR;
        } else if (distributionFile.toString().endsWith(ZIP)) {
            return ZIP;
        } else {
            fail(String.format("Distribution file format should either be %s or %s", TAR, ZIP));
        }
        return "";
    }

    private static Path distributionTarget(File distributionFile) {
        String format = distributionFormat(distributionFile);
        return Paths.get(distributionFile.toString().replaceAll(
                format.replace(".", "\\."), ""
        ));
    }

    private static void checkAndDeleteExistingDistribution(File distributionFile) throws IOException {
        Path target = distributionTarget(distributionFile);
        System.out.println("Checking for existing distribution at " + target.toAbsolutePath().toString());
        if (target.toFile().exists()) {
            System.out.println("An existing distribution found. Deleting...");
            FileUtils.deleteDirectory(target.toFile());
            System.out.println("Existing distribution deleted");
        } else {
            System.out.println("There is no existing distribution");
        }
    }

    @Override
    public String host() {
        return "127.0.0.1";
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String address() {
        return host() + ":" + port();
    }

    @Override
    public void start() {
        try {

            System.out.println("Starting " + name() + " database server at " + distributionDir.toAbsolutePath().toString());
            System.out.println("Database directory will be at " + dataDir.toAbsolutePath());
            graknProcess = executor.command(command()).start();

            Thread.sleep(15000);
            assertTrue(name() + " failed to start", graknProcess.getProcess().isAlive());

            System.out.println(name() + " database server started");
        } catch (Exception e) {
            printLogs();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (graknProcess != null) {
            try {
                System.out.println("Stopping " + name() + " database server");

                graknProcess.getProcess().destroy();

                System.out.println(name() + " database server stopped");
            } catch (Exception e) {
                printLogs();
                throw e;
            }
        }
    }

    abstract String name();

    abstract List<String> command();

    private Path distributionSetup(Path distributionDir) throws IOException, TimeoutException, InterruptedException {
        System.out.println("Unarchiving " + name() + " distribution");
        Files.createDirectories(distributionDir);
        if (distributionArchiveFormat.equals(TAR)) {
            executor.command("tar", "-xf", distributionArchive.toString(),
                    "-C", distributionDir.toString()).execute();
        } else {
            executor.command("unzip", "-q", distributionArchive.toString(),
                    "-d", distributionDir.toString()).execute();
        }
        // The Grakn Cluster archive extracts to a folder inside GRAKN_TARGET_DIRECTORY named
        // grakn-core-server-{platform}-{version}. We know it's the only folder, so we can retrieve it using Files.list.
        final Path graknPath = Files.list(distributionDir).findFirst().get();
        System.out.println(graknPath);
        executor = executor.directory(graknPath.toFile());
        System.out.println(name() + " distribution unarchived");
        return graknPath;
    }

    private void printLogs() {
        System.out.println("================");
        System.out.println(name() + " Logs:");
        System.out.println("================");
        Path logPath = Paths.get(".", "logs", "grakn.log");
        try {
            executor.command("cat", logPath.toString()).execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            System.out.println("Unable to print '" + logPath + "'");
            e.printStackTrace();
        }
    }
}

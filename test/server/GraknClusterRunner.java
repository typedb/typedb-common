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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GraknClusterRunner implements GraknRunner {

    private static final String[] ARGS = System.getProperty("sun.java.command").split(" ");
    private static final File DISTRIBUTION_FILE = ARGS.length > 1 ? new File(ARGS[1]) : null;

    private static final String TAR = ".tar.gz";
    private static final String ZIP = ".zip";

    private final File GRAKN_DISTRIBUTION_FILE;
    private final Path GRAKN_TARGET_DIRECTORY;
    private final String GRAKN_DISTRIBUTION_FORMAT;
    private final int port = ThreadLocalRandom.current().nextInt(40000, 60000);
    private final Path tmpDir;
    private final boolean debug;

    private ProcessExecutor executor;
    private StartedProcess graknProcess;

    public GraknClusterRunner() throws InterruptedException, TimeoutException, IOException {
        this(DISTRIBUTION_FILE, false);
    }

    public GraknClusterRunner(boolean debug) throws InterruptedException, TimeoutException, IOException {
        this(DISTRIBUTION_FILE, debug);
    }

    public GraknClusterRunner(File distributionFile, boolean debug) throws InterruptedException, TimeoutException, IOException {
        System.out.println("Constructing a Grakn Cluster runner");

        if (!distributionFile.exists()) {
            throw new IllegalArgumentException("Grakn distribution file is missing from " + distributionFile.getAbsolutePath());
        }

        checkAndDeleteExistingDistribution(distributionFile);

        GRAKN_DISTRIBUTION_FILE = distributionFile;
        GRAKN_DISTRIBUTION_FORMAT = distributionFormat(distributionFile);
        GRAKN_TARGET_DIRECTORY = distributionTarget(distributionFile);

        tmpDir = Files.createTempDirectory("grakn-cluster-runner");

        this.executor = new ProcessExecutor()
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();

        this.unzip();

        this.debug = debug;
        System.out.println("Grakn Cluster runner constructed");
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
        System.out.println("Checking for existing Grakn Cluster distribution at " + target.toAbsolutePath().toString());
        if (target.toFile().exists()) {
            System.out.println("There exists a Grakn Cluster distribution and will be deleted");
            FileUtils.deleteDirectory(target.toFile());
            System.out.println("Existing Grakn Cluster distribution deleted");
        } else {
            System.out.println("There is no existing Grakn Cluster distribution");
        }
    }

    private void unzip() throws IOException, TimeoutException, InterruptedException {
        System.out.println("Unarchiving Grakn Cluster distribution");
        Files.createDirectory(GRAKN_TARGET_DIRECTORY);
        if (GRAKN_DISTRIBUTION_FORMAT.equals(TAR)) {
            executor.command("tar", "-xf", GRAKN_DISTRIBUTION_FILE.toString(),
                    "-C", GRAKN_TARGET_DIRECTORY.toString()).execute();
        } else {
            executor.command("unzip", "-q", GRAKN_DISTRIBUTION_FILE.toString(),
                    "-d", GRAKN_TARGET_DIRECTORY.toString()).execute();
        }
        // The Grakn Cluster archive extracts to a folder inside GRAKN_TARGET_DIRECTORY named
        // grakn-core-server-{platform}-{version}. We know it's the only folder, so we can retrieve it using Files.list.
        final Path graknPath = Files.list(GRAKN_TARGET_DIRECTORY).findFirst().get();
        System.out.println(graknPath);
        executor = executor.directory(graknPath.toFile());

        System.out.println("Grakn Cluster distribution unarchived");
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
            System.out.println("Starting Grakn Cluster database server at " + GRAKN_TARGET_DIRECTORY.toAbsolutePath().toString());
            System.out.println("Database directory will be at " + tmpDir.toAbsolutePath());

            List<String> arguments = new ArrayList<>();
            arguments.add("./grakn");
            arguments.add("server");
            if (debug) {
//                arguments.add("--debug"); // TODO: support --debug
            }
            // TODO: support --host and --port in place of --address in order to be compatible with Grakn Core?
//            arguments.add("--port");
//            arguments.add(Integer.toString(port));
            arguments.add("--address");
            arguments.add(address() + ":" + (port+1));
            arguments.add("--data");
            arguments.add(tmpDir.toAbsolutePath().toString());
            graknProcess = executor.command(arguments).start();

            Thread.sleep(10000);
            assertTrue("Grakn Cluster failed to start", graknProcess.getProcess().isAlive());

            System.out.println("Grakn Cluster database server started");
        } catch (Exception e) {
            printLogs();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (graknProcess != null) {
            try {
                System.out.println("Stopping Grakn Cluster database server");

                graknProcess.getProcess().destroy();

                System.out.println("Grakn Cluster database server stopped");
            } catch (Exception e) {
                printLogs();
                throw e;
            }
        }
    }

    private void printLogs() {
        System.out.println("================");
        System.out.println("Grakn Cluster Logs:");
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

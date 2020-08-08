/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GraknSetup {

    private static final String[] ARGS = System.getProperty("sun.java.command").split(" ");
    private static final File DISTRIBUTION_FILE = ARGS.length > 1 ? new File(ARGS[1]) : null;

    private static final String TAR = ".tar.gz";
    private static final String ZIP = ".zip";

    private static GraknSetup graknRunner;

    public static GraknSetup bootup() throws InterruptedException, IOException, TimeoutException {
        if (DISTRIBUTION_FILE == null) {
            throw new IllegalArgumentException("No grakn distribution path specified on the command line\n" +
                                                       "Check your test rule, it is recommended to use the `grakn_test` rule from @graknlabs_common");
        }

        return bootup(DISTRIBUTION_FILE);
    }

    public static GraknSetup bootup(File distributionFile) throws InterruptedException, TimeoutException, IOException {
        if (!distributionFile.exists()) {
            throw new IllegalArgumentException("Grakn distribution file is missing from " + distributionFile.getAbsolutePath());
        }

        checkAndDeleteExistingDistribution(distributionFile);
        graknRunner = new GraknSetup(distributionFile);
        graknRunner.start();
        return graknRunner;
    }

    public static GraknSetup instance() {
        return graknRunner;
    }

    public static void shutdown() throws InterruptedException, TimeoutException, IOException {
        graknRunner.stop();
        graknRunner = null;
    }

    private final File GRAKN_DISTRIBUTION_FILE;
    private final Path GRAKN_TARGET_DIRECTORY;
    private final String GRAKN_DISTRIBUTION_FORMAT;
    private final int port = ThreadLocalRandom.current().nextInt(40000, 60000);
    private final Path tmpDir;

    private ProcessExecutor executor;
    private StartedProcess graknProcess;

    public GraknSetup(File distributionFile) throws InterruptedException, TimeoutException, IOException {
        System.out.println("Constructing a Grakn Core runner");

        GRAKN_DISTRIBUTION_FILE = distributionFile;
        GRAKN_DISTRIBUTION_FORMAT = distributionFormat(distributionFile);
        GRAKN_TARGET_DIRECTORY = distributionTarget(distributionFile);

        tmpDir = Files.createTempDirectory("grakn_core_test");

        this.executor = new ProcessExecutor()
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();

        this.unzip();

        System.out.println("Grakn Core runner constructed");
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
        System.out.println("Checking for existing Grakn distribution at " + target.toAbsolutePath().toString());
        if (target.toFile().exists()) {
            System.out.println("There exists a Grakn Core distribution and will be deleted");
            FileUtils.deleteDirectory(target.toFile());
            System.out.println("Existing Grakn Core distribution deleted");
        } else {
            System.out.println("There is no existing Grakn Core distribution");
        }
    }

    private void unzip() throws IOException, TimeoutException, InterruptedException {
        System.out.println("Unarchiving Grakn Core distribution");
        if (GRAKN_DISTRIBUTION_FORMAT.equals(TAR)) {
            executor.command("tar", "-xf", GRAKN_DISTRIBUTION_FILE.toString(),
                             "-C", GRAKN_TARGET_DIRECTORY.getParent().toString()).execute();
        } else {
            executor.command("unzip", "-q", GRAKN_DISTRIBUTION_FILE.toString(),
                             "-d", GRAKN_TARGET_DIRECTORY.getParent().toString()).execute();
        }
        executor = executor.directory(GRAKN_TARGET_DIRECTORY.toFile());

        System.out.println("Grakn Core distribution unarchived");
    }

    public String host() {
        return "127.0.0.1";
    }

    public int port() {
        return port;
    }

    public String address() {
        return host() + ":" + port();
    }

    private void start() throws InterruptedException, IOException, TimeoutException {
        try {
            System.out.println("Starting Grakn Core database server at " + GRAKN_TARGET_DIRECTORY.toAbsolutePath().toString());
            System.out.println("Database directory will be at " + tmpDir.toAbsolutePath());

            graknProcess = executor.command("./grakn", "server",
                                            "--database-port", Integer.toString(port),
                                            "--database-directory", tmpDir.toAbsolutePath().toString()).start();

            Thread.sleep(5000);
            assertTrue("Grakn Core failed to start", graknProcess.getProcess().isAlive());

            System.out.println("Grakn Core database server started");
        } catch (Throwable e) {
            printLogs();
            throw e;
        }
    }

    private void stop() throws InterruptedException, IOException, TimeoutException {
        if (graknProcess != null) {
            try {
                System.out.println("Stopping Grakn Core database server");

                graknProcess.getProcess().destroy();

                System.out.println("Grakn Core database server stopped");
            } catch (Exception e) {
                printLogs();
                throw e;
            }
        }
    }

    private void printLogs() throws InterruptedException, TimeoutException, IOException {
        System.out.println("================");
        System.out.println("Grakn Core Logs:");
        System.out.println("================");
        executor.command("cat", Paths.get(".", "logs", "grakn.log").toString()).execute();
    }
}

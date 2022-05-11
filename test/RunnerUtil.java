/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.common.test;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

class RunnerUtil {

    private static final String TAR_GZ = ".tar.gz";
    private static final String ZIP = ".zip";
    private static final int PORT_ALLOCATION_MAX_RETRIES = 15;
    static final int SERVER_STARTUP_TIMEOUT_MILLIS = 30000;
    private static final int SERVER_ALIVE_POLL_INTERVAL_MILLIS = 500;
    private static final int SERVER_ALIVE_POLL_MAX_RETRIES = SERVER_STARTUP_TIMEOUT_MILLIS / SERVER_ALIVE_POLL_INTERVAL_MILLIS;

    static Path distributionSetup() throws IOException, InterruptedException, TimeoutException {
        return distributionSetup(getArchive());
    }

    static Path distributionSetup(File archive) throws IOException, TimeoutException, InterruptedException {
        Path runnerDir = createRunnerDir();
        extract(archive, runnerDir);
        // The TypeDB Cluster archive extracts to a folder inside TYPEDB_TARGET_DIRECTORY named
        // typedb-server-{platform}-{version}. We know it's the only folder, so we can retrieve it using Files.list.
        List<Path> parentDirContent = Files.list(runnerDir.getParent()).collect(Collectors.toList());
        System.out.println("parentDirContent: " + parentDirContent);
        Stream<Path> runnerDirContent = Files.list(runnerDir);
        System.out.println("after list");
        return runnerDirContent.findFirst().get().toAbsolutePath();
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

    private static void extract(File archive, Path outputDir) throws IOException, InterruptedException, TimeoutException {
        ProcessExecutor executor = new ProcessExecutor()
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        if (archive.toString().endsWith(TAR_GZ)) {
            executor.command("tar", "-xf", archive.toString(),
                    "-C", outputDir.toString()).execute();
        } else if (archive.toString().endsWith(ZIP)) {
            executor.command("unzip", "-q", archive.toString(),
                    "-d", outputDir.toString()).execute();
        } else {
            throw new IllegalStateException(String.format("The distribution archive format must be either %s or %s", TAR_GZ, ZIP));
        }
    }

    static List<String> bin() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return Collections.singletonList("typedb");
        } else {
            return Arrays.asList("cmd.exe", "/c", "typedb.bat");
        }
    }

    static CountDownLatch checkServerStarted(String host, int port) {
        CountDownLatch latch = new CountDownLatch(1);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int retryNumber = 0;

            @Override
            public void run() {
                retryNumber++;
                if (retryNumber % 4 == 0) {
                    System.out.println(String.format("%s: waiting for server to start (%ds)...",
                            host + ":" + port, retryNumber * SERVER_ALIVE_POLL_INTERVAL_MILLIS / 1000));
                }
                if (canConnectToServer()) {
                    latch.countDown();
                    timer.cancel();
                }
                if (retryNumber > SERVER_ALIVE_POLL_MAX_RETRIES) timer.cancel();
            }

            private boolean canConnectToServer() {
                try {
                    Socket s = new Socket(host, port);
                    s.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        }, 0, 500);
        return latch;
    }

    static List<Integer> findUnusedPorts(int count) {
        assert count > 0;
        try {
            for (int retries = 0; retries < PORT_ALLOCATION_MAX_RETRIES; retries++) {
                List<Integer> ports = new ArrayList<>(count);
                // using port 0 automatically allocates a valid free port
                ServerSocket seed = new ServerSocket(0);
                ports.add(seed.getLocalPort());
                seed.close();
                for (int i = 1; i < count; i++) {
                    if (isPortUnused(ports.get(0) + i)) {
                        ports.add(ports.get(0) + i);
                    } else {
                        break;
                    }
                }
                if (ports.size() == count) return ports;
            }
            throw new RuntimeException("Failed to allocate ports within  " + PORT_ALLOCATION_MAX_RETRIES + " retries");
        } catch (IOException e) {
            throw new RuntimeException("Error while searching for unused port.");
        }
    }

    static boolean isPortUnused(int port) {
        try {
            ServerSocket socket = new ServerSocket(port);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File getArchive() {
        String[] args = System.getProperty("sun.java.command").split(" ");
        assert args.length > 1;
        File file = new File(args[1]);
        if (!file.exists()) {
            throw new IllegalArgumentException("Distribution archive missing: " + file.getAbsolutePath());
        }
        return file;
    }
}

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

import grakn.common.test.Runner;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

public abstract class GraknRunner extends Runner {

    private static final int SERVER_STARTUP_TIMEOUT_MILLIS = 30000;
    private static final int SERVER_ALIVE_POLL_INTERVAL_MILLIS = 500;
    private static final int SERVER_ALIVE_POLL_MAX_RETRIES = SERVER_STARTUP_TIMEOUT_MILLIS / SERVER_ALIVE_POLL_INTERVAL_MILLIS;

    protected final Path dataDir;
    private StartedProcess serverProcess;

    public GraknRunner() throws InterruptedException, TimeoutException, IOException {
        super();
        this.dataDir = rootPath.resolve("server").resolve("data");
    }

    protected abstract List<String> command();

    protected abstract int listeningPort();

    public void start() {
        try {
            System.out.println("Starting " + name() + " database server at " + rootPath.toAbsolutePath().toString());
            System.out.println("Database directory will be at " + dataDir.toAbsolutePath());
            serverProcess = executor.command(command()).start();
            boolean started = checkServerStarted().await(SERVER_STARTUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            assertTrue(name() + " failed to start", started);
            System.out.println(name() + " database server started");
        } catch (Exception e) {
            printLogs();
            throw new RuntimeException(e);
        }
    }

    private CountDownLatch checkServerStarted() {
        CountDownLatch latch = new CountDownLatch(1);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int retryNumber = 0;
            @Override
            public void run() {
                retryNumber++;
                if (retryNumber % 4 == 0) {
                    System.out.println(String.format("Waiting for %s server to start (%ds)...",
                            name(), retryNumber * SERVER_ALIVE_POLL_INTERVAL_MILLIS / 1000));
                }
                String lsof;
                try {
                    lsof = executor.command("lsof", "-i", ":" + listeningPort()).readOutput(true).execute().outputString();
                } catch (IOException | InterruptedException | TimeoutException e) {
                    lsof = "";
                }
                if (lsof != null && !lsof.isEmpty()) {
                    latch.countDown();
                    timer.cancel();
                }
                if (retryNumber > SERVER_ALIVE_POLL_MAX_RETRIES) timer.cancel();
            }
        }, 0, 500);
        return latch;
    }

    public void stop() {
        if (serverProcess != null) {
            try {
                System.out.println("Stopping " + name() + " database server");
                serverProcess.getProcess().destroy();
                System.out.println(name() + " database server stopped");
            } catch (Exception e) {
                printLogs();
                throw e;
            }
        }
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

    @Override
    protected File distributionArchive() {
        String[] args = System.getProperty("sun.java.command").split(" ");
        assert args.length > 1;
        return new File(args[1]);
    }
}

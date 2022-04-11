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

package com.vaticle.typedb.common.test.server;

import com.vaticle.typedb.common.test.Runner;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class TypeDBRunner extends Runner {

    private static final int SERVER_STARTUP_TIMEOUT_MILLIS = 30000;
    private static final int SERVER_ALIVE_POLL_INTERVAL_MILLIS = 500;
    private static final int SERVER_ALIVE_POLL_MAX_RETRIES = SERVER_STARTUP_TIMEOUT_MILLIS / SERVER_ALIVE_POLL_INTERVAL_MILLIS;

    protected final Path dataDir;
    protected final Path logsDir;
    private StartedProcess serverProcess;

    public TypeDBRunner() throws InterruptedException, TimeoutException, IOException {
        super();
        this.dataDir = rootPath.resolve("server").resolve("data");
        this.logsDir = rootPath.resolve("server").resolve("logs");
    }

    @Override
    protected File distributionArchive() {
        String[] args = System.getProperty("sun.java.command").split(" ");
        assert args.length > 1;
        return new File(args[1]);
    }

    protected abstract List<String> command();

    public String address() {
        return host() + ":" + port();
    }

    protected static String host() {
        return "127.0.0.1";
    }

    protected abstract int port();

    protected boolean isPortOpen(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
            s.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static List<Integer> findUnusedPorts(int count) {
        assert count > 0;
        try {
            while (true) {
                List<Integer> ports = new ArrayList<>(count);
                // using port 0 automatically allocates a valid free port
                ServerSocket seed = new ServerSocket(0);
                ports.add(seed.getLocalPort());
                seed.close();
                for (int i = 1; i < count; i++) {
                    try {
                        ServerSocket socket = new ServerSocket(ports.get(0) + i);
                        ports.add(socket.getLocalPort());
                        socket.close();
                    } catch (IOException e) {
                        break;
                    }
                }
                if (ports.size() == count) return ports;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while searching for unused port.");
        }
    }

    public void start() {
        try {
            System.out.println(address() + ": starting... ");
            System.out.println(address() + ": distribution is located at " + rootPath.toAbsolutePath());
            System.out.println(address() + ": data directory is located at " + dataDir.toAbsolutePath());
            System.out.println(address() + ": command = " + command());
            serverProcess = executor.command(command()).start();
            boolean started = checkServerStarted().await(SERVER_STARTUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!started) {
                String message = address() + ": unable to start. ";
                if (serverProcess.getFuture().isDone()) {
                    ProcessResult processResult = serverProcess.getFuture().get();
                    message += address() + ": process exited with code '" + processResult.getExitValue() + "'. ";
                    if (processResult.hasOutput()) {
                        message += "Output: " + processResult.outputUTF8();
                    }
                }
                throw new RuntimeException(message);
            } else {
                System.out.println(address() + ": started");
            }
        } catch (Throwable e) {
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
                    System.out.println(String.format("%s: waiting for server to start (%ds)...",
                                                     address(), retryNumber * SERVER_ALIVE_POLL_INTERVAL_MILLIS / 1000));
                }
                if (isPortOpen(host(), port())) {
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
                System.out.println(address() + ": stopping...");
                serverProcess.getProcess().destroyForcibly();
                System.out.println(address() + ": stopped.");
            } catch (Exception e) {
                printLogs();
                throw e;
            }
        }
    }

    private void printLogs() {
        System.out.println("================");
        System.out.println(address() + ": logs:");
        System.out.println("================");
        Path logPath = logsDir.resolve("typedb.log").toAbsolutePath();
        try {
            executor.command("cat", logPath.toString()).execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            System.out.println(address() + ": unable to print '" + logPath + "'");
            e.printStackTrace();
        }
    }
}

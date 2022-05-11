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

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TypeDBCoreRunner {

    private final Path distribution;
    private final Path dataDir;
    private final Path logsDir;
    private final int port;
    private StartedProcess process;
    private final ProcessExecutor executor;

    public TypeDBCoreRunner() throws InterruptedException, TimeoutException, IOException {
        port = RunnerUtil.findUnusedPorts(1).get(0);
        System.out.println(address() + ": Constructing " + name() + " runner");
        System.out.println(address() + ": Extracting distribution archive...");
        distribution = RunnerUtil.unarchive();
        System.out.println(address() + ": distribution archive extracted.");
        dataDir = distribution.resolve("server").resolve("data");
        logsDir = distribution.resolve("server").resolve("logs");
        executor = new ProcessExecutor()
                .directory(distribution.toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        System.out.println(address() + ": runner constructed");
    }

    private String name() {
        return "TypeDB Core";
    }

    public String address() {
        return host() + ":" + port();
    }

    private static String host() {
        return "127.0.0.1";
    }

    private int port() {
        return port;
    }

    public void start() {
        try {
            System.out.println(address() + ": " +  name() + "is starting... ");
            System.out.println(address() + ": Distribution is located at " + distribution.toAbsolutePath());
            System.out.println(address() + ": Data directory is located at " + dataDir.toAbsolutePath());
            System.out.println(address() + ": Bootup command = " + command());
            process = executor.command(command()).start();
            boolean started = RunnerUtil.waitUntilPortUsed(host(), port())
                    .await(RunnerUtil.SERVER_STARTUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!started) {
                String message = address() + ": Unable to start. ";
                if (process.getFuture().isDone()) {
                    ProcessResult processResult = process.getFuture().get();
                    message += address() + ": Process exited with code '" + processResult.getExitValue() + "'. ";
                    if (processResult.hasOutput()) {
                        message += "Output: " + processResult.outputUTF8();
                    }
                }
                throw new RuntimeException(message);
            } else {
                System.out.println(address() + ": Started");
            }
        } catch (Throwable e) {
            printLogs();
            throw new RuntimeException(e);
        }
    }

    private List<String> command() {
        List<String> command = new ArrayList<>();
        command.addAll(RunnerUtil.bin());
        command.add("server");
        command.add("--server.address");
        command.add(address());
        command.add("--storage.data");
        command.add(dataDir.toAbsolutePath().toString());
        return command;
    }

    public void stop() {
        if (process != null) {
            try {
                System.out.println(address() + ": Stopping...");
                process.getProcess().destroyForcibly();
                System.out.println(address() + ": Stopped.");
            } catch (Exception e) {
                printLogs();
                throw e;
            }
        }
    }

    private void printLogs() {
        System.out.println(address() + ": ================");
        System.out.println(address() + ": logs:");
        Path logPath = logsDir.resolve("typedb.log").toAbsolutePath();
        try {
            executor.command("cat", logPath.toString()).execute();
        } catch (IOException | InterruptedException | TimeoutException e) {
            System.out.println(address() + ": Unable to print '" + logPath + "'");
            e.printStackTrace();
        }
        System.out.println(address() + ": ================");
    }
}

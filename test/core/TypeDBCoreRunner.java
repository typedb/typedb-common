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

package com.vaticle.typedb.common.test.core;

import com.vaticle.typedb.common.test.RunnerUtil;
import com.vaticle.typedb.common.test.TypeDBRunner;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.vaticle.typedb.common.test.RunnerUtil.createProcessExecutor;
import static com.vaticle.typedb.common.test.RunnerUtil.findUnusedPorts;
import static com.vaticle.typedb.common.test.RunnerUtil.typeDBCommand;
import static com.vaticle.typedb.common.test.RunnerUtil.unarchive;

public class TypeDBCoreRunner implements TypeDBRunner {

    private final Path distribution;
    private final Path dataDir;
    private final Path logsDir;
    private final int port;
    private StartedProcess process;
    private final ProcessExecutor executor;

    public TypeDBCoreRunner() throws InterruptedException, TimeoutException, IOException {
        port = findUnusedPorts(1).get(0);
        System.out.println(address() + ": Constructing " + name() + " runner");
        System.out.println(address() + ": Extracting distribution archive...");
        distribution = unarchive();
        System.out.println(address() + ": Distribution archive extracted.");
        dataDir = distribution.resolve("server").resolve("data");
        logsDir = distribution.resolve("server").resolve("logs");
        executor = createProcessExecutor(distribution);
        System.out.println(address() + ": Runner constructed");
    }

    private String name() {
        return "TypeDB Core";
    }

    @Override
    public String address() {
        return host() + ":" + port();
    }

    public String host() {
        return "127.0.0.1";
    }

    public int port() {
        return port;
    }

    @Override
    public void start() {
        System.out.println(address() + ": " +  name() + "is starting... ");
        System.out.println(address() + ": Distribution is located at " + distribution.toAbsolutePath());
        System.out.println(address() + ": Data directory is located at " + dataDir.toAbsolutePath());
        System.out.println(address() + ": Server bootup command = " + command());
        try {
            process = RunnerUtil.startProcess(executor, command(), new InetSocketAddress(host(), port()));
        } catch (Throwable e) {
            printLogs();
            throw new RuntimeException(e);
        }
    }

    private List<String> command() {
        return typeDBCommand(
                "server",
                "--server.address",
                address(),
                "--storage.data",
                dataDir.toAbsolutePath().toString()
        );
    }

    @Override
    public boolean isStopped() {
        return !process.getProcess().isAlive();
    }

    @Override
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
        System.out.println(address() + ": Logs:");
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

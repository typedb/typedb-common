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

package com.vaticle.typedb.common.test.cluster;

import com.vaticle.typedb.common.conf.cluster.Addresses;
import com.vaticle.typedb.common.test.RunnerUtil;
import com.vaticle.typedb.common.test.TypeDBRunner;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.vaticle.typedb.common.test.RunnerUtil.SERVER_STARTUP_TIMEOUT_MILLIS;
import static com.vaticle.typedb.common.test.RunnerUtil.createProcessExecutor;

public interface TypeDBClusterServerRunner extends TypeDBRunner {

    Addresses address();

    class Factory {

        protected TypeDBClusterServerRunner createServerRunner(Map<String, String> options) {
            try {
                return new Default(options);
            } catch (InterruptedException | TimeoutException | IOException e) {
                throw new RuntimeException("Unable to construct runner.");
            }
        }
    }

    class Default implements TypeDBClusterServerRunner {

        protected final Path distribution;
        protected final Map<String, String> serverOptions;
        private StartedProcess process;
        protected ProcessExecutor executor;

        public Default(Map<String, String> serverOptions) throws IOException, InterruptedException, TimeoutException {
            distribution = RunnerUtil.unarchive();
            this.serverOptions = serverOptions;
            System.out.println(address() + ": " + name() + " constructing runner...");
            Files.createDirectories(ClusterServerOpts.storageData(serverOptions));
            Files.createDirectories(ClusterServerOpts.logOutput(serverOptions));
            executor = createProcessExecutor(distribution);
            System.out.println(address() + ": " + name() + " runner constructed.");
        }

        private String name() {
            return "TypeDB Cluster";
        }

        public Map<String, String> options() {
            return serverOptions;
        }

        @Override
        public Addresses address() {
            return ClusterServerOpts.address(serverOptions);
        }

        public Set<Addresses> peers() {
            return ClusterServerOpts.peers(serverOptions);
        }

        private Path dataDir() {
            return ClusterServerOpts.storageData(serverOptions);
        }

        private Path logsDir() {
            return ClusterServerOpts.logOutput(serverOptions);
        }

        @Override
        public void start() {
            try {
                System.out.println(address() + ": " + name() + " is starting... ");
                System.out.println(address() + ": Distribution is located at " + distribution.toAbsolutePath());
                System.out.println(address() + ": Data directory is located at " + dataDir().toAbsolutePath());
                System.out.println(address() + ": Server bootup command: '" + command() + "'");
                process = executor.command(command()).start();
                boolean started = RunnerUtil.waitUntilPortUsed(address().external())
                        .await(SERVER_STARTUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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

        public List<String> command() {
            List<String> cmd = new ArrayList<>();
            cmd.add("cluster");
            serverOptions.forEach((key, value) -> cmd.add(key + "=" + value));
            return RunnerUtil.typeDBCommand(cmd);
        }

        @Override
        public boolean isStopped() {
            return process == null || !process.getProcess().isAlive();
        }

        @Override
        public void stop() {
            if (process != null) {
                try {
                    System.out.println(address() + ": Stopping...");
                    process.getProcess().destroyForcibly();
                    process = null;
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
            Path logPath = logsDir().resolve("typedb.log").toAbsolutePath();
            try {
                executor.command("cat", logPath.toString()).execute();
            } catch (IOException | InterruptedException | TimeoutException e) {
                System.out.println(address() + ": Unable to print '" + logPath + "'");
                e.printStackTrace();
            }
            System.out.println(address() + ": ================");
        }
    }

}

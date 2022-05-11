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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.set;
import static com.vaticle.typedb.common.test.RunnerUtil.findUnusedPorts;

public class TypeDBClusterServerRunner {

    private static final String OPT_ADDR = "--server.address";
    private static final String OPT_INTERNAL_ADDR_ZMQ = "--server.internal-address.zeromq";
    private static final String OPT_INTERNAL_ADDR_GRPC = "--server.internal-address.grpc";
    private static final String OPT_PEERS_ADDR = "--server.peers.server-peer-%d.address";
    private static final String OPT_PEERS_INTERNAL_ADDR_ZMQ = "--server.peers.server-peer-%d.internal-address.zeromq";
    private static final String OPT_PEERS_INTERNAL_ADDR_GRPC = "--server.peers.server-peer-%d.internal-address.grpc";

    protected final Path distribution;
    protected final Path dataDir;
    protected final Path logsDir;
    private final Ports ports;
    private final Set<Ports> peers;
    private final Map<String, String> remainingServerOpts;
    private StartedProcess process;
    protected ProcessExecutor executor;

    public static TypeDBClusterServerRunner create() throws InterruptedException, TimeoutException, IOException {
        List<Integer> ports = findUnusedPorts(3);
        Ports server = new Ports(ports.get(0), ports.get(1), ports.get(2));
        return create(server);
    }

    public static TypeDBClusterServerRunner create(Map<String, String> remainingServerOpts)
            throws IOException, InterruptedException, TimeoutException {
        List<Integer> ports = findUnusedPorts(3);
        Ports server = new Ports(ports.get(0), ports.get(1), ports.get(2));
        return create(server, remainingServerOpts);
    }

    public static TypeDBClusterServerRunner create(Ports ports) throws IOException, InterruptedException, TimeoutException {
        return create(ports, set());
    }

    public static TypeDBClusterServerRunner create(Ports ports, Set<Ports> peers)
            throws IOException, InterruptedException, TimeoutException {
        return create(ports, peers, map());
    }

    public static TypeDBClusterServerRunner create(Ports ports, Map<String, String> remainingServerOpts)
            throws IOException, InterruptedException, TimeoutException {
        return new TypeDBClusterServerRunner(ports, set(), remainingServerOpts);
    }

    public static TypeDBClusterServerRunner create(Ports ports, Set<Ports> peers, Map<String, String> remainingServerOpts)
            throws IOException, InterruptedException, TimeoutException {
        return new TypeDBClusterServerRunner(ports, peers, remainingServerOpts);
    }

    private TypeDBClusterServerRunner(Ports ports, Set<Ports> peers, Map<String, String> remainingServerOpts)
            throws InterruptedException, TimeoutException, IOException {
        this.ports = ports;
        System.out.println("Constructing " + name() + " runner");
        System.out.println(address() + ": Constructing " + name() + " runner");
        System.out.println(address() + ": Extracting distribution archive...");
        distribution = RunnerUtil.unarchive();
        System.out.println(address() + ": distribution archive extracted.");
        dataDir = distribution.resolve("server").resolve("data");
        logsDir = distribution.resolve("server").resolve("logs");
        this.peers = peers;
        this.remainingServerOpts = remainingServerOpts;
        executor = new ProcessExecutor()
                .directory(distribution.toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        System.out.println(name() + ": runner constructed");
    }

    private String name() {
        return "TypeDB Cluster";
    }

    public String address() {
        return host() + ":" + port();
    }

    public static String host() {
        return "127.0.0.1";
    }

    public int port() {
        return ports.external();
    }

    public Ports ports() {
        return ports;
    }

    public Set<Ports> peers() {
        return peers;
    }

    public Map<String, String> remainingServerOpts() {
        return remainingServerOpts;
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
        Map<String, String> serverOpts = new HashMap<>();
        serverOpts.putAll(portOptions(ports));
        serverOpts.putAll(peerOptions(peers));
        serverOpts.putAll(remainingServerOpts);

        List<String> command = new ArrayList<>();
        command.addAll(RunnerUtil.bin());
        command.add("cluster");
        serverOpts.forEach((key, value) -> command.add(key + "=" + value));
        return command;
    }

    private static Map<String, String> portOptions(Ports ports) {
        Map<String, String> options = new HashMap<>();
        options.put(OPT_ADDR, host() + ":" + ports.external());
        options.put(OPT_INTERNAL_ADDR_ZMQ, host() + ":" + ports.internalZMQ());
        options.put(OPT_INTERNAL_ADDR_GRPC, host() + ":" + ports.internalGRPC());
        return options;
    }

    private static Map<String, String> peerOptions(Set<Ports> peers) {
        Map<String, String> options = new HashMap<>();
        int index = 0;
        for (Ports peer : peers) {
            String addrKey = String.format(OPT_PEERS_ADDR, index);
            String intAddrZMQKey = String.format(OPT_PEERS_INTERNAL_ADDR_ZMQ, index);
            String intlAddrGRPCKey = String.format(OPT_PEERS_INTERNAL_ADDR_GRPC, index);
            options.put(addrKey, host() + ":" + peer.external());
            options.put(intAddrZMQKey, host() + ":" + peer.internalZMQ());
            options.put(intlAddrGRPCKey, host() + ":" + peer.internalGRPC());
            index++;
        }
        return options;
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

    public static class Ports {

        private final int external;
        private final int internalZMQ;
        private final int internalGRPC;

        public Ports(int external, int internalZMQ, int internalGRPC) {
            this.external = external;
            this.internalZMQ = internalZMQ;
            this.internalGRPC = internalGRPC;
        }

        public int external() {
            return external;
        }

        public int internalZMQ() {
            return internalZMQ;
        }

        public int internalGRPC() {
            return internalGRPC;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ports ports = (Ports) o;
            return external == ports.external && internalZMQ == ports.internalZMQ && internalGRPC == ports.internalGRPC;
        }

        @Override
        public int hashCode() {
            return Objects.hash(external, internalZMQ, internalGRPC);
        }
    }
}

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

import com.vaticle.typedb.common.concurrent.NamedThreadFactory;
import com.vaticle.typedb.common.conf.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.set;
import static com.vaticle.typedb.common.test.RunnerUtil.SERVER_STARTUP_TIMEOUT_MILLIS;
import static com.vaticle.typedb.common.test.RunnerUtil.createProcessExecutor;
import static com.vaticle.typedb.common.test.RunnerUtil.findUnusedPorts;
import static com.vaticle.typedb.common.test.RunnerUtil.typeDBCommand;
import static com.vaticle.typedb.common.test.RunnerUtil.unarchive;
import static com.vaticle.typedb.common.test.RunnerUtil.waitUntilPortUsed;

public class TypeDBClusterRunner implements TypeDBRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TypeDBClusterRunner.class);

    protected final List<Address> addrs;
    private final Map<String, ExecutorService> executorServices;
    protected final Map<String, ServerRunner> srvRunners;
    private final ServerRunnerFactory srvRunnerFactory;

    public TypeDBClusterRunner(Path locationBase, int srvCount) {
        this(locationBase, srvCount, new ServerRunnerFactory());
    }

    public TypeDBClusterRunner(Path locationBase, int srvCount, ServerRunnerFactory srvRunnerFactory) {
        assert srvCount >= 1;
        addrs = allocateAddresses(srvCount);
        this.srvRunnerFactory = srvRunnerFactory;
        executorServices = createExecutorServices(addrs);
        srvRunners = createServerRunners(locationBase, addrs);
    }

    private List<Address> allocateAddresses(int srvCount) {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < srvCount; i++) {
            String host = "127.0.0.1";
            int externalPort = 40000 + i * 1111;
            int internalPortZMQ = 50000 + i * 1111;
            int internalPortGRPC = 60000 + i * 1111;
            addresses.add(new Address(host, externalPort, host, internalPortZMQ, host, internalPortGRPC));
        }
        return addresses;
    }

    private Map<String, ExecutorService> createExecutorServices(List<Address> addrs) {
        Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>();
        for (Address address: addrs) {
            ExecutorService executorService = Executors.newSingleThreadExecutor(NamedThreadFactory.create(address.external() + "::cluster-runner"));
            executorServices.put(address.external(), executorService);
        }
        return executorServices;
    }

    private Map<String, ServerRunner> createServerRunners(Path locationBase, List<Address> addrs) {
        Map<String, ServerRunner> runners = new ConcurrentHashMap<>();
        for (Address address: addrs) {
            try {
                ServerRunner serverRunner = executorServices.get(address.external()).submit(() ->
                        srvRunnerFactory.createServerRunner(locationBase.resolve(address.external()).toAbsolutePath(), address, addrs)
                ).get();
                runners.put(address.external(), serverRunner);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return runners;
    }

    @Override
    public void start() {
        start(srvRunners);
    }

    private void start(Map<String, ServerRunner> srvs) {
        List<Future<?>> startFutures = new ArrayList<>();
        for (ServerRunner serverRunner : srvs.values()) {
            startFutures.add(executorServices.get(serverRunner.address()).submit(serverRunner::start));
        }
        join(startFutures);
    }

    public void start(String externalAddr) {
        join(executorServices.get(externalAddr).submit(() -> srvRunners.get(externalAddr).start()));
    }

    public void stop(String externalAddr) {
        join(executorServices.get(externalAddr).submit(() -> srvRunners.get(externalAddr).stop()));
        try {
            Thread.sleep(10000); // NOTE: add sleep since the port isn't immediately available after stopping the server
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        for (ServerRunner serverRunner : srvRunners.values()) {
            if (!serverRunner.isStopped()) {
                join(executorServices.get(serverRunner.address()).submit(serverRunner::stop));
            } else {
                LOG.debug("not stopping server {} - it is already stopped.", serverRunner.address());
            }
        }
    }

    private void join(List<Future<?>> futures) {
        futures.forEach(this::join);
    }

    private void join(Future<?> future) {
        try {
            future.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> externalAddresses() {
        return addrs.stream().map(Address::external).collect(Collectors.toSet());
    }

    public static class ServerRunnerFactory {
        ServerRunner createServerRunner(Path locationBase, Address address, List<Address> peers) {
            try {
                return ServerRunner.create(
                        address, new HashSet<>(peers)

                );
            } catch (InterruptedException | TimeoutException | IOException e) {
                throw new RuntimeException("Unable to construct runner '" + address.external() + "'");
            }
        }
    }

    public static class ServerRunner implements TypeDBRunner {

        private static final String OPT_ADDR = "--server.address";
        private static final String OPT_INTERNAL_ADDR_ZMQ = "--server.internal-address.zeromq";
        private static final String OPT_INTERNAL_ADDR_GRPC = "--server.internal-address.grpc";
        private static final String OPT_PEERS_ADDR = "--server.peers.server-peer-%d.address";
        private static final String OPT_PEERS_INTERNAL_ADDR_ZMQ = "--server.peers.server-peer-%d.internal-address.zeromq";
        private static final String OPT_PEERS_INTERNAL_ADDR_GRPC = "--server.peers.server-peer-%d.internal-address.grpc";
        private static final String HOST = "127.0.0.1";

        protected final Path distribution;
        protected final Path dataDir;
        protected final Path logsDir;
        private final Address ports;
        private final Set<Address> peers;
        private final Map<String, String> remainingServerOpts;
        private StartedProcess process;
        protected ProcessExecutor executor;

        public static ServerRunner create() throws InterruptedException, TimeoutException, IOException {
            List<Integer> ports = findUnusedPorts(3);
            Address server = new Address(HOST, ports.get(0), ports.get(1), ports.get(2));
            return create(server);
        }

        public static ServerRunner create(Map<String, String> remainingServerOpts)
                throws IOException, InterruptedException, TimeoutException {
            List<Integer> ports = findUnusedPorts(3);
            Address server = new Address(HOST, ports.get(0), ports.get(1), ports.get(2));
            return create(server, remainingServerOpts);
        }

        public static ServerRunner create(Address ports) throws IOException, InterruptedException, TimeoutException {
            return create(ports, set());
        }

        public static ServerRunner create(Address address, Set<Address> peers)
                throws IOException, InterruptedException, TimeoutException {
            return create(address, peers, map());
        }

        public static ServerRunner create(Address address, Map<String, String> remainingServerOpts)
                throws IOException, InterruptedException, TimeoutException {
            return new ServerRunner(address, set(), remainingServerOpts);
        }

        public static ServerRunner create(Address address, Set<Address> peers, Map<String, String> remainingServerOpts)
                throws IOException, InterruptedException, TimeoutException {
            return new ServerRunner(address, peers, remainingServerOpts);
        }

        private ServerRunner(Address address, Set<Address> peers, Map<String, String> remainingServerOpts)
                throws InterruptedException, TimeoutException, IOException {
            this.ports = address;
            System.out.println("Constructing " + name() + " runner");
            System.out.println(address() + ": Constructing " + name() + " runner");
            System.out.println(address() + ": Extracting distribution archive...");
            distribution = unarchive();
            System.out.println(address() + ": Distribution archive extracted.");
            dataDir = distribution.resolve("server").resolve("data");
            logsDir = distribution.resolve("server").resolve("logs");
            this.peers = peers;
            this.remainingServerOpts = remainingServerOpts;
            executor = createProcessExecutor(distribution);
            System.out.println(name() + ": Runner constructed");
        }

        private String name() {
            return "TypeDB Cluster";
        }

        public String address() {
            return host() + ":" + port();
        }

        public String host() {
            return HOST;
        }

        public int port() {
            return ports.externalPort();
        }

        public Address ports() {
            return ports;
        }

        public Set<Address> peers() {
            return peers;
        }

        public Map<String, String> remainingServerOpts() {
            return remainingServerOpts;
        }

        @Override
        public void start() {
            try {
                System.out.println(address() + ": " +  name() + " is starting... ");
                System.out.println(address() + ": Distribution is located at " + distribution.toAbsolutePath());
                System.out.println(address() + ": Data directory is located at " + dataDir.toAbsolutePath());
                System.out.println(address() + ": Server bootup command: '" + command() + "'");
                process = executor.command(command()).start();
                boolean started = waitUntilPortUsed(host(), port())
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

        private List<String> command() {
            Map<String, String> serverOpts = new HashMap<>();
            serverOpts.putAll(portOptions(ports));
            serverOpts.putAll(peerOptions(peers));
            serverOpts.putAll(remainingServerOpts);

            List<String> cmd = new ArrayList<>();
            cmd.add("cluster");
            serverOpts.forEach((key, value) -> cmd.add(key + "=" + value));
            return typeDBCommand(cmd);
        }

        private Map<String, String> portOptions(Address address) {
            Map<String, String> options = new HashMap<>();
            options.put(OPT_ADDR, address.external());
            options.put(OPT_INTERNAL_ADDR_ZMQ, address.internalZMQ());
            options.put(OPT_INTERNAL_ADDR_GRPC, address.internalGRPC());
            return options;
        }

        private Map<String, String> peerOptions(Set<Address> peers) {
            Map<String, String> options = new HashMap<>();
            int index = 0;
            for (Address peer : peers) {
                String addrKey = String.format(OPT_PEERS_ADDR, index);
                String intAddrZMQKey = String.format(OPT_PEERS_INTERNAL_ADDR_ZMQ, index);
                String intAddrGRPCKey = String.format(OPT_PEERS_INTERNAL_ADDR_GRPC, index);
                options.put(addrKey, peer.external());
                options.put(intAddrZMQKey, peer.internalZMQ());
                options.put(intAddrGRPCKey, peer.internalGRPC());
                index++;
            }
            return options;
        }

        public boolean isStopped() {
            throw new UnsupportedOperationException();
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
}

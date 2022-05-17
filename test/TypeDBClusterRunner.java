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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.pair;
import static com.vaticle.typedb.common.collection.Collections.set;
import static com.vaticle.typedb.common.test.RunnerUtil.SERVER_STARTUP_TIMEOUT_MILLIS;
import static com.vaticle.typedb.common.test.RunnerUtil.createProcessExecutor;
import static com.vaticle.typedb.common.test.RunnerUtil.typeDBCommand;
import static com.vaticle.typedb.common.test.RunnerUtil.unarchive;
import static com.vaticle.typedb.common.test.RunnerUtil.waitUntilPortUsed;

public class TypeDBClusterRunner implements TypeDBRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TypeDBClusterRunner.class);

    private static final String OPT_ADDR = "--server.address";
    private static final String OPT_INTERNAL_ADDR_ZMQ = "--server.internal-address.zeromq";
    private static final String OPT_INTERNAL_ADDR_GRPC = "--server.internal-address.grpc";
    private static final String OPT_PEERS_ADDR = "--server.peers.server-peer-%d.address";
    private static final String OPT_PEERS_INTERNAL_ADDR_ZMQ = "--server.peers.server-peer-%d.internal-address.zeromq";
    private static final String OPT_PEERS_INTERNAL_ADDR_GRPC = "--server.peers.server-peer-%d.internal-address.grpc";
    private static final String STORAGE_DATA = "--storage.data";
    private static final String STORAGE_REPLICATION = "--storage.replication";
    private static final String STORAGE_USER = "--storage.user";
    private static final String LOG_OUTPUT_FILE_DIRECTORY = "--log.output.file.directory";
    private static final String HOST = "127.0.0.1";

    protected final Set<Address> serverAddrs;
    protected final Map<Address, ServerRunner> serverRunners;
    private final Map<Address, ExecutorService> serverRunnerES;
    private final ServerRunner.Factory serverRunnerFactory;

    public TypeDBClusterRunner(Path clusterRunnerDir, int serverCount) {
        this(clusterRunnerDir, serverCount, new ServerRunner.Factory());
    }

    public TypeDBClusterRunner(Path clusterRunnerDir, int serverCount, ServerRunner.Factory serverRunnerFactory) {
        assert serverCount >= 1;
        serverAddrs = allocateAddresses(serverCount);
        this.serverRunnerFactory = serverRunnerFactory;
        serverRunnerES = createServerRunnerES(serverAddrs);
        serverRunners = createServerRunners(clusterRunnerDir, serverAddrs);
    }

    private Set<Address> allocateAddresses(int serverCount) {
        Set<Address> addresses = new HashSet<>();
        for (int i = 0; i < serverCount; i++) {
            String host = "127.0.0.1";
            int externalPort = 40000 + i * 1111;
            int internalPortZMQ = 50000 + i * 1111;
            int internalPortGRPC = 60000 + i * 1111;
            addresses.add(new Address(host, externalPort, host, internalPortZMQ, host, internalPortGRPC));
        }
        return addresses;
    }

    private Map<Address, ExecutorService> createServerRunnerES(Set<Address> serverAddrs) {
        Map<Address, ExecutorService> executorServices = new ConcurrentHashMap<>();
        for (Address addr: serverAddrs) {
            NamedThreadFactory threadFactory = NamedThreadFactory.create(addr.external() + "::server-runner");
            ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
            executorServices.put(addr, executorService);
        }
        return executorServices;
    }

    private Map<Address, ServerRunner> createServerRunners(Path clusterRunnerDir, Set<Address> serverAddrs) {
        Map<Address, ServerRunner> srvRunners = new ConcurrentHashMap<>();
        for (Address addr: serverAddrs) {
            try {
                ExecutorService es = serverRunnerES.get(addr);
                Path srvRunnerDir = clusterRunnerDir.resolve(addr.external()).toAbsolutePath();
                Map<String, String> options = map(
                        pair(STORAGE_DATA, srvRunnerDir.resolve("server/data").toAbsolutePath().toString()),
                        pair(STORAGE_REPLICATION, srvRunnerDir.resolve("server/replication").toAbsolutePath().toString()),
                        pair(STORAGE_USER, srvRunnerDir.resolve("server/user").toAbsolutePath().toString()),
                        pair(LOG_OUTPUT_FILE_DIRECTORY, srvRunnerDir.resolve("server/logs").toAbsolutePath().toString())
                );
                ServerRunner srvRunner = es.submit(() -> serverRunnerFactory.createServerRunner(options)).get();
                srvRunners.put(addr, srvRunner);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return srvRunners;
    }

    @Override
    public void start() {
        List<Future<?>> tasks = new ArrayList<>();
        for (ServerRunner runner : serverRunners.values()) {
            ExecutorService es = serverRunnerES.get(runner.address());
            tasks.add(es.submit(runner::start));
        }
        join(tasks);
    }

    public void start(String externalAddr) {
        Address addr = serverAddrs
                .stream()
                .filter(addr2 -> addr2.external().equals(externalAddr))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Server '" + externalAddr + "' not found"));
        Future<?> task = serverRunnerES.get(addr)
                .submit(() -> serverRunners.get(addr).start());
        join(task);
    }

    @Override
    public boolean isStopped() {
        return serverRunners.values().stream().allMatch(TypeDBRunner::isStopped);
    }

    public void stop(String externalAddr) {
        Address addr = serverAddrs
                .stream()
                .filter(addr2 -> addr2.external().equals(externalAddr))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Server '" + externalAddr + "' not found"));
        Future<?> task = serverRunnerES.get(addr).submit(() -> serverRunners.get(addr).stop());
        join(task);
        try {
            Thread.sleep(10000); // NOTE: add sleep since the port isn't immediately available after stopping the server
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        for (ServerRunner serverRunner : serverRunners.values()) {
            if (!serverRunner.isStopped()) {
                Future<?> task = serverRunnerES.get(serverRunner.address()).submit(serverRunner::stop);
                join(task);
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

    public Set<Address> addresses() {
        return serverAddrs;
    }

    public Set<String> externalAddresses() {
        return serverAddrs.stream().map(Address::external).collect(Collectors.toSet());
    }

    public interface ServerRunner extends TypeDBRunner {

        Map<String, String> options();

        Address address();

        class Default implements ServerRunner {

            protected final Path distribution;
            protected final Map<String, String> options;
            private StartedProcess process;
            protected ProcessExecutor executor;

            public static Default create(Address address, Set<Address> peers)
                    throws IOException, InterruptedException, TimeoutException {
                return create(address, peers, map());
            }

            public static Default create(Address address, Set<Address> peers, Map<String, String> remainingOptions)
                    throws IOException, InterruptedException, TimeoutException {
                Map<String, String> options = new HashMap<>();
                options.putAll(addressOpt(address));
                options.putAll(peersOpt(peers));
                options.putAll(remainingOptions);
                return Default.create(options);
            }

            public static Default create(Map<String, String> options) throws IOException, InterruptedException, TimeoutException {
                return new Default(options);
            }

            private Default(Map<String, String> options) throws IOException, InterruptedException, TimeoutException {
                distribution = unarchive();
                System.out.println(address() + ": " + name() + " constructing runner...");
                this.options = options;
                Files.createDirectories(storageDataOpt(options));
                Files.createDirectories(logOutputOpt(options));
                executor = createProcessExecutor(distribution);
                System.out.println(address() + ": " + name() + " runner constructed.");
            }

            private String name() {
                return "TypeDB Cluster";
            }

            @Override
            public Map<String, String> options() {
                return options;
            }

            @Override
            public Address address() {
                return addressOpt(options);
            }

            public Set<Address> peers() {
                return peersOpt(options);
            }

            private Path dataDir() {
                return storageDataOpt(options);
            }

            private Path logsDir() {
                return logOutputOpt(options);
            }

            @Override
            public void start() {
                try {
                    System.out.println(address() + ": " +  name() + " is starting... ");
                    System.out.println(address() + ": Distribution is located at " + distribution.toAbsolutePath());
                    System.out.println(address() + ": Data directory is located at " + dataDir().toAbsolutePath());
                    System.out.println(address() + ": Server bootup command: '" + command() + "'");
                    process = executor.command(command()).start();
                    boolean started = waitUntilPortUsed(address().external2())
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
                List<String> cmd = new ArrayList<>();
                cmd.add("cluster");
                options.forEach((key, value) -> cmd.add(key + "=" + value));
                return typeDBCommand(cmd);
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
                Path logPath = logsDir().resolve("typedb.log").toAbsolutePath();
                try {
                    executor.command("cat", logPath.toString()).execute();
                } catch (IOException | InterruptedException | TimeoutException e) {
                    System.out.println(address() + ": Unable to print '" + logPath + "'");
                    e.printStackTrace();
                }
                System.out.println(address() + ": ================");
            }

            private Address addressOpt(Map<String, String> options) {
                return Address.create(options.get(OPT_ADDR), options.get(OPT_INTERNAL_ADDR_ZMQ), options.get(OPT_INTERNAL_ADDR_GRPC));
            }

            private static Map<String, String> addressOpt(Address address) {
                Map<String, String> options = new HashMap<>();
                options.put(OPT_ADDR, address.external());
                options.put(OPT_INTERNAL_ADDR_ZMQ, address.internalZMQ());
                options.put(OPT_INTERNAL_ADDR_GRPC, address.internalGRPC());
                return options;
            }

            private Set<Address> peersOpt(Map<String, String> options) {
                Set<String> names = new HashSet<>(); // "--server.peers.{name}.{x}";
                Pattern namePattern = Pattern.compile("--server.peers.(.+).*$");
                for (String opt: options.keySet()) {
                    Matcher nameMatcher = namePattern.matcher(opt);
                    if (nameMatcher.find()) {
                        names.add(nameMatcher.group(1));
                    }
                }
                Set<Address> peers = new HashSet<>();
                for (String name: names) {
                    Address peer = Address.create(
                            options.get("--server.peers." + name + ".address"),
                            options.get("--server.peers." + name + ".internal-address.zeromq"),
                            options.get("--server.peers." + name + ".internal-address.grpc")
                    );
                    peers.add(peer);
                }
                return peers;
            }

            private static Map<String, String> peersOpt(Set<Address> peers) {
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

            private Path storageDataOpt(Map<String, String> options) {
                return options.get(STORAGE_DATA) != null ? Paths.get(options.get(STORAGE_DATA)) : distribution.resolve("server").resolve("data");
            }

            private Path logOutputOpt(Map<String, String> options) {
                return options.get(LOG_OUTPUT_FILE_DIRECTORY) != null ? Paths.get(options.get(LOG_OUTPUT_FILE_DIRECTORY)) : distribution.resolve("server").resolve("logs");
            }
        }

        class Factory {

            protected ServerRunner createServerRunner(Map<String, String> options) {
                try {
                    return Default.create(options);
                } catch (InterruptedException | TimeoutException | IOException e) {
                    throw new RuntimeException("Unable to construct runner.");
                }
            }
        }
    }
}

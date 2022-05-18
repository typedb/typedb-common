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

    protected final Map<Address, Map<String, String>> serverOptionsMap;
    private final ServerRunner.Factory serverRunnerFactory;
    protected final Map<Address, ServerRunner> serverRunners;

    public static TypeDBClusterRunner create(Path clusterRunnerDir, int serverCount) {
        return create(clusterRunnerDir, serverCount, new ServerRunner.Factory());
    }

    public static TypeDBClusterRunner create(Path clusterRunnerDir, int serverCount, ServerRunner.Factory serverRunnerFactory) {
        Set<Address> serverAddrs = allocateAddresses(serverCount);
        Map<Address, Map<String, String>> serverOptionsMap = new HashMap<>();
        for (Address addr: serverAddrs) {
            Map<String, String> options = new HashMap<>();
            options.putAll(ServerRunner.Opts.addressOpt(addr));
            options.putAll(ServerRunner.Opts.peersOpt(serverAddrs));
            Path srvRunnerDir = clusterRunnerDir.resolve(addr.external()).toAbsolutePath();
            options.putAll(
                    map(
                            pair(STORAGE_DATA, srvRunnerDir.resolve("server/data").toAbsolutePath().toString()),
                            pair(STORAGE_REPLICATION, srvRunnerDir.resolve("server/replication").toAbsolutePath().toString()),
                            pair(STORAGE_USER, srvRunnerDir.resolve("server/user").toAbsolutePath().toString()),
                            pair(LOG_OUTPUT_FILE_DIRECTORY, srvRunnerDir.resolve("server/logs").toAbsolutePath().toString())
                    )
            );
            serverOptionsMap.put(addr, options);
        }
        return new TypeDBClusterRunner(serverOptionsMap, serverRunnerFactory);
    }

    private static Set<Address> allocateAddresses(int serverCount) {
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

    public TypeDBClusterRunner(Map<Address, Map<String, String>> serverOptionsMap, ServerRunner.Factory serverRunnerFactory) {
        this.serverOptionsMap = serverOptionsMap;
        this.serverRunnerFactory = serverRunnerFactory;
        serverRunners = createServerRunners(this.serverOptionsMap);
    }

    private Map<Address, ServerRunner> createServerRunners(Map<Address, Map<String, String>> serverOptsMap) {
        Map<Address, ServerRunner> srvRunners = new ConcurrentHashMap<>();
        for (Address addr: serverOptsMap.keySet()) {
            Map<String, String> options = serverOptsMap.get(addr);
            ServerRunner srvRunner = serverRunnerFactory.createServerRunner(options);
            srvRunners.put(addr, srvRunner);
        }
        return srvRunners;
    }

    @Override
    public void start() {
        for (ServerRunner runner : serverRunners.values()) {
            runner.start();
        }
    }

    public void start(String externalAddr) {
        Address addr = addresses()
                .stream()
                .filter(addr2 -> addr2.external().equals(externalAddr))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Server '" + externalAddr + "' not found"));
        serverRunners.get(addr).start();
    }

    @Override
    public boolean isStopped() {
        return serverRunners.values().stream().allMatch(TypeDBRunner::isStopped);
    }

    public void stop(String externalAddr) {
        Address addr = addresses()
                .stream()
                .filter(addr2 -> addr2.external().equals(externalAddr))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Server '" + externalAddr + "' not found"));
        serverRunners.get(addr).stop();
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
                serverRunner.stop();
            } else {
                LOG.debug("not stopping server {} - it is already stopped.", serverRunner.address());
            }
        }
    }

    public Set<Address> addresses() {
        return serverOptionsMap.keySet();
    }

    public Set<String> externalAddresses() {
        return addresses().stream().map(Address::external).collect(Collectors.toSet());
    }

    public interface ServerRunner extends TypeDBRunner {

        Address address();

        class Default implements ServerRunner {

            protected final Path distribution;
            protected final Map<String, String> serverOptions;
            private StartedProcess process;
            protected ProcessExecutor executor;

            public Default(Map<String, String> serverOptions) throws IOException, InterruptedException, TimeoutException {
                distribution = unarchive();
                this.serverOptions = serverOptions;
                System.out.println(address() + ": " + name() + " constructing runner...");
                Files.createDirectories(Opts.storageDataOpt(serverOptions));
                Files.createDirectories(Opts.logOutputOpt(serverOptions));
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
            public Address address() {
                return Opts.addressOpt(serverOptions);
            }

            public Set<Address> peers() {
                return Opts.peersOpt(serverOptions);
            }

            private Path dataDir() {
                return Opts.storageDataOpt(serverOptions);
            }

            private Path logsDir() {
                return Opts.logOutputOpt(serverOptions);
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
                serverOptions.forEach((key, value) -> cmd.add(key + "=" + value));
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
        }

        class Factory {

            protected ServerRunner createServerRunner(Map<String, String> options) {
                try {
                    return new Default(options);
                } catch (InterruptedException | TimeoutException | IOException e) {
                    throw new RuntimeException("Unable to construct runner.");
                }
            }
        }

        class Opts {

            private static Address addressOpt(Map<String, String> options) {
                return Address.create(options.get(OPT_ADDR), options.get(OPT_INTERNAL_ADDR_ZMQ), options.get(OPT_INTERNAL_ADDR_GRPC));
            }

            private static Map<String, String> addressOpt(Address address) {
                Map<String, String> options = new HashMap<>();
                options.put(OPT_ADDR, address.external());
                options.put(OPT_INTERNAL_ADDR_ZMQ, address.internalZMQ());
                options.put(OPT_INTERNAL_ADDR_GRPC, address.internalGRPC());
                return options;
            }

            private static Set<Address> peersOpt(Map<String, String> options) {
                Set<String> names = new HashSet<>();
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

            private static Path storageDataOpt(Map<String, String> options) {
                return Paths.get(options.get(STORAGE_DATA));
            }

            private static Path logOutputOpt(Map<String, String> options) {
                return Paths.get(options.get(LOG_OUTPUT_FILE_DIRECTORY));
            }
        }
    }
}

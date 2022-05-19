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
import com.vaticle.typedb.common.test.TypeDBRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.pair;

public class TypeDBClusterRunner implements TypeDBRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TypeDBClusterRunner.class);

    protected final Map<Addresses, Map<String, String>> serverOptionsMap;
    private final TypeDBClusterServerRunner.Factory serverRunnerFactory;
    protected final Map<Addresses, TypeDBClusterServerRunner> serverRunners;

    public static TypeDBClusterRunner create(Path clusterRunnerDir, int serverCount) {
        return create(clusterRunnerDir, serverCount, new TypeDBClusterServerRunner.Factory());
    }

    public static TypeDBClusterRunner create(Path clusterRunnerDir, int serverCount, TypeDBClusterServerRunner.Factory serverRunnerFactory) {
        Set<Addresses> serverAddrs = allocateAddresses(serverCount);
        Map<Addresses, Map<String, String>> serverOptionsMap = new HashMap<>();
        for (Addresses addr: serverAddrs) {
            Map<String, String> options = new HashMap<>();
            options.putAll(ClusterServerOpts.address(addr));
            options.putAll(ClusterServerOpts.peers(serverAddrs));
            Path srvRunnerDir = clusterRunnerDir.resolve(addr.external().toString()).toAbsolutePath();
            options.putAll(
                    map(
                            pair(ClusterServerOpts.STORAGE_DATA, srvRunnerDir.resolve("server/data").toAbsolutePath().toString()),
                            pair(ClusterServerOpts.STORAGE_REPLICATION, srvRunnerDir.resolve("server/replication").toAbsolutePath().toString()),
                            pair(ClusterServerOpts.STORAGE_USER, srvRunnerDir.resolve("server/user").toAbsolutePath().toString()),
                            pair(ClusterServerOpts.LOG_OUTPUT_FILE_DIRECTORY, srvRunnerDir.resolve("server/logs").toAbsolutePath().toString())
                    )
            );
            serverOptionsMap.put(addr, options);
        }
        return new TypeDBClusterRunner(serverOptionsMap, serverRunnerFactory);
    }

    private static Set<Addresses> allocateAddresses(int serverCount) {
        Set<Addresses> addresses = new HashSet<>();
        for (int i = 0; i < serverCount; i++) {
            String host = "127.0.0.1";
            int externalPort = 40000 + i * 1111;
            int internalPortZMQ = 50000 + i * 1111;
            int internalPortGRPC = 60000 + i * 1111;
            addresses.add(Addresses.create(host, externalPort, host, internalPortZMQ, host, internalPortGRPC));
        }
        return addresses;
    }

    private TypeDBClusterRunner(Map<Addresses, Map<String, String>> serverOptionsMap, TypeDBClusterServerRunner.Factory serverRunnerFactory) {
        this.serverOptionsMap = serverOptionsMap;
        this.serverRunnerFactory = serverRunnerFactory;
        serverRunners = createServerRunners(this.serverOptionsMap);
    }

    private Map<Addresses, TypeDBClusterServerRunner> createServerRunners(Map<Addresses, Map<String, String>> serverOptsMap) {
        Map<Addresses, TypeDBClusterServerRunner> srvRunners = new ConcurrentHashMap<>();
        for (Addresses addr: serverOptsMap.keySet()) {
            Map<String, String> options = serverOptsMap.get(addr);
            TypeDBClusterServerRunner srvRunner = serverRunnerFactory.createServerRunner(options);
            srvRunners.put(addr, srvRunner);
        }
        return srvRunners;
    }

    @Override
    public void start() {
        for (TypeDBClusterServerRunner runner : serverRunners.values()) {
            if (runner.isStopped()) {
                runner.start();
            } else {
                LOG.debug("not starting server {} - it is already started.", runner.address());
            }
        }
    }

    @Override
    public boolean isStopped() {
        return serverRunners.values().stream().allMatch(TypeDBRunner::isStopped);
    }

    public Set<Addresses> addresses() {
        return serverOptionsMap.keySet();
    }

    public Set<String> externalAddresses() {
        return addresses().stream().map(addr -> addr.external().toString()).collect(Collectors.toSet());
    }

    public Map<Addresses, TypeDBClusterServerRunner> serverRunners() {
        return serverRunners;
    }

    public TypeDBClusterServerRunner serverRunner(String externalAddr) {
        Addresses addr = addresses()
                .stream()
                .filter(addr2 -> addr2.external().toString().equals(externalAddr))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Server runner '" + externalAddr + "' not found"));
        return serverRunner(addr);
    }

    public TypeDBClusterServerRunner serverRunner(Addresses addr) {
        return serverRunners.get(addr);
    }

    @Override
    public void stop() {
        for (TypeDBClusterServerRunner runner : serverRunners.values()) {
            if (!runner.isStopped()) {
                runner.stop();
            } else {
                LOG.debug("not stopping server {} - it is already stopped.", runner.address());
            }
        }
    }
}

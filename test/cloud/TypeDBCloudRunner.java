/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.common.test.cloud;

import com.vaticle.typedb.common.conf.cloud.Addresses;
import com.vaticle.typedb.common.test.TypeDBRunner;
import com.vaticle.typedb.common.test.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.pair;

public class TypeDBCloudRunner implements TypeDBRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TypeDBCloudRunner.class);

    protected final Map<Addresses, Map<String, String>> serverOptionsMap;
    private final TypeDBCloudServerRunner.Factory serverRunnerFactory;
    protected final Map<Addresses, TypeDBCloudServerRunner> serverRunners;
    private static Path runnerPath;

    public static TypeDBCloudRunner create(Path cloudRunnerDir, int serverCount) {
        return create(cloudRunnerDir, serverCount, new HashMap<>(),
                new TypeDBCloudServerRunner.Factory());
    }

    public static TypeDBCloudRunner create(Path cloudRunnerDir, int serverCount, Map<String, String> extraOptions) {
        return create(cloudRunnerDir, serverCount, extraOptions, new TypeDBCloudServerRunner.Factory());
    }

    public static TypeDBCloudRunner create(Path cloudRunnerDir, int serverCount, Map<String, String> extraOptions,
                                             TypeDBCloudServerRunner.Factory serverRunnerFactory) {
        Set<Addresses> serverAddressesSet = allocateAddressesSet(serverCount);
        Map<Addresses, Map<String, String>> serverOptionsMap = new HashMap<>();
        runnerPath = cloudRunnerDir;
        cloudRunnerDir = cloudRunnerDir.resolve(java.util.UUID.randomUUID().toString());
        for (Addresses addrs: serverAddressesSet) {
            Map<String, String> options = new HashMap<>();
            options.putAll(extraOptions);
            options.putAll(CloudServerOpts.address(addrs));
            options.putAll(CloudServerOpts.peers(serverAddressesSet));
            Path srvRunnerDir = cloudRunnerDir.resolve(addrs.externalString()).toAbsolutePath();
            options.putAll(
                    map(
                            pair(CloudServerOpts.STORAGE_DATA, srvRunnerDir.resolve("server/data").toAbsolutePath().toString()),
                            pair(CloudServerOpts.STORAGE_REPLICATION, srvRunnerDir.resolve("server/replication").toAbsolutePath().toString()),
                            pair(CloudServerOpts.STORAGE_USER, srvRunnerDir.resolve("server/user").toAbsolutePath().toString()),
                            pair(CloudServerOpts.LOG_OUTPUT_FILE_DIRECTORY, srvRunnerDir.resolve("server/logs").toAbsolutePath().toString())
                    )
            );
            serverOptionsMap.put(addrs, options);
        }
        return new TypeDBCloudRunner(serverOptionsMap, serverRunnerFactory);
    }

    private static Set<Addresses> allocateAddressesSet(int serverCount) {
        Set<Addresses> addresses = new HashSet<>();
        List<Integer> ports = Util.findUnusedPorts(serverCount * 3);
        for (int i = 0; i < serverCount; i++) {
            String host = "localhost";
            int externalPort = ports.get(3 * i);
            int internalPortZMQ = ports.get(3 * i + 1);
            int internalPortGRPC = ports.get(3 * i + 2);
            addresses.add(Addresses.create(host, externalPort, host, internalPortZMQ, host, internalPortGRPC));
        }
        return addresses;
    }

    private TypeDBCloudRunner(Map<Addresses, Map<String, String>> serverOptionsMap, TypeDBCloudServerRunner.Factory serverRunnerFactory) {
        assert serverOptionsMap.size() >= 1;
        this.serverOptionsMap = serverOptionsMap;
        this.serverRunnerFactory = serverRunnerFactory;
        serverRunners = createServerRunners(this.serverOptionsMap);
    }

    private Map<Addresses, TypeDBCloudServerRunner> createServerRunners(Map<Addresses, Map<String, String>> serverOptsMap) {
        Map<Addresses, TypeDBCloudServerRunner> srvRunners = new HashMap<>();
        for (Addresses addrs: serverOptsMap.keySet()) {
            Map<String, String> options = serverOptsMap.get(addrs);
            TypeDBCloudServerRunner srvRunner = serverRunnerFactory.createServerRunner(options);
            srvRunners.put(addrs, srvRunner);
        }
        return srvRunners;
    }

    @Override
    public void start() {
        for (TypeDBCloudServerRunner runner : serverRunners.values()) {
            if (runner.isStopped()) {
                runner.start();
            } else {
                LOG.debug("not starting server {} - it is already started.", runner.addresses());
            }
        }
    }

    @Override
    public boolean isStopped() {
        return serverRunners.values().stream().allMatch(TypeDBRunner::isStopped);
    }

    @Override
    public String address() {
        return externalAddresses().stream().findAny().get();
    }

    public Set<Addresses> addressesSet() {
        return serverOptionsMap.keySet();
    }

    public Set<String> externalAddresses() {
        return addressesSet().stream().map(Addresses::externalString).collect(Collectors.toSet());
    }

    public Map<Addresses, TypeDBCloudServerRunner> serverRunners() {
        return serverRunners;
    }

    public TypeDBCloudServerRunner serverRunner(String externalAddr) {
        Addresses addresses = addressesSet()
                .stream()
                .filter(addrs -> addrs.externalString().equals(externalAddr))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Server runner '" + externalAddr + "' not found"));
        return serverRunner(addresses);
    }

    public TypeDBCloudServerRunner serverRunner(Addresses addrs) {
        return serverRunners.get(addrs);
    }

    @Override
    public void stop() {
        for (TypeDBCloudServerRunner runner : serverRunners.values()) {
            if (!runner.isStopped()) {
                runner.stop();
            } else {
                LOG.debug("not stopping server {} - it is already stopped.", runner.addresses());
            }
        }
    }

    @Override
    public void deleteFiles() {
        for (TypeDBRunner runner : serverRunners.values()) {
            runner.deleteFiles();
        }
    }

    @Override
    public void reset() {
        for (TypeDBRunner runner : serverRunners.values()) {
            runner.reset();
        }
    }
}

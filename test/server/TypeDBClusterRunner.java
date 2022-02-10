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

import javax.sound.sampled.Port;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.set;

public class TypeDBClusterRunner extends TypeDBRunner {

    public static final String OPT_ADDR = "--server.address";
    private static final String OPT_INTERNAL_ADDR_ZMQ = "--server.internal-address.zeromq";
    private static final String OPT_INTERNAL_ADDR_GRPC = "--server.internal-address.grpc";
    public static final String OPT_PEERS_ADDR = "--server.peers.server-{index}.address";
    private static final String OPT_PEERS_INTERNAL_ADDR_ZMQ = "--server.peers.server-{index}.internal-address.zeromq";
    private static final String OPT_PEERS_INTERNAL_ADDR_GRPC = "--server.peers.server-{index}.internal-address.grpc";

    private final Ports server;
    private final Set<Ports> peers;
    private final Map<String, String> remainingOptions;

    public static TypeDBClusterRunner create() throws InterruptedException, TimeoutException, IOException {
        int port = ThreadLocalRandom.current().nextInt(40000, 60000);
        Ports server = new Ports(port, port+1, port+2);
        return TypeDBClusterRunner.create(server);
    }

    public static TypeDBClusterRunner create(Map<String, String> remainingOptions)
            throws IOException, InterruptedException, TimeoutException {
        int port = ThreadLocalRandom.current().nextInt(40000, 60000);
        Ports server = new Ports(port, port+1, port+2);
        return TypeDBClusterRunner.create(server, remainingOptions);
    }

    public static TypeDBClusterRunner create(Ports server) throws IOException, InterruptedException, TimeoutException {
        return TypeDBClusterRunner.create(server, set());
    }

    public static TypeDBClusterRunner create(Ports server, Set<Ports> peers)
            throws IOException, InterruptedException, TimeoutException {
        return TypeDBClusterRunner.create(server, peers, map());
    }

    public static TypeDBClusterRunner create(Ports server, Map<String, String> remainingOptions)
            throws IOException, InterruptedException, TimeoutException {
        return new TypeDBClusterRunner(server, set(), remainingOptions);
    }

    public static TypeDBClusterRunner create(Ports server, Set<Ports> peers, Map<String, String> remainingOptions)
            throws IOException, InterruptedException, TimeoutException {
        return new TypeDBClusterRunner(server, peers, remainingOptions);
    }

    private TypeDBClusterRunner(Ports server, Set<Ports> peers, Map<String, String> remainingOptions)
            throws InterruptedException, TimeoutException, IOException {
        super();
        this.server = server;
        this.peers = peers;
        this.remainingOptions = remainingOptions;
    }

    @Override
    protected String name() {
        return "TypeDB Cluster";
    }

    @Override
    protected int port() {
        return server.port();
    }

    public Ports ports() {
        return server;
    }

    public Set<Ports> peers() {
        return peers;
    }

    public Map<String, String> remainingOptions() {
        return remainingOptions;
    }

    @Override
    protected void verifyPortUnused() {
        super.verifyPortUnused(port());
        super.verifyPortUnused(ports().internalZMQ());
        super.verifyPortUnused(ports().internalGRPC());
    }

    @Override
    protected List<String> command() {
        Map<String, String> options = new HashMap<>();
        addOptions(options, server);
        addOptions(options, peers);
        options.putAll(remainingOptions);

        List<String> command = new ArrayList<>();
        command.addAll(getTypeDBBinary());
        command.add("cluster");
        options.forEach((key, value) -> command.add(key + "=" + value));
        return command;
    }

    private static void addOptions(Map<String, String> options, Ports serverPorts) {
        options.put(OPT_ADDR, host() + ":" + serverPorts.port());
        options.put(OPT_INTERNAL_ADDR_ZMQ, host() + ":" + serverPorts.internalZMQ());
        options.put(OPT_INTERNAL_ADDR_GRPC, host() + ":" + serverPorts.internalGRPC());
    }

    private static void addOptions(Map<String, String> options, Set<Ports> peerPorts) {
        int index = 0;
        for (Ports peer: peerPorts) {
            String addrKey = OPT_PEERS_ADDR.replace("{index}", "" + index);
            String intAddrZMQKey = OPT_PEERS_INTERNAL_ADDR_ZMQ.replace("{index}", "" + index);
            String intlAddrGRPCKey = OPT_PEERS_INTERNAL_ADDR_GRPC.replace("{index}", "" + index);
            options.put(addrKey, host() + ":" + peer.port());
            options.put(intAddrZMQKey, host() + ":" + peer.internalZMQ());
            options.put(intlAddrGRPCKey, host() + ":" + peer.internalGRPC());
            index++;
        }
    }

    public static class Ports {
        private final int port;

        private final int internalZMQ;
        private final int internalGRPC;
        public Ports(int port, int internalZMQ, int internalGRPC) {
            this.port = port;
            this.internalZMQ = internalZMQ;
            this.internalGRPC = internalGRPC;
        }

        public int port() {
            return port;
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
            return port == ports.port && internalZMQ == ports.internalZMQ && internalGRPC == ports.internalGRPC;
        }

        @Override
        public int hashCode() {
            return Objects.hash(port, internalZMQ, internalGRPC);
        }
    }
}

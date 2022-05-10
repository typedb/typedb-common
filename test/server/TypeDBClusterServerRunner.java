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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static com.vaticle.typedb.common.collection.Collections.map;
import static com.vaticle.typedb.common.collection.Collections.set;

public class TypeDBClusterServerRunner extends TypeDBRunner {

    private static final String OPT_ADDR = "--server.address";
    private static final String OPT_INTERNAL_ADDR_ZMQ = "--server.internal-address.zeromq";
    private static final String OPT_INTERNAL_ADDR_GRPC = "--server.internal-address.grpc";
    private static final String OPT_PEERS_ADDR = "--server.peers.server-peer-%d.address";
    private static final String OPT_PEERS_INTERNAL_ADDR_ZMQ = "--server.peers.server-peer-%d.internal-address.zeromq";
    private static final String OPT_PEERS_INTERNAL_ADDR_GRPC = "--server.peers.server-peer-%d.internal-address.grpc";

    private final Ports ports;
    private final Set<Ports> peers;
    private final Map<String, String> remainingServerOpts;

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
        super();
        this.ports = ports;
        this.peers = peers;
        this.remainingServerOpts = remainingServerOpts;
    }

    @Override
    protected String name() {
        return "TypeDB Cluster";
    }

    @Override
    protected int port() {
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

    @Override
    protected List<String> command() {
        Map<String, String> serverOpts = new HashMap<>();
        serverOpts.putAll(portOptions(ports));
        serverOpts.putAll(peerOptions(peers));
        serverOpts.putAll(remainingServerOpts);

        List<String> command = new ArrayList<>();
        command.addAll(getTypeDBBinary());
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

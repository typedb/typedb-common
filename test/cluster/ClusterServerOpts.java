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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ClusterServerOpts {
    private static final String OPT_ADDR = "--server.address";
    private static final String OPT_INTERNAL_ADDR_ZMQ = "--server.internal-address.zeromq";
    private static final String OPT_INTERNAL_ADDR_GRPC = "--server.internal-address.grpc";
    private static final Pattern OPT_PEERS_PATTERN = Pattern.compile("--server.peers.(.+).*$");
    private static final String OPT_PEERS_ADDR = "--server.peers.server-peer-%d.address";
    private static final String OPT_PEERS_INTERNAL_ADDR_ZMQ = "--server.peers.server-peer-%d.internal-address.zeromq";
    private static final String OPT_PEERS_INTERNAL_ADDR_GRPC = "--server.peers.server-peer-%d.internal-address.grpc";
    static final String STORAGE_DATA = "--storage.data";
    static final String STORAGE_REPLICATION = "--storage.replication";
    static final String STORAGE_USER = "--storage.user";
    static final String LOG_OUTPUT_FILE_DIRECTORY = "--log.output.file.directory";

    static Addresses addressOpt(Map<String, String> options) {
        return Addresses.create(options.get(OPT_ADDR), options.get(OPT_INTERNAL_ADDR_ZMQ), options.get(OPT_INTERNAL_ADDR_GRPC));
    }

    static Map<String, String> addressOpt(Addresses addresses) {
        Map<String, String> options = new HashMap<>();
        options.put(OPT_ADDR, addresses.external().toString());
        options.put(OPT_INTERNAL_ADDR_ZMQ, addresses.internalZMQ().toString());
        options.put(OPT_INTERNAL_ADDR_GRPC, addresses.internalGRPC().toString());
        return options;
    }

    static Set<Addresses> peersOpt(Map<String, String> options) {
        Set<String> names = new HashSet<>();
        for (String opt : options.keySet()) {
            Matcher nameMatcher = OPT_PEERS_PATTERN.matcher(opt);
            if (nameMatcher.find()) {
                names.add(nameMatcher.group(1));
            }
        }
        Set<Addresses> peers = new HashSet<>();
        for (String name : names) {
            Addresses peer = Addresses.create(
                    options.get("--server.peers." + name + ".address"),
                    options.get("--server.peers." + name + ".internal-address.zeromq"),
                    options.get("--server.peers." + name + ".internal-address.grpc")
            );
            peers.add(peer);
        }
        return peers;
    }

    static Map<String, String> peersOpt(Set<Addresses> peers) {
        Map<String, String> options = new HashMap<>();
        int index = 0;
        for (Addresses peer : peers) {
            String addrKey = String.format(OPT_PEERS_ADDR, index);
            String intAddrZMQKey = String.format(OPT_PEERS_INTERNAL_ADDR_ZMQ, index);
            String intAddrGRPCKey = String.format(OPT_PEERS_INTERNAL_ADDR_GRPC, index);
            options.put(addrKey, peer.external().toString());
            options.put(intAddrZMQKey, peer.internalZMQ().toString());
            options.put(intAddrGRPCKey, peer.internalGRPC().toString());
            index++;
        }
        return options;
    }

    static Path storageDataOpt(Map<String, String> options) {
        return Paths.get(options.get(STORAGE_DATA));
    }

    static Path logOutputOpt(Map<String, String> options) {
        return Paths.get(options.get(LOG_OUTPUT_FILE_DIRECTORY));
    }
}

/*
 * Copyright (C) 2021 Grakn Labs
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

package grakn.common.test.server;

import grakn.common.collection.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static grakn.common.collection.Collections.list;
import static grakn.common.collection.Collections.pair;

public class GraknClusterRunner extends GraknRunner {

    private final Pair<Integer, Integer> ports;
    private final List<Pair<Integer, Integer>> peerPorts;

    public GraknClusterRunner(Pair<Integer, Integer> ports, List<Pair<Integer, Integer>> peerPorts) throws InterruptedException, TimeoutException, IOException {
        super();
        this.ports = ports;
        this.peerPorts = peerPorts;
    }

    public GraknClusterRunner(Integer port) throws InterruptedException, TimeoutException, IOException {
        this(pair(port, port + 1), list(pair(port, port + 1)));
    }

    public GraknClusterRunner() throws InterruptedException, TimeoutException, IOException {
        this(ThreadLocalRandom.current().nextInt(40000, 60000));
    }

    @Override
    protected String name() {
        return "Grakn Cluster";
    }

    @Override
    protected int port() {
        return ports.first();
    }

    @Override
    protected List<String> command() {
        List<String> command = new ArrayList<>();
        command.addAll(getGraknBinary());
        command.add("server");
        command.add("--address");
        command.add(getAddressString(ports));
        peerPorts.forEach(peerPort -> {
            command.add("--peer");
            command.add(getAddressString(peerPort));
        });
        command.add("--data");
        command.add(dataDir.toAbsolutePath().toString());
        return command;
    }

    private String getAddressString(Pair<Integer, Integer> ports) {
        return "127.0.0.1" + ":" + ports.first() + ":" + ports.second();
    }
}

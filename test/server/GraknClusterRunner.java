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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GraknClusterRunner extends GraknRunner {

    public GraknClusterRunner() throws InterruptedException, TimeoutException, IOException {
        this(false);
    }

    public GraknClusterRunner(boolean debug) throws InterruptedException, TimeoutException, IOException {
        super(debug);
    }

    @Override
    protected String name() {
        return "Grakn Cluster";
    }

    @Override
    List<String> command() {
        List<String> command = new ArrayList<>();
        command.addAll(getGraknBinary());
        command.add("server");
        if (debug) {
            command.add("--debug");
        }
        command.add("--address");
        command.add(host() + ":" + port() + ":" + serverPort());
        command.add("--data");
        command.add(dataDir.toAbsolutePath().toString());
        return command;
    }

    private int serverPort() {
        return port() + 1;
    }
}

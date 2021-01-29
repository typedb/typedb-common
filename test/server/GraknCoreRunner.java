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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GraknCoreRunner extends GraknRunnerBase {

    public GraknCoreRunner() throws InterruptedException, TimeoutException, IOException {
        super();
    }

    public GraknCoreRunner(boolean debug) throws InterruptedException, TimeoutException, IOException {
        super(debug);
    }

    public GraknCoreRunner(File distributionFile, boolean debug) throws InterruptedException, TimeoutException, IOException {
        super(distributionFile, debug);
    }

    @Override
    String name() {
        return "Grakn Core";
    }

    @Override
    List<String> command() {
        List<String> command = new ArrayList<>();
        command.add("./grakn");
        command.add("server");
        if (debug) {
            command.add("--debug");
        }
        command.add("--port");
        command.add(Integer.toString(port));
        command.add("--data");
        command.add(dataDir.toAbsolutePath().toString());

        return command;
    }
}

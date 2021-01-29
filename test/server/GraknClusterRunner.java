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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GraknClusterRunner extends GraknRunnerBase {

    public static final String NAME = "Grakn Cluster";

    public GraknClusterRunner() throws InterruptedException, TimeoutException, IOException {
        super("Grakn Cluster");
    }

    public GraknClusterRunner(File distributionFile) throws InterruptedException, TimeoutException, IOException {
        super(NAME, distributionFile, false);
    }

    @Override
    List<String> command() {
        return Arrays.asList(
                "./grakn",
                "server",
                "--address", address() + ":" + serverPort(),
                "--data", tmpDir.toAbsolutePath().toString()
        );
    }

    private int serverPort() {
        return port() + 1;
    }
}

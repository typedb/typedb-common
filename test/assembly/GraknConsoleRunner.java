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

package grakn.common.test.assembly;

import org.zeroturnaround.exec.StartedProcess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GraknConsoleRunner extends GraknRunner {

    public GraknConsoleRunner() throws InterruptedException, TimeoutException, IOException {
        super();
    }

    public int run(String address, boolean isCluster, Path scriptFile) {
        try {
            StartedProcess consoleProcess = executor.command(command(address, isCluster, scriptFile)).start();
            return consoleProcess.getProcess().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> command(String address, boolean isCluster, Path scriptFile) {
        List<String> command = new ArrayList<>();
        command.addAll(getGraknBinary());
        command.add("console");
        command.add(isCluster ? "--cluster" : "--server");
        command.add(address);
        command.add("--script");
        command.add(scriptFile.toAbsolutePath().toString());
        return command;
    }

    @Override
    String name() {
        return "Grakn Console";
    }
}

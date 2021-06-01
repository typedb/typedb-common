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

package com.vaticle.typedb.common.test.console;

import com.vaticle.typedb.common.test.Runner;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class TypeDBConsoleRunner extends Runner {

    public TypeDBConsoleRunner() throws InterruptedException, TimeoutException, IOException {
        super();
    }

    public int run(String address, boolean isCluster, Path scriptFile) {
        return run(isCluster ? "--cluster" : "--server", address, "--script", scriptFile.toAbsolutePath().toString());
    }

    public int run(String address, boolean isCluster, String... consoleCommands) {
        List<String> options = new ArrayList<>();
        options.add(isCluster ? "--cluster" : "--server");
        options.add(address);
        for (String commandString : consoleCommands) {
            options.add("--command");
            options.add(commandString);
        }
        return run(options.toArray(new String[] {}));
    }

    public int run(String... options) {
        try {
            StartedProcess consoleProcess = executor.command(command(options)).start();
            return consoleProcess.getProcess().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> command(String... options) {
        List<String> command = new ArrayList<>();
        command.addAll(getTypeDBBinary());
        command.add("console");
        command.addAll(Arrays.asList(options));
        return command;
    }

    @Override
    protected File distributionArchive() {
        String[] args = System.getProperty("sun.java.command").split(" ");
        assert args.length > 2;
        return new File(args[2]);
    }

    @Override
    protected String name() {
        return "TypeDB Console";
    }
}

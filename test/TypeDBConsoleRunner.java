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

package com.vaticle.typedb.common.test;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class TypeDBConsoleRunner {

    protected final Path distribution;
    protected ProcessExecutor executor;

    public TypeDBConsoleRunner() throws InterruptedException, TimeoutException, IOException {
        System.out.println("Constructing " + name() + " runner");
        File archive = archive();
        if (!archive.exists()) {
            throw new IllegalArgumentException("Distribution archive missing: " + archive.getAbsolutePath());
        }
        distribution = RunnerUtil.distributionSetup(name(), archive);
        executor = new ProcessExecutor()
                .directory(distribution.toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        System.out.println(name() + " runner constructed");
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
        command.addAll(RunnerUtil.bin());
        command.add("console");
        command.addAll(Arrays.asList(options));
        return command;
    }

    private File archive() {
        String[] args = System.getProperty("sun.java.command").split(" ");
        assert args.length > 2;
        return new File(args[2]);
    }

    private String name() {
        return "TypeDB Console";
    }
}

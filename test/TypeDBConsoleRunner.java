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
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.vaticle.typedb.common.collection.Collections.list;

public class TypeDBConsoleRunner {

    protected final Path distribution;
    protected ProcessExecutor executor;

    public TypeDBConsoleRunner() throws InterruptedException, TimeoutException, IOException {
        System.out.println("Constructing " + name() + " runner");
        File archive = archive();
        if (!archive.exists()) {
            throw new IllegalArgumentException("Distribution archive missing: " + archive.getAbsolutePath());
        }
        System.out.println("Extracting " + name() + " distribution archive.");
        distribution = RunnerUtil.unarchive(archive);
        System.out.println(name() + " distribution archive extracted.");
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
        List<String> cmd = list((List<String>) list("console"), list(options));
        return RunnerUtil.typeDBCommand(cmd);
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

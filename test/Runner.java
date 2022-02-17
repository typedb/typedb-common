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

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;

public abstract class Runner {

    private static final String TAR_GZ = ".tar.gz";
    private static final String ZIP = ".zip";

    protected final Path rootPath;
    protected ProcessExecutor executor;

    public Runner() throws InterruptedException, TimeoutException, IOException {
        System.out.println("Constructing a " + name() + " runner");

        File distributionArchive = distributionArchive();
        if (!distributionArchive.exists()) {
            throw new IllegalArgumentException("TypeDB distribution file is missing from " + distributionArchive.getAbsolutePath());
        }

        checkAndDeleteExistingDistribution();

        String distributionArchiveFormat = distributionFormat(distributionArchive);
        Path distributionDir = distributionTarget();
        executor = new ProcessExecutor()
                .directory(Paths.get(".").toAbsolutePath().toFile())
                .redirectOutput(System.out)
                .redirectError(System.err)
                .readOutput(true)
                .destroyOnExit();
        rootPath = distributionSetup(distributionDir, distributionArchiveFormat, distributionArchive);
        System.out.println(name() + " runner constructed");
    }

    protected abstract String name();

    protected abstract File distributionArchive();

    private String distributionFormat(File distributionFile) {
        if (distributionFile.toString().endsWith(TAR_GZ)) {
            return TAR_GZ;
        } else if (distributionFile.toString().endsWith(ZIP)) {
            return ZIP;
        } else {
            fail(String.format("Distribution file format should either be %s or %s", TAR_GZ, ZIP));
        }
        return "";
    }

    private Path distributionTarget() throws IOException {
        return Files.createTempDirectory("typedb");
    }

    private void checkAndDeleteExistingDistribution() throws IOException {
        Path target = distributionTarget();
        System.out.println("Checking for existing distribution at " + target.toAbsolutePath());
        if (target.toFile().exists()) {
            System.out.println("An existing distribution found. Deleting...");
            FileUtils.deleteDirectory(target.toFile());
            System.out.println("Existing distribution deleted");
        } else {
            System.out.println("There is no existing distribution");
        }
    }

    private Path distributionSetup(Path distributionDir, String distributionArchiveFormat, File distributionArchive) throws IOException, TimeoutException, InterruptedException {
        System.out.println("Unarchiving " + name() + " distribution");
        Files.createDirectories(distributionDir);
        if (distributionArchiveFormat.equals(TAR_GZ)) {
            executor.command("tar", "-xf", distributionArchive.toString(),
                             "-C", distributionDir.toString()).execute();
        } else {
            executor.command("unzip", "-q", distributionArchive.toString(),
                             "-d", distributionDir.toString()).execute();
        }
        // The TypeDB Cluster archive extracts to a folder inside TYPEDB_TARGET_DIRECTORY named
        // typedb-server-{platform}-{version}. We know it's the only folder, so we can retrieve it using Files.list.
        final Path typeDBPath = Files.list(distributionDir).findFirst().get();
        executor = executor.directory(typeDBPath.toAbsolutePath().toFile());
        System.out.println(name() + " distribution unarchived");
        return typeDBPath;
    }

    protected List<String> getTypeDBBinary() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? Arrays.asList("cmd.exe", "/c", "typedb.bat") : Collections.singletonList("typedb");
    }
}

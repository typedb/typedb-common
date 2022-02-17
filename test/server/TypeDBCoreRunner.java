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

package com.vaticle.typedb.common.test.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

public class TypeDBCoreRunner extends TypeDBRunner {

    private final int port;

    public TypeDBCoreRunner() throws InterruptedException, TimeoutException, IOException {
        super();
        this.port = ThreadLocalRandom.current().nextInt(40000, 60000);
    }

    @Override
    protected String name() {
        return "TypeDB Core";
    }

    @Override
    protected int port() {
        return port;
    }

    @Override
    protected void verifyPortUnused() {
        verifyPortUnused(port());
    }

    @Override
    protected List<String> command() {
        List<String> command = new ArrayList<>();
        command.addAll(getTypeDBBinary());
        command.add("server");
        command.add("--server.address");
        command.add("0.0.0.0:" + port);
        command.add("--storage.data");
        command.add(dataDir.toAbsolutePath().toString());
        return command;
    }
}

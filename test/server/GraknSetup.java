/*
 * Copyright (C) 2020 Grakn Labs
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
 */

package grakn.common.test.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface GraknSetup {

    static GraknSetup bootup() throws InterruptedException, TimeoutException, IOException {
        return GraknCoreSetup.bootup();
    }

    static GraknSetup bootup(File distributionFile) throws InterruptedException, TimeoutException, IOException {
        return GraknCoreSetup.bootup(distributionFile);
    }

    static GraknSetup instance() {
        return GraknCoreSetup.instance();
    }

    static void shutdown() throws InterruptedException, IOException, TimeoutException {
        GraknCoreSetup.shutdown();
    }

    String host();

    int port();

    String address();

    void start() throws InterruptedException, IOException, TimeoutException;

    void stop() throws InterruptedException, IOException, TimeoutException;
}

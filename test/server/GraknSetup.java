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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class GraknSetup {

    private static GraknRunner graknRunner;

    public static void setGraknRunner(GraknRunner instance) {
        graknRunner = instance;
    }

    public static GraknRunner getGraknRunner() {
        return graknRunner;
    }

    public static GraknCoreDistributionRunner usingCore() throws InterruptedException, IOException, TimeoutException {
        GraknCoreDistributionRunner coreRunner = new GraknCoreDistributionRunner();
        setGraknRunner(coreRunner);
        return coreRunner;
    }

    public static void bootup() throws Exception {
        if (graknRunner != null) {
            graknRunner.start();
        } else {
            throw new IllegalStateException("No GraknRunner setup");
        }
    }

    public static void shutdown() throws Exception {
        if (graknRunner != null) {
            graknRunner.stop();
        } else {
            throw new IllegalStateException("No GraknRunner setup");
        }
    }
}

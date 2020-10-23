package grakn.cluster.execution.eventloop;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class GlobalSystem {
    private static Supplier<Long> getTime = () -> java.lang.System.currentTimeMillis();
    private static Random random = ThreadLocalRandom.current();

    private GlobalSystem() {}

    public static void set(Supplier<Long> getTime, Random random) {
        GlobalSystem.getTime = getTime;
        GlobalSystem.random = random;
    }

    public static long time() {
        return getTime.get();
    }

    public static Random random() {
        return random;
    }
}

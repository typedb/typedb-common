package grakn.common.test.server;

public interface GraknRunner {

    String host();

    int port();

    String address();

    void start() throws Exception;

    void stop() throws Exception;
}

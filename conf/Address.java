package com.vaticle.typedb.common.conf;

import java.net.InetSocketAddress;
import java.util.Objects;

import static java.lang.Integer.parseInt;

public class Address {

    private final String externalHost;
    private final int externalPort;
    private final String internalHostZMQ;
    private final int internalPortZMQ;
    private final String internalHostGRPC;
    private final int internalPortGRPC;

    // 127.0.0.1:1729,127.0.0.1:1730
    // localhost:1729:1730
    public static Address parse(String address) {
        String[] s1 = address.split(",");
        if (s1.length == 1) {
            String[] s2 = address.split(":");
            return new Address(s2[0], parseInt(s2[1]), s2[0], parseInt(s2[2]), s2[0], parseInt(s2[3]));
        } else if (s1.length == 3) {
            String[] externalAddress = s1[0].split(":");
            if (externalAddress.length != 2) throw new IllegalArgumentException("Illegal argument provided: " + address);

            String[] internalZMQAddress = s1[1].split(":");
            if (internalZMQAddress.length != 2) throw new IllegalArgumentException("Illegal argument provided: " + address);

            String[] internalGRPCAddress = s1[2].split(":");
            if (internalGRPCAddress.length != 2) throw new IllegalArgumentException("Illegal argument provided: " + address);

            return new Address(
                    externalAddress[0], parseInt(externalAddress[1]),
                    internalZMQAddress[0], parseInt(internalZMQAddress[1]),
                    internalGRPCAddress[0], parseInt(internalGRPCAddress[1])
            );
        } else throw new IllegalArgumentException("Illegal argument provided: " + address);
    }

    public Address(InetSocketAddress address, InetSocketAddress zeromq, InetSocketAddress grpc) {
        this(address.getHostString(), address.getPort(), zeromq.getHostString(), zeromq.getPort(), grpc.getHostString(), grpc.getPort());
    }

    public Address(String host, int externalPort, int internalPortZMQ, int internalPortGRPC) {
        this(host, externalPort, host, internalPortZMQ, host, internalPortGRPC);
    }

    public Address(String externalHost, int externalPort, String internalHostZMQ, int internalPortZMQ, String internalHostGRPC, int internalPortGRPC) {
        this.externalHost = externalHost;
        this.externalPort = externalPort;
        this.internalHostZMQ = internalHostZMQ;
        this.internalPortZMQ = internalPortZMQ;
        this.internalHostGRPC = internalHostGRPC;
        this.internalPortGRPC = internalPortGRPC;
    }

    public String external() {
        return externalHost + ":" + externalPort;
    }

    public int externalPort() {
        return externalPort;
    }

    public String internalZMQ() {
        return internalHostZMQ + ":" + internalPortZMQ;
    }

    public int internalPortZMQ() {
        return internalPortZMQ;
    }

    public String internalGRPC() {
        return internalHostGRPC + ":" + internalPortGRPC;
    }

    public String internalHostGRPC() {
        return internalHostGRPC;
    }

    public int internalPortGRPC() {
        return internalPortGRPC;
    }

    @Override
    public String toString() {
        return external() + "," + internalZMQ() + "," + internalGRPC();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return externalPort == address.externalPort &&
                internalPortZMQ == address.internalPortZMQ &&
                internalPortGRPC == address.internalPortGRPC &&
                Objects.equals(externalHost, address.externalHost) &&
                Objects.equals(internalHostZMQ, address.internalHostZMQ) &&
                Objects.equals(internalHostGRPC, address.internalHostGRPC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalHost, externalPort, internalHostZMQ, internalPortZMQ, internalHostGRPC, internalPortGRPC);
    }
}

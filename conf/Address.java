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

package com.vaticle.typedb.common.conf;

import java.net.InetSocketAddress;
import java.util.Objects;

import static java.lang.Integer.parseInt;

public class Address {

    private final InetSocketAddress external;
    private final InetSocketAddress internalZMQ;
    private final InetSocketAddress internalGRPC;

    public static Address create(String external, String internalZMQ, String internalGRPC) {
        String[] ext = external.split(":");
        String[] intZMQ = internalZMQ.split(":");
        String[] intGRPC = internalGRPC.split(":");
        return Address.create(ext[0], parseInt(ext[1]), intZMQ[0], parseInt(intZMQ[1]), intGRPC[0], parseInt(intGRPC[1]));
    }

    public static Address create(
            String externalHost,
            int externalPort,
            String internalHostZMQ,
            int internalPortZMQ,
            String internalHostGRPC,
            int internalPortGRPC
    ) {
        return create(
                new InetSocketAddress(externalHost, externalPort),
                new InetSocketAddress(internalHostZMQ, internalPortZMQ),
                new InetSocketAddress(internalHostGRPC, internalPortGRPC)
        );
    }

    public static Address create(InetSocketAddress external, InetSocketAddress internalZMQ, InetSocketAddress internalGRPC) {
        return new Address(external, internalZMQ, internalGRPC);
    }

    private Address(InetSocketAddress external, InetSocketAddress internalZMQ, InetSocketAddress internalGRPC) {
        this.external = external;
        this.internalZMQ = internalZMQ;
        this.internalGRPC = internalGRPC;
    }

    public InetSocketAddress external() {
        return external;
    }

    public InetSocketAddress internalZMQ() {
        return internalZMQ;
    }

    public InetSocketAddress internalGRPC() {
        return internalGRPC;
    }

    @Override
    public String toString() {
        return "Address(external=" + external + ", internalZMQ=" + internalZMQ + "internalGRPC=" + internalGRPC + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(external, address.external) && Objects.equals(internalZMQ, address.internalZMQ) && Objects.equals(internalGRPC, address.internalGRPC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(external, internalZMQ, internalGRPC);
    }
}

package com.infilos.utils;

import java.net.*;
import java.util.Enumeration;

/**
 * @author zhiguang.zhang on 2020-12-09.
 */

public final class Network {
    private Network() {}

    public static final String Localhost = localhost();

    private static String localhost() {
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface net = netInterfaces.nextElement();
                Enumeration<InetAddress> address = net.getInetAddresses();
                while (address.hasMoreElements()) {
                    InetAddress ip = address.nextElement();
                    if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                        && !ip.getHostAddress().contains(":")) {
                        return ip.getHostAddress();
                    }
                }
            }
            return "127.0.0.1";
        } catch (SocketException e) {
            e.printStackTrace();
            return "127.0.0.1";
        }
    }
}

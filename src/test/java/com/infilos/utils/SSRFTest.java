package com.infilos.utils;

import com.google.common.net.InetAddresses;
import inet.ipaddr.*;
import org.junit.Assert;
import org.junit.Test;

import java.net.*;
import java.util.*;

public class SSRFTest extends Assert {

    private static final List<String> ALLOWED = Collections.singletonList("http://allowed.com");
    private static final List<String> BLOCKED = Collections.singletonList("http://blocked.com");

    @Test
    public void test() {
        assertTrue(Security.checkSSRF("http://allowed.com", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("http://allowed.com:80", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("http://allowed.com:443", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("http://allowed.com:8080", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("http://example.com", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("https://example.com", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("http://baidu.com", ALLOWED, BLOCKED, true));
        assertTrue(Security.checkSSRF("http://www.baidu.com", ALLOWED, BLOCKED, true));

        mustThrow("blocked", () -> Security.checkSSRF("http://blocked.com", ALLOWED, BLOCKED, true));
        mustThrow("not valid", () -> Security.checkSSRF("invalid.com", ALLOWED, BLOCKED, true));
        mustThrow("HTTP/HTTPS", () -> Security.checkSSRF("file://example", ALLOWED, BLOCKED, true));

        mustThrow("legal host", () -> Security.checkSSRF("http://14256", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://localhost", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://10.0.0.1", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://0x7f.0x0.0x0.0x1", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://[::]:80", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://0.0.0.0:80", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://0177.0.0.1/", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://0177.0.0.0x1/", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://2130706433/", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://16373751032", ALLOWED, BLOCKED, true));
        mustThrow("legal host", () -> Security.checkSSRF("http://example.com@127.0.0.1", ALLOWED, BLOCKED, true));

        mustThrow("legal port", () -> Security.checkSSRF("http://example.com:3344", ALLOWED, BLOCKED, true));
        mustThrow("legal port", () -> Security.checkSSRF("https://security.tuya-inc.top:7799", ALLOWED, BLOCKED, true));
        mustThrow("legal port", () -> Security.checkSSRF("https://dome.tuya-inc.top:7799/#/order/myorder/list", ALLOWED, BLOCKED, true));

        assertTrue(InetAddresses.isInetAddress(Security.clearIPV6Host("[fe80::42:b9ff:fe75:769e]")));
        assertTrue(InetAddresses.isInetAddress(Security.clearIPV6Host("fe80::42:b9ff:fe75:769e]")));
        assertTrue(InetAddresses.isInetAddress(Security.clearIPV6Host("[fe80::42:b9ff:fe75:769e")));

        assertFalse(Security.isInternalHost("9.0.0.0"));
        assertTrue(Security.isInternalHost("10.0.0.1"));
        assertFalse(Security.isInternalHost("11.0.0.0"));

        assertFalse(Security.isInternalHost("172.15.0.0"));
        assertTrue(Security.isInternalHost("172.16.0.1"));
        assertFalse(Security.isInternalHost("172.32.255.255"));

        assertFalse(Security.isInternalHost("192.169.0.0"));
        assertTrue(Security.isInternalHost("192.168.0.1"));
        assertFalse(Security.isInternalHost("192.169.255.255"));

        assertFalse(Security.isInternalHost("65.49.206.246"));
    }

    private void mustThrow(String identifier, Runnable runnable) {
        try {
            runnable.run();
            fail("check not work");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(identifier));
        }
    }

    static final class Security {
        private Security() {
        }

        private static final Set<Integer> COMMON_PORTS = new HashSet<Integer>(){{
            add(80);
            add(443);
            add(8080);
        }};

        private static final IPAddressSeqRange A_CLASS_RANGE = new IPAddressString("10.0.0.0").getAddress().toSequentialRange(new IPAddressString("10.255.255.255").getAddress());
        private static final IPAddressSeqRange B_CLASS_RANGE = new IPAddressString("172.16.0.0").getAddress().toSequentialRange(new IPAddressString("172.31.255.255").getAddress());
        private static final IPAddressSeqRange C_CLASS_RANGE = new IPAddressString("192.168.0.0").getAddress().toSequentialRange(new IPAddressString("192.168.255.255").getAddress());


        public static boolean checkSSRF(String urlStr, Collection<String> allowed, Collection<String> blocked, boolean interrupt) {
            if (allowed.contains(urlStr)) {
                return true;
            }
            if (blocked.contains(urlStr)) {
                return throwOrReturn(interrupt, new IllegalArgumentException("URL is blocked"), false);
            }

            URL url;
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                return throwOrReturn(interrupt, new IllegalArgumentException("URL is not valid", e), false);
            }

            String protocol = url.getProtocol();
            if (!Strings.equals(protocol, "http") && !Strings.equals(protocol, "https")) {
                return throwOrReturn(interrupt, new IllegalArgumentException("URL scheme must be HTTP/HTTPS"), false);
            }

            String host = url.getHost();
            if (isIllegalInetAddress(urlStr, host)) {
                return throwOrReturn(interrupt, new IllegalArgumentException("URL host must be legal host"), false);
            }

            int port = url.getPort();
            if (-1 != port && !COMMON_PORTS.contains(port)) {
                return throwOrReturn(interrupt, new IllegalArgumentException("URL port must be legal port"), false);
            }

            return true;
        }

        private static <T> T throwOrReturn(boolean interrupt, RuntimeException error, T value) {
            if (interrupt) {
                throw error;
            } else {
                return value;
            }
        }

        private static boolean isIllegalInetAddress(String url, String host) {
            // numeric host
            if (Numbers.isNumber(host)) {
                return true;
            }

            // http basic auth
            if (url.charAt(url.indexOf(host) - 1) == '@') {
                return true;
            }

            // local host
            if (isLocalhost(host)) {
                return true;
            }

            // ip address
            if (InetAddresses.isInetAddress(host)) {
                return true;
            }

            // ip address of ipv6, eg. [...]
            if (host.startsWith("[") || host.endsWith("]")) {
                if (InetAddresses.isInetAddress(clearIPV6Host(host))) {
                    return true;
                }
            }

            // internal ip address
            try {
                java.net.InetAddress targetInetAddress = java.net.InetAddress.getByName(host);
                String targetHost = targetInetAddress.getHostAddress();

                if (targetHost != null && (isLocalhost(targetHost) || isInternalHost(targetHost))) {
                    return true;
                }
            } catch (UnknownHostException ignore) {
                return true;
            }

            return false;
        }

        static boolean isLocalhost(String host) {
            return Strings.equals("localhost", host)
                || Strings.equals("127.0.0.1", host)
                || Strings.equals("0177.0.0.1", host)
                || Strings.equals("0.0.0.0", host)
                || Strings.equals("[::]", host);
        }

        static boolean isInternalHost(String ip) {
            IPAddress ipAddress = new IPAddressString(ip).getAddress();

            return A_CLASS_RANGE.contains(ipAddress) ||
                B_CLASS_RANGE.contains(ipAddress) ||
                C_CLASS_RANGE.contains(ipAddress);
        }

        static String clearIPV6Host(String host) {
            String clear = host;
            if (clear.startsWith("[")) {
                clear = clear.substring(1);
            }
            if (clear.endsWith("]")) {
                clear = clear.substring(0, clear.length() - 1);
            }

            return clear;
        }
    }
}

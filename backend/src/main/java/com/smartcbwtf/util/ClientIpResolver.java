package com.smartcbwtf.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ClientIpResolver {
    private static final int MAX_IP_LENGTH = 64;

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String remoteAddr = cleanLine(request.getRemoteAddr());
        if (isTrustedProxyAddress(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (!isBlank(forwardedFor)) {
                return truncate(cleanLine(forwardedFor.split(",")[0]), MAX_IP_LENGTH);
            }

            String realIp = request.getHeader("X-Real-IP");
            if (!isBlank(realIp)) {
                return truncate(cleanLine(realIp), MAX_IP_LENGTH);
            }
        }

        return isBlank(remoteAddr) ? "unknown" : truncate(remoteAddr, MAX_IP_LENGTH);
    }

    static boolean isTrustedProxyAddress(String remoteAddr) {
        if (isBlank(remoteAddr)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddr);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || isUniqueLocalIpv6(address);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private static String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

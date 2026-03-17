package com.sipserver.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 网络工具类
 */
public class NetworkUtil {
    
    private static final String TAG = "NetworkUtil";
    
    /**
     * 获取本地IP地址
     * 优先返回WiFi网络IP
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                // 优先返回wlan接口
                String interfaceName = networkInterface.getName();
                if (interfaceName.startsWith("wlan") || interfaceName.startsWith("eth")) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        
                        // 只要IPv4地址
                        if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
            
            // 如果没找到wlan接口，返回任意可用IP
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) {
                        return address.getHostAddress();
                    }
                }
            }
            
        } catch (SocketException e) {
            LogUtil.e(TAG, "获取本地IP失败: " + e.getMessage(), e);
        }
        
        return "127.0.0.1";
    }
    
    /**
     * 检查是否为有效IP地址
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查号码是否为内部号码（4位数字）
     */
    public static boolean isInternalNumber(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }
        
        return number.matches("^\\d{4}$");
    }
    
    /**
     * 检查号码是否为外部号码（5位及以上）
     */
    public static boolean isExternalNumber(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }
        
        return number.matches("^\\d{5,}$");
    }
}

package com.sipserver.model;

/**
 * 客户端信息模型
 * 存储已注册客户端的详细信息
 */
public class ClientInfo {
    
    // 客户端号码（4位数字）
    private String extension;
    
    // SIP地址（IP:端口）
    private String sipAddress;
    
    // 客户端IP地址
    private String ipAddress;
    
    // 客户端端口
    private int port;
    
    // 注册时间戳
    private long registerTime;
    
    // 最后活跃时间
    private long lastActiveTime;
    
    // 认证用户名
    private String authUsername;
    
    // 认证密码（加密存储）
    private String authPassword;
    
    // 注册状态
    private RegisterStatus status;
    
    // 用户代理
    private String userAgent;
    
    // 联系人地址
    private String contactUri;
    
    // 过期时间（秒）
    private int expires;
    
    /**
     * 注册状态枚举
     */
    public enum RegisterStatus {
        ONLINE,      // 在线
        OFFLINE,     // 离线
        BUSY,        // 忙线
        AWAY         // 离开
    }
    
    // 构造函数
    public ClientInfo() {
        this.registerTime = System.currentTimeMillis();
        this.lastActiveTime = this.registerTime;
        this.status = RegisterStatus.ONLINE;
    }
    
    public ClientInfo(String extension, String ipAddress, int port) {
        this();
        this.extension = extension;
        this.ipAddress = ipAddress;
        this.port = port;
        this.sipAddress = "sip:" + extension + "@" + ipAddress + ":" + port;
    }
    
    // Getters and Setters
    
    public String getExtension() {
        return extension;
    }
    
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    public String getSipAddress() {
        return sipAddress;
    }
    
    public void setSipAddress(String sipAddress) {
        this.sipAddress = sipAddress;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        updateSipAddress();
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
        updateSipAddress();
    }
    
    private void updateSipAddress() {
        if (extension != null && ipAddress != null) {
            this.sipAddress = "sip:" + extension + "@" + ipAddress + ":" + port;
        }
    }
    
    public long getRegisterTime() {
        return registerTime;
    }
    
    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }
    
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    public String getAuthUsername() {
        return authUsername;
    }
    
    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }
    
    public String getAuthPassword() {
        return authPassword;
    }
    
    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }
    
    public RegisterStatus getStatus() {
        return status;
    }
    
    public void setStatus(RegisterStatus status) {
        this.status = status;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getContactUri() {
        return contactUri;
    }
    
    public void setContactUri(String contactUri) {
        this.contactUri = contactUri;
    }
    
    public int getExpires() {
        return expires;
    }
    
    public void setExpires(int expires) {
        this.expires = expires;
    }
    
    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 检查注册是否过期
     */
    public boolean isExpired() {
        if (expires <= 0) {
            return false; // 永不过期
        }
        long elapsed = System.currentTimeMillis() - lastActiveTime;
        return elapsed > (expires * 1000L);
    }
    
    @Override
    public String toString() {
        return "ClientInfo{" +
            "extension='" + extension + '\'' +
            ", ipAddress='" + ipAddress + '\'' +
            ", port=" + port +
            ", status=" + status +
            ", userAgent='" + userAgent + '\'' +
            '}';
    }
}

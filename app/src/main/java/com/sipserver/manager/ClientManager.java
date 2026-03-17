package com.sipserver.manager;

import com.sipserver.model.ClientInfo;
import com.sipserver.util.LogUtil;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端管理器
 * 管理已注册的SIP客户端
 */
public class ClientManager {
    
    private static final String TAG = "ClientManager";
    
    private static ClientManager instance;
    
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    
    private ClientManager() {
    }
    
    public static synchronized ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }
    
    public void registerClient(ClientInfo client) {
        String extension = client.getExtension();
        
        ClientInfo existing = clients.get(extension);
        if (existing != null) {
            client.setRegisterTime(existing.getRegisterTime());
            LogUtil.i(TAG, "客户端重新注册: " + extension);
        } else {
            LogUtil.i(TAG, "新客户端注册: " + extension);
        }
        
        client.updateActiveTime();
        clients.put(extension, client);
    }
    
    public void unregisterClient(String extension) {
        ClientInfo removed = clients.remove(extension);
        if (removed != null) {
            LogUtil.i(TAG, "客户端注销: " + extension);
        }
    }
    
    public ClientInfo getClient(String extension) {
        ClientInfo client = clients.get(extension);
        if (client != null) {
            if (client.isExpired()) {
                clients.remove(extension);
                LogUtil.i(TAG, "客户端注册已过期: " + extension);
                return null;
            }
            client.updateActiveTime();
        }
        return client;
    }
    
    public Collection<ClientInfo> getAllClients() {
        return clients.values();
    }
    
    public int getClientCount() {
        return clients.size();
    }
    
    public void setClientStatus(String extension, ClientInfo.RegisterStatus status) {
        ClientInfo client = clients.get(extension);
        if (client != null) {
            client.setStatus(status);
            LogUtil.i(TAG, "客户端状态更新: " + extension + " -> " + status);
        }
    }
    
    public void checkExpiredClients() {
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            if (entry.getValue().isExpired()) {
                clients.remove(entry.getKey());
                LogUtil.i(TAG, "移除过期客户端: " + entry.getKey());
            }
        }
    }
    
    public void clear() {
        clients.clear();
        LogUtil.i(TAG, "所有客户端已清除");
    }
}

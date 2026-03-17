package com.sipserver.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import com.sipserver.model.SimCardInfo;
import com.sipserver.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SIP配置管理器
 */
public class SIPConfigManager {
    
    private static final String TAG = "SIPConfigManager";
    private static final String PREF_NAME = "sip_server_config";
    
    private static SIPConfigManager instance;
    private SharedPreferences preferences;
    private Context context;
    
    // SIP服务配置
    private int sipPort = 5060;
    private String sipDomain = "sip.local";
    private String localIp;
    
    // SIM卡配置
    private Map<Integer, SimCardInfo> simCards = new HashMap<>();
    
    // 默认外呼SIM卡（0或1）
    private int defaultSimSlot = 0;
    
    private SIPConfigManager() {
    }
    
    public static synchronized SIPConfigManager getInstance() {
        if (instance == null) {
            instance = new SIPConfigManager();
        }
        return instance;
    }
    
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        loadConfig();
        detectSimCards();
    }
    
    private void loadConfig() {
        sipPort = preferences.getInt("sip_port", 5060);
        sipDomain = preferences.getString("sip_domain", "sip.local");
        defaultSimSlot = preferences.getInt("default_sim_slot", 0);
        
        LogUtil.i(TAG, "配置已加载: 端口=" + sipPort + ", 域=" + sipDomain);
    }
    
    private void detectSimCards() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        
        if (tm == null) {
            LogUtil.w(TAG, "无法获取TelephonyManager");
            return;
        }
        
        // 检测双卡（需要API 22+）
        for (int slotId = 0; slotId < 2; slotId++) {
            SimCardInfo simInfo = new SimCardInfo(slotId);
            
            try {
                // 获取运营商名称
                String carrierName = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    carrierName = tm.createForSubscriptionId(
                        android.telephony.SubscriptionManager.getActiveSubscriptionIdForSimSlotIndex(slotId)
                    ).getNetworkOperatorName();
                }
                simInfo.setCarrierName(carrierName);
                
                // 获取SIM卡状态
                int state = tm.getSimState(slotId);
                simInfo.setState(convertSimState(state));
                
                // 获取SIM卡号码
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    String number = tm.createForSubscriptionId(
                        android.telephony.SubscriptionManager.getActiveSubscriptionIdForSimSlotIndex(slotId)
                    ).getLine1Number();
                    simInfo.setPhoneNumber(number);
                }
                
            } catch (Exception e) {
                LogUtil.e(TAG, "获取SIM卡" + slotId + "信息失败: " + e.getMessage());
            }
            
            simCards.put(slotId, simInfo);
            LogUtil.i(TAG, "SIM卡" + slotId + ": " + simInfo);
        }
    }
    
    private SimCardInfo.SimState convertSimState(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return SimCardInfo.SimState.ABSENT;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return SimCardInfo.SimState.PIN_REQUIRED;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return SimCardInfo.SimState.PUK_REQUIRED;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return SimCardInfo.SimState.NETWORK_LOCKED;
            case TelephonyManager.SIM_STATE_READY:
                return SimCardInfo.SimState.READY;
            case TelephonyManager.SIM_STATE_NOT_READY:
                return SimCardInfo.SimState.NOT_READY;
            default:
                return SimCardInfo.SimState.UNKNOWN;
        }
    }
    
    // 配置保存方法
    public void saveConfig() {
        preferences.edit()
            .putInt("sip_port", sipPort)
            .putString("sip_domain", sipDomain)
            .putInt("default_sim_slot", defaultSimSlot)
            .apply();
        
        LogUtil.i(TAG, "配置已保存");
    }
    
    // Getters and Setters
    public int getSipPort() {
        return sipPort;
    }
    
    public void setSipPort(int sipPort) {
        this.sipPort = sipPort;
    }
    
    public String getSipDomain() {
        return sipDomain;
    }
    
    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }
    
    public String getLocalIp() {
        return localIp;
    }
    
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }
    
    public int getDefaultSimSlot() {
        return defaultSimSlot;
    }
    
    public void setDefaultSimSlot(int defaultSimSlot) {
        this.defaultSimSlot = defaultSimSlot;
    }
    
    public SimCardInfo getSimCard(int slotId) {
        return simCards.get(slotId);
    }
    
    public List<SimCardInfo> getSimCards() {
        return new ArrayList<>(simCards.values());
    }
    
    public List<SimCardInfo> getReadySimCards() {
        List<SimCardInfo> ready = new ArrayList<>();
        for (SimCardInfo sim : simCards.values()) {
            if (sim.isReady()) {
                ready.add(sim);
            }
        }
        return ready;
    }
}

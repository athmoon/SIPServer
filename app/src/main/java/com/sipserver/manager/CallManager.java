package com.sipserver.manager;

import com.sipserver.model.CallInfo;
import com.sipserver.util.LogUtil;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通话管理器
 * 管理当前通话和历史记录
 */
public class CallManager {
    
    private static final String TAG = "CallManager";
    private static final int MAX_HISTORY_SIZE = 100;
    
    private static CallManager instance;
    
    private final Map<String, CallInfo> activeCalls = new ConcurrentHashMap<>();
    private final Map<String, CallInfo> callHistory = new ConcurrentHashMap<>();
    
    private CallManager() {
    }
    
    public static synchronized CallManager getInstance() {
        if (instance == null) {
            instance = new CallManager();
        }
        return instance;
    }
    
    public void addCall(CallInfo call) {
        activeCalls.put(call.getCallId(), call);
        LogUtil.i(TAG, "通话添加: " + call.getCallId() + " " + call.getCallerNumber() + " -> " + call.getCalleeNumber());
    }
    
    public CallInfo getCall(String callId) {
        return activeCalls.get(callId);
    }
    
    public void removeCall(String callId) {
        CallInfo call = activeCalls.remove(callId);
        if (call != null) {
            call.setStatus(CallInfo.CallStatus.ENDED);
            
            if (callHistory.size() >= MAX_HISTORY_SIZE) {
                String oldestKey = callHistory.keySet().iterator().next();
                callHistory.remove(oldestKey);
            }
            callHistory.put(callId, call);
            
            LogUtil.i(TAG, "通话移除: " + callId + " 时长: " + call.getDuration() + "秒");
        }
    }
    
    public void updateCallStatus(String callId, CallInfo.CallStatus status) {
        CallInfo call = activeCalls.get(callId);
        if (call != null) {
            call.setStatus(status);
            LogUtil.i(TAG, "通话状态更新: " + callId + " -> " + status);
        }
    }
    
    public Collection<CallInfo> getActiveCalls() {
        return activeCalls.values();
    }
    
    public Collection<CallInfo> getCallHistory() {
        return callHistory.values();
    }
    
    public int getActiveCallCount() {
        return activeCalls.size();
    }
    
    public void clear() {
        activeCalls.clear();
        callHistory.clear();
        LogUtil.i(TAG, "所有通话记录已清除");
    }
}

package com.sipserver.broadcast;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import com.sipserver.SIPServerApp;
import com.sipserver.config.SIPConfigManager;
import com.sipserver.manager.CallManager;
import com.sipserver.manager.ClientManager;
import com.sipserver.model.CallInfo;
import com.sipserver.model.ClientInfo;
import com.sipserver.model.SimCardInfo;
import com.sipserver.sip.SIPStack;
import com.sipserver.util.LogUtil;

import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

/**
 * 来电状态接收器
 * 监听SIM卡来电并转发至SIP客户端
 */
public class PhoneStateReceiver extends BroadcastReceiver {
    
    private static final String TAG = "PhoneStateReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            LogUtil.w(TAG, "没有READ_PHONE_STATE权限");
            return;
        }
        
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        
        LogUtil.i(TAG, "来电状态: " + state + " 号码: " + phoneNumber);
        
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            handleIncomingCall(context, phoneNumber);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            handleCallAnswered(context, phoneNumber);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            handleCallEnded(context, phoneNumber);
        }
    }
    
    private void handleIncomingCall(Context context, String phoneNumber) {
        LogUtil.i(TAG, "处理来电: " + phoneNumber);
        
        SimCardInfo simCard = findForwardingSimCard();
        if (simCard == null) {
            LogUtil.w(TAG, "未配置来电转发");
            return;
        }
        
        String targetExtension = simCard.getForwardTarget();
        ClientInfo targetClient = ClientManager.getInstance().getClient(targetExtension);
        
        if (targetClient == null) {
            LogUtil.w(TAG, "转发目标客户端未注册: " + targetExtension);
            return;
        }
        
        forwardCallToSIPClient(context, phoneNumber, targetClient);
    }
    
    private void handleCallAnswered(Context context, String phoneNumber) {
        LogUtil.i(TAG, "通话接通: " + phoneNumber);
    }
    
    private void handleCallEnded(Context context, String phoneNumber) {
        LogUtil.i(TAG, "通话结束: " + phoneNumber);
    }
    
    private SimCardInfo findForwardingSimCard() {
        for (SimCardInfo sim : SIPConfigManager.getInstance().getSimCards()) {
            if (sim.isForwardIncoming()) {
                return sim;
            }
        }
        return null;
    }
    
    private void forwardCallToSIPClient(Context context, String phoneNumber, ClientInfo targetClient) {
        LogUtil.i(TAG, "转发来电到SIP客户端: " + phoneNumber + " -> " + targetClient.getExtension());
        
        // TODO: 实现SIP INVITE发送
        // 需要SIPStack支持作为UAC发起呼叫
    }
}

package com.sipserver.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.sipserver.config.SIPConfigManager;
import com.sipserver.manager.ClientManager;
import com.sipserver.model.ClientInfo;
import com.sipserver.model.SimCardInfo;
import com.sipserver.util.LogUtil;

/**
 * 短信接收器
 * 监听SIM卡短信并转发至SIP客户端
 */
public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SmsReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(action)) {
            return;
        }
        
        LogUtil.i(TAG, "收到短信广播");
        
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }
        
        String format = bundle.getString("format");
        
        StringBuilder messageBody = new StringBuilder();
        String senderNumber = null;
        
        for (Object pdu : pdus) {
            SmsMessage message;
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                message = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                message = SmsMessage.createFromPdu((byte[]) pdu);
            }
            
            if (senderNumber == null) {
                senderNumber = message.getOriginatingAddress();
            }
            messageBody.append(message.getMessageBody());
        }
        
        LogUtil.i(TAG, "短信来自: " + senderNumber + " 内容: " + messageBody);
        
        handleIncomingSms(context, senderNumber, messageBody.toString());
    }
    
    private void handleIncomingSms(Context context, String senderNumber, String messageBody) {
        SimCardInfo simCard = findForwardingSimCard();
        if (simCard == null) {
            LogUtil.w(TAG, "未配置短信转发");
            return;
        }
        
        String targetExtension = simCard.getSmsForwardTarget();
        ClientInfo targetClient = ClientManager.getInstance().getClient(targetExtension);
        
        if (targetClient == null) {
            LogUtil.w(TAG, "短信转发目标客户端未注册: " + targetExtension);
            return;
        }
        
        forwardSmsToSIPClient(context, senderNumber, messageBody, targetClient);
    }
    
    private SimCardInfo findForwardingSimCard() {
        for (SimCardInfo sim : SIPConfigManager.getInstance().getSimCards()) {
            if (sim.isForwardSms()) {
                return sim;
            }
        }
        return null;
    }
    
    private void forwardSmsToSIPClient(Context context, String senderNumber, 
                                      String messageBody, ClientInfo targetClient) {
        LogUtil.i(TAG, "转发短信到SIP客户端: " + senderNumber + " -> " + targetClient.getExtension());
        
        // TODO: 实现SIP MESSAGE发送
        // 需要SIPStack支持发送MESSAGE请求
    }
}

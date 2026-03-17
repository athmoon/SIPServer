package com.sipserver.manager;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsManager;

import androidx.core.app.ActivityCompat;

import com.sipserver.SIPServerApp;
import com.sipserver.config.SIPConfigManager;
import com.sipserver.model.CallInfo;
import com.sipserver.model.SimCardInfo;
import com.sipserver.util.LogUtil;

import java.util.ArrayList;

/**
 * SIM卡通话管理器
 * 处理SIM卡的拨打电话和发送短信
 */
public class SimCallManager {
    
    private static final String TAG = "SimCallManager";
    
    private static SimCallManager instance;
    private Context context;
    
    private SimCallManager() {
        this.context = SIPServerApp.getContext();
    }
    
    public static synchronized SimCallManager getInstance() {
        if (instance == null) {
            instance = new SimCallManager();
        }
        return instance;
    }
    
    public boolean makeCall(String phoneNumber, int simSlot, CallInfo callInfo) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            LogUtil.e(TAG, "没有拨打电话权限");
            return false;
        }
        
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (tm == null) {
                LogUtil.e(TAG, "无法获取TelephonyManager");
                return false;
            }
            
            SimCardInfo simInfo = SIPConfigManager.getInstance().getSimCard(simSlot);
            if (simInfo == null || !simInfo.isReady()) {
                LogUtil.e(TAG, "SIM卡" + simSlot + "不可用");
                return false;
            }
            
            String uri = "tel:" + phoneNumber;
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(android.net.Uri.parse(uri));
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                callIntent.putExtra("com.android.phone.extra.slot", simSlot);
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                callIntent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", 
                    getPhoneAccountHandle(simSlot));
            }
            
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            
            LogUtil.i(TAG, "发起外呼: " + phoneNumber + " via SIM" + simSlot);
            return true;
            
        } catch (Exception e) {
            LogUtil.e(TAG, "拨打电话失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    public boolean sendSms(String phoneNumber, String message, int simSlot) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            LogUtil.e(TAG, "没有发送短信权限");
            return false;
        }
        
        try {
            SmsManager smsManager;
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.telephony.SubscriptionManager sm = context.getSystemService(
                    android.telephony.SubscriptionManager.class);
                if (sm != null) {
                    int subId = sm.getActiveSubscriptionIdForSimSlotIndex(simSlot);
                    smsManager = context.getSystemService(SmsManager.class)
                        .createForSubscriptionId(subId);
                } else {
                    smsManager = SmsManager.getDefault();
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                android.telephony.SubscriptionManager sm = android.telephony.SubscriptionManager.from(context);
                if (sm != null) {
                    int subId = sm.getActiveSubscriptionIdForSimSlotIndex(simSlot);
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                } else {
                    smsManager = SmsManager.getDefault();
                }
            } else {
                smsManager = SmsManager.getDefault();
            }
            
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0,
                new Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(context, 0,
                new Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE);
            
            if (parts.size() > 1) {
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();
                
                for (int i = 0; i < parts.size(); i++) {
                    sentIntents.add(sentIntent);
                    deliveredIntents.add(deliveredIntent);
                }
                
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, 
                    sentIntents, deliveredIntents);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, 
                    sentIntent, deliveredIntent);
            }
            
            LogUtil.i(TAG, "短信已发送: " + phoneNumber + " via SIM" + simSlot);
            return true;
            
        } catch (Exception e) {
            LogUtil.e(TAG, "发送短信失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    private Object getPhoneAccountHandle(int simSlot) {
        // 这个方法需要返回PhoneAccountHandle对象
        // 由于兼容性问题，这里简化处理
        return null;
    }
}

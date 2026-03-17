package com.sipserver;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.sipserver.config.SIPConfigManager;
import com.sipserver.util.LogUtil;

/**
 * SIP服务端应用类
 * 负责初始化全局组件和通知渠道
 */
public class SIPServerApp extends Application {
    
    private static final String TAG = "SIPServerApp";
    private static SIPServerApp instance;
    
    // 通知渠道ID
    public static final String CHANNEL_SERVICE = "sip_service";
    public static final String CHANNEL_CALL = "sip_call";
    public static final String CHANNEL_MESSAGE = "sip_message";
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化日志
        LogUtil.init(this);
        LogUtil.i(TAG, "SIP服务端应用启动");
        
        // 创建通知渠道
        createNotificationChannels();
        
        // 初始化配置管理
        SIPConfigManager.getInstance().init(this);
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            
            // 服务通知渠道
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_SERVICE,
                "SIP服务",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("SIP服务运行状态通知");
            serviceChannel.setShowBadge(false);
            manager.createNotificationChannel(serviceChannel);
            
            // 来电通知渠道
            NotificationChannel callChannel = new NotificationChannel(
                CHANNEL_CALL,
                "通话通知",
                NotificationManager.IMPORTANCE_HIGH
            );
            callChannel.setDescription("来电和通话状态通知");
            callChannel.enableVibration(true);
            manager.createNotificationChannel(callChannel);
            
            // 消息通知渠道
            NotificationChannel messageChannel = new NotificationChannel(
                CHANNEL_MESSAGE,
                "消息通知",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            messageChannel.setDescription("短信和SIP消息通知");
            manager.createNotificationChannel(messageChannel);
        }
    }
    
    /**
     * 获取应用实例
     */
    public static SIPServerApp getInstance() {
        return instance;
    }
    
    /**
     * 获取应用上下文
     */
    public static Context getContext() {
        return instance.getApplicationContext();
    }
}

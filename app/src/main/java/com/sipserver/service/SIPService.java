package com.sipserver.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.sipserver.R;
import com.sipserver.SIPServerApp;
import com.sipserver.config.SIPConfigManager;
import com.sipserver.manager.CallManager;
import com.sipserver.manager.ClientManager;
import com.sipserver.sip.SIPStack;
import com.sipserver.ui.MainActivity;
import com.sipserver.util.LogUtil;

/**
 * SIP后台服务
 * 保持SIP服务运行，处理保活逻辑
 */
public class SIPService extends Service {
    
    private static final String TAG = "SIPService";
    private static final int NOTIFICATION_ID = 1001;
    
    private SIPStack sipStack;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    
    private ServiceBinder binder = new ServiceBinder();
    
    public class ServiceBinder extends Binder {
        public SIPService getService() {
            return SIPService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.i(TAG, "SIP服务创建");
        
        sipStack = new SIPStack(getApplicationContext());
        
        acquireWakeLock();
        acquireWifiLock();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.i(TAG, "SIP服务启动");
        
        startForeground();
        
        if (!sipStack.isRunning()) {
            boolean started = sipStack.start();
            if (!started) {
                LogUtil.e(TAG, "SIP协议栈启动失败");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        LogUtil.i(TAG, "SIP服务销毁");
        
        sipStack.stop();
        
        releaseWakeLock();
        releaseWifiLock();
        
        stopForeground(true);
        
        super.onDestroy();
    }
    
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
            notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        int clientCount = ClientManager.getInstance().getClientCount();
        int callCount = CallManager.getInstance().getActiveCallCount();
        
        String statusText = String.format("客户端: %d | 通话: %d", clientCount, callCount);
        
        Notification notification = new NotificationCompat.Builder(this, SIPServerApp.CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.service_name))
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }
    
    public void updateNotification() {
        startForeground();
    }
    
    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SIPServer:ServiceWakeLock");
            wakeLock.acquire(10 * 60 * 1000L);
            LogUtil.d(TAG, "获取WakeLock");
        }
    }
    
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            LogUtil.d(TAG, "释放WakeLock");
        }
    }
    
    private void acquireWifiLock() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "SIPServer:WifiLock");
            wifiLock.acquire();
            LogUtil.d(TAG, "获取WifiLock");
        }
    }
    
    private void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            LogUtil.d(TAG, "释放WifiLock");
        }
    }
    
    public boolean isRunning() {
        return sipStack != null && sipStack.isRunning();
    }
    
    public SIPStack getSipStack() {
        return sipStack;
    }
}

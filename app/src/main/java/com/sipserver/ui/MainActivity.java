package com.sipserver.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sipserver.R;
import com.sipserver.config.SIPConfigManager;
import com.sipserver.manager.CallManager;
import com.sipserver.manager.ClientManager;
import com.sipserver.model.ClientInfo;
import com.sipserver.service.SIPService;
import com.sipserver.util.LogUtil;
import com.sipserver.util.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面Activity
 * 显示服务状态、客户端列表和配置入口
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextView tvServerStatus;
    private TextView tvServerAddress;
    private TextView tvClientCount;
    private TextView tvCallCount;
    private Button btnStartStop;
    private RecyclerView rvClients;
    
    private SIPService sipService;
    private boolean serviceBound = false;
    private ClientAdapter clientAdapter;
    
    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.FOREGROUND_SERVICE
    };
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SIPService.ServiceBinder binder = (SIPService.ServiceBinder) service;
            sipService = binder.getService();
            serviceBound = true;
            updateUI();
            LogUtil.i(TAG, "已绑定SIP服务");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            sipService = null;
            serviceBound = false;
            updateUI();
            LogUtil.i(TAG, "已断开SIP服务");
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
    }
    
    private void initViews() {
        tvServerStatus = findViewById(R.id.tv_server_status);
        tvServerAddress = findViewById(R.id.tv_server_address);
        tvClientCount = findViewById(R.id.tv_client_count);
        tvCallCount = findViewById(R.id.tv_call_count);
        btnStartStop = findViewById(R.id.btn_start_stop);
        rvClients = findViewById(R.id.rv_clients);
        
        clientAdapter = new ClientAdapter(new ArrayList<>());
        rvClients.setLayoutManager(new LinearLayoutManager(this));
        rvClients.setAdapter(clientAdapter);
        
        btnStartStop.setOnClickListener(v -> {
            if (serviceBound && sipService != null && sipService.isRunning()) {
                stopSipService();
            } else {
                startSipService();
            }
        });
        
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });
        
        findViewById(R.id.btn_sim_config).setOnClickListener(v -> {
            startActivity(new Intent(this, SimConfigActivity.class));
        });
    }
    
    private void checkPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        // Android 10+ 需要额外权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.READ_CALL_LOG);
            }
        }
        
        // Android 12+ 需要READ_CONTACTS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.READ_CONTACTS);
            }
        }
        
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                missingPermissions.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            requestBatteryOptimization();
        }
    }
    
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(this)
                    .setTitle("电池优化")
                    .setMessage("为确保SIP服务稳定运行，请将本应用加入电池优化白名单")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivity(intent);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "部分权限未授予，功能可能受限", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void startSipService() {
        Intent intent = new Intent(this, SIPService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        Toast.makeText(this, "SIP服务启动中...", Toast.LENGTH_SHORT).show();
    }
    
    private void stopSipService() {
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        stopService(new Intent(this, SIPService.class));
        
        Toast.makeText(this, "SIP服务已停止", Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    private void updateUI() {
        runOnUiThread(() -> {
            boolean isRunning = serviceBound && sipService != null && sipService.isRunning();
            
            if (isRunning) {
                tvServerStatus.setText(R.string.server_running);
                tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.status_running));
                btnStartStop.setText(R.string.stop_service);
            } else {
                tvServerStatus.setText(R.string.server_stopped);
                tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.status_stopped));
                btnStartStop.setText(R.string.start_service);
            }
            
            String localIp = NetworkUtil.getLocalIpAddress();
            int port = SIPConfigManager.getInstance().getSipPort();
            tvServerAddress.setText(localIp + ":" + port);
            
            int clientCount = ClientManager.getInstance().getClientCount();
            tvClientCount.setText(String.valueOf(clientCount));
            
            int callCount = CallManager.getInstance().getActiveCallCount();
            tvCallCount.setText(String.valueOf(callCount));
            
            List<ClientInfo> clients = new ArrayList<>(ClientManager.getInstance().getAllClients());
            clientAdapter.updateData(clients);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (!serviceBound) {
            Intent intent = new Intent(this, SIPService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        
        updateUI();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_refresh) {
            updateUI();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("关于SIP服务端")
            .setMessage("版本: 1.0.0\n\n基于JAIN-SIP实现的Android SIP服务端\n支持标准SIP软电话客户端注册和呼叫")
            .setPositiveButton("确定", null)
            .show();
    }
}

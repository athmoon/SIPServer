package com.sipserver.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sipserver.R;
import com.sipserver.config.SIPConfigManager;

/**
 * 设置界面
 * 配置SIP服务参数
 */
public class SettingsActivity extends AppCompatActivity {
    
    private EditText etPort;
    private EditText etDomain;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("SIP设置");
        }
        
        initViews();
        loadSettings();
    }
    
    private void initViews() {
        etPort = findViewById(R.id.et_sip_port);
        etDomain = findViewById(R.id.et_sip_domain);
        
        findViewById(R.id.btn_save).setOnClickListener(v -> saveSettings());
    }
    
    private void loadSettings() {
        SIPConfigManager config = SIPConfigManager.getInstance();
        
        etPort.setText(String.valueOf(config.getSipPort()));
        etDomain.setText(config.getSipDomain());
    }
    
    private void saveSettings() {
        String portStr = etPort.getText().toString().trim();
        String domain = etDomain.getText().toString().trim();
        
        if (portStr.isEmpty()) {
            Toast.makeText(this, "请输入端口号", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                Toast.makeText(this, "端口号必须在1-65535之间", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "端口号格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (domain.isEmpty()) {
            domain = "sip.local";
        }
        
        SIPConfigManager config = SIPConfigManager.getInstance();
        config.setSipPort(port);
        config.setSipDomain(domain);
        config.saveConfig();
        
        Toast.makeText(this, "设置已保存，重启服务生效", Toast.LENGTH_LONG).show();
        finish();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

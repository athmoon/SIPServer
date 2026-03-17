package com.sipserver.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sipserver.R;
import com.sipserver.config.SIPConfigManager;
import com.sipserver.model.SimCardInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * SIM卡配置界面
 * 配置双卡选择和转发规则
 */
public class SimConfigActivity extends AppCompatActivity {
    
    private Spinner spDefaultSim;
    private CheckBox cbSim0ForwardCall;
    private EditText etSim0CallTarget;
    private CheckBox cbSim0ForwardSms;
    private EditText etSim0SmsTarget;
    
    private CheckBox cbSim1ForwardCall;
    private EditText etSim1CallTarget;
    private CheckBox cbSim1ForwardSms;
    private EditText etSim1SmsTarget;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_config);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("SIM卡配置");
        }
        
        initViews();
        loadConfig();
    }
    
    private void initViews() {
        spDefaultSim = findViewById(R.id.sp_default_sim);
        
        cbSim0ForwardCall = findViewById(R.id.cb_sim0_forward_call);
        etSim0CallTarget = findViewById(R.id.et_sim0_call_target);
        cbSim0ForwardSms = findViewById(R.id.cb_sim0_forward_sms);
        etSim0SmsTarget = findViewById(R.id.et_sim0_sms_target);
        
        cbSim1ForwardCall = findViewById(R.id.cb_sim1_forward_call);
        etSim1CallTarget = findViewById(R.id.et_sim1_call_target);
        cbSim1ForwardSms = findViewById(R.id.cb_sim1_forward_sms);
        etSim1SmsTarget = findViewById(R.id.et_sim1_sms_target);
        
        List<String> simOptions = new ArrayList<>();
        simOptions.add("SIM卡1");
        simOptions.add("SIM卡2");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, simOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDefaultSim.setAdapter(adapter);
        
        findViewById(R.id.btn_save).setOnClickListener(v -> saveConfig());
    }
    
    private void loadConfig() {
        SIPConfigManager config = SIPConfigManager.getInstance();
        
        spDefaultSim.setSelection(config.getDefaultSimSlot());
        
        SimCardInfo sim0 = config.getSimCard(0);
        if (sim0 != null) {
            cbSim0ForwardCall.setChecked(sim0.isForwardIncoming());
            etSim0CallTarget.setText(sim0.getForwardTarget());
            cbSim0ForwardSms.setChecked(sim0.isForwardSms());
            etSim0SmsTarget.setText(sim0.getSmsForwardTarget());
        }
        
        SimCardInfo sim1 = config.getSimCard(1);
        if (sim1 != null) {
            cbSim1ForwardCall.setChecked(sim1.isForwardIncoming());
            etSim1CallTarget.setText(sim1.getForwardTarget());
            cbSim1ForwardSms.setChecked(sim1.isForwardSms());
            etSim1SmsTarget.setText(sim1.getSmsForwardTarget());
        }
    }
    
    private void saveConfig() {
        SIPConfigManager config = SIPConfigManager.getInstance();
        
        config.setDefaultSimSlot(spDefaultSim.getSelectedItemPosition());
        
        SimCardInfo sim0 = config.getSimCard(0);
        if (sim0 != null) {
            sim0.setForwardIncoming(cbSim0ForwardCall.isChecked());
            sim0.setForwardTarget(etSim0CallTarget.getText().toString().trim());
            sim0.setForwardSms(cbSim0ForwardSms.isChecked());
            sim0.setSmsForwardTarget(etSim0SmsTarget.getText().toString().trim());
        }
        
        SimCardInfo sim1 = config.getSimCard(1);
        if (sim1 != null) {
            sim1.setForwardIncoming(cbSim1ForwardCall.isChecked());
            sim1.setForwardTarget(etSim1CallTarget.getText().toString().trim());
            sim1.setForwardSms(cbSim1ForwardSms.isChecked());
            sim1.setSmsForwardTarget(etSim1SmsTarget.getText().toString().trim());
        }
        
        config.saveConfig();
        
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

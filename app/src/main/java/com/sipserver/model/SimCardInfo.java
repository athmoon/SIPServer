package com.sipserver.model;

/**
 * SIM卡信息模型
 * 管理SIM卡状态和配置
 */
public class SimCardInfo {
    
    // SIM卡槽ID（0或1）
    private int slotId;
    
    // SIM卡运营商名称
    private String carrierName;
    
    // SIM卡号码
    private String phoneNumber;
    
    // SIM卡状态
    private SimState state;
    
    // SIM卡序列号
    private String serialNumber;
    
    // SIM卡国家代码
    private String countryCode;
    
    // 是否启用（用于外呼）
    private boolean enabled;
    
    // 是否用于转发来电
    private boolean forwardIncoming;
    
    // 来电转发目标分机
    private String forwardTarget;
    
    // 是否用于转发短信
    private boolean forwardSms;
    
    // 短信转发目标分机
    private String smsForwardTarget;
    
    /**
     * SIM卡状态枚举
     */
    public enum SimState {
        UNKNOWN,        // 未知
        ABSENT,         // 未插入
        PIN_REQUIRED,   // 需要PIN码
        PUK_REQUIRED,   // 需要PUK码
        NETWORK_LOCKED, // 网络锁定
        READY,          // 就绪
        NOT_READY,      // 未就绪
        PERM_DISABLED   // 永久禁用
    }
    
    // 构造函数
    public SimCardInfo() {
        this.state = SimState.UNKNOWN;
        this.enabled = true;
        this.forwardIncoming = false;
        this.forwardSms = false;
    }
    
    public SimCardInfo(int slotId) {
        this();
        this.slotId = slotId;
    }
    
    // Getters and Setters
    
    public int getSlotId() {
        return slotId;
    }
    
    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }
    
    public String getCarrierName() {
        return carrierName;
    }
    
    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public SimState getState() {
        return state;
    }
    
    public void setState(SimState state) {
        this.state = state;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isForwardIncoming() {
        return forwardIncoming;
    }
    
    public void setForwardIncoming(boolean forwardIncoming) {
        this.forwardIncoming = forwardIncoming;
    }
    
    public String getForwardTarget() {
        return forwardTarget;
    }
    
    public void setForwardTarget(String forwardTarget) {
        this.forwardTarget = forwardTarget;
    }
    
    public boolean isForwardSms() {
        return forwardSms;
    }
    
    public void setForwardSms(boolean forwardSms) {
        this.forwardSms = forwardSms;
    }
    
    public String getSmsForwardTarget() {
        return smsForwardTarget;
    }
    
    public void setSmsForwardTarget(String smsForwardTarget) {
        this.smsForwardTarget = smsForwardTarget;
    }
    
    /**
     * 检查SIM卡是否可用
     */
    public boolean isReady() {
        return state == SimState.READY;
    }
    
    @Override
    public String toString() {
        return "SimCardInfo{" +
            "slotId=" + slotId +
            ", carrierName='" + carrierName + '\'' +
            ", phoneNumber='" + phoneNumber + '\'' +
            ", state=" + state +
            ", enabled=" + enabled +
            '}';
    }
}

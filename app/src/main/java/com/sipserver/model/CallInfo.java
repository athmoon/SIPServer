package com.sipserver.model;

/**
 * 通话信息模型
 * 管理通话状态和历史记录
 */
public class CallInfo {
    
    // 通话ID
    private String callId;
    
    // 主叫号码
    private String callerNumber;
    
    // 被叫号码
    private String calleeNumber;
    
    // 主叫客户端信息
    private ClientInfo caller;
    
    // 被叫客户端信息
    private ClientInfo callee;
    
    // 通话状态
    private CallStatus status;
    
    // 通话类型
    private CallType type;
    
    // 开始时间
    private long startTime;
    
    // 接通时间
    private long answerTime;
    
    // 结束时间
    private long endTime;
    
    // 通话方向
    private CallDirection direction;
    
    // 是否通过SIM卡
    private boolean viaSimCard;
    
    // 使用的SIM卡ID
    private int simSlotId;
    
    /**
     * 通话状态枚举
     */
    public enum CallStatus {
        IDLE,           // 空闲
        RINGING,        // 振铃中
        DIALING,        // 拨号中
        ANSWERED,       // 已接通
        HOLD,           // 保持
        ENDED,          // 已结束
        FAILED          // 失败
    }
    
    /**
     * 通话类型枚举
     */
    public enum CallType {
        INTERNAL,       // 内部通话
        EXTERNAL,       // 外部通话（通过SIM卡）
        INCOMING        // 外部来电
    }
    
    /**
     * 通话方向枚举
     */
    public enum CallDirection {
        INBOUND,        // 呼入
        OUTBOUND        // 呼出
    }
    
    // 构造函数
    public CallInfo() {
        this.startTime = System.currentTimeMillis();
        this.status = CallStatus.IDLE;
    }
    
    public CallInfo(String callId, String callerNumber, String calleeNumber) {
        this();
        this.callId = callId;
        this.callerNumber = callerNumber;
        this.calleeNumber = calleeNumber;
    }
    
    // Getters and Setters
    
    public String getCallId() {
        return callId;
    }
    
    public void setCallId(String callId) {
        this.callId = callId;
    }
    
    public String getCallerNumber() {
        return callerNumber;
    }
    
    public void setCallerNumber(String callerNumber) {
        this.callerNumber = callerNumber;
    }
    
    public String getCalleeNumber() {
        return calleeNumber;
    }
    
    public void setCalleeNumber(String calleeNumber) {
        this.calleeNumber = calleeNumber;
    }
    
    public ClientInfo getCaller() {
        return caller;
    }
    
    public void setCaller(ClientInfo caller) {
        this.caller = caller;
    }
    
    public ClientInfo getCallee() {
        return callee;
    }
    
    public void setCallee(ClientInfo callee) {
        this.callee = callee;
    }
    
    public CallStatus getStatus() {
        return status;
    }
    
    public void setStatus(CallStatus status) {
        this.status = status;
        
        // 自动记录时间
        if (status == CallStatus.ANSWERED && answerTime == 0) {
            answerTime = System.currentTimeMillis();
        } else if (status == CallStatus.ENDED && endTime == 0) {
            endTime = System.currentTimeMillis();
        }
    }
    
    public CallType getType() {
        return type;
    }
    
    public void setType(CallType type) {
        this.type = type;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getAnswerTime() {
        return answerTime;
    }
    
    public void setAnswerTime(long answerTime) {
        this.answerTime = answerTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public CallDirection getDirection() {
        return direction;
    }
    
    public void setDirection(CallDirection direction) {
        this.direction = direction;
    }
    
    public boolean isViaSimCard() {
        return viaSimCard;
    }
    
    public void setViaSimCard(boolean viaSimCard) {
        this.viaSimCard = viaSimCard;
    }
    
    public int getSimSlotId() {
        return simSlotId;
    }
    
    public void setSimSlotId(int simSlotId) {
        this.simSlotId = simSlotId;
    }
    
    /**
     * 计算通话时长（秒）
     */
    public long getDuration() {
        if (answerTime == 0) {
            return 0;
        }
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return (end - answerTime) / 1000;
    }
    
    /**
     * 判断是否为内部通话
     */
    public boolean isInternalCall() {
        return type == CallType.INTERNAL;
    }
    
    @Override
    public String toString() {
        return "CallInfo{" +
            "callId='" + callId + '\'' +
            ", callerNumber='" + callerNumber + '\'' +
            ", calleeNumber='" + calleeNumber + '\'' +
            ", status=" + status +
            ", type=" + type +
            ", duration=" + getDuration() + "s" +
            '}';
    }
}

package com.sipserver.sip;

import android.content.Context;

import com.sipserver.config.SIPConfigManager;
import com.sipserver.manager.CallManager;
import com.sipserver.manager.ClientManager;
import com.sipserver.manager.SimCallManager;
import com.sipserver.model.CallInfo;
import com.sipserver.model.ClientInfo;
import com.sipserver.util.LogUtil;
import com.sipserver.util.NetworkUtil;

import javax.sip.ClientTransaction;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * SIP请求处理器
 * 处理INVITE、ACK、BYE、CANCEL、MESSAGE等请求
 */
public class SIPRequestHandler {
    
    private static final String TAG = "SIPRequestHandler";
    
    private Context context;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    
    private ClientManager clientManager;
    private CallManager callManager;
    private SimCallManager simCallManager;
    
    public SIPRequestHandler(Context context, AddressFactory addressFactory, 
                            HeaderFactory headerFactory, MessageFactory messageFactory) {
        this.context = context;
        this.addressFactory = addressFactory;
        this.headerFactory = headerFactory;
        this.messageFactory = messageFactory;
        
        this.clientManager = ClientManager.getInstance();
        this.callManager = CallManager.getInstance();
        this.simCallManager = SimCallManager.getInstance();
    }
    
    public void handleInvite(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        Request request = requestEvent.getRequest();
        
        javax.sip.header.FromHeader fromHeader = (javax.sip.header.FromHeader) 
            request.getHeader(javax.sip.header.FromHeader.NAME);
        javax.sip.header.ToHeader toHeader = (javax.sip.header.ToHeader) 
            request.getHeader(javax.sip.header.ToHeader.NAME);
        javax.sip.header.CallIdHeader callIdHeader = (javax.sip.header.CallIdHeader) 
            request.getHeader(javax.sip.header.CallIdHeader.NAME);
        
        String callerNumber = extractExtension(fromHeader.getAddress().getURI().toString());
        String calleeNumber = extractExtension(toHeader.getAddress().getURI().toString());
        String callId = callIdHeader.getCallId();
        
        LogUtil.i(TAG, "INVITE请求: " + callerNumber + " -> " + calleeNumber);
        
        ClientInfo caller = clientManager.getClient(callerNumber);
        if (caller == null) {
            LogUtil.w(TAG, "主叫未注册: " + callerNumber);
            sendResponse(requestEvent, serverTransaction, Response.FORBIDDEN);
            return;
        }
        
        if (NetworkUtil.isInternalNumber(calleeNumber)) {
            handleInternalCall(requestEvent, serverTransaction, caller, calleeNumber, callId);
        } else if (NetworkUtil.isExternalNumber(calleeNumber)) {
            handleExternalCall(requestEvent, serverTransaction, caller, calleeNumber, callId);
        } else {
            LogUtil.w(TAG, "无效的被叫号码: " + calleeNumber);
            sendResponse(requestEvent, serverTransaction, Response.NOT_FOUND);
        }
    }
    
    private void handleInternalCall(RequestEvent requestEvent, ServerTransaction serverTransaction,
                                   ClientInfo caller, String calleeNumber, String callId) 
            throws Exception {
        ClientInfo callee = clientManager.getClient(calleeNumber);
        
        if (callee == null) {
            LogUtil.w(TAG, "被叫未注册: " + calleeNumber);
            sendResponse(requestEvent, serverTransaction, Response.NOT_FOUND);
            return;
        }
        
        if (callee.getStatus() == ClientInfo.RegisterStatus.BUSY) {
            LogUtil.w(TAG, "被叫忙线: " + calleeNumber);
            sendResponse(requestEvent, serverTransaction, Response.BUSY_HERE);
            return;
        }
        
        CallInfo call = new CallInfo(callId, caller.getExtension(), calleeNumber);
        call.setCaller(caller);
        call.setCallee(callee);
        call.setType(CallInfo.CallType.INTERNAL);
        call.setDirection(CallInfo.CallDirection.OUTBOUND);
        call.setStatus(CallInfo.CallStatus.RINGING);
        
        callManager.addCall(call);
        
        Response ringing = messageFactory.createResponse(Response.RINGING, requestEvent.getRequest());
        sendResponse(requestEvent, serverTransaction, ringing);
        
        LogUtil.i(TAG, "内部呼叫建立: " + caller.getExtension() + " -> " + calleeNumber);
    }
    
    private void handleExternalCall(RequestEvent requestEvent, ServerTransaction serverTransaction,
                                   ClientInfo caller, String calleeNumber, String callId) 
            throws Exception {
        
        int simSlot = SIPConfigManager.getInstance().getDefaultSimSlot();
        
        CallInfo call = new CallInfo(callId, caller.getExtension(), calleeNumber);
        call.setCaller(caller);
        call.setType(CallInfo.CallType.EXTERNAL);
        call.setDirection(CallInfo.CallDirection.OUTBOUND);
        call.setViaSimCard(true);
        call.setSimSlotId(simSlot);
        call.setStatus(CallInfo.CallStatus.DIALING);
        
        callManager.addCall(call);
        
        Response trying = messageFactory.createResponse(Response.TRYING, requestEvent.getRequest());
        sendResponse(requestEvent, serverTransaction, trying);
        
        boolean success = simCallManager.makeCall(calleeNumber, simSlot, call);
        
        if (success) {
            LogUtil.i(TAG, "外呼已发起: " + calleeNumber + " via SIM" + simSlot);
        } else {
            LogUtil.e(TAG, "外呼失败: " + calleeNumber);
            call.setStatus(CallInfo.CallStatus.FAILED);
            sendResponse(requestEvent, serverTransaction, Response.SERVICE_UNAVAILABLE);
        }
    }
    
    public void handleAck(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        LogUtil.d(TAG, "收到ACK");
    }
    
    public void handleBye(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        Request request = requestEvent.getRequest();
        javax.sip.header.CallIdHeader callIdHeader = (javax.sip.header.CallIdHeader) 
            request.getHeader(javax.sip.header.CallIdHeader.NAME);
        
        String callId = callIdHeader.getCallId();
        CallInfo call = callManager.getCall(callId);
        
        if (call != null) {
            call.setStatus(CallInfo.CallStatus.ENDED);
            callManager.removeCall(callId);
            LogUtil.i(TAG, "通话结束: " + callId);
        }
        
        Response ok = messageFactory.createResponse(Response.OK, request);
        sendResponse(requestEvent, serverTransaction, ok);
    }
    
    public void handleCancel(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        Request request = requestEvent.getRequest();
        
        LogUtil.i(TAG, "收到CANCEL请求");
        
        Response ok = messageFactory.createResponse(Response.OK, request);
        sendResponse(requestEvent, serverTransaction, ok);
        
        Response requestTerminated = messageFactory.createResponse(
            Response.REQUEST_TERMINATED, request);
        sendResponse(requestEvent, serverTransaction, requestTerminated);
    }
    
    public void handleMessage(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        Request request = requestEvent.getRequest();
        
        javax.sip.header.FromHeader fromHeader = (javax.sip.header.FromHeader) 
            request.getHeader(javax.sip.header.FromHeader.NAME);
        javax.sip.header.ToHeader toHeader = (javax.sip.header.ToHeader) 
            request.getHeader(javax.sip.header.ToHeader.NAME);
        
        String sender = extractExtension(fromHeader.getAddress().getURI().toString());
        String recipient = extractExtension(toHeader.getAddress().getURI().toString());
        
        byte[] content = request.getRawContent();
        String messageContent = content != null ? new String(content) : "";
        
        LogUtil.i(TAG, "MESSAGE: " + sender + " -> " + recipient + ": " + messageContent);
        
        if (NetworkUtil.isExternalNumber(recipient)) {
            handleSmsToExternal(requestEvent, serverTransaction, sender, recipient, messageContent);
        } else {
            handleInternalMessage(requestEvent, serverTransaction, sender, recipient, messageContent);
        }
    }
    
    private void handleInternalMessage(RequestEvent requestEvent, ServerTransaction serverTransaction,
                                      String sender, String recipient, String content) 
            throws Exception {
        ClientInfo recipientClient = clientManager.getClient(recipient);
        
        if (recipientClient == null) {
            LogUtil.w(TAG, "消息接收方未注册: " + recipient);
            sendResponse(requestEvent, serverTransaction, Response.NOT_FOUND);
            return;
        }
        
        Response ok = messageFactory.createResponse(Response.OK, requestEvent.getRequest());
        sendResponse(requestEvent, serverTransaction, ok);
        
        LogUtil.i(TAG, "内部消息已转发: " + sender + " -> " + recipient);
    }
    
    private void handleSmsToExternal(RequestEvent requestEvent, ServerTransaction serverTransaction,
                                    String sender, String recipient, String content) 
            throws Exception {
        
        int simSlot = SIPConfigManager.getInstance().getDefaultSimSlot();
        
        boolean success = simCallManager.sendSms(recipient, content, simSlot);
        
        if (success) {
            Response ok = messageFactory.createResponse(Response.OK, requestEvent.getRequest());
            sendResponse(requestEvent, serverTransaction, ok);
            LogUtil.i(TAG, "短信已发送: " + recipient + " via SIM" + simSlot);
        } else {
            Response error = messageFactory.createResponse(
                Response.SERVICE_UNAVAILABLE, requestEvent.getRequest());
            sendResponse(requestEvent, serverTransaction, error);
            LogUtil.e(TAG, "短信发送失败: " + recipient);
        }
    }
    
    private void sendResponse(RequestEvent requestEvent, ServerTransaction serverTransaction, 
                             int statusCode) throws Exception {
        Response response = messageFactory.createResponse(statusCode, requestEvent.getRequest());
        sendResponse(requestEvent, serverTransaction, response);
    }
    
    private void sendResponse(RequestEvent requestEvent, ServerTransaction serverTransaction, 
                             Response response) throws Exception {
        if (serverTransaction == null) {
            // 需要从SIPStack获取provider创建事务
            // 这里简化处理，实际应该通过回调或其他方式获取
        }
        serverTransaction.sendResponse(response);
    }
    
    private String extractExtension(String sipUri) {
        if (sipUri == null) return null;
        
        if (sipUri.startsWith("sip:")) {
            sipUri = sipUri.substring(4);
        }
        
        int atIndex = sipUri.indexOf('@');
        if (atIndex > 0) {
            return sipUri.substring(0, atIndex);
        }
        
        return sipUri;
    }
}

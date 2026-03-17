package com.sipserver.sip;

import android.content.Context;

import com.sipserver.config.SIPConfigManager;
import com.sipserver.manager.ClientManager;
import com.sipserver.model.ClientInfo;
import com.sipserver.util.LogUtil;
import com.sipserver.util.NetworkUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.javax.sip.SipStackImpl;

/**
 * SIP协议栈核心类
 * 基于JAIN-SIP实现SIP服务端功能
 */
public class SIPStack implements SipListener {
    
    private static final String TAG = "SIPStack";
    
    private Context context;
    private SipStack sipStack;
    private SipProvider sipProvider;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private ListeningPoint listeningPoint;
    
    private ClientManager clientManager;
    private SIPRequestHandler requestHandler;
    
    private boolean isRunning = false;
    
    public SIPStack(Context context) {
        this.context = context;
        this.clientManager = ClientManager.getInstance();
    }
    
    /**
     * 启动SIP服务
     */
    public synchronized boolean start() {
        if (isRunning) {
            LogUtil.w(TAG, "SIP服务已在运行");
            return true;
        }
        
        try {
            String localIp = NetworkUtil.getLocalIpAddress();
            SIPConfigManager.getInstance().setLocalIp(localIp);
            int port = SIPConfigManager.getInstance().getSipPort();
            String domain = SIPConfigManager.getInstance().getSipDomain();
            
            LogUtil.i(TAG, "启动SIP服务: " + localIp + ":" + port + " 域: " + domain);
            
            initSipStack(localIp, port, domain);
            
            isRunning = true;
            LogUtil.i(TAG, "SIP服务启动成功");
            return true;
            
        } catch (Exception e) {
            LogUtil.e(TAG, "SIP服务启动失败: " + e.getMessage(), e);
            stop();
            return false;
        }
    }
    
    /**
     * 停止SIP服务
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }
        
        LogUtil.i(TAG, "停止SIP服务");
        
        try {
            if (sipProvider != null) {
                sipProvider.removeListeningPoint(listeningPoint);
                sipStack.deleteSipProvider(sipProvider);
            }
            
            if (sipStack != null) {
                sipStack.stop();
            }
            
        } catch (Exception e) {
            LogUtil.e(TAG, "停止SIP服务失败: " + e.getMessage(), e);
        }
        
        sipStack = null;
        sipProvider = null;
        isRunning = false;
    }
    
    private void initSipStack(String localIp, int port, String domain) 
            throws PeerUnavailableException, TransportNotSupportedException, 
                   InvalidArgumentException, ObjectInUseException, 
                   TooManyListenersException {
        
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "SIPServer");
        properties.setProperty("javax.sip.IP_ADDRESS", localIp);
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "off");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "off");
        properties.setProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE", "10000");
        properties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
        properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "true");
        
        sipStack = (SipStack) sipFactory.createSipStack(properties);
        
        addressFactory = sipFactory.createAddressFactory();
        headerFactory = sipFactory.createHeaderFactory();
        messageFactory = sipFactory.createMessageFactory();
        
        listeningPoint = sipStack.createListeningPoint(localIp, port, "udp");
        
        sipProvider = sipStack.createSipProvider(listeningPoint);
        sipProvider.addSipListener(this);
        
        requestHandler = new SIPRequestHandler(context, addressFactory, headerFactory, messageFactory);
        
        LogUtil.i(TAG, "SIP协议栈初始化完成");
    }
    
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransaction = requestEvent.getServerTransaction();
        
        String method = request.getMethod();
        LogUtil.d(TAG, "收到SIP请求: " + method + " from " + request.getRemoteAddress());
        
        try {
            switch (method) {
                case Request.REGISTER:
                    handleRegister(requestEvent, serverTransaction);
                    break;
                    
                case Request.INVITE:
                    requestHandler.handleInvite(requestEvent, serverTransaction);
                    break;
                    
                case Request.ACK:
                    requestHandler.handleAck(requestEvent, serverTransaction);
                    break;
                    
                case Request.BYE:
                    requestHandler.handleBye(requestEvent, serverTransaction);
                    break;
                    
                case Request.CANCEL:
                    requestHandler.handleCancel(requestEvent, serverTransaction);
                    break;
                    
                case Request.MESSAGE:
                    requestHandler.handleMessage(requestEvent, serverTransaction);
                    break;
                    
                case Request.OPTIONS:
                    handleOptions(requestEvent, serverTransaction);
                    break;
                    
                default:
                    LogUtil.w(TAG, "不支持的SIP方法: " + method);
                    sendResponse(requestEvent, serverTransaction, Response.NOT_IMPLEMENTED);
            }
            
        } catch (Exception e) {
            LogUtil.e(TAG, "处理SIP请求失败: " + e.getMessage(), e);
            try {
                sendResponse(requestEvent, serverTransaction, Response.SERVER_INTERNAL_ERROR);
            } catch (Exception ex) {
                LogUtil.e(TAG, "发送错误响应失败: " + ex.getMessage(), ex);
            }
        }
    }
    
    private void handleRegister(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        Request request = requestEvent.getRequest();
        
        javax.sip.header.FromHeader fromHeader = (javax.sip.header.FromHeader) request.getHeader(javax.sip.header.FromHeader.NAME);
        javax.sip.header.ContactHeader contactHeader = (javax.sip.header.ContactHeader) request.getHeader(javax.sip.header.ContactHeader.NAME);
        javax.sip.header.ExpiresHeader expiresHeader = (javax.sip.header.ExpiresHeader) request.getHeader(javax.sip.header.ExpiresHeader.NAME);
        
        if (fromHeader == null) {
            sendResponse(requestEvent, serverTransaction, Response.BAD_REQUEST);
            return;
        }
        
        Address fromAddress = fromHeader.getAddress();
        String sipUri = fromAddress.getURI().toString();
        String extension = extractExtension(sipUri);
        
        if (!isValidExtension(extension)) {
            LogUtil.w(TAG, "无效的分机号: " + extension);
            sendResponse(requestEvent, serverTransaction, Response.FORBIDDEN);
            return;
        }
        
        int expires = expiresHeader != null ? expiresHeader.getExpires() : 3600;
        
        if (expires == 0) {
            clientManager.unregisterClient(extension);
            sendResponse(requestEvent, serverTransaction, Response.OK);
            LogUtil.i(TAG, "客户端注销: " + extension);
            return;
        }
        
        String clientIp = request.getRemoteAddress().getHostAddress();
        int clientPort = request.getRemotePort();
        
        ClientInfo client = new ClientInfo(extension, clientIp, clientPort);
        client.setExpires(expires);
        
        if (contactHeader != null) {
            client.setContactUri(contactHeader.getAddress().getURI().toString());
        }
        
        clientManager.registerClient(client);
        
        Response response = messageFactory.createResponse(Response.OK, request);
        
        javax.sip.header.ContactHeader responseContact = headerFactory.createContactHeader(
            addressFactory.createAddress(
                addressFactory.createSipURI(extension, clientIp + ":" + clientPort)
            )
        );
        response.addHeader(responseContact);
        
        javax.sip.header.ExpiresHeader responseExpires = headerFactory.createExpiresHeader(expires);
        response.addHeader(responseExpires);
        
        sendResponse(requestEvent, serverTransaction, response);
        LogUtil.i(TAG, "客户端注册成功: " + extension + " " + clientIp + ":" + clientPort);
    }
    
    private void handleOptions(RequestEvent requestEvent, ServerTransaction serverTransaction) 
            throws Exception {
        Response response = messageFactory.createResponse(Response.OK, requestEvent.getRequest());
        
        javax.sip.header.AllowHeader allow = headerFactory.createAllowHeader(
            "INVITE, ACK, CANCEL, OPTIONS, BYE, MESSAGE"
        );
        response.addHeader(allow);
        
        sendResponse(requestEvent, serverTransaction, response);
    }
    
    private void sendResponse(RequestEvent requestEvent, ServerTransaction serverTransaction, int statusCode) 
            throws Exception {
        Response response = messageFactory.createResponse(statusCode, requestEvent.getRequest());
        sendResponse(requestEvent, serverTransaction, response);
    }
    
    private void sendResponse(RequestEvent requestEvent, ServerTransaction serverTransaction, Response response) 
            throws Exception {
        if (serverTransaction == null) {
            serverTransaction = sipProvider.getNewServerTransaction(requestEvent.getRequest());
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
    
    private boolean isValidExtension(String extension) {
        return extension != null && extension.matches("^\\d{4}$");
    }
    
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        LogUtil.d(TAG, "收到SIP响应: " + responseEvent.getResponse().getStatusCode());
    }
    
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        LogUtil.w(TAG, "SIP事务超时");
    }
    
    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        LogUtil.e(TAG, "SIP IO异常: " + exceptionEvent.getHost() + ":" + exceptionEvent.getPort());
    }
    
    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        LogUtil.d(TAG, "SIP事务终止");
    }
    
    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        LogUtil.d(TAG, "SIP对话终止");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public SipProvider getSipProvider() {
        return sipProvider;
    }
    
    public AddressFactory getAddressFactory() {
        return addressFactory;
    }
    
    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }
    
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }
}

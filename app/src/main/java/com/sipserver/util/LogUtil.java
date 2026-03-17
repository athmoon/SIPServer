package com.sipserver.util;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 日志工具类
 * 使用SLF4J统一日志接口
 */
public class LogUtil {
    
    private static final String TAG = "LogUtil";
    private static boolean initialized = false;
    
    public static void init(Context context) {
        if (initialized) {
            return;
        }
        
        try {
            // 配置logback
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // 设置系统属性
            System.setProperty("log.dir", logDir.getAbsolutePath());
            
            initialized = true;
            i(TAG, "日志系统初始化完成");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "日志初始化失败: " + e.getMessage(), e);
        }
    }
    
    public static void v(String tag, String msg) {
        Logger logger = LoggerFactory.getLogger(tag);
        logger.trace(msg);
        android.util.Log.v(tag, msg);
    }
    
    public static void d(String tag, String msg) {
        Logger logger = LoggerFactory.getLogger(tag);
        logger.debug(msg);
        android.util.Log.d(tag, msg);
    }
    
    public static void i(String tag, String msg) {
        Logger logger = LoggerFactory.getLogger(tag);
        logger.info(msg);
        android.util.Log.i(tag, msg);
    }
    
    public static void w(String tag, String msg) {
        Logger logger = LoggerFactory.getLogger(tag);
        logger.warn(msg);
        android.util.Log.w(tag, msg);
    }
    
    public static void e(String tag, String msg) {
        Logger logger = LoggerFactory.getLogger(tag);
        logger.error(msg);
        android.util.Log.e(tag, msg);
    }
    
    public static void e(String tag, String msg, Throwable tr) {
        Logger logger = LoggerFactory.getLogger(tag);
        logger.error(msg, tr);
        android.util.Log.e(tag, msg, tr);
    }
}

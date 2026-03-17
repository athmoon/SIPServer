# SIP服务端混淆规则

# JAIN-SIP保留规则
-keep class javax.sip.** { *; }
-keep class gov.nist.javax.sip.** { *; }
-keep class gov.nist.core.** { *; }

# 保留所有SIP消息类
-keep class ** extends javax.sip.message.Message { *; }
-keep class ** implements javax.sip.header.Header { *; }

# 保留序列化相关
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留Retrofit/Gson相关（如果有）
-keepattributes Signature
-keepattributes *Annotation*

# 保留自定义类
-keep class com.sipserver.model.** { *; }
-keep class com.sipserver.sip.** { *; }
-keep class com.sipserver.config.** { *; }

# 移除日志（发布版本）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

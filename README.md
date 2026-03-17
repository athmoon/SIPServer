# SIP服务端

运行在Android设备上的SIP服务端应用，支持标准SIP客户端注册、内部通话、SIM卡外呼、来电/短信转发。

## 功能特性

- ✅ SIP 2.0协议支持
- ✅ 4位分机号注册
- ✅ 内部通话（客户端之间）
- ✅ SIM卡外呼（5位及以上号码）
- ✅ 双SIM卡支持
- ✅ 来电转发到SIP客户端
- ✅ 短信转发功能
- ✅ 开机自启动
- ✅ 后台保活
- ✅ Web配置界面

## 技术栈

- **SIP协议栈**: JAIN-SIP
- **最低SDK**: Android 8.0 (API 26)
- **目标SDK**: Android 14 (API 34)
- **编程语言**: Java

## 项目结构

```
SIPServer/
├── app/
│   ├── src/main/
│   │   ├── java/com/sipserver/
│   │   │   ├── sip/           # SIP协议栈实现
│   │   │   ├── service/       # Android后台服务
│   │   │   ├── manager/       # 业务管理器
│   │   │   ├── model/         # 数据模型
│   │   │   ├── ui/            # 用户界面
│   │   │   ├── broadcast/     # 广播接收器
│   │   │   ├── config/        # 配置管理
│   │   │   └── util/          # 工具类
│   │   └── res/               # 资源文件
│   └── build.gradle           # 模块构建配置
├── docs/
│   ├── 配置说明文档.md
│   └── 故障排查指南.md
├── build.gradle               # 项目构建配置
└── settings.gradle            # 项目设置
```

## 编译构建

### 环境要求

- JDK 17+
- Android SDK 34
- Gradle 8.0+

### 编译命令

```bash
# 编译Debug版本
./gradlew assembleDebug

# 编译Release版本
./gradlew assembleRelease

# 输出APK位置
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

## 安装运行

### 通过ADB安装

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 通过Android Studio

1. 打开项目
2. 点击Run按钮
3. 选择目标设备

## 快速开始

1. 安装APK到Android设备
2. 启动应用，授予所有权限
3. 点击"启动服务"
4. 使用SIP软电话（如Zoiper）注册：
   - 服务器地址：设备IP
   - 端口：5060
   - 分机号：任意4位数字（如1001）
5. 拨打测试：
   - 拨打其他4位分机号 → 内部通话
   - 拨打外部号码 → 通过SIM卡外呼

## 配置说明

详见 [配置说明文档](docs/配置说明文档.md)

## 故障排查

详见 [故障排查指南](docs/故障排查指南.md)

## 开发计划

- [ ] SIP认证（Digest Authentication）
- [ ] TLS加密
- [ ] STUN/TURN支持
- [ ] WebSocket传输
- [ ] Web管理界面
- [ ] 多路通话支持
- [ ] 通话录音

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request。

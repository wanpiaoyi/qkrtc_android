# 一.	运行环境
    安卓5.0（api 21）及以上系统
# 二.	快速接入方式
# 1.导入依赖
在项目的gradle文件中加入maven地址

    maven { url "https://dl.bintray.com/shxm/maven" }
 
在build.gradle中加入以下依赖

    implementation 'com.qukan.qkwebrtcsdk:qkrtc:1.0.1'

    api 'implementation 'org.webrtc:libwebrtc:1.0.4'
    
    api 'com.google.code.gson:gson:2.8.5'
    
    implementation 'com.tencent.bugly:crashreport:2.6.6.1'
    
    implementation('io.socket:socket.io-client:0.9.0') {
        exclude group: 'org.json', module: 'json'
    }
# 2.添加权限
      <uses-permission android:name="android.permission.CAMERA" />

      <uses-permission android:name="android.permission.RECORD_AUDIO" />

      <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

      <uses-permission android:name="android.permission.INTERNET" />

      <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

# 3.申请appKey和appSecret

# 三.    屏幕共享的环境
      安卓5.0（api 21）及以上系统，target sdk 26
        当一个 Android 系统上的后台 App 在持续使用 CPU 时，很容易会被系统强行杀掉，而且屏幕分享本身又必然会消耗 CPU。要解决这个看似矛盾的冲突，我们需要在 App 启动屏幕分享的同时，在 Android       系统上弹出悬浮窗。由于 Android 不会强杀包含前台 UI 的 App 进程，因此该种方案可以让您的 App 可以持续进行屏幕分享而不被系统自动回收，具体实现参考demo
        
# 四.    demo流程
        
     获取sdk rtc实例 -》开启摄像头预览-》添加sdk回调并进入房间-》开始推流和显示远端用户流-》离开房间

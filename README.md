# 一.	运行环境
    安卓5.0（api 21）及以上系统
# 二.	快速接入方式
# 1.导入依赖
在项目的gradle文件中加入maven地址

    maven { url "https://dl.bintray.com/shxm/maven" }
 
在build.gradle中加入以下依赖

    implementation 'com.qukan.qkwebrtcsdk:qkrtc:1.0.1'

    api 'org.webrtc:google-webrtc:1.0.+'
    
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

# 3.初始化sdk
      QkWebRTC.init(getApplication(), true);

# 4.申请appKey和appSecret
# 5.登录鉴权
      QkCallManager.getInstance().initCallManager();

      QkCallManager.getInstance().sendLogin(qkLoginAuth, callback)

鉴权成功后进入对应房间即可正常使用

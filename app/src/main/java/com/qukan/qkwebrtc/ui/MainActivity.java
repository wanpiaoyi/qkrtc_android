package com.qukan.qkwebrtc.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qukan.qkwebrtc.HMACSHA1;
import com.qukan.qkwebrtc.R;
import com.qukan.qkwebrtcsdk.bean.QkEnterBean;
import com.qukan.qkwebrtcsdk.util.QkRTCDef;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private EditText etId;
    private EditText etRoomId;
    private Button btnEnter;
    private RadioGroup rbRole;
    private boolean isMeeting = true;
    private String mSelfId = "111";
    private String mRoomId = "558302072";
    private String mRole = QkRTCDef.RULE_PARTNER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //防止切换到后台后重新启动应用
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        rbRole = findViewById(R.id.rb_role);
        etRoomId = findViewById(R.id.et_room);
        etRoomId.setText(mRoomId);
        etId = findViewById(R.id.et_id);
        etId.setText(mSelfId);
        btnEnter = findViewById(R.id.btn_enter);
        btnEnter.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etRoomId.getText())) {
                mRoomId = etRoomId.getText().toString();
            }
            if (!TextUtils.isEmpty(etId.getText())) {
                mSelfId = etId.getText().toString();
            }
            if (isMeeting) {
                loginMeeting();
            } else {
                login();
            }
        });

        RadioGroup radioGroup = findViewById(R.id.rb_type);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isMeeting = checkedId == R.id.rb_meeting;
            if (checkedId == R.id.rb_meeting) {
                rbRole.setVisibility(View.VISIBLE);
            } else {
                rbRole.setVisibility(View.GONE);
            }
        });
        rbRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_partner) {
                mRole = QkRTCDef.RULE_PARTNER;
            } else {
                mRole = QkRTCDef.RULE_WATCHER;
            }
        });
    }

    //加入sdk匿名房间
    //base64(hmacsha1(session_id_role_roomId))
    @SuppressLint("CheckResult")
    private void login() {
        String appKey = "appKey";
        String sig = "";
        String session = UUID.randomUUID().toString();
        String role = QkRTCDef.RULE_PARTNER;
        try {
            String content = session + "_" + mSelfId + "_" + role + "_" + mRoomId;
            sig = new String(Base64.encode(HMACSHA1.HmacSHA1Encrypt(content, "appSecret"),
                    Base64.NO_WRAP), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e("error", e.getMessage());
        }
        QkEnterBean qkEnterBean = new QkEnterBean();
        qkEnterBean.appKey = appKey;
        qkEnterBean.sig = sig;
        qkEnterBean.session = session;
        qkEnterBean.roomId = mRoomId;
        qkEnterBean.id = mSelfId;
        qkEnterBean.saveRecord = true;
        qkEnterBean.role = role;
        qkEnterBean.isCloudMeeting = false;
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        startActivity(QKRTCDemoActivity.getIntent(this, qkEnterBean));
                    } else {
                        Toast.makeText(this, "需要权限", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //加入趣看云会议房间
    @SuppressLint("CheckResult")
    private void loginMeeting() {
        String appKey = "appkey";
        String sig = "";
        String session = UUID.randomUUID().toString();
        String role = mRole;
        try {
            String content = session + "_" + mSelfId + "_" + role + "_" + mRoomId;
            sig = new String(Base64.encode(HMACSHA1.HmacSHA1Encrypt(content, "appSecret"),
                    Base64.NO_WRAP), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e("error", e.getMessage());
        }
        QkEnterBean qkEnterBean = new QkEnterBean();
        qkEnterBean.appKey = appKey;
        qkEnterBean.sig = sig;
        qkEnterBean.session = session;
        qkEnterBean.roomId = mRoomId;
        qkEnterBean.id = mSelfId;
        qkEnterBean.battery = 100;
        qkEnterBean.clientType = "android";
        qkEnterBean.name = "hui" + new Random().nextInt(100 + 1);
        qkEnterBean.role = role;
        qkEnterBean.isCloudMeeting = true;
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        startActivity(QKMeetingDemoActivity.getIntent(this, qkEnterBean));
                    } else {
                        Toast.makeText(this, "需要权限", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

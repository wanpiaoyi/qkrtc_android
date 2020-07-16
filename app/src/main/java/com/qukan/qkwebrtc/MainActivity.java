package com.qukan.qkwebrtc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.qukan.qkwebrtcsdk.QkCallManager;
import com.qukan.qkwebrtcsdk.QkWebRTC;
import com.qukan.qkwebrtcsdk.bean.QkLoginAuth;
import com.qukan.qkwebrtcsdk.callback.QkEnterRoomCallback;
import com.qukan.qkwebrtcsdk.callback.QkLoginCallback;
import com.qukan.qkwebrtcsdk.util.ToastUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;


public class MainActivity extends AppCompatActivity {

    private EditText etId;
    private EditText etRoomId;
    private Button btnLogin;
    private Button btnEnter;
    private boolean isLand = false;
    private String mSelfId = "4";
    private String mRoomId = "111";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QkWebRTC.init(getApplication(), true);
        //QkCallManager.getInstance().setServiceUrl("http://116.52.2.51")
        QkCallManager.getInstance().initCallManager();
        etRoomId = findViewById(R.id.et_room);
        etId = findViewById(R.id.et_id);
        btnLogin = findViewById(R.id.btn_login);
        btnEnter = findViewById(R.id.btn_enter);
        btnLogin.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etId.getText())) {
                mSelfId = etId.getText().toString();
            }
            long nonce = System.currentTimeMillis();
            QkLoginAuth qkLoginAuth = new QkLoginAuth();
            qkLoginAuth.setAppKey("appKey");
            qkLoginAuth.setSig("sig");
            qkLoginAuth.setId(mSelfId);
            qkLoginAuth.setNonce(nonce);
            QkCallManager.getInstance().sendLogin(qkLoginAuth, new QkLoginCallback() {

                @Override
                public void onLoginSuccess() {
                    runOnUiThread(() -> ToastUtils.shortShow("登录成功"));
                }

                @Override
                public void onLoginFail(String errorMsg) {
                    runOnUiThread(() -> ToastUtils.shortShow(errorMsg));
                }
            });
        });
        btnEnter.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(etRoomId.getText())) {
                mRoomId = etRoomId.getText().toString();
            }
            QkCallManager.getInstance().enterRoom(mRoomId, new QkEnterRoomCallback() {
                @SuppressLint("CheckResult")
                @Override
                public void onEnterSuccess() {
                    runOnUiThread(() -> {
                        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
                        rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                .subscribe(b -> {
                                    if (b) {
                                        if (isLand) {
                                            startActivity(ScreenLandActivity.getIntent(MainActivity.this, mSelfId));
                                        } else {
                                            startActivity(ScreenConnectActivity.getIntent(MainActivity.this, mSelfId));
                                        }
                                    } else {
                                        ToastUtils.shortShow("需要权限");
                                    }
                                });
                    });
                }

                @Override
                public void onEnterFail(String errorMsg) {
                    runOnUiThread(() -> ToastUtils.shortShow(errorMsg));
                }
            });
        });

        RadioGroup radioGroup = findViewById(R.id.rb_orientation);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> isLand = checkedId == R.id.rb_land);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //断开socket连接
        QkCallManager.getInstance().disconnect();
    }
}

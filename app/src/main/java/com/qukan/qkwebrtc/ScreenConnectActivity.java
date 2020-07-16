package com.qukan.qkwebrtc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.qukan.qkwebrtcsdk.QkCallManager;
import com.qukan.qkwebrtcsdk.WebRtcHelper;
import com.qukan.qkwebrtcsdk.callback.QkRoomCallback;
import com.qukan.qkwebrtcsdk.util.LogUtil;
import com.qukan.qkwebrtcsdk.util.ToastUtils;
import com.qukan.qkwebrtcsdk.widget.RtcVideoView;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

public class ScreenConnectActivity extends AppCompatActivity implements View.OnClickListener, QkRoomCallback {

    private Context mContext;
    private ImageView ivOff;
    private ImageView ivSwitch;
    private ImageView ivCamera;
    private ImageView ivVoice;
    private TextView tvQuality;
    private Button btnPush;
    private WebRtcVideoViewLayout mVideoViewLayout;
    private RtcVideoView mLocalVideoView;
    private WebRtcHelper mWebRtcHelper;

    protected static final String TAG = "ScreenConnectActivity";

    private boolean isVideoOpen = true;
    private boolean isAudioOpen = true;
    private boolean isFrontCamera = true;
    private boolean isStartPush = false;
    //0 超清 1 高清 2普通 3流畅
    private int mDpi = 1;
    private String[] items = new String[]{"超清", "高清", "标清", "流畅"};

    protected static Intent getIntent(Context context, String id) {
        Intent intent = new Intent(context, ScreenConnectActivity.class);
        intent.putExtra(TAG, id);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_connect);
        mContext = this;
        String mSelfMemberId = getIntent().getStringExtra(TAG);

        ivOff = findViewById(R.id.iv_off);
        ivOff.setOnClickListener(this);
        btnPush = findViewById(R.id.btn_push);
        btnPush.setOnClickListener(this);
        ivSwitch = findViewById(R.id.iv_switch);
        ivSwitch.setOnClickListener(this);
        ivCamera = findViewById(R.id.iv_camera);
        ivCamera.setOnClickListener(this);
        ivVoice = findViewById(R.id.iv_voice);
        ivVoice.setOnClickListener(this);
        tvQuality = findViewById(R.id.tv_dpi);
        tvQuality.setOnClickListener(this);
        mVideoViewLayout = findViewById(R.id.video_view_layout);

        mWebRtcHelper = new WebRtcHelper(mContext);
        //初始化摄像头
        mWebRtcHelper.initCameraView(isFrontCamera);
        //设置分辨率，帧率，最大码率
        mWebRtcHelper.setCaptureFormat(WebRtcHelper.SIZE_1280X720, WebRtcHelper.FPS_DEFAULT, WebRtcHelper.BITRATE_DEFAULT);
        mWebRtcHelper.registerRoomCallback(this);

        mVideoViewLayout.setUserId(mSelfMemberId);
        mVideoViewLayout.setRootEglBase(mWebRtcHelper.getEglBase());
        mLocalVideoView = mVideoViewLayout.getSurfaceViewByIndex(0);
        mLocalVideoView.setMirror(isFrontCamera);
        mLocalVideoView.setUserId(mSelfMemberId);
        mLocalVideoView.setVisibility(View.VISIBLE);

        mWebRtcHelper.setLocalView(mLocalVideoView);
        mVideoViewLayout.onRoomEnter();
        //拉流
        mWebRtcHelper.startPull();
        //推流
        mWebRtcHelper.startPush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume");
        mWebRtcHelper.startCapture();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.d(TAG, "onStop");
        mWebRtcHelper.stopCapture();
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        try {
            mVideoViewLayout.release();
            mWebRtcHelper.unregisterRoomCallback();
            mWebRtcHelper.destroy();
            mWebRtcHelper = null;
        } catch (Exception e) {
            LogUtil.e(e.toString());
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == ivOff) {
            exitRoom();
        } else if (v == ivSwitch) {
            isFrontCamera = !isFrontCamera;
            mLocalVideoView.setMirror(isFrontCamera);
            mWebRtcHelper.switchCamera(isFrontCamera);
        } else if (v == ivVoice) {
            isAudioOpen = !isAudioOpen;
            ivVoice.setImageResource(isAudioOpen ? R.drawable.voic : R.drawable.close_voic);
            mWebRtcHelper.enableAudio(isAudioOpen);
        } else if (v == ivCamera) {
            isVideoOpen = !isVideoOpen;
            ivCamera.setImageResource(isVideoOpen ? R.drawable.camera : R.drawable.close_camera);
            mWebRtcHelper.enableVideo(isVideoOpen);
        } else if (v == tvQuality) {
            showChooseDpiDialog();
        } else if (v == btnPush) {
            if (!isStartPush) {
                mWebRtcHelper.startPush();
            } else {
                mWebRtcHelper.stopPush();
            }
        }
    }

    @Override
    public void onRoomMemberPush(String id, MediaStreamTrack track) {
        //可以单独对视频流和音频流做处理
        if (track instanceof VideoTrack) {
            runOnUiThread(() -> {
                RtcVideoView videoView = mVideoViewLayout.onMemberEnter(id);
                if (videoView != null) {
                    videoView.setVisibility(View.VISIBLE);
                    VideoTrack remoteVideoTrack = (VideoTrack) track;
                    remoteVideoTrack.setEnabled(true);
                    remoteVideoTrack.addSink(videoView);
                }
            });
        }
        if (track instanceof AudioTrack) {
            AudioTrack remoteAudioTrack = (AudioTrack) track;
            remoteAudioTrack.setEnabled(true);
        }
    }

    @Override
    public void onRoomMemberStopPush(String id) {
        runOnUiThread(() -> mVideoViewLayout.onMemberLeave(id));
    }

    @Override
    public void onRoomMemberAdd(String id) {
        LogUtil.d(TAG, "有用户进入房间" + id);
    }

    @Override
    public void onRoomMemberLeave(String id) {
        LogUtil.d(TAG, "有用户离开房间" + id);
    }

    @Override
    public void onStopPush() {
        isStartPush = false;
        runOnUiThread(() -> btnPush.setText("开始推流"));
    }

    @Override
    public void onStartPush() {
        isStartPush = true;
        runOnUiThread(() -> btnPush.setText("停止推流"));
    }

    @Override
    public void onLoginOut() {
        runOnUiThread(() ->  ToastUtils.shortShow("该用户在其他地方登陆"));
        finish();
    }

    @Override
    public void onError(String msg) {
        LogUtil.e(TAG, msg);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                exitRoom();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                mWebRtcHelper.addVoice();
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mWebRtcHelper.subVoice();
                break;
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void showChooseDpiDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setSingleChoiceItems(items, mDpi, (dialog, which) -> {
                    mDpi = which;
                    tvQuality.setText(items[mDpi]);
                    if (mDpi == 0) {
                        mWebRtcHelper.changeCaptureFormat(WebRtcHelper.SIZE_1920X1080, 25, 3000);
                    } else if (mDpi == 1) {
                        mWebRtcHelper.changeCaptureFormat(WebRtcHelper.SIZE_1280X720, 20, 1200);
                    } else if (mDpi == 2){
                        mWebRtcHelper.changeCaptureFormat(WebRtcHelper.SIZE_960X540, 20, 900);
                    } else {
                        mWebRtcHelper.changeCaptureFormat(WebRtcHelper.SIZE_640X480, 20, 600);
                    }
                    dialog.dismiss();
                })
                .create();
        alertDialog.show();
    }

    private void exitRoom() {
        QkCallManager.getInstance().leaveRoom();
        finish();
    }
}

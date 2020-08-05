package com.qukan.qkwebrtc.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qukan.qkwebrtc.R;
import com.qukan.qkwebrtc.Utils;
import com.qukan.qkwebrtc.bean.PreviewBean;
import com.qukan.qkwebrtcsdk.QKRTCCloud;
import com.qukan.qkwebrtcsdk.bean.QkEnterBean;
import com.qukan.qkwebrtcsdk.bean.RTCVideoEncParam;
import com.qukan.qkwebrtcsdk.callback.QkRTCListener;
import com.qukan.qkwebrtcsdk.screen.ScreenShareX;
import com.qukan.qkwebrtcsdk.service.QkRTCCode;
import com.qukan.qkwebrtcsdk.util.LogUtil;
import com.qukan.qkwebrtcsdk.util.QkRTCDef;
import com.qukan.qkwebrtcsdk.util.ToastUtils;
import com.qukan.qkwebrtcsdk.widget.RtcVideoView;

import org.webrtc.RendererCommon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QKRTCDemoActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "QKRTCDemoActivity";
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private QKRTCCloud qkrtcCloud;
    private Context mContext;

    private TextView tvQuit;
    private TextView tvCamera;
    private TextView tvMir;
    private TextView tvTime;
    private TextView tvResolution;
    private ImageView ivCamera;
    private ImageView ivMir;
    private ImageView ivScreen;
    private ImageView ivSwitch;
    private FloatingView mFloatingView;// 悬浮球
    private RecyclerView rvList;
    private FrameLayout flContain;

    private QkEnterBean mEnterBean;
    private List<PreviewBean> mList = Collections.synchronizedList(new ArrayList<>());
    private PreViewAdapter adapter;

    private boolean isFront = true;
    private boolean isVideoOpen = true;
    private boolean isAudioOpen = true;
    private boolean isScreenShare = false;
    private String mMainId = "";
    private int mDpi = 1;
    private String[] items = new String[]{"1080p", "720p", "540p"};
    private long mConnectTime = 0L;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            changeTime();
            timeHandler.postDelayed(this, 1000);
        }
    };

    public static Intent getIntent(Context context, QkEnterBean bean) {
        Intent intent = new Intent(context, QKRTCDemoActivity.class);
        intent.putExtra(TAG, new Gson().toJson(bean));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.fullScreen(this);
        setContentView(R.layout.activity_qkrtc_demo);
        mContext = this;

        tvQuit = findViewById(R.id.tv_connect_close);
        tvTime = findViewById(R.id.tv_connect_time);
        tvResolution = findViewById(R.id.tv_resolution);
        tvResolution.setOnClickListener(this);
        rvList = findViewById(R.id.rv_review);
        ivMir = findViewById(R.id.iv_open_mic);
        ivMir.setOnClickListener(this);
        tvMir = findViewById(R.id.tv_mir_state);
        ivCamera = findViewById(R.id.iv_open_camera);
        ivCamera.setOnClickListener(this);
        tvCamera = findViewById(R.id.tv_camera_state);
        tvQuit = findViewById(R.id.tv_connect_close);
        tvQuit.setOnClickListener(this);
        ivScreen = findViewById(R.id.iv_screen_share);
        ivScreen.setVisibility(View.VISIBLE);
        ivScreen.setOnClickListener(this);
        ivSwitch = findViewById(R.id.iv_switch_camera);
        ivSwitch.setOnClickListener(this);
        flContain = findViewById(R.id.fl_contain);
        //悬浮球界面
        mFloatingView = new FloatingView(getApplicationContext(), R.layout.view_floating_default);
        mFloatingView.setPopupWindow(R.layout.screen_popup_layout);
        mFloatingView.setOnPopupItemClickListener(this);

        new Handler().postDelayed(() ->
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED), 1000);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rvList.setLayoutManager(layoutManager);
        adapter = new PreViewAdapter(mList);
        adapter.setOnItemClickListener((adapter, view, position) -> changeMainView(position));
        rvList.setAdapter(adapter);
        updateCamera();
        updateMir();
        initRTC();
    }

    @Override
    protected void onStart() {
        super.onStart();
        timeHandler.postDelayed(timeRunnable, 1000);
        if (qkrtcCloud != null) {
            qkrtcCloud.startLocalPreview(isFront, mList.get(0).rtcVideoView);
        }
        if (mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        timeHandler.removeCallbacks(timeRunnable);
        if (qkrtcCloud != null) {
            qkrtcCloud.stopLocalPreview();
        }
        if (isScreenShare && qkrtcCloud != null) {
            requestDrawOverLays();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qkrtcCloud != null) {
            qkrtcCloud.setListener(null);
            qkrtcCloud.destroyInstance();
            qkrtcCloud = null;
        }
        if (mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        qkrtcCloud.exitRoom();
    }

    @Override
    public void onClick(View v) {
        v.setEnabled(false);
        new Handler().postDelayed(() -> v.setEnabled(true), 500);
        if (v == tvQuit) {
            qkrtcCloud.exitRoom();
        } else if (v == ivSwitch) {
            isFront = !isFront;
            qkrtcCloud.switchCamera(isFront);
        } else if (v == ivCamera) {
            isVideoOpen = !isVideoOpen;
            if (isVideoOpen) {
                qkrtcCloud.startLocalPreview(isFront, mList.get(0).rtcVideoView);
            } else {
                qkrtcCloud.stopLocalPreview();
            }
            updateCamera();
        } else if (v == ivMir) {
            isAudioOpen = !isAudioOpen;
            qkrtcCloud.enableAudio(isAudioOpen);
            updateMir();
        } else if (v == tvResolution) {
            showChooseDpiDialog();
        } else if (v == ivScreen) {
            changeScreenShare();
        } else if (v.getId() == R.id.btn_return) {
            //悬浮球返回主界面按钮
            //需要允许应用从后台弹出应用权限
            Toast.makeText(getApplicationContext(), "返回主界面", Toast.LENGTH_SHORT).show();
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                pendingIntent.send();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Utils.fullScreen(this);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changeOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(mContext)) {
                Toast.makeText(getApplicationContext(), "请在设置-权限设置里打开悬浮窗权限", Toast.LENGTH_SHORT).show();
            } else {
                showFloatingView();
            }
        }
    }

    private void initRTC() {
        String dataJson;
        if (getIntent().getExtras() != null) {
            dataJson = getIntent().getExtras().getString(TAG, "");
            mEnterBean = new Gson().fromJson(dataJson, QkEnterBean.class);
        } else {
            ToastUtils.shortShow("参数错误");
            return;
        }
        qkrtcCloud = QKRTCCloud.getInstance(mContext);
        qkrtcCloud.setDebugMode(true);
        qkrtcCloud.enableBeauty(true);
        RtcVideoView rtcVideoView = createMainView();
        PreviewBean mSelfBean = new PreviewBean(mEnterBean.id, rtcVideoView, false, false);
        mSelfBean.isShowLoading = false;
        mList.add(mSelfBean);
        adapter.notifyItemInserted(mList.size());
        mMainId = mSelfBean.id;
        flContain.addView(rtcVideoView);
        qkrtcCloud.startLocalPreview(isFront, rtcVideoView);
        qkrtcCloud.setListener(new QkRTCListener() {
            @Override
            public void onEnterRoom() {
                LogUtil.d("加入房间成功");
                qkrtcCloud.startPush();
            }

            @Override
            public void onExitRoom() {
                LogUtil.d("离开房间成功");
                qkrtcCloud.destroyInstance();
                qkrtcCloud = null;
                finish();
            }

            @Override
            public void onError(int errCode, String errMsg) {
                LogUtil.e("errCode=" + errCode + " errMsg=" + errMsg);
                if (errCode == QkRTCCode.ERR_ROOM_ENTER_FAIL) {
                    //加入房间失败
                    ToastUtils.shortShow(errMsg);
                    //finish();
                } else if (errCode == QkRTCCode.ERR_ROOM_LEAVE_FAIL) {
                    qkrtcCloud.destroyInstance();
                    qkrtcCloud = null;
                    finish();
                } else if (errCode == QkRTCCode.ERR_SCREEN_FAIL) {
                    qkrtcCloud.stopScreenCapture();
                    ToastUtils.shortShow(errMsg);
                    for (int i = 0; i < mList.size(); i++) {
                        if (mList.get(i).id.equals(mEnterBean.id + QkRTCDef.TYPE_SCREEN)) {
                            mList.remove(i);
                            adapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onUserVideoAvailable(String id, boolean available) {
                LogUtil.d(id + "推流" + available);
                if (available) {
                    RtcVideoView rtcVideoView = createPreView();
                    qkrtcCloud.startRemoteView(id, QkRTCDef.TYPE_MAIN, rtcVideoView);
                    mList.add(new PreviewBean(id, rtcVideoView, true, false));
                    adapter.notifyItemInserted(mList.size());
                } else {
                    qkrtcCloud.stopRemoteView(id, QkRTCDef.TYPE_MAIN);
                    for (int i = mList.size() - 1; i >= 0; i--) {
                        if (mList.get(i).userId.equals(id)) {
                            mList.remove(i);
                            adapter.notifyItemRemoved(i);
                        }
                    }
                    if (mMainId.equals(id) && !mList.isEmpty()) {
                        changeMainView(0);
                    }
                }
            }

            @Override
            public void onUserScreenAvailable(String id, boolean available) {
                LogUtil.d(id + "屏幕分享" + available);
                if (available) {
                    RtcVideoView rtcVideoView = createPreView();
                    mList.add(new PreviewBean(id, rtcVideoView, true, true));
                    adapter.notifyItemInserted(mList.size());
                    qkrtcCloud.startRemoteView(id, QkRTCDef.TYPE_SCREEN, rtcVideoView);
                } else {
                    qkrtcCloud.stopRemoteView(id, QkRTCDef.TYPE_SCREEN);
                    for (int i = mList.size() - 1; i >= 0; i--) {
                        if (mList.get(i).userId.equals(id) && mList.get(i).isScreen) {
                            mList.get(i).rtcVideoView.release();
                            mList.remove(i);
                            adapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                    if (mMainId.equals(id + QkRTCDef.TYPE_SCREEN) && !mList.isEmpty()) {
                        changeMainView(0);
                    }
                }
            }

            @Override
            public void onFirstFrameRendered(String id, String type) {
                boolean isScreen = type.equals(QkRTCDef.TYPE_SCREEN);
                for (int i = 0; i < mList.size(); i++) {
                    PreviewBean bean = mList.get(i);
                    if (bean.userId.equals(id) && bean.isScreen == isScreen) {
                        bean.isShowLoading = false;
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            }

            @Override
            public void onOffLine() {
                ToastUtils.shortShow("账号在其他设备登录");
                finish();
            }
        });
        qkrtcCloud.enterRoom(mEnterBean);
    }

    private void updateCamera() {
        if (isVideoOpen) {
            ivCamera.setImageResource(R.drawable.main_ic_connect_camera_open);
            tvCamera.setText("关闭摄像头");
        } else {
            ivCamera.setImageResource(R.drawable.main_ic_connect_camera_close);
            tvCamera.setText("开启摄像头");
        }
    }

    private void updateMir() {
        if (isAudioOpen) {
            ivMir.setImageResource(R.drawable.main_ic_connect_mir_open);
            tvMir.setText("关闭麦克风");
        } else {
            ivMir.setImageResource(R.drawable.main_ic_connect_mic_close);
            tvMir.setText("开启麦克风");
        }
    }

    public void requestDrawOverLays() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(mContext)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mContext.getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            showFloatingView();
        }
    }

    private void showFloatingView() {
        if (!mFloatingView.isShown()) {
            if ((null != qkrtcCloud)) {
                mFloatingView.show();
                mFloatingView.setOnPopupItemClickListener(this);
            }
        }
    }

    private void changeScreenShare() {
        if (isScreenShare) {
            isScreenShare = false;
            qkrtcCloud.stopScreenCapture();
            ToastUtils.shortShow("关闭屏幕分享");
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i).id.equals(mEnterBean.id + QkRTCDef.TYPE_SCREEN)) {
                    mList.get(i).rtcVideoView.release();
                    mList.remove(i);
                    adapter.notifyItemRemoved(i);
                    break;
                }
            }
            if (mMainId.equals(mEnterBean.id + QkRTCDef.TYPE_SCREEN) && !mList.isEmpty()) {
                changeMainView(0);
            }
        } else {
            new ScreenShareX(this).requestScreenShare((isGranted, data) -> {
                if (isGranted) {
                    isScreenShare = true;
                    RtcVideoView videoView = createPreView();
                    videoView.setUserId("screen");
                    PreviewBean bean = new PreviewBean(mEnterBean.id, videoView, true, true);
                    bean.isShowLoading = false;
                    mList.add(bean);
                    adapter.notifyItemInserted(mList.size());
                    qkrtcCloud.startScreenCapture(data, bean.rtcVideoView);
                } else {
                    ToastUtils.shortShow("取消屏幕分享");
                }
            });
        }
    }

    private void showChooseDpiDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setSingleChoiceItems(items, mDpi, (dialog, which) -> {
                    mDpi = which;
                    tvResolution.setText(items[mDpi]);
                    changeCaptureFormat();
                    dialog.dismiss();
                })
                .create();
        alertDialog.show();
    }

    private void changeCaptureFormat() {
        RTCVideoEncParam param = new RTCVideoEncParam();
        if (mDpi == 0) {
            param.videoResolution = QkRTCDef.QK_RTC_RESOLUTION_1080X1920;
        } else if (mDpi == 1) {
            param.videoResolution = QkRTCDef.QK_RTC_RESOLUTION_720X1280;
        } else if (mDpi == 2) {
            param.videoResolution = QkRTCDef.QK_RTC_RESOLUTION_540X960;
        } else {
            param.videoResolution = QkRTCDef.QK_RTC_RESOLUTION_720X1280;
        }
        qkrtcCloud.setVideoEncoderParam(param);
    }

    private void changeMainView(int prePos) {
        if (mList.get(prePos).id.equals(mMainId)) {
            return;
        }
        for (int i = 0; i < mList.size(); i++) {
            PreviewBean mainBean = mList.get(i);
            if (mainBean.id.equals(mMainId)) {
                FrameLayout.LayoutParams layoutParams = new
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mainBean.rtcVideoView.setLayoutParams(layoutParams);
                mainBean.rtcVideoView.clearImage();
                mainBean.rtcVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                flContain.removeAllViews();
                mainBean.isShow = true;
                adapter.notifyItemChanged(i);
                break;
            }
        }

        PreviewBean preBean = mList.get(prePos);
        FrameLayout.LayoutParams preParams = new
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        preParams.gravity = Gravity.CENTER;
        preBean.rtcVideoView.clearImage();
        preBean.rtcVideoView.setLayoutParams(preParams);
        preBean.rtcVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        FrameLayout frameLayout = (FrameLayout) preBean.rtcVideoView.getParent();
        if (frameLayout != null) {
            frameLayout.removeAllViews();
        }

        flContain.addView(preBean.rtcVideoView);
        mList.get(prePos).isShow = false;
        adapter.notifyItemChanged(prePos);
        mMainId = mList.get(prePos).id;
    }

    private RtcVideoView createMainView() {
        RtcVideoView rtcVideoView = new RtcVideoView(mContext);
        FrameLayout.LayoutParams layoutParams = new
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        rtcVideoView.setLayoutParams(layoutParams);
        rtcVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        return rtcVideoView;
    }

    private RtcVideoView createPreView() {
        RtcVideoView rtcVideoView = new RtcVideoView(mContext);
        FrameLayout.LayoutParams layoutParams = new
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rtcVideoView.setLayoutParams(layoutParams);
        rtcVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        return rtcVideoView;
    }

    private void changeTime() {
        mConnectTime++;
        tvTime.setText(Utils.getTimerStr(mConnectTime, ":"));
    }

    private void changeOrientation(boolean isLand) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        RelativeLayout.LayoutParams rvReviewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin110 = Utils.dip2px(mContext, 110f);
        int margin10 = Utils.dip2px(mContext, 10f);
        if (isLand) {
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            rvReviewParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            rvReviewParams.addRule(RelativeLayout.BELOW, R.id.ll_top_button);
            rvReviewParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            rvReviewParams.setMarginEnd(margin10);
        } else {
            layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            rvReviewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            rvReviewParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            rvReviewParams.bottomMargin = margin110;
        }
        rvList.setLayoutParams(rvReviewParams);
        rvList.setLayoutManager(layoutManager);
        adapter.setNewData(mList);
    }
}

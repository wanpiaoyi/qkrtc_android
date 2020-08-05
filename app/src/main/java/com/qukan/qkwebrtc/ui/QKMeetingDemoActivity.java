package com.qukan.qkwebrtc.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.qukan.qkwebrtc.R;
import com.qukan.qkwebrtc.Utils;
import com.qukan.qkwebrtc.bean.PreviewBean;
import com.qukan.qkwebrtcsdk.QKMeetingCloud;
import com.qukan.qkwebrtcsdk.bean.PushBean;
import com.qukan.qkwebrtcsdk.bean.QkEnterBean;
import com.qukan.qkwebrtcsdk.bean.QkMemberBean;
import com.qukan.qkwebrtcsdk.bean.RTCVideoEncParam;
import com.qukan.qkwebrtcsdk.callback.QkMeetingListener;
import com.qukan.qkwebrtcsdk.service.QkRTCCode;
import com.qukan.qkwebrtcsdk.util.LogUtil;
import com.qukan.qkwebrtcsdk.util.QkRTCDef;
import com.qukan.qkwebrtcsdk.util.ToastUtils;
import com.qukan.qkwebrtcsdk.widget.RtcVideoView;

import org.webrtc.RendererCommon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QKMeetingDemoActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "QKMeetingDemoActivity";
    private QKMeetingCloud qkMeetingCloud;
    private Context mContext;

    private TextView tvQuit;
    private TextView tvCamera;
    private TextView tvMir;
    private TextView tvTime;
    private TextView tvWatcher;
    private TextView tvResolution;
    private ImageView ivCamera;
    private ImageView ivMir;
    private ImageView ivSwitch;
    private RecyclerView rvList;
    private FrameLayout flContain;
    private LinearLayout llTop;

    private QkEnterBean mEnterBean;
    private String mRole;
    private List<PreviewBean> mList = Collections.synchronizedList(new ArrayList<>());
    private MeetingAdapter adapter;

    private boolean isFront = true;
    private boolean isVideoOpen = true;
    private boolean isAudioOpen = true;
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
        Intent intent = new Intent(context, QKMeetingDemoActivity.class);
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
        tvWatcher = findViewById(R.id.tv_watcher);
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
        ivSwitch = findViewById(R.id.iv_switch_camera);
        ivSwitch.setOnClickListener(this);
        flContain = findViewById(R.id.fl_contain);
        llTop = findViewById(R.id.ll_top_button);
        new Handler().postDelayed(() ->
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED), 1000);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rvList.setLayoutManager(layoutManager);
        adapter = new MeetingAdapter(mList);
        adapter.setOnItemClickListener((adapter, view, position) -> showActionDialog(position));
        rvList.setAdapter(adapter);
        initRTC();
    }

    @Override
    protected void onStart() {
        super.onStart();
        timeHandler.postDelayed(timeRunnable, 1000);
        if (qkMeetingCloud != null && !mRole.equals(QkRTCDef.RULE_WATCHER)) {
            qkMeetingCloud.startLocalPreview(isFront, mList.get(0).rtcVideoView);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        timeHandler.removeCallbacks(timeRunnable);
        if (qkMeetingCloud != null) {
            qkMeetingCloud.stopLocalPreview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qkMeetingCloud != null) {
            qkMeetingCloud.setListener(null);
            qkMeetingCloud.destroyInstance();
            qkMeetingCloud = null;
        }
    }

    @Override
    public void onBackPressed() {
        qkMeetingCloud.exitRoom();
    }

    @Override
    public void onClick(View v) {
        v.setEnabled(false);
        new Handler().postDelayed(() -> v.setEnabled(true), 500);
        if (v == tvQuit) {
            qkMeetingCloud.exitRoom();
        } else if (v == ivSwitch) {
            isFront = !isFront;
            qkMeetingCloud.switchCamera(isFront);
        } else if (v == ivCamera) {
            isVideoOpen = !isVideoOpen;
            if (isVideoOpen) {
                qkMeetingCloud.startLocalPreview(isFront, mList.get(0).rtcVideoView);
            } else {
                qkMeetingCloud.stopLocalPreview();
            }
            updateCamera();
        } else if (v == ivMir) {
            isAudioOpen = !isAudioOpen;
            qkMeetingCloud.enableAudio(isAudioOpen);
            updateMir();
        } else if (v == tvResolution) {
            showChooseDpiDialog();
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

    private void initRTC() {
        String dataJson;
        if (getIntent().getExtras() != null) {
            dataJson = getIntent().getExtras().getString(TAG, "");
            mEnterBean = new Gson().fromJson(dataJson, QkEnterBean.class);
            mRole = mEnterBean.role;
        } else {
            ToastUtils.shortShow("参数错误");
            return;
        }
        qkMeetingCloud = QKMeetingCloud.getInstance(mContext);
        qkMeetingCloud.setDebugMode(true);
        qkMeetingCloud.enableBeauty(true);
        if (!mRole.equals(QkRTCDef.RULE_WATCHER)) {
            RtcVideoView rtcVideoView = createMainView();
            PreviewBean mSelfBean = new PreviewBean(mEnterBean.id, rtcVideoView, false, false);
            mSelfBean.name = mEnterBean.name;
            mSelfBean.isAudioOpen = isAudioOpen;
            mSelfBean.isVideoOpen = isVideoOpen;
            mMainId = mSelfBean.id;
            mList.add(mSelfBean);
            adapter.notifyItemInserted(mList.size());
            flContain.addView(rtcVideoView);
            qkMeetingCloud.startLocalPreview(isFront, rtcVideoView);
        }
        qkMeetingCloud.setListener(new QkMeetingListener() {
            @Override
            public void onEnterRoom() {
                LogUtil.d("加入房间成功");
                if (!mRole.equals(QkRTCDef.RULE_WATCHER)) {
                    qkMeetingCloud.startPush();
                } else {
                    watcherHideAction(View.GONE);
                }
            }

            @Override
            public void onExitRoom() {
                LogUtil.d("离开房间成功");
                qkMeetingCloud.destroyInstance();
                qkMeetingCloud = null;
                finish();
            }

            @Override
            public void onError(int errCode, String errMsg) {
                LogUtil.e("errCode=" + errCode + " errMsg=" + errMsg);
                if (errCode == QkRTCCode.ERR_ROOM_ENTER_FAIL) {
                    ToastUtils.shortShow(errMsg);
                    //finish();
                } else if (errCode == QkRTCCode.ERR_ROOM_LEAVE_FAIL) {
                    qkMeetingCloud.destroyInstance();
                    qkMeetingCloud = null;
                    finish();
                }
            }

            @Override
            public void onUserVideoAvailable(String id, boolean available) {
                if (available) {
                    QkMemberBean memberBean = qkMeetingCloud.getMemberById(id);
                    RtcVideoView rtcVideoView;
                    PreviewBean bean;
                    if (mMainId.equals("")) {
                        rtcVideoView = createMainView();
                        flContain.addView(rtcVideoView);
                        bean = new PreviewBean(id, rtcVideoView, false, false);
                        qkMeetingCloud.startRemoteView(id, QkRTCDef.TYPE_MAIN, rtcVideoView);
                        mMainId = id;
                    } else {
                        rtcVideoView = createPreView();
                        bean = new PreviewBean(id, rtcVideoView, true, false);
                        qkMeetingCloud.startRemoteView(id, QkRTCDef.TYPE_MAIN, rtcVideoView);
                    }
                    if (memberBean != null) {
                        bean.name = memberBean.getName();
                        for (PushBean pushBean : memberBean.getPushList()) {
                            if (!pushBean.getType().equals(QkRTCDef.TYPE_SCREEN)) {
                                bean.isAudioOpen = pushBean.getAudio() == 1;
                                bean.isVideoOpen = pushBean.getVideo() == 1;
                                break;
                            }
                        }
                    }
                    mList.add(bean);
                    adapter.notifyItemInserted(mList.size());
                } else {
                    qkMeetingCloud.stopRemoteView(id, QkRTCDef.TYPE_MAIN);
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
                    PreviewBean bean = new PreviewBean(id, rtcVideoView, true, true);
                    bean.isAudioOpen = false;
                    bean.isVideoOpen = true;
                    QkMemberBean member = qkMeetingCloud.getMemberById(id);
                    if (member != null) {
                        bean.name = member.getName();
                    }
                    mList.add(bean);
                    adapter.notifyItemInserted(mList.size());
                    qkMeetingCloud.startRemoteView(id, QkRTCDef.TYPE_SCREEN, rtcVideoView);
                } else {
                    qkMeetingCloud.stopRemoteView(id, QkRTCDef.TYPE_SCREEN);
                    for (int i = mList.size() - 1; i >= 0; i--) {
                        if (mList.get(i).userId.equals(id) && mList.get(i).isScreen) {
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
            public void onOffLine() {
                ToastUtils.shortShow("账号在其他设备登录");
                finish();
            }

            @Override
            public void onCameraClose() {
                qkMeetingCloud.stopLocalPreview();
                isVideoOpen = false;
                updateCamera();
                mList.get(0).isVideoOpen = false;
                adapter.notifyItemChanged(0);
            }

            @Override
            public void onRemoteMsgUpdate(QkMemberBean memberBean) {
                for (int i = 0; i < mList.size(); i++) {
                    PreviewBean bean = mList.get(i);
                    if (bean.userId.equals(memberBean.getId())) {
                        for (PushBean pushBean : memberBean.getPushList()) {
                            if (!pushBean.getType().equals(QkRTCDef.TYPE_SCREEN)) {
                                bean.isAudioOpen = pushBean.getAudio() == 1;
                                bean.isVideoOpen = pushBean.getVideo() == 1;
                                break;
                            }
                        }
                        adapter.notifyItemChanged(i);
                    }
                }
            }

            @Override
            public void onKickOut() {
                ToastUtils.shortShow("被管理员踢出会议");
                qkMeetingCloud.exitRoom();
            }

            @Override
            public void onRoomClose(String msg) {
                ToastUtils.shortShow(msg);
                qkMeetingCloud.exitRoom();
            }

            @Override
            public void onMute() {
                qkMeetingCloud.enableAudio(false);
                isAudioOpen = false;
                updateMir();
                mList.get(0).isAudioOpen = false;
                adapter.notifyItemChanged(0);
            }

            @Override
            public void onRoleUpdate(String role) {
                ToastUtils.shortShow("角色变更为" + role);
                mRole = role;
                if (role.equals(QkRTCDef.RULE_WATCHER)) {
                    watcherHideAction(View.GONE);
                    if (!mList.isEmpty() && mList.get(0).userId.equals(mEnterBean.id)) {
                        mList.remove(0);
                        adapter.notifyItemRemoved(0);
                    }
                } else {
                    watcherHideAction(View.VISIBLE);
                    RtcVideoView rtcVideoView;
                    PreviewBean mSelfBean;
                    if (mMainId.equals(mEnterBean.id)) {
                        rtcVideoView = createMainView();
                        mSelfBean = new PreviewBean(mEnterBean.id, rtcVideoView, false, false);
                        mSelfBean.name = mEnterBean.name;
                        mSelfBean.isAudioOpen = isAudioOpen;
                        mSelfBean.isVideoOpen = isVideoOpen;
                        flContain.removeAllViews();
                        flContain.addView(rtcVideoView);
                    } else {
                        rtcVideoView = createPreView();
                        mSelfBean = new PreviewBean(mEnterBean.id, rtcVideoView, true, false);
                        mSelfBean.name = mEnterBean.name;
                        mSelfBean.isAudioOpen = isAudioOpen;
                        mSelfBean.isVideoOpen = isVideoOpen;
                    }
                    if (isVideoOpen) {
                        qkMeetingCloud.startLocalPreview(isFront, mSelfBean.rtcVideoView);
                    }
                    mList.add(0, mSelfBean);
                    adapter.notifyItemInserted(0);
                }
            }

        });
        qkMeetingCloud.enterRoom(mEnterBean);
    }

    private void updateCamera() {
        if (isVideoOpen) {
            ivCamera.setImageResource(R.drawable.main_ic_connect_camera_open);
            tvCamera.setText("关闭摄像头");
        } else {
            ivCamera.setImageResource(R.drawable.main_ic_connect_camera_close);
            tvCamera.setText("开启摄像头");
        }
        mList.get(0).isVideoOpen = isVideoOpen;
        adapter.notifyItemChanged(0);
    }

    private void updateMir() {
        if (isAudioOpen) {
            ivMir.setImageResource(R.drawable.main_ic_connect_mir_open);
            tvMir.setText("关闭麦克风");
        } else {
            ivMir.setImageResource(R.drawable.main_ic_connect_mic_close);
            tvMir.setText("开启麦克风");
        }
        mList.get(0).isAudioOpen = isAudioOpen;
        adapter.notifyItemChanged(0);
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
        qkMeetingCloud.setVideoEncoderParam(param);
    }

    private void showActionDialog(int pos) {
        if (!mEnterBean.id.equals(mList.get(pos).userId)) {
            String[] items = new String[]{"切换画面", "踢出房间", "禁言", "关闭摄像头"};
            AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                    .setItems(items, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                changeMainView(pos);
                                break;
                            case 1:
                                qkMeetingCloud.hangupById(mList.get(pos).userId);
                                break;
                            case 2:
                                qkMeetingCloud.muteById(mList.get(pos).userId);
                                break;
                            case 3:
                                qkMeetingCloud.closeVideoById(mList.get(pos).userId);
                                break;
                        }
                        dialog.dismiss();
                    }).create();
            alertDialog.show();
        } else {
            changeMainView(pos);
        }
    }

    private void changeMainView(int prePos) {
        if (mList.get(prePos).id.equals(mMainId)) {
            return;
        }
        for (int i = 0; i < mList.size(); i++) {
            PreviewBean mainBean = mList.get(i);
            if (mainBean.id.equals(mMainId)) {
                mainBean.rtcVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                FrameLayout.LayoutParams layoutParams = new
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                mainBean.rtcVideoView.setLayoutParams(layoutParams);
                flContain.removeAllViews();
                mainBean.isShow = true;
                adapter.notifyItemChanged(i);
                break;
            }
        }

        PreviewBean preBean = mList.get(prePos);
        preBean.rtcVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        FrameLayout.LayoutParams preParams = new
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        preParams.gravity = Gravity.CENTER;
        preBean.rtcVideoView.setLayoutParams(preParams);
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
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private void watcherHideAction(int visibility) {
        ivMir.setVisibility(visibility);
        tvMir.setVisibility(visibility);
        ivCamera.setVisibility(visibility);
        tvCamera.setVisibility(visibility);
        llTop.setVisibility(visibility);
        tvWatcher.setVisibility(visibility == View.GONE ? View.VISIBLE : View.GONE);
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

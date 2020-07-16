package com.qukan.qkwebrtc;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.qukan.qkwebrtcsdk.widget.RtcVideoView;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;

import java.util.ArrayList;

/**
 * Created by SmallCat on 2019-08-14.
 */
public class WebRtcVideoViewLayout extends RelativeLayout {

    private static final String TAG = "Call";
    public static final int MAX_USER = 7;

    private Context mContext;
    private EglBase mRootEglBase;

    private ArrayList<RtcVideoView> mVideoViewList;
    private ArrayList<LayoutParams> mLayoutParamList;
    private RelativeLayout mLayout;
    private int mCount = 0;
    private String mSelfUserId;

    public WebRtcVideoViewLayout(Context context) {
        super(context);
        initView(context);
    }

    public WebRtcVideoViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public WebRtcVideoViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.webrtc_show_view, this);
        mLayout = findViewById(R.id.ll_mainview);

        initFloatLayoutParams();
        initWebRtcVideoView();
        showView();
    }

    public void setRootEglBase(EglBase mRootEglBase) {
        this.mRootEglBase = mRootEglBase;
        initEglBase();
    }

    public void initFloatLayoutParams() {
        mLayoutParamList = new ArrayList<>();
        LayoutParams layoutParams0 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams0.addRule(RelativeLayout.CENTER_IN_PARENT);
        mLayoutParamList.add(layoutParams0);

        int midMargin = dip2px(10);
        int lrMargin = dip2px(15);
        int bottomMargin = dip2px(20);
        int subWidth = dip2px(120);
        int subHeight = dip2px(80);

        for (int i = 0; i < 3; i++) {
            LayoutParams layoutParams = new LayoutParams(subWidth, subHeight);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            layoutParams.leftMargin = lrMargin;
            layoutParams.bottomMargin = bottomMargin + midMargin * (i + 1) + subHeight * i;

            mLayoutParamList.add(layoutParams);
        }

        for (int i = 0; i < 3; i++) {
            LayoutParams layoutParams = new LayoutParams(subWidth, subHeight);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layoutParams.rightMargin = lrMargin;
            layoutParams.bottomMargin = bottomMargin + midMargin * (i + 1) + subHeight * i;

            mLayoutParamList.add(layoutParams);
        }
    }

    private void initWebRtcVideoView() {
        mVideoViewList = new ArrayList<>();
        RtcVideoView surfaceView0 = new RtcVideoView(mContext);
        surfaceView0.setVisibility(GONE);
        surfaceView0.setMirror(true);
        surfaceView0.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL, RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        surfaceView0.setEnableHardwareScaler(false);
        surfaceView0.setClickable(false);
        surfaceView0.setTag(R.string.str_tag_pos, 0);
        mVideoViewList.add(surfaceView0);

        for (int i = 1; i < MAX_USER; i++) {
            RtcVideoView surfaceView = new RtcVideoView(mContext);
            surfaceView.setVisibility(GONE);
            surfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
            surfaceView.setEnableHardwareScaler(false);
            surfaceView.setClickable(false);
            surfaceView.setTag(R.string.str_tag_pos, i);
            mVideoViewList.add(surfaceView);
        }
    }

    private void showView() {
        mLayout.removeAllViews();
        for (int i = 0; i < mVideoViewList.size(); i++) {
            RtcVideoView surfaceView = mVideoViewList.get(i);
            RelativeLayout.LayoutParams layoutParams = mLayoutParamList.get(i);
            surfaceView.setLayoutParams(layoutParams);
            mLayout.addView(surfaceView);
        }
    }

    private void initEglBase() {
        for (RtcVideoView rtcVideoView : mVideoViewList) {
           rtcVideoView.init(mRootEglBase.getEglBaseContext(), null);
        }
    }

    public void updateLayout() {
        for (int i = 0; i < mVideoViewList.size(); i++) {
            RtcVideoView surfaceViewRenderer = mVideoViewList.get(i);
            if (i < mLayoutParamList.size()) {
                LayoutParams layoutParams = mLayoutParamList.get(i);
                surfaceViewRenderer.setLayoutParams(layoutParams);
            }
            surfaceViewRenderer.setTag(R.string.str_tag_pos, i);
            if (i != 0) {
                surfaceViewRenderer.setClickable(true);
                surfaceViewRenderer.setOnClickListener(v -> {
                    Object object = v.getTag(R.string.str_tag_pos);
                    if (object != null) {
                        int pos = (int) object;
                        RtcVideoView renderView = (RtcVideoView) v;
                        Log.i(TAG, "click on pos: " + pos + "/userId: " + renderView.getUserId());
                        if (null != renderView.getUserId()) {
                            swapViewByIndex(0, pos);
                        }
                    }

                });
            } else {
                surfaceViewRenderer.setClickable(false);
            }
            /*if (i != 0) {
                surfaceViewRenderer.bringToFront();
                //mLayout.bringChildToFront(surfaceViewRenderer);
            }*/
        }
        showView();
    }

    public void swapViewByIndex(int src, int dst) {
        Log.i(TAG, "swapViewByIndex src:" + src + ",dst:" + dst);
        RtcVideoView srcView = mVideoViewList.get(src);
        RtcVideoView dstView = mVideoViewList.get(dst);
        mVideoViewList.set(src, dstView);
        mVideoViewList.set(dst, srcView);

        updateLayout();
    }

    public RtcVideoView getSurfaceViewByIndex(int index) {
        return mVideoViewList.get(index);
    }

    public RtcVideoView getSurfaceViewByUseId(String userId) {
        for (RtcVideoView videoView : mVideoViewList) {
            String tempUserID = videoView.getUserId();
            if (tempUserID != null && tempUserID.equals(userId)) {
                return videoView;
            }
        }
        return null;
    }

    public void setLayoutRotation(int rotation) {
        for (RtcVideoView videoView : mVideoViewList) {
            videoView.setRotation(rotation);
            videoView.requestLayout();
        }
    }

    public void setUserId(String userId) {
        mSelfUserId = userId;
    }

    /**
     * 更新进入房间人数
     */
    public RtcVideoView onMemberEnter(String userId) {
        Log.d(TAG, "onMemberEnter: userId = " + userId);

        if (TextUtils.isEmpty(userId)) return null;
        RtcVideoView videoView = null;
        int posIdx = 0;
        int posLocal = mVideoViewList.size();
        for (int i = 0; i < mVideoViewList.size(); i++) {
            RtcVideoView renderView = mVideoViewList.get(i);
            if (renderView != null) {
                String vUserId = renderView.getUserId();
                if (userId.equalsIgnoreCase(vUserId)) {
                    return renderView;
                }
                if (videoView == null && TextUtils.isEmpty(vUserId)) {
                    renderView.setUserId(userId);
                    videoView = renderView;
                    posIdx = i;
                    mCount++;
                } else if (!TextUtils.isEmpty(vUserId) && vUserId.equalsIgnoreCase(mSelfUserId)) {
                    posLocal = i;
                }
            }
        }

        /*if (0 == posLocal) {
            swapViewByIndex(posIdx, posLocal);
        }*/
        updateLayout();
        return videoView;
    }

    public void onMemberLeave(String userId) {
        Log.d(TAG, "onMemberLeave: userId = " + userId);

        int posIdx = 0, posLocal = mVideoViewList.size();
        for (int i = 0; i < mVideoViewList.size(); i++) {
            RtcVideoView renderView = mVideoViewList.get(i);
            if (renderView != null && null != renderView.getUserId()) {
                if (renderView.getUserId().equals(userId)) {
                    renderView.setUserId(null);
                    renderView.setVisibility(View.GONE);
                    posIdx = i;
                    mCount--;
                } else if (renderView.getUserId().equalsIgnoreCase(mSelfUserId)) {
                    posLocal = i;
                }
            }
        }
        if (0 == posIdx) {
            swapViewByIndex(posIdx, posLocal);
        }
        updateLayout();
    }

    public void onRoomEnter() {
        mCount++;
        updateLayout();
    }

    public void release() {
        mRootEglBase.release();
        for (RtcVideoView videoView : mVideoViewList) {
            videoView.release();
        }
        mVideoViewList.clear();
        mLayoutParamList.clear();
    }

    public int dip2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int px2dip(float pxValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

}

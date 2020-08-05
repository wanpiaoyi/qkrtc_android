package com.qukan.qkwebrtc.bean;

import com.qukan.qkwebrtcsdk.util.QkRTCDef;
import com.qukan.qkwebrtcsdk.widget.RtcVideoView;

public class PreviewBean {

    public String id;
    public String userId;
    public RtcVideoView rtcVideoView;
    public boolean isShow;
    public boolean isScreen;
    public String name = "";
    public boolean isAudioOpen;
    public boolean isVideoOpen;
    public boolean isShowLoading = true;

    public PreviewBean(String userId, RtcVideoView rtcVideoView, boolean isShow, boolean isScreen) {
        this.userId = userId;
        this.rtcVideoView = rtcVideoView;
        this.isShow = isShow;
        this.isScreen = isScreen;
        if (isScreen) {
            id = userId + QkRTCDef.TYPE_SCREEN;
        } else {
            id = userId;
        }
    }

    public PreviewBean() {
    }

}

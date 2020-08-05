package com.qukan.qkwebrtc.ui;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.qukan.qkwebrtc.R;
import com.qukan.qkwebrtc.bean.PreviewBean;

import java.util.List;

public class MeetingAdapter extends BaseQuickAdapter<PreviewBean, BaseViewHolder> {

    public MeetingAdapter(@Nullable List<PreviewBean> data) {
        super(R.layout.item_meeting, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, PreviewBean item) {
        helper.setText(R.id.tv_name, item.name);
        FrameLayout frameLayout = helper.getView(R.id.fl_view);
        if (item.isVideoOpen && item.isShow) {
            item.rtcVideoView.setZOrderMediaOverlay(true);
            ViewGroup viewGroup = (ViewGroup) item.rtcVideoView.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(item.rtcVideoView);
            }
            frameLayout.addView(item.rtcVideoView);
        } else {
            item.rtcVideoView.setZOrderMediaOverlay(false);
            item.rtcVideoView.clearImage();
            frameLayout.removeAllViews();
        }
        ImageView ivMic = helper.getView(R.id.iv_mir_state);
        if (item.isAudioOpen) {
            ivMic.setImageResource(R.drawable.main_ic_user_mir_open);
        } else {
            ivMic.setImageResource(R.drawable.main_ic_user_mir_close);
        }
    }
}

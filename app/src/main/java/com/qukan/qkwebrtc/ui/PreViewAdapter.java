package com.qukan.qkwebrtc.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.qukan.qkwebrtc.R;
import com.qukan.qkwebrtc.bean.PreviewBean;

import java.util.List;

public class PreViewAdapter extends BaseQuickAdapter<PreviewBean, BaseViewHolder> {

    public PreViewAdapter(@Nullable List<PreviewBean> data) {
        super(R.layout.item_review, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, PreviewBean item) {
        helper.setText(R.id.tv_name, item.name);
        FrameLayout frameLayout = helper.getView(R.id.fl_view);
        FrameLayout flLoading = helper.getView(R.id.fl_loading);
        if (item.isShow) {
            item.rtcVideoView.setZOrderMediaOverlay(true);
            ViewGroup viewGroup = (ViewGroup) item.rtcVideoView.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(item.rtcVideoView);
            }
            frameLayout.addView(item.rtcVideoView);
            if (item.isShowLoading) {
                flLoading.setVisibility(View.VISIBLE);
            } else {
                flLoading.setVisibility(View.GONE);
            }
        } else {
            item.rtcVideoView.setZOrderMediaOverlay(false);
            frameLayout.removeAllViews();
            flLoading.setVisibility(View.GONE);
        }

    }
}

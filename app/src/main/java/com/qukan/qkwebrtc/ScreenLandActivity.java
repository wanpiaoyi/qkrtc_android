package com.qukan.qkwebrtc;


import android.content.Context;
import android.content.Intent;

public class ScreenLandActivity extends ScreenConnectActivity {

    protected static Intent getIntent(Context context, String id) {
        Intent intent = new Intent(context, ScreenLandActivity.class);
        intent.putExtra(TAG, id);
        return intent;
    }

}

package com.tttrtc.rtmp;

import android.app.Application;

import com.tttrtc.rtmp.callback.MyTTTRtcEngineEventHandler;

import java.util.Random;

public class MainApplication extends Application {

    public MyTTTRtcEngineEventHandler mMyTTTRtcEngineEventHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Random mRandom = new Random();
        LocalConfig.mLocalUserID = mRandom.nextInt(999999);
    }
}

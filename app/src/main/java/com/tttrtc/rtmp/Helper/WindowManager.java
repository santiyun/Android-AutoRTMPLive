package com.tttrtc.rtmp.Helper;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.tttrtc.rtmp.R;
import com.tttrtc.rtmp.bean.EnterUserInfo;
import com.tttrtc.rtmp.ui.MainActivity;
import com.wushuangtech.expansion.bean.VideoCompositingLayout;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.util.ArrayList;
import java.util.List;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;
import static com.wushuangtech.library.Constants.*;

public class WindowManager {

    private final int videoProfile;
    private ArrayList<AudioRemoteWindow> mRemoteWindowList = new ArrayList();

    private int mScreenWidth;
    private int mScreenHeight;
    private int width;
    private int height;
    public WindowManager(MainActivity mainActivity, int videoProfile) {
        this.videoProfile=videoProfile;
        //获取屏幕的宽和高
        DisplayMetrics dm = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        AudioRemoteWindow mAudioRemoteWindow0 = mainActivity.findViewById(R.id.remote1);
        mAudioRemoteWindow0.mIndex = 0;
        mRemoteWindowList.add(mAudioRemoteWindow0);
        mAudioRemoteWindow0.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        mAudioRemoteWindow0.getViewTreeObserver().removeOnPreDrawListener(this);
                        width = mAudioRemoteWindow0.getWidth();// 获取宽度
                        height = mAudioRemoteWindow0.getHeight();// 获取高度
                        return true;
                    }
                });
        AudioRemoteWindow mAudioRemoteWindow1 = mainActivity.findViewById(R.id.remote2);
        mAudioRemoteWindow1.mIndex = 1;
        mRemoteWindowList.add(mAudioRemoteWindow1);
        AudioRemoteWindow mAudioRemoteWindow2 = mainActivity.findViewById(R.id.remote3);
        mAudioRemoteWindow2.mIndex = 2;
        mRemoteWindowList.add(mAudioRemoteWindow2);
        AudioRemoteWindow mAudioRemoteWindow3 = mainActivity.findViewById(R.id.remote4);
        mAudioRemoteWindow3.mIndex = 3;
        mRemoteWindowList.add(mAudioRemoteWindow3);
        AudioRemoteWindow mAudioRemoteWindow4 = mainActivity.findViewById(R.id.remote5);
        mAudioRemoteWindow4.mIndex = 4;
        mRemoteWindowList.add(mAudioRemoteWindow4);
        AudioRemoteWindow mAudioRemoteWindow5 = mainActivity.findViewById(R.id.remote6);
        mAudioRemoteWindow5.mIndex = 5;
        mRemoteWindowList.add(mAudioRemoteWindow5);
    }

    public void add(long localId, long id, int oratation, int index) {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mIndex == index && audioRemoteWindow.mId != id) {
                Log.e("###","remotemanager add id = "+id);
                audioRemoteWindow.hide();
                audioRemoteWindow.show(localId, id, oratation);
                return;
            }
        }
    }

    public void addAndSendSei(long loginId, EnterUserInfo userInfo) {
        PviewLog.d("#####windowmanager   join addAndSendSei  selfId = "+loginId+"   userInfo = "+userInfo.toString());
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            PviewLog.d("#####遍历时获取的的窗口id  mRemoteWindowList get id = "+mRemoteWindowList.get(i).getId()+"    add role = "+userInfo.getRole());
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == -1) {
                if (userInfo.getRole() == CLIENT_ROLE_BROADCASTER)
                    audioRemoteWindow.show(loginId,userInfo.getId());
                break;
            }
        }

        // 发送SEI
        VideoCompositingLayout layout = new VideoCompositingLayout();
        layout.regions = buildRemoteLayoutLocation(loginId);
        layout.mCanvasHeight = getVideoProfileHeight(videoProfile);
        layout.mCanvasWidth = getVideoProfileWidth(videoProfile);
        int i = TTTRtcEngine.getInstance().setVideoCompositingLayout(layout);
        PviewLog.d("#####windowmanager  set  addAndSendSei end selfId = "+loginId+"   userInfo = "+userInfo.toString());
    }

    /**
     * 获取分辨率的宽
     * @return
     * @param
     */
    public int getVideoProfileWidth(int videoProfile) {
        int width = 360;
        switch (videoProfile){
            case TTTRTC_VIDEOPROFILE_120P:
                width = 120;
                break;
            case TTTRTC_VIDEOPROFILE_180P:
                width = 180;
                break;
            case TTTRTC_VIDEOPROFILE_240P:
                width = 240;
                break;
            case TTTRTC_VIDEOPROFILE_360P:
                width = 360;
                break;
            case TTTRTC_VIDEOPROFILE_480P:
                width = 480;
                break;
            case TTTRTC_VIDEOPROFILE_720P:
                width = 720;
                break;
            case TTTRTC_VIDEOPROFILE_1080P:
                width = 1080;
                break;
        }
        return width;
    }

    /**
     * 获取对应分辨率的高
     * @param videoProfile
     * @return
     */
    public int getVideoProfileHeight(int videoProfile) {
        int height = 640;
        switch (videoProfile){
            case TTTRTC_VIDEOPROFILE_120P:
                height = 160;
                break;
            case TTTRTC_VIDEOPROFILE_180P:
                height = 320;
                break;
            case TTTRTC_VIDEOPROFILE_240P:
                height = 320;
                break;
            case TTTRTC_VIDEOPROFILE_360P:
                height = 640;
                break;
            case TTTRTC_VIDEOPROFILE_480P:
                height= 640;
                break;
            case TTTRTC_VIDEOPROFILE_720P:
                height = 1280;
                break;
            case TTTRTC_VIDEOPROFILE_1080P:
                height = 1920;
                break;
        }
        return height;
    }

    /**
     * 获取对应分辨率的帧率
     * @param videoProfile
     * @return
     */
    public int getVideoProfileFbs(int videoProfile) {

        return 15;
    }
    /**
     * 获取对应分辨率的码率
     * @param videoProfile
     * @return
     */
    public int getVideoProfileFbps(int videoProfile) {
        int fpbs = 400;
        switch (videoProfile){
            case TTTRTC_VIDEOPROFILE_120P:
                fpbs = 65;
                break;
            case TTTRTC_VIDEOPROFILE_180P:
                fpbs = 140;
                break;
            case TTTRTC_VIDEOPROFILE_240P:
                fpbs = 200;
                break;
            case TTTRTC_VIDEOPROFILE_360P:
                fpbs = 400;
                break;
            case TTTRTC_VIDEOPROFILE_480P:
                fpbs= 500;
                break;
            case TTTRTC_VIDEOPROFILE_720P:
                fpbs = 1130;
                break;
            case TTTRTC_VIDEOPROFILE_1080P:
                fpbs = 2080;
                break;
        }
        return fpbs;
    }
    public void removeAll() {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            audioRemoteWindow.hide();
        }
    }

    public void removeAndSendSei(long loginId, long id) {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.mute(false);
                audioRemoteWindow.hide();
                return;
            }
        }

        // 发送SEI
        VideoCompositingLayout layout = new VideoCompositingLayout();
        layout.regions = buildRemoteLayoutLocation(loginId);
        layout.mCanvasHeight = getVideoProfileHeight(videoProfile);
        layout.mCanvasWidth = getVideoProfileWidth(videoProfile);
        TTTRtcEngine.getInstance().setVideoCompositingLayout(layout);
    }

    public void muteAudio(long id, boolean mute) {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.mute(mute);
                return;
            }
        }
    }

    public void updateAudioBitrate(long id, String bitrate) {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.updateAudioBitrate(bitrate);
                return;
            }
        }
    }

    public void updateVideoBitrate(long id, String bitrate) {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.updateVideoBitrate(bitrate);
                return;
            }
        }
    }

    public void updateSpeakState(long id, int volumeLevel) {
        for (int i = 0; i < mRemoteWindowList.size(); i++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.updateSpeakState(volumeLevel);
                return;
            }
        }
    }

    public VideoCompositingLayout.Region[] buildRemoteLayoutLocation(long loginId) {
        List<VideoCompositingLayout.Region> tempList = new ArrayList<>();

        for (AudioRemoteWindow remoteWindow : mRemoteWindowList) {
            if (remoteWindow.mId != -1) {
                int[] location = new int[2];
                remoteWindow.getLocationOnScreen(location);
                VideoCompositingLayout.Region mRegion = new VideoCompositingLayout.Region();
                mRegion.mUserID = remoteWindow.mId;
                mRegion.x = (double) location[0] / mScreenWidth;
                mRegion.y = (double) location[1] / mScreenHeight;
                mRegion.width = width==0?(double) 1 / 3:(double)width/(double)mScreenWidth; // view宽度占屏幕宽度的比例
                mRegion.height = height==0?(double) mScreenWidth/3d*4/3/mScreenHeight:(double)height/(double)mScreenHeight; // view高度占屏幕的比例
                mRegion.zOrder = 1;
                tempList.add(mRegion);
                PviewLog.d("#####buildRemoteLayoutLocation  mUserID = "+mRegion.mUserID+" x = "+mRegion.x+" y = "+mRegion.y+"mRegion.height = " +mRegion.height+" mRegion.width = "+mRegion.width);

            }
        }

        VideoCompositingLayout.Region mRegion = new VideoCompositingLayout.Region();
        mRegion.mUserID = loginId;
        mRegion.x = 0;
        mRegion.y = 0;
        mRegion.width = 1;
        mRegion.height = 1;
        mRegion.zOrder = 0;
        tempList.add(mRegion);
        PviewLog.d("#####buildRemoteLayoutLocation host mUserID = "+mRegion.mUserID+" x = "+mRegion.x+" y = "+mRegion.y+"mRegion.height = " +mRegion.height+" mRegion.width = "+mRegion.width);
        VideoCompositingLayout.Region[] mRegions = new VideoCompositingLayout.Region[tempList.size()];
        for (int k = 0; k < tempList.size(); k++) {
            VideoCompositingLayout.Region region = tempList.get(k);
            mRegions[k] = region;
        }
        return mRegions;
    }

}

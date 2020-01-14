package com.tttrtc.rtmp.callback;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tttrtc.rtmp.LocalConstans;
import com.tttrtc.rtmp.bean.JniObjs;
import com.tttrtc.rtmp.ui.MainActivity;
import com.tttrtc.rtmp.utils.MyLog;
import com.wushuangtech.expansion.bean.LocalAudioStats;
import com.wushuangtech.expansion.bean.LocalVideoStats;
import com.wushuangtech.expansion.bean.RemoteAudioStats;
import com.wushuangtech.expansion.bean.RemoteVideoStats;
import com.wushuangtech.expansion.bean.RtcStats;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngineEventHandler;

import java.util.ArrayList;
import java.util.List;

import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_AUDIO_ROUTE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_CONNECTLOST;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_ENTER_ROOM;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_ERROR;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_MUTE_AUDIO;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_REMOVE_FIRST_DECODED;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_ROOM_PUSH_STATE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_ROOM_VIDEO_ENABLE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_RTMP_PUSH_STATE;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_SEI;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_SPEAK_MUTE_AUDIO;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_USER_JOIN;
import static com.tttrtc.rtmp.LocalConstans.CALL_BACK_ON_USER_OFFLINE;
import static com.wushuangtech.wstechapi.model.SwitchConfig.isFirstPull;


/**
 * Created by wangzhiguo on 17/10/24.
 */

public class MyTTTRtcEngineEventHandler extends TTTRtcEngineEventHandler {

    public static final String TAG = "MyTTTRtcEngineEventHandler_RtmpLive";
    public static final String MSG_TAG = "MyTTTRtcEngineEventHandlerMSG_RtmpLive";
    private boolean mIsSaveCallBack;
    private List<JniObjs> mSaveCallBack;
    private Context mContext;

    public MyTTTRtcEngineEventHandler(Context mContext) {
        this.mContext = mContext;
        mSaveCallBack = new ArrayList<>();
    }

    @Override
    public void onJoinChannelSuccess(String channel, long uid, int elapsed) {
        super.onJoinChannelSuccess(channel,uid,elapsed);
        MyLog.i("wzg", "RTMP_COVER onJoinChannelSuccess.... channel ： " + channel + " | uid : " + uid);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ENTER_ROOM;
        mJniObjs.mChannelName = channel;
        mJniObjs.mUid = uid;
        sendMessage(mJniObjs);
    }

    @Override
    public void onStatusOfRtmpPublish(int status) {
        super.onStatusOfRtmpPublish(status);
        Log.e("wzg", "RTMP_COVER onStatusOfRtmpPublish status : " + status);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_RTMP_PUSH_STATE;
        mJniObjs.mErrorType = status;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRtcPushStatus(String url, boolean status) {
        super.onRtcPushStatus(url,status);
        MyLog.d("wzg", "RTMP_COVER onRtcPushStatus status : " + status);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ROOM_PUSH_STATE;
        mJniObjs.mRoomPushStatus = status;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onError(final int errorType) {
        super.onError(errorType);
        MyLog.i("wzg", "RTMP_COVER onError.... errorType ： " + errorType + "mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ERROR;
        mJniObjs.mErrorType = errorType;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
        MyLog.i("wzg", "RTMP_COVER onLeaveChannel...");
    }

    @Override
    public void onUserKicked(long uid, int reason) {
        super.onUserKicked(uid,reason);
        MyLog.i("#####", "onUserKicked.... uid ： " + uid + "reason : " + reason + "mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_KICK;
        mJniObjs.mErrorType = reason;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }


    @Override
    public void onUserJoined(long nUserId, int identity, int elapsed) {
        super.onUserJoined(nUserId,identity,elapsed);
        MyLog.i("###", "onUserJoined.... nUserId ： " + nUserId + " | identity : " + identity
                + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_JOIN;
        mJniObjs.mUid = nUserId;
        mJniObjs.mIdentity = identity;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserOffline(long nUserId, int reason) {
        MyLog.i("wzg", "onUserOffline.... nUserId ： " + nUserId + " | reason : " + reason);

        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_OFFLINE;
        mJniObjs.mUid = nUserId;
        mJniObjs.mReason = reason;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onReconnectServerFailed() {
        MyLog.i("wzg", "onReconnectServerFailed.... ");
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_CONNECTLOST;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserEnableVideo(long uid, String deviceID, boolean enabled) {
        super.onUserEnableVideo(uid, deviceID,enabled);
        Log.e("###", "onUserEnableVideo.... uid : " + uid + " | deviceID : " + deviceID+"  enabled = "+enabled);
        //CALL_BACK_ON_ROOM_VIDEO_ENABLE
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ROOM_VIDEO_ENABLE;
        mJniObjs.mUid = uid;
        mJniObjs.mIsEnableVideo = enabled;
        mJniObjs.mChannelName = deviceID;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserEnableVideo(long uid, String deviceID, int videoType, boolean enabled) {
        MyLog.i("###", "onUserEnableVideo.... uid : " + uid + " | deviceID : " + deviceID+"  videoType = "+videoType+"  enabled = "+enabled);
    }

    @Override
    public void onFirstRemoteVideoDecoded(long uid, int width, int height, int elapsed) {
        Log.e("###", "onFirstRemoteVideoDecoded.... uid ： " + uid + " | width : " + width + " | height : " + height);
    }

    @Override
    public void onFirstRemoteVideoFrame(long uid, String deviceID, int width, int height, int elapsed) {
        super.onFirstRemoteVideoFrame(uid,deviceID,width,height,elapsed);
        Log.e("###","onFirstRemoteVideoFrame");
    }

    @Override
    public void onFirstRemoteVideoFrame(long uid, int width, int height, int elapsed) {
        Log.e("###","onFirstRemoteVideoFrame2");
    }

    @Override
    public void onFirstRemoteVideoDecoded(long uid, String deviceID,int width, int height, int elapsed) {
        super.onFirstRemoteVideoDecoded(uid,deviceID,width,height,elapsed);
        Log.e("###","onFirstRemoteVideoDecoded.... uid ： " + uid + "  deviceID = "+deviceID+" | width : " + width + " | height : " + height);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOVE_FIRST_DECODED;
        mJniObjs.mUid = uid;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRemoteVideoStats(RemoteVideoStats stats) {
//        MyLog.i("wzg", "onRemoteVideoStats.... uid : " + stats.getUid() + " | bitrate : " + stats.getReceivedBitrate()
//                + " | framerate : " + stats.getReceivedFrameRate());
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOTE_VIDEO_STATE;
        mJniObjs.mRemoteVideoStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRemoteAudioStats(RemoteAudioStats stats) {
//        MyLog.i("wzg", "RemoteAudioStats.... uid : " + stats.getUid() + " | bitrate : " + stats.getReceivedBitrate());
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOTE_AUDIO_STATE;
        mJniObjs.mRemoteAudioStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLocalVideoStats(LocalVideoStats stats) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_LOCAL_VIDEO_STATE;
        mJniObjs.mLocalVideoStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLocalAudioStats(LocalAudioStats stats) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_LOCAL_AUDIO_STATE;
        mJniObjs.mLocalAudioStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onSetSEI(String sei) {
        MyLog.i("wzg", "onSei.... sei : " + sei);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_SEI;
        mJniObjs.mSEI = sei;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserMuteAudio(long uid, boolean muted) {
        MyLog.i("wzg", "OnRemoteAudioMuted.... uid : " + uid + " | muted : " + muted + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_MUTE_AUDIO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsDisableAudio = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onSpeakingMuted(long uid, boolean muted) {
        MyLog.i("wzg", "onSpeakingMuted.... uid : " + uid + " | muted : " + muted + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_SPEAK_MUTE_AUDIO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsDisableAudio = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onAudioRouteChanged(int routing) {
        MyLog.i("wzg", "onAudioRouteChanged.... routing : " + routing);
        MainActivity.mCurrentAudioRoute = routing;
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_AUDIO_ROUTE;
        mJniObjs.mAudioRoute = routing;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onClientRoleChanged(long uid, int userRole) {
        super.onClientRoleChanged(uid, userRole);
        MyLog.i("wzg", "onUserRoleChanged... userID : " + uid + " userRole : " + userRole);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_ROLE_CHANGED;
        mJniObjs.mUid = uid;
        mJniObjs.mIdentity = userRole;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    private void sendMessage(JniObjs mJniObjs) {
        Intent i = new Intent();
        i.setAction(TAG);
        i.putExtra(MSG_TAG, mJniObjs);
        i.setExtrasClassLoader(JniObjs.class.getClassLoader());
        mContext.sendBroadcast(i);
    }

    public void setIsSaveCallBack(boolean mIsSaveCallBack) {
        this.mIsSaveCallBack = mIsSaveCallBack;
        if (!mIsSaveCallBack) {
            for (int i = 0; i < mSaveCallBack.size(); i++) {
                sendMessage(mSaveCallBack.get(i));
            }
            mSaveCallBack.clear();
        }
    }

    private void saveCallBack(JniObjs mJniObjs) {
        if (mIsSaveCallBack) {
            mSaveCallBack.add(mJniObjs);
        }
    }

    @Override
    public void onPlayRTMPSuccess() {
        super.onPlayRTMPSuccess();
        MyLog.i("wzg", "onPlayIJKSuccess..." );
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_ROOM_IJK_SUCCESS;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onPlayRTMPCompletion() {
        MyLog.i("wzg", "onPlayRTMPCompletion..." );
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_ROOM_IJK_COMPLETION;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onPlayStatus(int videoFps,int videoBitrate,int audioBitrate,long videoDelay,long audioDelay) {
        MyLog.i("wzg", "onPlayStatus...videoFps = "+videoFps+"  videoBitrate = "+videoBitrate+" audioBitrate = "+audioBitrate+" videoDelay = "+videoDelay+" audioDelay = "+audioDelay );
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_ROOM_IJK_STATUS;
        RemoteVideoStats remoteVideoStats = new RemoteVideoStats(0,"",videoDelay,videoBitrate,0,0,0,0f);
        mJniObjs.mRemoteVideoStats = remoteVideoStats;
        RemoteAudioStats remoteAudioStats = new RemoteAudioStats(0,audioBitrate,0,audioDelay);
        mJniObjs.mRemoteAudioStats = remoteAudioStats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onPlayRTMPError(boolean error) {
        super.onPlayRTMPError(error);
        PviewLog.d("#####ijkplayer onPlayRTMPError error = "+error);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_ROOM_IJK_ERROR;
        mJniObjs.mIsEnableVideo = error;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }
}

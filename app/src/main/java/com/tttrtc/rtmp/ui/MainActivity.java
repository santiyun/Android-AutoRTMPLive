package com.tttrtc.rtmp.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtc.rtmp.Helper.WindowManager;
import com.tttrtc.rtmp.LocalConfig;
import com.tttrtc.rtmp.LocalConstans;
import com.tttrtc.rtmp.R;
import com.tttrtc.rtmp.bean.EnterUserInfo;
import com.tttrtc.rtmp.bean.JniObjs;
import com.tttrtc.rtmp.callback.MyTTTRtcEngineEventHandler;
import com.tttrtc.rtmp.callback.PhoneListener;
import com.tttrtc.rtmp.dialog.ExitRoomDialog;
import com.tttrtc.rtmp.utils.MyLog;
import com.tttrtc.rtmp.utils.SharedPreferencesUtil;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.LocalSDKConstants;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ttt.ijk.media.exo.widget.media.IRenderView;
import ttt.ijk.media.exo.widget.media.IjkVideoView;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.TTT_AUDIO_CODEC_AAC;

public class MainActivity extends BaseActivity {

    private long mUserId;
    private long mRoomID;
    private int mRole = CLIENT_ROLE_ANCHOR;
    private long mAnchorId = -1;

    private TextView mAudioSpeedShow, mVideoSpeedShow, mFpsSpeedShow;
    private ImageView mShangMaiTV;
    private ViewGroup mLocalShowLayoutOne;
    private ViewGroup mRemoteVideoLy;
    private SurfaceView mAnchorView;

    private ExitRoomDialog mExitRoomDialog;
    private AlertDialog mErrorExitDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private ProgressDialog mDialog;
    private boolean mIsPhoneComing;
    private boolean mIsSpeaker, mIsBackCamera, mIsJoinRoom, mIsShowSecond;
    private boolean mIsFirst;
    private int videoProfile = Constants.TTTRTC_VIDEOPROFILE_360P;
    private WindowManager mWindowManager;
    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;
    private final Object obj = new Object();
    private Map<Long, Boolean> mUserMutes = new HashMap<>();

    public static int mCurrentAudioRoute;
    private int mLevCount;
    private IjkVideoView mIjkVideoView;
    private boolean mIsRtmpPushMode = true;
    private long mLastClickTime;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEngine();
        initDialog();
        initData();
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }

        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 摄像头切换接口是全局的，所以退房间需要复位
        if (mIsBackCamera) {
            mTTTEngine.switchCamera();
        }
        mTTTEngine.destroy();
        stopRtmpPublish();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mLocalShowLayoutOne.removeAllViews();
        super.onDestroy();
        MyLog.d("MainActivity onDestroy... ");
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mFpsSpeedShow = findViewById(R.id.main_btn_fpsup);
        mLocalShowLayoutOne = findViewById(R.id.local_view_layout_one);
        mRemoteVideoLy = findViewById(R.id.main_video_ly);
        mShangMaiTV = findViewById(R.id.main_btn_shangmai);

        Intent intent = getIntent();
        mRoomID = intent.getLongExtra("ROOM_ID", 0);
        mUserId = intent.getLongExtra("USER_ID", 0);
        mRole = intent.getIntExtra("ROLE", CLIENT_ROLE_ANCHOR);
        String localChannelName = getString(R.string.ttt_prefix_channel_name) + ":" + mRoomID;
        ((TextView) findViewById(R.id.main_btn_title)).setText(localChannelName);

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        if (mRole != CLIENT_ROLE_ANCHOR)
            findViewById(R.id.main_btn_switch_camera).setVisibility(View.GONE);

        // 摄像头切换控件的点击事件
        findViewById(R.id.main_btn_switch_camera).setOnClickListener(v -> {
            // 摄像头前后置切换接口
            mTTTEngine.switchCamera();
            mIsBackCamera = !mIsBackCamera;
        });

        // 上麦控件的点击事件
        mShangMaiTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //设置音频编码
                mTTTEngine.setPreferAudioCodec(TTT_AUDIO_CODEC_AAC, 48, 1);
                // 默认进房间是直推
                if (!mIsJoinRoom) {
                    // 开始上麦流程
                    // 1.设置频道模式，这里用直播模式
                    mTTTEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
                    // 2.设置角色
                    if (CLIENT_ROLE_ANCHOR == mRole) {
                        mTTTEngine.setClientRole(Constants.CLIENT_ROLE_ANCHOR);
                    } else {
                        mTTTEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
                    }
                    // 3.设置视频质量
                    mTTTEngine.setVideoProfile(360, 640, 15, 800);
                    //4.设置流媒体地址
                    String roomID = (String) SharedPreferencesUtil.getParam(MainActivity.this, "RoomID", "");
                    String port = (String) SharedPreferencesUtil.getParam(MainActivity.this, "port", "");
                    int ip = (Integer) SharedPreferencesUtil.getParam(MainActivity.this, "ip", 0);
                    if (!TextUtils.isEmpty(port)&&ip>0){
                        mTTTEngine.setServerIp(port,ip);
                    }

                    // 5.调用进房间接口
                    int i = -1;
                    if (Constants.CLIENT_ROLE_ANCHOR == mRole) {
                        i = mTTTEngine.anchorJoinChannel("", String.valueOf(mRoomID), mUserId, LocalConfig.mPushPreifx + mRoomID);
                    } else {
                        i = mTTTEngine.audienceJoinChannel("", String.valueOf(mRoomID), mUserId, MainActivity.this, mLocalShowLayoutOne);
                    }
                    if (LocalSDKConstants.FUNCTION_SUCCESS != i) {
                        if (i == 101) {
                            Toast.makeText(MainActivity.this, "正在推RTMP流，请勿频繁操作", Toast.LENGTH_SHORT).show();
                        } else if (i == 108) {
                            Toast.makeText(MainActivity.this, "已经是webrtc流了，请勿重复操作", Toast.LENGTH_SHORT).show();
                        } else if (i == 201) {
                            Toast.makeText(MainActivity.this, "正在拉RTMP流，请勿频繁操作", Toast.LENGTH_SHORT).show();
                        } else if (i == 204) {
                            Toast.makeText(MainActivity.this, "正在拉webrtc流，请勿重复操作", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        mIsRtmpPushMode = false;
                        showProgressDialog(getString(R.string.ttt_enter_channel));
                    }

                } else {
                    // 开始下麦流程
                    // 如果是主播下麦
                    if (Constants.CLIENT_ROLE_ANCHOR == mRole) {
                        // 先开启RTMP直推
                        int i = mTTTEngine.anchorStartRtmpPublish(LocalConfig.mPushPreifx + mRoomID);
                        if (LocalSDKConstants.FUNCTION_SUCCESS != i) {
                            if (i == 101) {
                                Toast.makeText(MainActivity.this, "正在进行rtmp推流,请勿重复操作", Toast.LENGTH_SHORT).show();
                            } else if (i == 104) {
                                Toast.makeText(MainActivity.this, "正在rtc推流,请勿频繁操作", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            showProgressDialog("正在推流中...");
                        }

                    } else { // 如果是副播下麦变观众
                        int i = mTTTEngine.pullRtmp(mLocalShowLayoutOne, LocalConfig.mPullPreifx + mRoomID, true, MainActivity.this, IRenderView.AR_MATCH_PARENT);
                        if (LocalSDKConstants.FUNCTION_SUCCESS != i) {
                            if (i == 202) {
                                Toast.makeText(MainActivity.this, "正在拉rtmo流,请勿重复操作", Toast.LENGTH_SHORT).show();
                            } else if (i == 204) {
                                Toast.makeText(MainActivity.this, "正在拉webrtc流,请勿频繁操作", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });
    }

    // 结束RTMP直推
    private void stopRtmpPublish() {
        mTTTEngine.stopRtmpPublish();
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
    }

    private void initDialog() {
        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            exitRoom();
            mExitRoomDialog.dismiss();
        });
        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());


        //添加确定按钮
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ttt_error_exit_dialog_title))//设置对话框标题
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ttt_confirm), (dialog, which) -> {//确定按钮的响应事件
                    exitRoom();
                });
        mErrorExitDialog = builder.create();

        // 创建dialog
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setMessage(getString(R.string.ttt_hint_loading_channel));
    }

    private void initData() {
        mWindowManager = new WindowManager(this,videoProfile);
        String localUserName = getString(R.string.ttt_prefix_user_name) + ":" + mUserId;
        ((TextView) findViewById(R.id.main_btn_host)).setText(localUserName);

        mRemoteVideoLy.setVisibility(View.INVISIBLE);
        mShangMaiTV.setImageResource(R.drawable.shangmai);
        mTTTEngine.setVideoMixerParams(mWindowManager.getVideoProfileFbps(videoProfile),mWindowManager.getVideoProfileFbs(videoProfile) ,
                mWindowManager.getVideoProfileWidth(videoProfile),
                mWindowManager.getVideoProfileHeight(videoProfile) );
        if (mRole == CLIENT_ROLE_ANCHOR) { // 如果角色是主播，进行RTMP推流
            SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(this);
            mTTTEngine.setVideoProfile(videoProfile, false);
            mTTTEngine.setPreferAudioCodec(TTT_AUDIO_CODEC_AAC, 48, 1);
            mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, mSurfaceView), getRequestedOrientation());
            mLocalShowLayoutOne.addView(mSurfaceView, 0);
            mTTTEngine.startPreview();
            int i = mTTTEngine.anchorStartRtmpPublish(LocalConfig.mPushPreifx + mRoomID);
            if (LocalSDKConstants.FUNCTION_SUCCESS != i) {
                showErrorExitDialog("推流失败");
            } else {
                showProgressDialog("正在推流中...");
            }
        } else { // 如果角色是观众，进行RTMP拉流
            int i = mTTTEngine.pullRtmp(mLocalShowLayoutOne, LocalConfig.mPullPreifx + mRoomID, true, this, IRenderView.AR_MATCH_PARENT);
            if (LocalSDKConstants.FUNCTION_SUCCESS != i) {
                showErrorExitDialog("拉流失败");
            } else {
                showProgressDialog("正在拉流中...");
            }
            mIsFirst = true;
        }
    }

    private void changeAnchorLayout(boolean isInRoom) {
        if (isInRoom) {
            mRemoteVideoLy.setVisibility(View.VISIBLE);
            mShangMaiTV.setImageResource(R.drawable.shangmaizhong);
            mIsJoinRoom = true;
        } else {
            mRemoteVideoLy.setVisibility(View.INVISIBLE);
            mShangMaiTV.setImageResource(R.drawable.shangmai);
            mIsJoinRoom = false;
            mWindowManager.removeAll();
        }
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... mIsJoinRoom : " + mIsJoinRoom);
        if (mRole == CLIENT_ROLE_ANCHOR) {
            if (mIsJoinRoom) {
                mTTTEngine.leaveChannel();
            } else {
                stopRtmpPublish();
            }
        } else {
            mTTTEngine.leaveChannel();
            mTTTEngine.stopIjkPlayer();
            if (mIjkVideoView != null) {
                mIjkVideoView.release(true);
                mIjkVideoView = null;
            }
        }
        setResult(SplashActivity.ACTIVITY_MAIN);
        finish();
    }

    public void showErrorExitDialog(String message) {
        if (mErrorExitDialog != null && mErrorExitDialog.isShowing()) {
            return;
        }

        if (!TextUtils.isEmpty(message)) {
            String msg = getString(R.string.ttt_error_exit_dialog_prefix_msg) + ": " + message;
            mErrorExitDialog.setMessage(msg);//设置显示的内容
            mErrorExitDialog.show();
        }
    }

    /**
     * 显示进度对话框
     *
     * @param hintText 提示文字
     */
    private void showProgressDialog(String hintText) {
        mDialog.setMessage(hintText);
        mDialog.show();
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                MyLog.d("UI onReceive callBack... mJniType : " + mJniObjs.mJniType);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_RTMP_PUSH_STATE:
                        mDialog.dismiss();
                        String toastmsg = "";
                        if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_LINKSUCCESSED) {
                            Toast.makeText(mContext, "推流成功", Toast.LENGTH_SHORT).show();
                            changeAnchorLayout(false);
                            break;
                        }
                        if (!mIsJoinRoom) {
                            if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_INITERROR) {
                                toastmsg = "推流初始化失败";
                                showErrorExitDialog(toastmsg);
                                stopRtmpPublish();
                            } else if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_OPENERROR) {
                                toastmsg = "连接已断开";
                                showErrorExitDialog(toastmsg);
                                stopRtmpPublish();
                            } else if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_LINKFAILED) {
                                toastmsg = "推流发送失败";
                                showErrorExitDialog(toastmsg);
                                stopRtmpPublish();
                            }
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        mDialog.dismiss();
                        int mJoinRoomResult = mJniObjs.mErrorType;
                        if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_format), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_TIMEOUT) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_timeout), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_token_invaild), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_BAD_VERSION) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_version), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_CONNECT_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unconnect), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_NOEXIST) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_room_no_exist), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_SERVER_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_verification_failed), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_UNKNOW) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unknow), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        //来电
                        mIsPhoneComing = true;
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        //电话挂断
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        //远端音频下行状态
                        if (mJniObjs.mRemoteAudioStats.getUid() != mAnchorId) {
                            String audioString = getResources().getString(R.string.ttt_audio_downspeed);
                            String audioResult = String.format(audioString, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                            mWindowManager.updateAudioBitrate(mJniObjs.mRemoteAudioStats.getUid(), audioResult);
                        } else
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_downspeed, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        //远端视频下行状态
                        if (mJniObjs.mRemoteVideoStats.getUid() != mAnchorId) {
                            String videoString = getResources().getString(R.string.ttt_video_downspeed);
                            String videoResult = String.format(videoString, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                            mWindowManager.updateVideoBitrate(mJniObjs.mRemoteVideoStats.getUid(), videoResult);
                        } else
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_downspeed, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        //本地音频上行状态
                        if (mRole == CLIENT_ROLE_ANCHOR)
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_upspeed, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                        else {
                            String localAudioString = getResources().getString(R.string.ttt_audio_upspeed);
                            String localAudioResult = String.format(localAudioString, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                            mWindowManager.updateAudioBitrate(mUserId, localAudioResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        //本地视频上行状态
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            mFpsSpeedShow.setText("FPS-" + mJniObjs.mLocalVideoStats.getSentFrameRate());
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_upspeed, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                        } else {
                            String localVideoString = getResources().getString(R.string.ttt_video_upspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                            mWindowManager.updateVideoBitrate(mUserId, localVideoResult);
                        }
                        break;

                    case LocalConstans.CALL_BACK_ON_ENTER_ROOM:
                        //加入房间
                        changeAnchorLayout(true);
                        Toast.makeText(mContext, "进入房间成功", Toast.LENGTH_SHORT).show();
                        mDialog.dismiss();
                        break;
                    case LocalConstans.CALL_BACK_ON_ROOM_PUSH_STATE:
                        //rtc推流状态
                        mDialog.dismiss();
                        if (!mJniObjs.mRoomPushStatus) {
                            mTTTEngine.leaveChannel();
                            Toast.makeText(mContext, "进入房间推流失败", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "进入房间推流成功", Toast.LENGTH_SHORT).show();
                            changeAnchorLayout(true);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_KICK:
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            //监听到主播下麦 需要去打开ijkplayer
                            PviewLog.d("#####activity onerror " );
                            int i = mTTTEngine.pullRtmp(mLocalShowLayoutOne, LocalConfig.mPullPreifx + mRoomID, true, MainActivity.this, IRenderView.AR_MATCH_PARENT);
                            PviewLog.d("#####activity pullRtmp result =  " +i);
                            if (LocalSDKConstants.FUNCTION_SUCCESS != i) {
                                if (i == 202) {
                                    Toast.makeText(MainActivity.this, "正在拉rtmp流,请勿重复操作", Toast.LENGTH_SHORT).show();
                                } else if (i == 204) {
                                    Toast.makeText(MainActivity.this, "正在拉webrtc流,请勿频繁操作", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                changeAnchorLayout(false);
                            }
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (errorType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }
                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST:
                        showErrorExitDialog(getString(R.string.ttt_error_network_disconnected));
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        //用户加入房间
                        long uid = mJniObjs.mUid;
                        int identity = mJniObjs.mIdentity;
                        if (identity == CLIENT_ROLE_ANCHOR) {
                            mAnchorId = uid;
                            String localAnchorName = getString(R.string.ttt_role_anchor) + "ID: " + mAnchorId;
                            ((TextView) findViewById(R.id.main_btn_host)).setText(localAnchorName);
                        }
                        PviewLog.d("#####mainactivity onUserJoin mRole = "+mRole);
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            EnterUserInfo userInfo = new EnterUserInfo(uid, identity);
                            PviewLog.d("#####mainactivity setSei userInfo = "+userInfo.toString());
                            mWindowManager.addAndSendSei(mUserId, userInfo);
                        }

                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        //直播用户退出
                        long offLineUserID = mJniObjs.mUid;
                        mWindowManager.removeAndSendSei(mUserId, offLineUserID);
                        break;
                    case LocalConstans.CALL_BACK_ON_SEI:
                        List<EnterUserInfo> mInfos = new LinkedList<>();
                        try {
                            JSONObject jsonObject = new JSONObject(mJniObjs.mSEI);
                            JSONArray jsonArray = jsonObject.getJSONArray("pos");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float x = Float.valueOf(jsonobject2.getString("x"));
                                float y = Float.valueOf(jsonobject2.getString("y"));
                                float w = Float.valueOf(jsonobject2.getString("w"));
                                float h = Float.valueOf(jsonobject2.getString("h"));

                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }
                                MyLog.d("CALL_BACK_ON_SEI", "parse user id : " + userId);
                                if (userId != mAnchorId) {
                                    EnterUserInfo temp = new EnterUserInfo(userId, Constants.CLIENT_ROLE_BROADCASTER);
                                    temp.setXYLocation(x, y);
                                    mInfos.add(temp);
                                }

                            }
                        } catch (JSONException e) {
                            MyLog.d("CALL_BACK_ON_SEI", "parse xml error : " + e.getLocalizedMessage());
                        }

                        int count = 0;
                        for (EnterUserInfo temp : mInfos) {
                            temp.mShowIndex = count;
                            count++;
                        }

                        for (EnterUserInfo next : mInfos) {
                            MyLog.d("CALL_BACK_ON_SEI", "user list : " + next.getId() + " | index : " + next.mShowIndex);
                            mWindowManager.add(mUserId, next.getId(), getRequestedOrientation(), next.mShowIndex);
                        }

                        synchronized (obj) {
                            if (mUserMutes.size() > 0) {
                                Set<Map.Entry<Long, Boolean>> entries = mUserMutes.entrySet();
                                for (Map.Entry<Long, Boolean> next : entries) {
                                    mWindowManager.muteAudio(next.getKey(), next.getValue());
                                }
                            }
                            mUserMutes.clear();
                        }
                        break;

                    case LocalConstans.CALL_BACK_ON_ROOM_IJK_ERROR:
                        PviewLog.d("#####mainactivity onPlayRTMPError error = "+mJniObjs.mIsEnableVideo);
                        PviewLog.d("#####mainactivity dialog = "+mDialog==null?"":mDialog.isShowing()+"");
                        //纯拉流播放错误回调
                        if (mDialog != null && mDialog.isShowing())
                            mDialog.dismiss();
                        if (mJniObjs.mIsEnableVideo) {
                            showErrorExitDialog("拉流失败");
                        } else {
                            showErrorExitDialog("拉流失败");
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_ROOM_IJK_SUCCESS:
                        //拉流成功
                        if (mDialog != null && mDialog.isShowing())
                            mDialog.dismiss();
                        changeAnchorLayout(false);
                        Toast.makeText(mContext, "拉流成功", Toast.LENGTH_SHORT).show();
                        break;
                    case LocalConstans.CALL_BACK_ON_ROOM_IJK_STATUS:
                        //设置纯拉流主播的下行音视频码率
                        long audioDelay = mJniObjs.mRemoteAudioStats.getmAudioDelay();
                        int audioBitrate = mJniObjs.mRemoteAudioStats.getReceivedBitrate();
                        long videoDelay = mJniObjs.mRemoteVideoStats.getDelay();
                        int videoBitrate1 = mJniObjs.mRemoteVideoStats.getReceivedBitrate();
                        setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_downspeed, String.valueOf(audioBitrate));
                        setTextViewContent(mVideoSpeedShow, R.string.ttt_video_downspeed, String.valueOf(videoBitrate1));
                        break;

                }
            }
        }
    }
}

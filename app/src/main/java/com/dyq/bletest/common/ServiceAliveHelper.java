package com.dyq.bletest.common;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.dyq.bletest.R;

/**
 * 提高Service存活率的帮助类
 */
public class ServiceAliveHelper implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{

    private static ServiceAliveHelper mInstance;

    private Context mContext;
    private MediaPlayer mMediaPlayer;//音乐播放器
    private boolean isPlaying = true;//是否正在播放


    /**
     * 获取Service保活助手实例
     *
     * @param context 上下文
     * */
    public static ServiceAliveHelper getInstance(Context context){
        if(mInstance == null){
            mInstance = new ServiceAliveHelper(context);
            mInstance.mContext = context;
        }
        return mInstance;
    }

    private ServiceAliveHelper(Context context){
        mContext = context;
        initMediaPlayer(context);
    }

    /**初始化音乐播放器*/
    private void initMediaPlayer(Context context){
        //利用Android系统支持音乐在后台长期播放来保活Service
        mMediaPlayer = MediaPlayer.create(context, R.raw.everlast);//一个空声音文件
        if(mMediaPlayer == null){
            mMediaPlayer = MediaPlayer.create(context, R.raw.everlast);//一个空声音文件
        }
        /** 解决bug: java.lang.NullPointerException: Attempt to invoke virtual method
         * 'void android.media.MediaPlayer.setLooping(boolean)' on a null object reference*/
        if(mMediaPlayer != null) {
            mMediaPlayer.setLooping(true);//循环播放
//        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
//        Log.i("TT", "ServiceAliveHelper-->initMediaPlayer mMediaPlayer is null:" + (mMediaPlayer == null));
        }
    }

    /**
     * 开始保持Service存活
     * */
    public void startKeep(){
        if(mMediaPlayer != null){
            mMediaPlayer.start();
            isPlaying = true;
//            Log.i("TT", "ServiceAliveHelper-->startKeep isPlaying:" + isPlaying + " mInstance is null:" + (mInstance == null));
        }
    }

    /**结束保存Service存活*/
    public void stopKeep(){
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            isPlaying = false;
//            Log.i("TT","ServiceAliveHelper-->stopKeep isPlaying:"+isPlaying +" mInstance is null:"+(mInstance == null));
        }
        mInstance = null;//清除实例
    }

    @Override
    public void onCompletion(MediaPlayer mp) {//播放完成后,继续播放
//        Log.i("TT","ServiceAliveHelper-->onCompletion mp is null:"+(mp == null));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {//尽量处理错误
//        Log.e("TT", "ServiceAliveHelper-->onError mp is null:" + (mp == null) + " what:" + what + " extra:" + extra);
        //1.重置播放器状态
        if(mMediaPlayer != null){
            mMediaPlayer.reset();
            mMediaPlayer.start();
        }else {
            if(mContext != null) {
                initMediaPlayer(mContext);
            }
        }
        //2.根据当前状态,是否开始播放
        if (isPlaying) {
            try {
                mMediaPlayer.start();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        return false;
    }


}

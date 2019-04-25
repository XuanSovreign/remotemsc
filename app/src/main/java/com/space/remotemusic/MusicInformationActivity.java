package com.space.remotemusic;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.RemoteController;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by licht on 2019/2/26.
 */

public class MusicInformationActivity extends Activity implements View.OnClickListener {
    private static final String TAG = MusicInformationActivity.class.getName();
    private Button mBtnPre;
    private Button mBtnStop;
    private Button mBtnNext;
    private TextView mTvMusicTitle;
    private TextView mTvMusicName;
    private RemoteController remoteController;
    private RemoteControlService mRemoteControlService;
    private Handler mHandler = new Handler();
    private String mTitle = "";
    private long mDuration;
    private long lastTime = 0;
    private ExecutorService singleThreadExecutor;
    private MyRunable mMyRunable;
    private String jsonStr = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_info);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        mBtnPre = findViewById(R.id.btn_music_previous);
        mBtnStop = findViewById(R.id.btn_music_stop);
        mBtnNext = findViewById(R.id.btn_music_next);
        mTvMusicTitle = findViewById(R.id.tv_music_name);
        mTvMusicName = findViewById(R.id.tv_music_personal);
    }

    private void initData() {
        singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                TcpNetUtil.connectTcp("192.168.30.180", 10086);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Instrumentation inst = new Instrumentation();
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        }, 500);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteControlService.RCBinder binder = (RemoteControlService.RCBinder) service;
            mRemoteControlService = binder.getService();
            mRemoteControlService.setClientUpdateListener(mExternalClientUpdateListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void initListener() {
        mBtnPre.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
    }

    public static void sendKeyEvent(final int KeyCode) {
        new Thread() {     //不可在主线程中调用
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }.start();
    }


    RemoteController.OnClientUpdateListener mExternalClientUpdateListener = new RemoteController.OnClientUpdateListener() {

        @Override
        public void onClientChange(boolean clearing) {

            Log.e(TAG, "onClientChange()...");

        }

        @Override
        public void onClientPlaybackStateUpdate(int state) {

            Log.e(TAG, "onClientPlaybackStateUpdate()..." + state);

        }

        @Override

        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {

            Log.e(TAG, "onClientPlaybackStateUpdate()..." + state + "======" + stateChangeTimeMs + "==========" + currentPosMs + "============" + speed);

            Log.e(TAG, "onClientPlaybackStateUpdate: state=" + state + "  stateChangeTimeMs=" + stateChangeTimeMs + "  currentPosMs=" + currentPosMs + " speed=" + speed);
            MessageBean messageBean = new MessageBean();
            messageBean.setTitle(mTitle);
            messageBean.setDuration(mDuration);
            messageBean.setState(state);
            messageBean.setCurrentPosition(currentPosMs);
            Gson gson = new Gson();
            String json = gson.toJson(messageBean);
            json += "\n";
            if (jsonStr.equals(json)) {
                return;
            }
            Log.e(TAG, "onClientPlaybackStateUpdate: json2=" + json);

//        final String finalJson = json;
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendJson(finalJson);
//                mHandler.postDelayed(this,100);
//            }
//        },100);
            sendJson(json);
            jsonStr = json;
        }

        @Override

        public void onClientTransportControlUpdate(int transportControlFlags) {

            // Log.e(TAG, "onClientTransportControlUpdate()...");

        }

        @Override

        public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {

            String artist = metadataEditor.

                    getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, "null");

            String album = metadataEditor.

                    getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "null");

            String title = metadataEditor.

                    getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "null");

            Long duration = metadataEditor.

                    getLong(MediaMetadataRetriever.METADATA_KEY_DURATION, -1);

            Bitmap defaultCover = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_compass);

            Bitmap bitmap = metadataEditor.

                    getBitmap(RemoteController.MetadataEditor.BITMAP_KEY_ARTWORK, defaultCover);
            //这里便获取到信息了，下面只是我设定给组件的方法，可以自己调用自己的设定方法，这里只要获取到结果就可以了

//            setCoverImage(bitmap);
//
//            setContentString(artist);
//
//            setTitleString(title);
//            malbum.setText(album);

            Log.e("结果为:", "artist:" + artist

                    + "album:" + album

                    + "title:" + title

                    + "duration:" + duration);

            mTitle = title;
            mDuration = duration;
        }

    };


    @Override
    protected void onResume() {
        super.onResume();
        //获取通知相关权限
        if (!isNotificationListenerServiceEnabled(this)) {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            Toast.makeText(this, "请授予通知使用权限", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent("com.space.remotemusic.BIND_RC_CONTROL_SERVICE");

            intent.setPackage(getPackageName());

            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_music_previous:
//                sendKeyEvent(88);
                mRemoteControlService.sendMusicKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case R.id.btn_music_stop:
//                sendKeyEvent(85);
                mRemoteControlService.sendMusicKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case R.id.btn_music_next:
//                sendKeyEvent(87);
                mRemoteControlService.sendMusicKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;

        }
    }


    /**
     * 是否已经授予通知相关权限
     *
     * @param context，上下文对象
     * @return
     */
    private boolean isNotificationListenerServiceEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }

    TcpNetUtil.SendDataListener mListener = new TcpNetUtil.SendDataListener() {
        @Override
        public void sendSuccess(byte[] bytes) {

        }

        @Override
        public void sendFail(byte[] bytes) {
            TcpNetUtil.sendData(bytes, mListener);
        }
    };

    private void sendJson(final String json) {
        if (mMyRunable == null) mMyRunable = new MyRunable();
        mMyRunable.SetData(json);
        singleThreadExecutor.execute(mMyRunable);
        Log.e(TAG, "sendJson: " + json);
    }

    private class MyRunable implements Runnable {
        public String data;

        public void SetData(String data) {
            this.data = data;
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(data)) return;
            TcpNetUtil.sendData(data.getBytes(), mListener);
        }
    }
}

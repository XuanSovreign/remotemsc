package com.space.remotemusic;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteController;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by licht on 2019/3/13.
 */

public class NewRemoteControlService extends Service implements RemoteController.OnClientUpdateListener {

    private static final String TAG = NewRemoteControlService.class.getName();
    private RemoteController mController;
    private Handler mHandler = new Handler();
    private String mAddress;
    private String mTitle = "";
    private long mDuration;
    private long lastTime = 0;
    private ExecutorService singleThreadExecutor;
    private  MyRunable mMyRunable;

    @Override
    public void onCreate() {
        registerRemoteController();
        SharedPreferences sp = getSharedPreferences("remote_data", MODE_PRIVATE);
        mAddress = sp.getString("ip_address", "");
        Log.e(TAG, "onCreate: " + mAddress);
        singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                TcpNetUtil.connectTcp(mAddress, 10086);
            }
        });

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMusicKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                sendMusicKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        }, 500);
        return START_STICKY;
    }


    private void registerRemoteController() {
        mController = new RemoteController(this, this);
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean register = audioService.registerRemoteController(mController);

        if (register) {
            mController.setArtworkConfiguration(100, 100);
            mController.setSynchronizationMode(RemoteController.POSITION_SYNCHRONIZATION_CHECK);
        }
    }


    public boolean sendMusicKeyEvent(int keyCode) {

        if (mController != null) {

            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);

            boolean down = mController.sendMediaKeyEvent(keyEvent);

            keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);

            boolean up = mController.sendMediaKeyEvent(keyEvent);

            return down && up;

        } else {

            long eventTime = SystemClock.uptimeMillis();

            KeyEvent key = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);

            dispatchMediaKeyToAudioService(key);

            dispatchMediaKeyToAudioService(KeyEvent.changeAction(key, KeyEvent.ACTION_UP));

        }

        return false;

    }

    private void dispatchMediaKeyToAudioService(KeyEvent event) {

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if (audioManager != null) {

            try {

                audioManager.dispatchMediaKeyEvent(event);

            } catch (Exception e) {

                e.printStackTrace();

            }

        }

    }

    @Override
    public void onClientChange(boolean clearing) {
        Log.e(TAG, "onClientChange: ");
    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {
        Log.e(TAG, "onClientPlaybackStateUpdate: ");
    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
        Log.e(TAG, "onClientPlaybackStateUpdate: state=" + state + "  stateChangeTimeMs=" + stateChangeTimeMs + "  currentPosMs=" + currentPosMs + " speed=" + speed);
        MessageBean messageBean = new MessageBean();
        messageBean.setTitle(mTitle);
        messageBean.setDuration(mDuration);
        messageBean.setState(state);
        messageBean.setCurrentPosition(currentPosMs);
        Gson gson = new Gson();
        String json = gson.toJson(messageBean);
        Log.e(TAG, "onClientPlaybackStateUpdate: json2=" + json);
        sendJson(json);
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {
        Log.e(TAG, "onClientTransportControlUpdate: transportControlFlags=" + transportControlFlags);
    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {

        String artist = metadataEditor.

                getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, "null");

        String album = metadataEditor.

                getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "null");

        String title = metadataEditor.

                getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "null");

        long duration = metadataEditor.

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
        if(mMyRunable == null) mMyRunable = new MyRunable();
        mMyRunable.SetData(json);
        singleThreadExecutor.execute(mMyRunable);
        Log.e(TAG, "sendJson: "+json );
    }

    private class MyRunable implements Runnable{
        public String data;

        public void SetData(String data){
            this.data = data;
        }

        @Override
        public void run() {
            if(TextUtils.isEmpty(data)) return;
            TcpNetUtil.sendData(data.getBytes(), mListener);
        }
    }
}

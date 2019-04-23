package com.space.remotemusic;

import android.content.Intent;
import android.media.AudioManager;
import android.media.RemoteController;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by licht on 2019/2/26.
 */

public class RemoteControlService extends NotificationListenerService implements RemoteController.OnClientUpdateListener{
    private static final String TAG = RemoteControlService.class.getName();
    public RemoteController remoteController;

    private RemoteController.OnClientUpdateListener mExternalClientUpdateListener;

    private IBinder mBinder = new RCBinder();

    @Override

    public void onCreate() {

        registerRemoteController();

    }

    @Override

    public IBinder onBind(Intent intent) {

        if (intent.getAction().equals("com.space.remotemusic.BIND_RC_CONTROL_SERVICE")) {//这里要根据实际进行替换

            return mBinder;

        } else {

            return super.onBind(intent);

        }

    }

    @Override

    public void onNotificationPosted(StatusBarNotification sbn) {

        // Log.e(TAG, "onNotificationPosted...");

        if (sbn.getPackageName().contains("music"))
        {
            Log.e(TAG, "音乐软件正在播放...");
            Log.e(TAG, sbn.getPackageName());
            Log.e(TAG, sbn.getNotification().toString());
        }
        Log.e("正在播放", sbn.getPackageName());
    }



    @Override

    public void onNotificationRemoved(StatusBarNotification sbn) {

        Log.e(TAG, "onNotificationRemoved...");

    }

    public void registerRemoteController() {

        remoteController = new RemoteController(this, this);

        boolean registered;

        try {

            registered = ((AudioManager) getSystemService(AUDIO_SERVICE))

                    .registerRemoteController(remoteController);

        } catch (NullPointerException e) {

            registered = false;

        }

        if (registered) {

            try {

                remoteController.setArtworkConfiguration(

                        100,

                        100);

                remoteController.setSynchronizationMode(RemoteController.POSITION_SYNCHRONIZATION_CHECK);

            } catch (IllegalArgumentException e) {

                e.printStackTrace();

            }

        }

    }

    public void setClientUpdateListener(RemoteController.OnClientUpdateListener listener) {

        mExternalClientUpdateListener = listener;

    }

    @Override

    public void onClientChange(boolean clearing) {

        if (mExternalClientUpdateListener != null) {

            mExternalClientUpdateListener.onClientChange(clearing);

        }

    }

    @Override

    public void onClientPlaybackStateUpdate(int state) {

        if (mExternalClientUpdateListener != null) {

            mExternalClientUpdateListener.onClientPlaybackStateUpdate(state);

        }

    }

    @Override

    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {

        if (mExternalClientUpdateListener != null) {

            mExternalClientUpdateListener.onClientPlaybackStateUpdate(state, stateChangeTimeMs, currentPosMs, speed);

        }

    }

    @Override

    public void onClientTransportControlUpdate(int transportControlFlags) {

        if (mExternalClientUpdateListener != null) {

            mExternalClientUpdateListener.onClientTransportControlUpdate(transportControlFlags);

        }

    }

    @Override

    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {

        if (mExternalClientUpdateListener != null) {

            mExternalClientUpdateListener.onClientMetadataUpdate(metadataEditor);

        }

    }

    public boolean sendMusicKeyEvent(int keyCode) {

        if (remoteController != null) {

            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);

            boolean down = remoteController.sendMediaKeyEvent(keyEvent);

            keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);

            boolean up = remoteController.sendMediaKeyEvent(keyEvent);

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
    public class RCBinder extends Binder {

        public RemoteControlService getService() {

            return RemoteControlService.this;

        }

    }

}

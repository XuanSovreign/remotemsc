package com.space.remotemusic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private EditText mEtMain;
    private SharedPreferences mSp;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private String[] musicFiles = {"AudioJungle - Way Above The Skyline [mqms2].mp3", "林俊杰-生生.mp3"
                                    ,"Kelly Clarkson - Catch My Breath.mp3","Maroon5-Maps.mp3","Maroon5-Sugar.mp3","Purity Ring - Flood On the Floor [mqms2].mp3"
                                    ,"班德瑞 - 伤感钢琴曲.mp3","被遗忘的天使.mp3","郭静_心墙.mp3","卡农+-+欢快版.mp3","林俊杰-那些很冒险的梦.mp3","周杰伦彩虹.mp3"
                                    ,"周杰伦-晴天.mp3","周杰伦一路向北.mp3"};
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    showDialog();
                    break;
                case 1:
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    break;
            }
        }
    };
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEtMain = findViewById(R.id.et_main);
        Button btnSure = findViewById(R.id.btn_sure);
        mSp = getSharedPreferences("remote_data", MODE_PRIVATE);
        String ipAddress = mSp.getString("ip_address", "");
        mEtMain.setText(ipAddress);
        requestPermissions();
        btnSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectEvent();
            }
        });
    }

    private void connectEvent() {
        String ip = mEtMain.getText().toString().trim();
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, "请输入ip", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences.Editor edit = mSp.edit();
        edit.putString("ip_address", ip);
        edit.apply();
        goNext();
    }

    private void requestPermissions() {

        if (Build.VERSION.SDK_INT >= 23) {
            int permission = -1;
            for (int i = 0; i < permissions.length; i++) {
                permission = ActivityCompat.checkSelfPermission(this, permissions[i]);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }
            if (permission != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, permissions, 0x0010);
            } else {
                //获取通知相关权限
                if (!isNotificationListenerServiceEnabled(this)) {
                    startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 100);
                    Toast.makeText(this, "请授予通知使用权限", Toast.LENGTH_SHORT).show();
                } else {
                    copyMusic();
                }
            }
        } else {
            //获取通知相关权限
            if (!isNotificationListenerServiceEnabled(this)) {
                startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 100);
                Toast.makeText(this, "请授予通知使用权限", Toast.LENGTH_SHORT).show();
            } else {
                copyMusic();
            }
        }

    }

    private void copyMusic() {
        Log.e(TAG, "copyMusic: ");
        new Thread() {
            @Override
            public void run() {
                File directory = new File(Environment.getExternalStorageDirectory(),"mymusic");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                if (directory.list().length > 0) {
                    Log.e(TAG, "copyMusic: mymusic");
                    return;
                }
                mHandler.sendEmptyMessage(0);
                for (int i = 0; i < musicFiles.length; i++) {
                    try {
                        InputStream is = getResources().getAssets().open(musicFiles[i]);
                        byte[] bytes = new byte[1024];
                        int length = 0;
                        File file = new File(directory, musicFiles[i]);
                        FileOutputStream os = new FileOutputStream(file);
                        while ((length = is.read(bytes)) > 0) {
                            os.write(bytes, 0, length);
                            os.flush();
                        }
                        Log.e(TAG, "copy: "+file.getAbsolutePath());
                        is.close();
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mHandler.sendEmptyMessage(1);
            }
        }.start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGrant = true;
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                isGrant = false;
                break;
            }
        }
        if (isGrant) {
            //获取通知相关权限
            if (!isNotificationListenerServiceEnabled(this)) {
                startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 100);
                Toast.makeText(this, "请授予通知使用权限", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "请申请读写权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.dialog_waite, null);
        builder.setView(view);
        builder.setCancelable(false);
        mDialog = builder.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isNotificationListenerServiceEnabled(this)) {
            Toast.makeText(this, "请授予通知使用权限", Toast.LENGTH_SHORT).show();
        } else {
            copyMusic();
        }
    }

    private void goNext() {
        Intent intent = new Intent("com.space.remotemusic.START_RC_CONTROL_SERVICE");
        intent.setPackage(getPackageName());
        startService(intent);
        finish();
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

}

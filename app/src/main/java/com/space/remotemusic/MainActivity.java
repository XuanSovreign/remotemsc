package com.space.remotemusic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText mEtMain;
    private SharedPreferences mSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEtMain = findViewById(R.id.et_main);
        Button btnSure = findViewById(R.id.btn_sure);
        mSp = getSharedPreferences("remote_data", MODE_PRIVATE);
        String ipAddress = mSp.getString("ip_address", "");
        mEtMain.setText(ipAddress);
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
            Toast.makeText(this,"请输入ip",Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences.Editor edit = mSp.edit();
        edit.putString("ip_address",ip);
        edit.apply();
        goNext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //获取通知相关权限
        if (!isNotificationListenerServiceEnabled(this)) {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            Toast.makeText(this, "请授予通知使用权限", Toast.LENGTH_SHORT).show();
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

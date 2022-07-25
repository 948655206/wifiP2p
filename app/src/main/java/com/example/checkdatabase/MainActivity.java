package com.example.checkdatabase;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.companion.WifiDeviceFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.checkdatabase.Contants.Contants;
import com.example.checkdatabase.activity.ReceiverActivity;
import com.example.checkdatabase.activity.SenderActivity;

import java.security.acl.Permission;

public class MainActivity extends AppCompatActivity {
        public static String TAG="MainActivity";
        private WifiP2pManager mManager;
        private BroadcastReceiver mReceiver;
        private IntentFilter mIntentFilter;
        private String[] mPermission;
        private Boolean mIsOpen=false;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initview();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initview() {
        mPermission = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        //添加权限
        ActivityCompat.requestPermissions(MainActivity.this,mPermission,123);
        int readPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.d(TAG, "permission: "+readPermission+writePermission);
        if (readPermission==PackageManager.PERMISSION_GRANTED && writePermission==PackageManager.PERMISSION_GRANTED ){
            mIsOpen=true;
        }
        Log.d(TAG, "mIsOpen: "+mIsOpen);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==123){
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                mIsOpen=true;
            }
            else {
                Toast.makeText(MainActivity.this,"请获取sd卡读取权限",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void ReceiverOnClick(View view) {

        if (mIsOpen){
            startActivity(new Intent(this, ReceiverActivity.class));
            Log.d(TAG, "成功打开权限: ");
        }
    }

    public void SenderOnClick(View view) {
        if (mIsOpen){
            startActivity(new Intent(this, SenderActivity.class));
            Log.d(TAG, "成功打开权限: ");
        }
    }

}
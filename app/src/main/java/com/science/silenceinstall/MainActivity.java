package com.science.silenceinstall;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName() + ">>>>>";
    public static final String APK_URL = "apk_url";
    private String apkUrl = "http://dakaapp.troila.com/download/daka.apk?v=3.0";
    private AppCompatCheckBox mCbSilenceRootInstall, mCbSilenceAutoInstall;
    private EditText mEdApkUrl;
    private DownLoadService mDownLoadService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCbSilenceRootInstall = (AppCompatCheckBox) findViewById(R.id.cb_silence_root_install);
        mCbSilenceAutoInstall = (AppCompatCheckBox) findViewById(R.id.cb_silence_auto_install);
        mEdApkUrl = (EditText) findViewById(R.id.et_apk_url);
        mEdApkUrl.setText(apkUrl);

        if (isAccessibilitySettingsOn(this)) {
            mCbSilenceAutoInstall.setChecked(true);
        } else {
            mCbSilenceAutoInstall.setChecked(false);
        }

        Intent intent = new Intent(MainActivity.this, DownLoadService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.rl_silence_root_install:
                silenceRootInstall();
                break;
            case R.id.rl_silence_auto_install:
                silenceAutoInstall();
                break;
            case R.id.btn_download:
                startDownload();
                break;
        }
    }

    private void silenceRootInstall() {
        if (isRoot()) {
            if (mCbSilenceRootInstall.isChecked()) {
                mCbSilenceRootInstall.setChecked(false);
            } else {
                mCbSilenceRootInstall.setChecked(true);
            }
        } else {
            Toast.makeText(this, "唔好意思，本机冇root权限~~", Toast.LENGTH_SHORT).show();
        }
    }

    private void silenceAutoInstall() {
        if (isAccessibilitySettingsOn(this)) {
            if (mCbSilenceAutoInstall.isChecked()) {
                mCbSilenceAutoInstall.setChecked(false);
            } else {
                mCbSilenceAutoInstall.setChecked(true);
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, 1);
        }
    }

    private void startDownload() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            File fileParent = Environment.getExternalStoragePublicDirectory(DownLoadService.FILE_DIR);
                            File[] files = fileParent.listFiles();
                            if (files == null || files.length == 0) {
                                mDownLoadService.startDownload(apkUrl);
                            } else {
                                for (File file : files) {
                                    if (file.isFile()) {
                                        if (file.getName().equals(DownLoadService.FILE_NAME)) {
                                            mDownLoadService.installRoot();
                                        }
                                    } else {
                                        mDownLoadService.startDownload(apkUrl);
                                    }
                                }
                            }

                        }
                    }
                });
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownLoadService = ((DownLoadService.MyBinder) service).getServices();
            mDownLoadService.registerReceiver();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isAccessibilitySettingsOn(this)) {
            mCbSilenceAutoInstall.setChecked(true);
        } else {
            mCbSilenceAutoInstall.setChecked(false);
        }
    }

    /**
     * 判断手机是否拥有Root权限。
     *
     * @return 有root权限返回true，否则返回false。
     */
    public boolean isRoot() {
        boolean bool = false;
        try {
            if (Runtime.getRuntime().exec("su").getOutputStream() == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bool;
    }

    /**
     * 检测辅助功能是否开启
     *
     * @param mContext
     * @return
     */
    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        // MyAccessibilityService为对应的服务
        final String service = getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();
        Log.e(TAG, "service:" + service);
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.e(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.e(TAG, "***ACCESSIBILITY IS ENABLED***");
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.e(TAG, "accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.e(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.e(TAG, "***ACCESSIBILITY IS DISABLED***");
        }
        return false;
    }
}

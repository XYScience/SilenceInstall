package com.science.silenceinstall;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName() + ">>>>>";
    public static final String SILENCE_ROOT_INSTALL = "SilenceRootInstall";
    public static final String DELETE_DOWNLOAD_FILE = "DeleteDownloadFile";
    private String apkUrl = "http://dakaapp.troila.com/download/daka.apk?v=3.0";
    private AppCompatCheckBox mCbSilenceRootInstall, mCbSilenceAutoInstall, mCbDeleteDownloadFile;
    private EditText mEdApkUrl;
    private DownLoadService mDownLoadService;
    private InstalledReceiver mInstalledReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCbSilenceRootInstall = (AppCompatCheckBox) findViewById(R.id.cb_silence_root_install);
        mCbSilenceAutoInstall = (AppCompatCheckBox) findViewById(R.id.cb_silence_auto_install);
        mCbDeleteDownloadFile = (AppCompatCheckBox) findViewById(R.id.cb_delete_download_file);
        mEdApkUrl = (EditText) findViewById(R.id.et_apk_url);
        mEdApkUrl.setText(apkUrl);

        if (isAccessibilitySettingsOn(this)) {
            mCbSilenceAutoInstall.setChecked(true);
        } else {
            mCbSilenceAutoInstall.setChecked(false);
        }

        /**
         * 开启下载服务
         */
        Intent intent = new Intent(MainActivity.this, DownLoadService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        /**
         * 注册安装程序广播(暂时发现在androidManifest.xml中注册，nexus5 Android7.1接收不到广播）
         */
        mInstalledReceiver = new InstalledReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.registerReceiver(mInstalledReceiver, filter);
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
            case R.id.rl_delete_download_file:
                deleteDownloadFile();
                break;
        }
    }

    /**
     * 是否开启静默安装
     */
    private void silenceRootInstall() {
        if (mCbSilenceRootInstall.isChecked()) {
            mCbSilenceRootInstall.setChecked(false);
            SharedPreferenceUtil.put(this, SILENCE_ROOT_INSTALL, false);
        } else {
            mCbSilenceRootInstall.setChecked(true);
            SharedPreferenceUtil.put(this, SILENCE_ROOT_INSTALL, true);
        }
    }

    /**
     * 是否开启自动安装
     */
    private void silenceAutoInstall() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, 1);
    }

    /**
     * 开始下载并安装
     */
    private void startDownload() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            mDownLoadService.startDownload(apkUrl);
                        }
                    }
                });
    }

    /**
     * 删除下载文件
     */
    private void deleteDownloadFile() {
        if (mCbDeleteDownloadFile.isChecked()) {
            mCbDeleteDownloadFile.setChecked(false);
            SharedPreferenceUtil.put(this, DELETE_DOWNLOAD_FILE, false);
        } else {
            mCbDeleteDownloadFile.setChecked(true);
            SharedPreferenceUtil.put(this, DELETE_DOWNLOAD_FILE, true);
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownLoadService = ((DownLoadService.MyBinder) service).getServices();
            mDownLoadService.registerReceiver(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        if (mInstalledReceiver != null) {
            this.unregisterReceiver(mInstalledReceiver);
        }
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

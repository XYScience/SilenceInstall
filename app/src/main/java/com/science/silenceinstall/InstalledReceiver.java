package com.science.silenceinstall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author SScience
 * @description
 * @email chentushen.science@gmail.com,274240671@qq.com
 * @data 2016/12/5
 */

public class InstalledReceiver extends BroadcastReceiver {

    private static final String TAG = InstalledReceiver.class.getSimpleName() + ">>>>>";

    @Override
    public void onReceive(Context context, Intent intent) {
        //接收安装广播
        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
            String packageName = intent.getDataString();
            Log.e(TAG, "安装了:" + packageName);
            Intent intentDownload = new Intent(context, DownLoadService.class);
            context.startService(intentDownload);
        }
        //接收卸载广播
        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
            String packageName = intent.getDataString();
            Log.e(TAG, "卸载了:" + packageName);
        }
    }
}

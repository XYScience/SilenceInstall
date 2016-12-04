package com.science.silenceinstall;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author SScience
 * @description
 * @email chentushen.science@gmail.com
 * @data 2016/12/4
 */

public class DownLoadService extends Service {

    public static final String FILE_DIR = "TestApk";
    public static final String FILE_NAME = "test.apk";
    private String filePath = Environment.getExternalStoragePublicDirectory(FILE_DIR).getPath() + "/" + FILE_NAME;
    private BroadcastReceiver mBroadcastReceiver;
    private DownloadManager mDownloadManager;
    /**
     * 系统下载器分配的唯一下载任务id，可以通过这个id查询或者处理下载任务
     */
    private long enqueue;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public DownLoadService getServices() {
            return DownLoadService.this;
        }
    }

    public void registerReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 接收下载完成的广播
                installRoot();
                // 销毁当前service
                stopSelf();
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @return 安装成功返回true，安装失败返回false。
     */
    public boolean installRoot() {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
            String command = "pm install -r " + filePath + "\n";
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            Log.e("TAG>>>>>>>", "install msg is " + msg);
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (msg.contains("Failure") || msg.contains("denied")) {
                installAuto();
            } else {
                result = true;
                Toast.makeText(this, "success!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("TAG>>>>>>>", e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        return result;
    }

    public void installAuto() {
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(this, "com.science.fileprovider", new File(filePath));
            localIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(new File(filePath));
        }
        localIntent.setDataAndType(uri, "application/vnd.android.package-archive"); //打开apk文件
        startActivity(localIntent);
    }

    public void startDownload(String apkUrl) {
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        // 设置下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        // 设置下载文件类型
        request.setMimeType("application/vnd.android.package-archive");
        // 设置下载到本地的文件夹和文件名
        request.setDestinationInExternalPublicDir(FILE_DIR, FILE_NAME);
        // 设置下载时或者下载完成时是否通知栏显示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("正在下载···");
        // 执行下载，并返回任务唯一id
        enqueue = mDownloadManager.enqueue(request);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }
}

package com.science.silenceinstall;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.nio.charset.Charset;

/**
 * @author SScience
 * @description 下载单个文件服务
 * @email chentushen.science@gmail.com
 * @data 2016/12/4
 */

public class DownLoadService extends Service {

    private static final String TAG = DownLoadService.class.getSimpleName() + ">>>>>";
    private static final String FILE_DIR = "TestApk";
    private Context mContext;
    private BroadcastReceiver mBroadcastReceiver;
    public DownloadManager mDownloadManager;
    /**
     * 系统下载器分配的唯一下载任务id，可以通过这个id查询或者处理下载任务
     */
    public long enqueueId = -1;
    private String apkPath;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /**
         * 安装完成删除文件
         */
        File file = new File(apkPath);
        if (file.exists()) {
            file.delete();
        }
        return START_STICKY;
    }

    public void registerReceiver(Context context) {
        mContext = context;
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 接收下载完成的广播
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if (downloadId != enqueueId) return;
                DownloadManager.Query query = new DownloadManager.Query().setFilterById(enqueueId);
                Cursor cursor = mDownloadManager.query(query);
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String apkUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                        File file = new File(Uri.parse(apkUri).getPath());
                        apkPath = file.getAbsolutePath();

                        if ((Boolean) SharedPreferenceUtil.get(mContext, MainActivity.SILENCE_ROOT_INSTALL, false)) {
                            installRoot(apkPath);
                        } else {
                            installAuto(apkPath);
                        }
                    }
                }
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    }

    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @param apkPath
     * @return 安装成功返回true，安装失败返回false。
     */
    public void installRoot(final String apkPath) {
        new AsyncTask<Boolean, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Boolean... params) {
                boolean result = false;
                DataOutputStream dataOutputStream = null;
                BufferedReader errorStream = null;
                try {
                    // 申请su权限
                    Process process = Runtime.getRuntime().exec("su");
                    dataOutputStream = new DataOutputStream(process.getOutputStream());
                    // 执行pm install命令
                    String command = "pm install -r " + apkPath + "\n";
                    dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
                    dataOutputStream.flush();
                    dataOutputStream.writeBytes("exit\n");
                    dataOutputStream.flush();
                    int i = process.waitFor();
                    if (i == 0) {
                        result = true; // 正确获取root权限
                    } else {
                        result = false; // 没有root权限，或者拒绝获取root权限
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    try {
                        if (dataOutputStream != null) {
                            dataOutputStream.close();
                        }
                        if (errorStream != null) {
                            errorStream.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean hasRoot) {
                // Toast.makeText(this, "唔好意思，本机冇root权限~~", Toast.LENGTH_SHORT).show();
                if (!hasRoot) {
                    installAuto(apkPath);
                } else {
                    Toast.makeText(DownLoadService.this, "安装完成!", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    /**
     * 自动安装
     *
     * @param apkPath
     */
    public void installAuto(String apkPath) {
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        /**
         * Android7.0+禁止应用对外暴露file://uri，改为content://uri；具体参考FileProvider
         */
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(this, "com.science.fileprovider", new File(apkPath));
            localIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(new File(apkPath));
        }
        localIntent.setDataAndType(uri, "application/vnd.android.package-archive"); //打开apk文件
        startActivity(localIntent);
    }

    /**
     * 开始下载apk
     *
     * @param apkUrl
     */
    public void startDownload(String apkUrl) {
        String apkName = "test.apk";
        File file = new File(Environment.getExternalStoragePublicDirectory(FILE_DIR) + "/" + apkName);
        if (!file.exists()) {
            // 设置下载地址
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            // 设置下载文件类型
            request.setMimeType("application/vnd.android.package-archive");
            // 设置下载到本地的文件夹和文件名
            request.setDestinationInExternalPublicDir(FILE_DIR, apkName);
            // 设置下载时或者下载完成时是否通知栏显示
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle("应用下载");
            // 执行下载，并返回任务唯一id
            enqueueId = mDownloadManager.enqueue(request);
        } else {
            apkPath = file.getAbsolutePath();
            if ((Boolean) SharedPreferenceUtil.get(mContext, MainActivity.SILENCE_ROOT_INSTALL, false)) {
                installRoot(file.getAbsolutePath());
            } else {
                installAuto(file.getAbsolutePath());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        Log.e(TAG, "onDestroy:>>>>>>>");
    }
}

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author SScience
 * @description
 * @email chentushen.science@gmail.com
 * @data 2016/12/4
 */

public class DownLoadService extends Service {

    private static final String TAG = DownLoadService.class.getSimpleName() + ">>>>>";
    private static final String FILE_DIR = "TestApk";
    private Context mContext;
    private BroadcastReceiver mBroadcastReceiver;
    public DownloadManager mDownloadManager;
    private ScheduledExecutorService scheduledExecutorService;
    private Future<?> future;
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
        File file = new File(apkPath);
        if (file.exists()) {
            file.delete();
        }
        // 销毁当前service
        stopSelf(startId);
        return START_STICKY;
    }

    public void registerReceiver(Context context) {
        mContext = context;
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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
                        // 接收下载完成的广播
                        if ((Boolean) SharedPreferenceUtil.get(mContext, MainActivity.SILENCE_ROOT_INSTALL, false)) {
                            installRoot();
                        } else {
                            installAuto();
                        }
                    }
                }
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @return 安装成功返回true，安装失败返回false。
     */
    public void installRoot() {
        new AsyncTask<Boolean, Boolean, Boolean>() {
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
                        result = true;
                    } else {
                        result = false;
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
                    installAuto();
                } else {
                    Toast.makeText(DownLoadService.this, "安装完成!", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    /**
     * 自动安装
     */
    public void installAuto() {
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
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
        String[] s1 = apkUrl.split("download/");
        String[] s2 = s1[1].split("\\?");
        // 设置下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        // 设置下载文件类型
        request.setMimeType("application/vnd.android.package-archive");
        // 设置下载到本地的文件夹和文件名
        request.setDestinationInExternalPublicDir(FILE_DIR, s2[0]);
        // 设置下载时或者下载完成时是否通知栏显示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setTitle("应用下载");
        // 执行下载，并返回任务唯一id
        enqueueId = mDownloadManager.enqueue(request);

        //每过100ms通知handler去查询下载状态
        future = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Message msg = mHandler.obtainMessage();
                mHandler.sendMessage(msg);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(enqueueId);
            Cursor cursor = mDownloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                //此处直接查询文件大小
                long downSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //获取文件下载总大小
                long fileTotalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                cursor.close();

                if (fileTotalSize != 0) {
                    int percentage = (int) (downSize * 100 / fileTotalSize);
                    if (mDownloadCallback != null) {
                        mDownloadCallback.callback(percentage);
                    }
                }

                //终止轮询task
                if (fileTotalSize == downSize)
                    future.cancel(true);
            }
            return false;
        }
    });

    public void cancelDownload() {
        mDownloadManager.remove(enqueueId);
        if (mDownloadCallback != null) {
            mDownloadCallback.callback(0);
        }
    }

    interface DownloadCallback {
        void callback(int percentage);
    }

    public DownloadCallback mDownloadCallback;

    public void setDownloadCallback(DownloadCallback downloadCallback) {
        this.mDownloadCallback = downloadCallback;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        if (future != null && !future.isCancelled())
            future.cancel(true);
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown())
            scheduledExecutorService.shutdown();
        Log.e(TAG, "onDestroy:>>>>>>>");
    }
}

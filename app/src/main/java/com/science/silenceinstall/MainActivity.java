package com.science.silenceinstall;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private String apkPath = Environment.getExternalStorageDirectory().toString() + "/" + "dpi.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_silence_install) {
            install(apkPath);
        } else if (id == R.id.btn_open_auxiliary) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } else if (id == R.id.btn_silence_install_no_root) {
            Intent localIntent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(this, "com.science.fileprovider", new File(apkPath));
                localIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(new File(apkPath));
            }
            localIntent.setDataAndType(uri, "application/vnd.android.package-archive"); //打开apk文件
            ComponentName comp;
            if (android.os.Build.VERSION.SDK_INT < 23) {
                comp = new ComponentName("com.android.packageinstaller", "com.android.packageinstaller.PackageInstallerActivity");
            } else {
                comp = new ComponentName("com.google.android.packageinstaller", "com.android.packageinstaller.PackageInstallerActivity");
            }
            localIntent.setComponent(comp);
            startActivity(localIntent);
        }
    }

    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @param apkPath 要安装的apk文件的路径
     * @return 安装成功返回true，安装失败返回false。
     */
    public boolean install(String apkPath) {
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
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            Log.e("TAG", "install msg is " + msg);
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (!msg.contains("Failure")) {
                result = true;
                Toast.makeText(this, "success!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("TAG", e.getMessage(), e);
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
}

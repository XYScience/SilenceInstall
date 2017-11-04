# SilenceInstall
静默安装和自动安装    

一，静默安装  
------------    
获取root进行静默安装比较简单：  
1，申请root权限`Runtime.getRuntime().exec("su")`;  
2，通过数据输出流`DataOutputStream`写入pm install命令；  
3，最后获取Process进程的返回值`int i = process.waitFor();`，如果i=0，则表明已获取root权限。    

二，免root自动安装  
------------  
>免root自动安装其实就是借助AccessibilityService无障服务  
>官网：https://developer.android.google.cn/reference/android/accessibilityservice/AccessibilityService.html  
  
* 1，在res/xml下新建`accessibility_service.xml`文件：  
   ```
   <?xml version="1.0" encoding="utf-8"?>
   <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
       android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewScrolled"
       android:accessibilityFeedbackType="feedbackGeneric"
       android:accessibilityFlags="flagDefault"
       android:canRetrieveWindowContent="true"
       android:description="@string/accessibility_service_description"
       android:notificationTimeout="100"
       android:packageNames="com.android.packageinstaller,com.google.android.packageinstaller,com.samsung.android.packageinstaller,co   m.lenovo.safecenter,com.lenovo.security,com.xiaomi.gamecenter" />
       <!--
           packageNames:指定监听哪个应用程序下的窗口活动，为了兼容多数Rom，可用","分割。
           description:指定在无障碍服务当中显示给用户看的说明信息。
           accessibilityEventTypes:指定我们在监听窗口中可以接收哪些事件；如题的三个事件便可完成自动安装。
           accessibilityFlags:可以指定无障碍服务的一些附加参数，传默认值flagDefault就行。
           accessibilityFeedbackType:指定无障碍服务的反馈方式，如语音，震动等。这里使用feedbackGeneric(普通回馈)。
           canRetrieveWindowContent:指定是否允许程序读取窗口中的节点和内容，允许则必须为true。
           notificationTimeout:响应事件的时间间隔
       -->
   ```  
* 2，新建`MyAccessibilityService`继承`AccessibilityService`   
`onAccessibilityEvent()`是主要的操作方法，响应AccessibilityEvent的事件，在用户操作的过程中，系统不断的发送。  
(1) 获取活动窗口的根节点
   `AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();`   
(2) 检查是安装还是卸载操作，避免误操作；  
(3) 通过`nodeInfo.findAccessibilityNodeInfosByText(clickText);`获取特定事件资源节点列表；  
(4) 如果可点击，则进行点击事件`info.performAction(AccessibilityNodeInfo.ACTION_CLICK);`。  

* 3，最后在`androidManifest.xml`中申明服务  
   ```  
   <service
       android:name=".MyAccessibilityService"
       android:label="SScience的自动安装服务测试"
       android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
           <intent-filter>
               <action android:name="android.accessibilityservice.AccessibilityService" />
           </intent-filter>
           <meta-data
               android:name="android.accessibilityservice"
               android:resource="@xml/accessibility_service" />
     </service>  
     <!--
         name:创建一个Service类继承AccessibilityService来处理接收到的事件
         label:在系统设置辅助功能中显示的名称。
         resource：指向配置的xml文件。
         其它都是固定的。
     -->
   ```  
三，兼容Android7.0+  
-----  
>实现应用安装需要构造`uri = Uri.fromFile(new File(apkPath));`  
>但是在Android7.0+以上，禁止对外暴露file://URI，解决办法是content://URI，具体见FileProvider  
>官网：https://developer.android.google.cn/reference/android/support/v4/content/FileProvider.html  

* 1，在res/xml下新建file_paths.xml文件  
   ```
   <?xml version="1.0" encoding="utf-8"?>
   <paths xmlns:android="http://schemas.android.com/apk/res/android">
       <external-path name="apk" path="TestApk"/>
   </paths>
   <!--
       <files-path/> 代表的根目录： Context.getFilesDir()
       <external-path/> 代表的根目录: Environment.getExternalStorageDirectory() or
       <cache-path/> 代表的根目录: getCacheDir()
       path：app要存储的位置，即Environment.getExternalStorageDirectory()根目录下的TestApk文件夹
    -->
   ```  
* 2，在`androidManifest.xml`中申provider  

   ```
   <provider
       android:name="android.support.v4.content.FileProvider"
       android:authorities="com.science.fileprovider"
       android:exported="false"
       android:grantUriPermissions="true">
       <meta-data
           android:name="android.support.FILE_PROVIDER_PATHS"
           android:resource="@xml/file_paths" />
   </provider>  

   <!--
        name:可以直接使用默认的FileProvider，也可以继承做其它操作。
        authorities:定义唯一一个authorities，和构建content://uri中的getUriForFile方法的authority参数一致
        resource：指向保存位置path配置的xml文件。
        其它都是固定的。
   -->
   ```  
* 3，java代码实现  

   ```
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
   ```  
四，已测试机型  
-----  
  * 1，坚果YQ601-Android5.1.1-Smartusan OS 2.5.3(未root)  
  * 2，OPPO R9m-Android5.1-ColorOS V3.0.0(未root)  
  * 3，MX4-Android5.1-Flyme6.6.12.2daily(有root)  
  * 4，MX6-Android6.0-Flyme5.2.4.1A(未root)  
  * 5，HM 1SLTETD-Android4.4.2开发版(有root)  
  * 6，MI2-Android5.0.2-MIUI8 6.12.8开发版(有root)    
  * 7，MI NOTE LTE-Android6.0.1-MIUI8 6.12.8开发版(有root)  
  * 8，Nexus5-Android7.1-AOSP(有root)  
  * 9，寨版Android4.4(有root) 

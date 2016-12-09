package com.science.silenceinstall;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * @author SScience
 * @description 自动安装服务
 * @email chentushen.science@gmail.com,274240671@qq.com
 * @data 2016/12/2
 */

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = MyAccessibilityService.class.getSimpleName() + ">>>>>";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        /**
         * 在某些rom上，event.getSource()获取到的某些值为null；
         */
        // AccessibilityNodeInfo nodeInfo = event.getSource()：
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            if (!checkIfUnInstall(nodeInfo)) {
                iterateNodes(nodeInfo);
            }
        }
    }

    /**
     * 检查是否是卸载操作（如果安装界面出现“卸载”，那就无计咯）
     *
     * @param nodeInfo
     * @return
     */
    private Boolean checkIfUnInstall(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> infos = nodeInfo.findAccessibilityNodeInfosByText("卸载");
        if (infos != null && !infos.isEmpty()) {
            return true;
        }
        return false;
    }

    private void iterateNodes(AccessibilityNodeInfo nodeInfo) {
        String[] clickTexts = {"安装", "继续", "继续安装", "替换", "下一步", "仅允许一次", "完成", "确定"};
        for (String clickText : clickTexts) {
            List<AccessibilityNodeInfo> infos = nodeInfo.findAccessibilityNodeInfosByText(clickText);
            for (AccessibilityNodeInfo info : infos) {
                if (info.isClickable() && info.isEnabled()) {
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        Log.e(TAG, "无障碍服务已开启");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "无障碍服务已关闭");
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }
}

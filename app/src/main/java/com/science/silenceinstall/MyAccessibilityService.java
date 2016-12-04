package com.science.silenceinstall;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author SScience
 * @description
 * @email chentushen.science@gmail.com,274240671@qq.com
 * @data 2016/12/2
 */

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = MyAccessibilityService.class.getSimpleName();
    Map<Integer, Boolean> handledMap = new HashMap<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.e(TAG, "onAccessibilityEvent:>>>>>>>>>>");
        // AccessibilityNodeInfo nodeInfo = event.getSource()：
        // 在Android6.0以上，经实测初步判断event.getSource()在typeWindowContentChanged下，即窗口内容改变时，
        // 获取不到"事件资源的根节点"，只能获取改变部分的第一个节点；
        // 因为在Android6.0以上，从正在安装到安装完成，窗口状态不变，但窗口内容改变，所以最后获取不到“完成”
        // 的节点，也就是没有自动点击安装完成。
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            // 只监听typeWindowStateChanged|typeWindowContentChanged事件，配置文件accessibility_service定义；
            // 两个事件同时都有被触发的可能，那么为了防止二次处理的情况，这里我们使用了一个Map来过滤掉重复事件
            if (handledMap.get(event.getWindowId()) == null) {
                boolean handled = iterateNodesAndHandle(nodeInfo);
                if (handled) {
                    handledMap.put(event.getWindowId(), true);
                }
            }
        }
    }

    private boolean iterateNodesAndHandle(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo.isClickable() && nodeInfo.isEnabled()) {
            String nodeContent = nodeInfo.getText().toString();
            String[] labels = new String[]{"安装", "下一步", "允许", "确定", "完成"};
            for (String label : labels) {
                if (nodeContent != null && nodeContent.contains(label)) {
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }
        if ("android.widget.ScrollView".equals(nodeInfo.getClassName())) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        int childCount = nodeInfo.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
            if (iterateNodesAndHandle(childNodeInfo)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onServiceConnected() {
        Log.e(TAG, "无障碍服务已开启:>>>>>>>>>>");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "无障碍服务已关闭:>>>>>>>>>>");
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }
}

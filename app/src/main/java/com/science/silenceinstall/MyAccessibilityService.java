package com.science.silenceinstall;

import android.accessibilityservice.AccessibilityService;
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
        //AccessibilityNodeInfo nodeInfo = event.getSource()：在nexus5 Android7.1上最后安装完成不出现“完成”的nodeInfo
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            int eventType = event.getEventType();
            //两个同时都有被触发的可能，那么为了防止二次处理的情况，这里我们使用了一个Map来过滤掉重复事件
            if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (handledMap.get(event.getWindowId()) == null) {
                    boolean handled = iterateNodesAndHandle(nodeInfo);
                    if (handled) {
                        handledMap.put(event.getWindowId(), true);
                    }
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
    public void onInterrupt() {
    }
}

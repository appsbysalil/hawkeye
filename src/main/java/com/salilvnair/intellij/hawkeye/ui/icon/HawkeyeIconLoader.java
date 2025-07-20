package com.salilvnair.intellij.hawkeye.ui.icon;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;

public class HawkeyeIconLoader {

    public static Icon getIcon(String iconPath, Class<?> aClass) {
        return IconLoader.getIcon(iconPath, aClass.getClassLoader());
    }

    public static Icon getIcon(String iconPath, String darkIconPath, Class<?> aClass) {
        boolean isDarkTheme = !JBColor.isBright() || (SystemInfo.isMac && UIUtil.isUnderIntelliJLaF());
        if(darkIconPath!=null && isDarkTheme) {
            return IconLoader.getIcon(darkIconPath, aClass.getClassLoader());
        }
        return IconLoader.getIcon(iconPath, aClass.getClassLoader());
    }

}

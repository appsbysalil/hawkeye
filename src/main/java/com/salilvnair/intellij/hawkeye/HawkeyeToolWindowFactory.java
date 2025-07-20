package com.salilvnair.intellij.hawkeye;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.salilvnair.intellij.hawkeye.ui.HawkeyeToolWindow;
import com.salilvnair.intellij.hawkeye.ui.icon.HawkeyeIcons;
import com.salilvnair.intellij.hawkeye.utils.HawkeyeUtils;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class HawkeyeToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        HawkeyeToolWindow window = new HawkeyeToolWindow(project);
        JComponent content = window.getComponent();
        toolWindow.getComponent().add(content);
        AnAction openAsTabAction = new AnAction("About Hawkeye") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                HawkeyeUtils.showAbout(content);
            }
        };
        openAsTabAction.getTemplatePresentation().setIcon(HawkeyeIcons.HawkeyeIcon);

        List<AnAction> actionList = Collections.singletonList(openAsTabAction);
        toolWindow.setTitleActions(actionList);
    }
}
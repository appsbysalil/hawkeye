package com.salilvnair.intellij.hawkeye.utils;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.salilvnair.intellij.hawkeye.ui.icon.HawkeyeIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HawkeyeUtils {
    private HawkeyeUtils() {}

    public static void hidePanelWithAnimation(final JPanel panel, boolean visibility) {
        Timer timer = new Timer(20, new ActionListener() {
            private float alpha = 1f;

            @Override
            public void actionPerformed(ActionEvent e) {
                alpha -= 0.05f;
                if (alpha <= 0f) {
                    ((Timer) e.getSource()).stop();
                    panel.setVisible(visibility);
                }
                else {
                    panel.setOpaque(true);
                    panel.setBackground(new JBColor(new Color(panel.getBackground().getRed(), panel.getBackground().getGreen(), panel.getBackground().getBlue(), (int) (alpha * 255)), new Color(panel.getBackground().getRed(), panel.getBackground().getGreen(), panel.getBackground().getBlue(), (int) (alpha * 255))));
                    panel.repaint();
                }
            }
        });
        timer.start();
    }

    public static void showAbout(Component component) {
        String message = """
                <html><font size="5"><b>Hawkeye 1.0.0 (Build HK-1.0.0)</b></font><br>
               
                <html>Website: <a href="www.salilvnair.com">www.salilvnair.com</a></html>
                <html>Support: <a href="mailto:support@salilvnair.com">support@salilvnair.com</a></html>
                
                Powered by open source software
                License: MIT
                Copyright © 2025
                """;
        JOptionPane.showMessageDialog(component, message, "About Hawkeye", JOptionPane.ERROR_MESSAGE, HawkeyeIcons.HawkeyeIcon48);
    }

    public static @NotNull BasicSplitPaneUI thinDivider() {
        return new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {}

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(new JBColor(Gray._213, Gray._50)); // Set the color of the divider
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        };
    }

}

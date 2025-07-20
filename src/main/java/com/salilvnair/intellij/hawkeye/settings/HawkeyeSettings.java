package com.salilvnair.intellij.hawkeye.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

@State(name = "HawkeyeSettings", storages = @Storage("HawkeyeSettings.xml"))
@Service(Service.Level.APP)
public final class HawkeyeSettings implements PersistentStateComponent<HawkeyeSettings.State> {

    public static class State {
        public String emoji = "👉";
        public Color lightColor = new Color(255, 255, 153);
        public Color darkColor = new Color(100, 100, 200);
        public java.util.List<String> includedExtensions = new ArrayList<>(Arrays.asList("class", "xml", "java", "properties", "json"));
    }

    private State state = new State();


    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // Static access using ApplicationService
    public static HawkeyeSettings getInstance() {
        return com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(HawkeyeSettings.class);
    }

    public static String getEmoji() {
        return getInstance().state.emoji;
    }

    public static void setEmoji(String emoji) {
        getInstance().state.emoji = emoji;
    }

    public static Color getLightColor() {
        return getInstance().state.lightColor;
    }

    public static Color getDarkColor() {
        return getInstance().state.darkColor;
    }

    public static void setHighlightColors(Color light, Color dark) {
        getInstance().state.lightColor = light;
        getInstance().state.darkColor = dark;
    }

    public static JBColor getHighlightColor() {
        return new JBColor(getLightColor(), getDarkColor());
    }

    public static java.util.List<String> getIncludedFileExtensions() {
        return getInstance().state.includedExtensions;
    }

    public static void setIncludedFileExtensions(java.util.List<String> extensions) {
        getInstance().state.includedExtensions = extensions;
    }
}

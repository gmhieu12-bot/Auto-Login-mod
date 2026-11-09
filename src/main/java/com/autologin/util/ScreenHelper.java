package com.autologin.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Universal Screen accessor compatible across Minecraft 26.1.2 and 26.2.
 * - In 26.2: client.gui.screen() & client.gui.setScreen(screen)
 * - In 26.1.2: client.screen & client.setScreen(screen)
 */
public class ScreenHelper {
    private static Method guiScreenMethod = null;
    private static Method guiSetScreenMethod = null;
    private static Field mcScreenField = null;
    private static Method mcSetScreenMethod = null;
    private static boolean initialized = false;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // 1. Try 26.2: net.minecraft.client.gui.Gui (screen() & setScreen(Screen))
        try {
            Class<?> guiClass = Class.forName("net.minecraft.client.gui.Gui");
            try {
                guiScreenMethod = guiClass.getMethod("screen");
                guiScreenMethod.setAccessible(true);
            } catch (Throwable ignored) {}
            try {
                guiSetScreenMethod = guiClass.getMethod("setScreen", Screen.class);
                guiSetScreenMethod.setAccessible(true);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        // 2. Try 26.1.2: net.minecraft.client.Minecraft (screen field & setScreen(Screen))
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (Screen.class.isAssignableFrom(f.getType())) {
                    mcScreenField = f;
                    mcScreenField.setAccessible(true);
                    break;
                }
            }
        } catch (Throwable ignored) {}

        try {
            mcSetScreenMethod = Minecraft.class.getMethod("setScreen", Screen.class);
            mcSetScreenMethod.setAccessible(true);
        } catch (Throwable ignored) {}
    }

    public static Screen getScreen(Minecraft client) {
        if (client == null) return null;
        init();

        // Check 26.2: client.gui.screen()
        if (guiScreenMethod != null) {
            try {
                Object gui = client.gui;
                if (gui != null) {
                    return (Screen) guiScreenMethod.invoke(gui);
                }
            } catch (Throwable ignored) {}
        }

        // Check 26.1.2: client.screen
        if (mcScreenField != null) {
            try {
                return (Screen) mcScreenField.get(client);
            } catch (Throwable ignored) {}
        }

        return null;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        if (client == null) return;
        init();

        // Check 26.2: client.gui.setScreen(screen)
        if (guiSetScreenMethod != null) {
            try {
                Object gui = client.gui;
                if (gui != null) {
                    guiSetScreenMethod.invoke(gui, screen);
                    return;
                }
            } catch (Throwable ignored) {}
        }

        // Check 26.1.2: client.setScreen(screen)
        if (mcSetScreenMethod != null) {
            try {
                mcSetScreenMethod.invoke(client, screen);
            } catch (Throwable ignored) {}
        }
    }
}

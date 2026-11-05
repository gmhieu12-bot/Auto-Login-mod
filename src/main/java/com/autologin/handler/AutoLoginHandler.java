package com.autologin.handler;

import com.autologin.config.AutoLoginConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AutoLoginHandler {
    private static int pendingTicks = -1;
    private static Screen targetScreen = null;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!AutoLoginConfig.isEnabled()) return;

            if (isLoginDialog(screen)) {
                targetScreen = screen;
                pendingTicks = AutoLoginConfig.getDelayTicks();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingTicks > 0) {
                pendingTicks--;
                if (pendingTicks == 0 && targetScreen != null) {
                    Screen active = client.gui.screen();
                    if (AutoLoginConfig.isEnabled() && active == targetScreen) {
                        executeSubmission(client, targetScreen);
                    }
                    targetScreen = null;
                }
            }
        });
    }

    public static boolean isLoginTitle(String rawTitle) {
        if (rawTitle == null) return false;
        String title = rawTitle.toLowerCase();
        return title.contains("welcome back")
                || title.contains("login")
                || title.contains("đăng nhập")
                || title.contains("dang nhap")
                || title.contains("xác thực")
                || title.contains("mật khẩu");
    }

    public static boolean isLoginDialog(Screen screen) {
        if (screen == null) return false;

        if (isLoginTitle(screen.getTitle().getString())) {
            return true;
        }

        if (screen instanceof DialogScreen<?>) {
            List<EditBox> editBoxes = new ArrayList<>();
            List<Button> buttons = new ArrayList<>();
            scanWidgetsRecursively(screen, editBoxes, buttons);
            return !editBoxes.isEmpty() && !buttons.isEmpty();
        }
        return false;
    }

    public static void scanWidgetsRecursively(ContainerEventHandler parent, List<EditBox> editBoxes, List<Button> buttons) {
        if (parent == null || parent.children() == null) return;
        for (GuiEventListener child : parent.children()) {
            if (child instanceof EditBox eb) {
                editBoxes.add(eb);
            } else if (child instanceof Button btn && !(child instanceof ImageButton)) {
                // Bỏ qua ImageButton (nút cảnh báo [!] 20x20 trên góc phải tiêu đề DialogScreen)
                buttons.add(btn);
            }
            if (child instanceof ContainerEventHandler nested) {
                scanWidgetsRecursively(nested, editBoxes, buttons);
            }
        }
    }

    public static Button selectActionButton(List<Button> buttons) {
        if (buttons == null || buttons.isEmpty()) return null;

        // Ưu tiên 1: Khớp từ khóa đăng nhập tiếng Việt / tiếng Anh (bao gồm "[ ĐĂNG NHẬP ]")
        for (Button btn : buttons) {
            String label = btn.getMessage().getString().toLowerCase();
            if (label.contains("đăng nhập") || label.contains("dang nhap")
                    || label.contains("sign in") || label.contains("login")
                    || label.contains("xác nhận") || label.contains("xac nhan")
                    || label.contains("tiếp tục") || label.contains("submit")
                    || label.contains("vào") || label.contains("ok")) {
                return btn;
            }
        }

        // Ưu tiên 2: Chọn nút đầu tiên không phải nút Hủy / Thoát / Quên mật khẩu
        for (Button btn : buttons) {
            String label = btn.getMessage().getString().toLowerCase();
            if (!label.contains("cancel") && !label.contains("hủy")
                    && !label.contains("thoát") && !label.contains("disconnect")
                    && !label.contains("quên") && !label.contains("forgot")) {
                return btn;
            }
        }

        return buttons.getFirst();
    }

    public static boolean submitToContainer(ContainerEventHandler root, String password) {
        if (root == null || password == null || password.isEmpty()) return false;

        List<EditBox> editBoxes = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();
        scanWidgetsRecursively(root, editBoxes, buttons);

        if (!editBoxes.isEmpty()) {
            EditBox passwordBox = editBoxes.getFirst();
            passwordBox.setValue(password);
            if (Minecraft.getInstance() != null) {
                try {
                    passwordBox.setFocused(true);
                } catch (Throwable ignored) {
                }
            }
        }

        Button actionButton = selectActionButton(buttons);
        if (actionButton != null && actionButton.active) {
            return triggerButtonPress(actionButton);
        }
        return false;
    }

    private static void executeSubmission(Minecraft client, Screen screen) {
        ServerData server = client.getCurrentServer();
        String serverIp = server != null ? server.ip : null;

        String password = AutoLoginConfig.getPassword(serverIp);
        if (password == null || password.isEmpty()) {
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal("§c[AutoLogin] Chưa cài mật khẩu! Mở Mod Menu hoặc gõ lệnh: §e/autologin")
                );
            }
            return;
        }

        boolean clicked = submitToContainer(screen, password);
        if (client.player != null && clicked) {
            client.player.sendSystemMessage(
                Component.literal("§a[AutoLogin] Đã tự động điền mật khẩu và ấn nút đăng nhập!")
            );
        }
    }

    /**
     * Kích hoạt trực tiếp callback Button.OnPress của Minecraft 26.2
     */
    public static boolean triggerButtonPress(Button button) {
        if (button == null) return false;

        // Lớp 1: Truy cập trực tiếp field protected final Button.OnPress onPress trên class Button
        try {
            for (Field field : Button.class.getDeclaredFields()) {
                if (Button.OnPress.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Button.OnPress onPressCallback = (Button.OnPress) field.get(button);
                    if (onPressCallback != null) {
                        onPressCallback.onPress(button);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Lớp 2: Quét toàn bộ phương thức onPress (kể cả protected/private) trong cây kế thừa của Button
        Class<?> curr = button.getClass();
        while (curr != null && curr != Object.class) {
            for (Method m : curr.getDeclaredMethods()) {
                if (m.getName().equals("onPress")) {
                    try {
                        m.setAccessible(true);
                        if (m.getParameterCount() == 0) {
                            m.invoke(button);
                            return true;
                        } else if (m.getParameterCount() == 1) {
                            m.invoke(button, new Object[]{null});
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            curr = curr.getSuperclass();
        }

        return false;
    }
}

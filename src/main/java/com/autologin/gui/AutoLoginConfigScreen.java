package com.autologin.gui;

import com.autologin.config.AutoLoginConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoLoginConfigScreen extends Screen {
    private final Screen parent;

    private EditBox defaultPassBox;
    private EditBox serverIpBox;
    private EditBox serverPassBox;

    private boolean showPasswords = false;
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ROWS = 4;

    public AutoLoginConfigScreen(Screen parent) {
        super(Component.literal("Cấu hình Auto Login Dialog (26.2)"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;

        // 1. Nút Bật / Tắt Auto Login
        Button toggleBtn = Button.builder(getEnabledText(), btn -> {
            AutoLoginConfig.setEnabled(!AutoLoginConfig.isEnabled());
            btn.setMessage(getEnabledText());
        }).bounds(centerX - 155, 28, 150, 20).build();
        this.addRenderableWidget(toggleBtn);

        // 2. Chỉnh Delay Ticks (- / +)
        Button minusBtn = Button.builder(Component.literal("- Delay"), btn -> {
            AutoLoginConfig.setDelayTicks(AutoLoginConfig.getDelayTicks() - 1);
        }).bounds(centerX + 5, 28, 72, 20).build();
        this.addRenderableWidget(minusBtn);

        Button plusBtn = Button.builder(Component.literal("+ Delay"), btn -> {
            AutoLoginConfig.setDelayTicks(AutoLoginConfig.getDelayTicks() + 1);
        }).bounds(centerX + 83, 28, 72, 20).build();
        this.addRenderableWidget(plusBtn);

        // 3. Ô nhập Mật khẩu mặc định (dùng chung cho mọi server)
        this.defaultPassBox = new EditBox(this.font, centerX - 155, 66, 230, 20, Component.literal("Mật khẩu mặc định"));
        this.defaultPassBox.setMaxLength(128);
        this.defaultPassBox.setValue(AutoLoginConfig.getDefaultPassword());
        this.defaultPassBox.setResponder(AutoLoginConfig::setDefaultPassword);
        this.addRenderableWidget(this.defaultPassBox);

        // Nút Ẩn / Hiện mật khẩu
        Button showPassBtn = Button.builder(getShowPassText(), btn -> {
            this.showPasswords = !this.showPasswords;
            btn.setMessage(getShowPassText());
        }).bounds(centerX + 80, 66, 75, 20).build();
        this.addRenderableWidget(showPassBtn);

        // 4. Thêm / Cập nhật mật khẩu theo từng Server IP
        this.serverIpBox = new EditBox(this.font, centerX - 155, 106, 115, 20, Component.literal("IP Server"));
        this.serverIpBox.setMaxLength(128);
        ServerData currentServer = Minecraft.getInstance().getCurrentServer();
        if (currentServer != null && this.serverIpBox.getValue().isEmpty()) {
            this.serverIpBox.setValue(currentServer.ip);
        }
        this.addRenderableWidget(this.serverIpBox);

        this.serverPassBox = new EditBox(this.font, centerX - 35, 106, 110, 20, Component.literal("Mật khẩu Server"));
        this.serverPassBox.setMaxLength(128);
        this.addRenderableWidget(this.serverPassBox);

        Button saveIpBtn = Button.builder(Component.literal("§aLưu IP"), btn -> {
            String ip = this.serverIpBox.getValue().trim();
            String pass = this.serverPassBox.getValue();
            if (!ip.isEmpty() && !pass.isEmpty()) {
                AutoLoginConfig.setPassword(ip, pass);
                this.serverPassBox.setValue("");
                this.rebuildWidgets();
            }
        }).bounds(centerX + 80, 106, 75, 20).build();
        this.addRenderableWidget(saveIpBtn);

        // 5. Danh sách các Server đã lưu
        List<Map.Entry<String, String>> entries = new ArrayList<>(AutoLoginConfig.getAllPasswords().entrySet());
        int startY = 148;
        int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE_ROWS);
        if (this.scrollOffset > maxScroll) {
            this.scrollOffset = maxScroll;
        }

        for (int i = 0; i < MAX_VISIBLE_ROWS; i++) {
            int index = i + this.scrollOffset;
            if (index >= entries.size()) break;

            Map.Entry<String, String> entry = entries.get(index);
            int rowY = startY + (i * 22);

            Button editBtn = Button.builder(Component.literal("Sửa"), btn -> {
                this.serverIpBox.setValue(entry.getKey());
                this.serverPassBox.setValue(entry.getValue());
            }).bounds(centerX + 68, rowY, 40, 18).build();
            this.addRenderableWidget(editBtn);

            Button deleteBtn = Button.builder(Component.literal("§cXóa"), btn -> {
                AutoLoginConfig.removePassword(entry.getKey());
                this.rebuildWidgets();
            }).bounds(centerX + 112, rowY, 43, 18).build();
            this.addRenderableWidget(deleteBtn);
        }

        if (entries.size() > MAX_VISIBLE_ROWS) {
            Button upBtn = Button.builder(Component.literal("▲"), btn -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    this.rebuildWidgets();
                }
            }).bounds(centerX + 160, startY, 20, 20).build();
            this.addRenderableWidget(upBtn);

            Button downBtn = Button.builder(Component.literal("▼"), btn -> {
                if (this.scrollOffset < maxScroll) {
                    this.scrollOffset++;
                    this.rebuildWidgets();
                }
            }).bounds(centerX + 160, startY + 64, 20, 20).build();
            this.addRenderableWidget(downBtn);
        }

        // 6. Nút Hoàn tất & Lưu
        Button doneBtn = Button.builder(Component.literal("Hoàn tất & Lưu"), btn -> this.onClose())
            .bounds(centerX - 100, this.height - 26, 200, 20)
            .build();
        this.addRenderableWidget(doneBtn);
    }

    private Component getEnabledText() {
        return Component.literal("Trạng thái: " + (AutoLoginConfig.isEnabled() ? "§aBẬT" : "§cTẮT"));
    }

    private Component getShowPassText() {
        return Component.literal(this.showPasswords ? "§eẨn Pass" : "§bHiện Pass");
    }

    private String maskPassword(String raw) {
        if (raw == null || raw.isEmpty()) return "§8(Trống)";
        if (this.showPasswords) return "§a" + raw;
        return "§7" + "*".repeat(Math.min(12, raw.length()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Tiêu đề chính (ARGB format bắt buộc cho 26.2: 0xFFFFFFFF)
        int titleWidth = this.font.width(this.title);
        graphics.text(this.font, this.title, centerX - (titleWidth / 2), 10, 0xFFFFFFFF, true);

        // Hiển thị số Delay Ticks hiện tại
        String delayLabel = String.format("Độ trễ: §e%d ticks §7(%.2fs)", AutoLoginConfig.getDelayTicks(), AutoLoginConfig.getDelayTicks() * 0.05f);
        graphics.text(this.font, Component.literal(delayLabel), centerX + 5, 17, 0xFFCCCCCC, true);

        // Nhãn Mật khẩu mặc định
        graphics.text(
            this.font,
            Component.literal("Mật khẩu mặc định (dùng chung mọi server): " + maskPassword(AutoLoginConfig.getDefaultPassword())),
            centerX - 155, 54, 0xFFFFFFFF, true
        );

        // Nhãn Thêm mật khẩu riêng theo IP
        graphics.text(
            this.font,
            Component.literal("Thêm / Cập nhật mật khẩu riêng (Nhập IP Server & Mật khẩu):"),
            centerX - 155, 94, 0xFFFFFFFF, true
        );

        // Nhãn Danh sách Server đã lưu
        List<Map.Entry<String, String>> entries = new ArrayList<>(AutoLoginConfig.getAllPasswords().entrySet());
        graphics.text(
            this.font,
            Component.literal("Danh sách Server đã lưu (" + entries.size() + "):"),
            centerX - 155, 134, 0xFFFFFF55, true
        );

        int startY = 148;
        if (entries.isEmpty()) {
            graphics.text(
                this.font,
                Component.literal("§7Chưa có server nào được lưu riêng. Đang dùng mật khẩu mặc định."),
                centerX - 155, startY + 6, 0xFF888888, true
            );
        } else {
            for (int i = 0; i < MAX_VISIBLE_ROWS; i++) {
                int index = i + this.scrollOffset;
                if (index >= entries.size()) break;

                Map.Entry<String, String> entry = entries.get(index);
                int rowY = startY + (i * 22) + 5;
                String line = "§f" + entry.getKey() + " §7→ " + maskPassword(entry.getValue());
                graphics.text(this.font, Component.literal(line), centerX - 155, rowY, 0xFFFFFFFF, true);
            }
        }
    }

    @Override
    public void onClose() {
        AutoLoginConfig.save();
        if (this.minecraft != null) {
            com.autologin.util.ScreenHelper.setScreen(this.minecraft, this.parent);
        }
    }
}

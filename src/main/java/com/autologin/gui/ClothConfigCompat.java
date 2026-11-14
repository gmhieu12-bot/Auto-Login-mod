package com.autologin.gui;

import com.autologin.config.AutoLoginConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class ClothConfigCompat {
    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Cấu hình Auto Login Dialog"));

        builder.setSavingRunnable(AutoLoginConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 1. Mục Cài đặt chung
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("Cài đặt chung"));

        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Bật / Tắt Auto Login"),
                AutoLoginConfig.isEnabled())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Tự động nhận diện Native Dialog đăng nhập ([ ĐĂNG NHẬP ]), tự điền mật khẩu và ấn nút."))
                .setSaveConsumer(AutoLoginConfig::setEnabled)
                .build());

        general.addEntry(entryBuilder.startIntSlider(
                Component.literal("Độ trễ phản hồi (ticks)"),
                AutoLoginConfig.getDelayTicks(),
                0,
                100)
                .setDefaultValue(10)
                .setTooltip(Component.literal("Số tick chờ trước khi tự động bấm nút Đăng nhập (20 ticks = 1 giây)."))
                .setSaveConsumer(AutoLoginConfig::setDelayTicks)
                .build());

        general.addEntry(entryBuilder.startStrField(
                Component.literal("Mật khẩu mặc định"),
                AutoLoginConfig.getDefaultPassword())
                .setDefaultValue("")
                .setTooltip(Component.literal("Mật khẩu dùng chung cho mọi server khi server đó chưa cài mật khẩu riêng."))
                .setSaveConsumer(AutoLoginConfig::setDefaultPassword)
                .build());

        // 2. Mục Mật khẩu Server riêng
        ConfigCategory serverCategory = builder.getOrCreateCategory(Component.literal("Mật khẩu Server"));

        Map<String, String> passwords = AutoLoginConfig.getAllPasswords();
        if (passwords.isEmpty()) {
            serverCategory.addEntry(entryBuilder.startTextDescription(
                    Component.literal("§7Chưa có server nào được đặt mật khẩu riêng. Bạn có thể dùng lệnh §e/autologin set <pass> §7khi vào server.")
            ).build());
        } else {
            for (Map.Entry<String, String> entry : passwords.entrySet()) {
                String ip = entry.getKey();
                String pass = entry.getValue();
                serverCategory.addEntry(entryBuilder.startStrField(Component.literal("Server: " + ip), pass)
                        .setDefaultValue(pass)
                        .setTooltip(Component.literal("Mật khẩu riêng cho " + ip + ". Xóa sạch chữ để bỏ lưu server này."))
                        .setSaveConsumer(newVal -> {
                            if (newVal == null || newVal.trim().isEmpty()) {
                                AutoLoginConfig.removePassword(ip);
                            } else {
                                AutoLoginConfig.setPassword(ip, newVal);
                            }
                        })
                        .build());
            }
        }

        return builder.build();
    }
}

package com.autologin.command;

import com.autologin.config.AutoLoginConfig;
import com.autologin.gui.AutoLoginConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public class AutoLoginCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("autologin")
            // /autologin -> Mở bảng GUI cài đặt (hỗ trợ Cloth Config nếu có, hoặc Custom GUI)
            .executes(context -> {
                Minecraft client = Minecraft.getInstance();
                Screen current = com.autologin.util.ScreenHelper.getScreen(client);
                client.execute(() -> {
                    Screen screen = null;
                    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config2")
                            || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) {
                        try {
                            screen = com.autologin.gui.ClothConfigCompat.createScreen(current);
                        } catch (Throwable ignored) {}
                    }
                    if (screen == null) {
                        screen = new AutoLoginConfigScreen(current);
                    }
                    com.autologin.util.ScreenHelper.setScreen(client, screen);
                });
                return 1;
            })
            // /autologin gui -> Mở trực tiếp Custom GUI
            .then(ClientCommands.literal("gui")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    Screen current = com.autologin.util.ScreenHelper.getScreen(client);
                    client.execute(() -> com.autologin.util.ScreenHelper.setScreen(client, new AutoLoginConfigScreen(current)));
                    return 1;
                })
            )
            // /autologin config -> Mở cấu hình Cloth Config nếu có
            .then(ClientCommands.literal("config")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    Screen current = com.autologin.util.ScreenHelper.getScreen(client);
                    client.execute(() -> {
                        Screen screen = null;
                        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config2")
                                || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("cloth-config")) {
                            try {
                                screen = com.autologin.gui.ClothConfigCompat.createScreen(current);
                            } catch (Throwable ignored) {}
                        }
                        if (screen == null) {
                            screen = new AutoLoginConfigScreen(current);
                        }
                        com.autologin.util.ScreenHelper.setScreen(client, screen);
                    });
                    return 1;
                })
            )
            // /autologin set <password>
            .then(ClientCommands.literal("set")
                .then(ClientCommands.argument("password", StringArgumentType.greedyString())
                    .executes(context -> {
                        String pass = StringArgumentType.getString(context, "password");
                        ServerData server = Minecraft.getInstance().getCurrentServer();
                        if (server == null) {
                            AutoLoginConfig.setDefaultPassword(pass);
                            context.getSource().sendFeedback(Component.literal("§a[AutoLogin] Đã lưu làm mật khẩu mặc định cho mọi server!"));
                            return 1;
                        }
                        AutoLoginConfig.setPassword(server.ip, pass);
                        context.getSource().sendFeedback(Component.literal("§a[AutoLogin] Đã lưu mật khẩu thành công cho server: §e" + server.ip));
                        return 1;
                    })
                )
            )
            // /autologin remove
            .then(ClientCommands.literal("remove")
                .executes(context -> {
                    ServerData server = Minecraft.getInstance().getCurrentServer();
                    if (server == null) {
                        context.getSource().sendFeedback(Component.literal("§cKhông xác định được server hiện tại!"));
                        return 0;
                    }
                    boolean removed = AutoLoginConfig.removePassword(server.ip);
                    if (removed) {
                        context.getSource().sendFeedback(Component.literal("§e[AutoLogin] Đã xóa mật khẩu của server: " + server.ip));
                    } else {
                        context.getSource().sendFeedback(Component.literal("§c[AutoLogin] Server này chưa có mật khẩu lưu riêng."));
                    }
                    return 1;
                })
            )
            // /autologin status
            .then(ClientCommands.literal("status")
                .executes(context -> {
                    ServerData server = Minecraft.getInstance().getCurrentServer();
                    String state = AutoLoginConfig.isEnabled() ? "§aBẬT" : "§cTẮT";
                    context.getSource().sendFeedback(Component.literal("§b[AutoLogin] Trạng thái: " + state + " §7| Độ trễ: §e" + AutoLoginConfig.getDelayTicks() + " ticks"));
                    if (server != null) {
                        String pass = AutoLoginConfig.getPassword(server.ip);
                        if (pass != null) {
                            context.getSource().sendFeedback(Component.literal("§a[AutoLogin] Server §e" + server.ip + " §ađã sẵn sàng mật khẩu tự động."));
                        } else {
                            context.getSource().sendFeedback(Component.literal("§c[AutoLogin] Server §e" + server.ip + " §cchưa được cài mật khẩu."));
                        }
                    }
                    return 1;
                })
            )
        );
    }
}

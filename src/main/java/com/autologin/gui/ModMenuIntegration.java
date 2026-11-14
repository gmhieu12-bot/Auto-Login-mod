package com.autologin.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            if (FabricLoader.getInstance().isModLoaded("cloth-config2")
                    || FabricLoader.getInstance().isModLoaded("cloth-config")) {
                try {
                    return ClothConfigCompat.createScreen(parent);
                } catch (Throwable ignored) {
                    // Fallback to built-in screen if Cloth Config fails
                }
            }
            return new AutoLoginConfigScreen(parent);
        };
    }
}

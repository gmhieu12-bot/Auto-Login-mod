package com.autologin;

import com.autologin.command.AutoLoginCommand;
import com.autologin.config.AutoLoginConfig;
import com.autologin.handler.AutoLoginHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class AutoLoginMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoLoginConfig.load();
        AutoLoginHandler.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AutoLoginCommand.register(dispatcher);
        });
    }
}

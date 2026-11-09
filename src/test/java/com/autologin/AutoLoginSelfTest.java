package com.autologin;

import com.autologin.config.AutoLoginConfig;
import com.autologin.handler.AutoLoginHandler;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AutoLoginSelfTest {

    @BeforeAll
    public static void setupMinecraftBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void testConfigPersistenceAndFallback(@TempDir Path tempDir) {
        File testConfig = tempDir.resolve("autologin_test.json").toFile();
        AutoLoginConfig.setCustomConfigFileForTest(testConfig);

        AutoLoginConfig.setDefaultPassword("GlobalPass123");
        AutoLoginConfig.setPassword("Play.ServerVN.Net:25565", "SecretPass999");

        // Reload from disk to verify JSON serialization
        AutoLoginConfig.load();

        assertEquals("SecretPass999", AutoLoginConfig.getPassword("play.servervn.net:25565"),
            "Must retrieve exact server password regardless of uppercase/lowercase IP");
        assertEquals("GlobalPass123", AutoLoginConfig.getPassword("other.server.com"),
            "Must fall back to defaultPassword when server IP is not in the map");
    }

    @Test
    public void testVietnameseTitleAndButtonRecognition() {
        assertTrue(AutoLoginHandler.isLoginTitle("✦ | ĐĂNG NHẬP SƯ PHỤ"),
            "Must recognize Vietnamese dialog title '✦ | ĐĂNG NHẬP SƯ PHỤ'");
        assertTrue(AutoLoginHandler.isLoginTitle("Welcome Back!"),
            "Must recognize English dialog title 'Welcome Back!'");

        AtomicBoolean cancelClicked = new AtomicBoolean(false);
        AtomicBoolean loginClicked = new AtomicBoolean(false);

        Button cancelBtn = Button.builder(Component.literal("Hủy bỏ"), b -> cancelClicked.set(true)).bounds(0, 0, 80, 20).build();
        Button loginBtn = Button.builder(Component.literal("[ ĐĂNG NHẬP ]"), b -> loginClicked.set(true)).bounds(0, 0, 120, 20).build();

        List<Button> buttons = List.of(cancelBtn, loginBtn);
        Button selected = AutoLoginHandler.selectActionButton(buttons);

        assertSame(loginBtn, selected, "Must select '[ ĐĂNG NHẬP ]' button over cancel button");

        boolean triggered = AutoLoginHandler.triggerButtonPress(selected);
        assertTrue(triggered, "triggerButtonPress must return true");
        assertTrue(loginClicked.get(), "Button.OnPress callback for '[ ĐĂNG NHẬP ]' must be executed!");
        assertFalse(cancelClicked.get(), "Cancel button must NOT be clicked");
    }

    @Test
    public void testEndToEndNestedContainerSubmission() {
        AtomicBoolean submitted = new AtomicBoolean(false);

        // Build a nested ContainerEventHandler hierarchy mimicking DialogScreen -> ScrollableLayout$Container -> EditBox + Button
        EditBox passwordBox = new EditBox(null, 0, 0, 200, 20, Component.literal("🔑 Mật Khẩu"));
        Button signInBtn = Button.builder(Component.literal("[ ĐĂNG NHẬP ]"), b -> submitted.set(true)).bounds(0, 30, 200, 20).build();

        AbstractContainerEventHandler innerScrollContainer = new AbstractContainerEventHandler() {
            private final List<GuiEventListener> children = List.of(passwordBox);
            @Override
            public List<? extends GuiEventListener> children() {
                return children;
            }
        };

        AbstractContainerEventHandler mockDialogScreen = new AbstractContainerEventHandler() {
            private final List<GuiEventListener> children = new ArrayList<>(List.of(innerScrollContainer, signInBtn));
            @Override
            public List<? extends GuiEventListener> children() {
                return children;
            }
        };

        boolean result = AutoLoginHandler.submitToContainer(mockDialogScreen, "HaruPass2026");

        assertTrue(result, "submitToContainer must succeed");
        assertEquals("HaruPass2026", passwordBox.getValue(), "EditBox must be populated with the exact password");
        assertTrue(submitted.get(), "The '[ ĐĂNG NHẬP ]' button callback must be fired after filling the password");
    }
}

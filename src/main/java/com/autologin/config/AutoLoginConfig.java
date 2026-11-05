package com.autologin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoLoginConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile = null;

    public static class ConfigData {
        public boolean enabled = true;
        public int delayTicks = 6;
        public String defaultPassword = "";
        public Map<String, String> passwords = new LinkedHashMap<>();
    }

    public static ConfigData DATA = new ConfigData();

    public static synchronized File getConfigFile() {
        if (configFile == null) {
            try {
                configFile = new File(
                    FabricLoader.getInstance().getConfigDir().toFile(),
                    "autologin_dialog_26_2.json"
                );
            } catch (Throwable t) {
                configFile = new File("build/tmp/autologin_dialog_26_2_test.json");
                configFile.getParentFile().mkdirs();
            }
        }
        return configFile;
    }

    public static synchronized void setCustomConfigFileForTest(File customFile) {
        configFile = customFile;
    }

    public static synchronized void load() {
        File file = getConfigFile();
        if (!file.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                DATA = loaded;
                if (DATA.passwords == null) {
                    DATA.passwords = new LinkedHashMap<>();
                }
                if (DATA.defaultPassword == null) {
                    DATA.defaultPassword = "";
                }
                if (DATA.delayTicks < 1) {
                    DATA.delayTicks = 6;
                }
            }
        } catch (Exception e) {
            System.err.println("[AutoLogin] Failed to load config, resetting to defaults: " + e.getMessage());
        }
    }

    public static synchronized void save() {
        File file = getConfigFile();
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(DATA, writer);
        } catch (Exception e) {
            System.err.println("[AutoLogin] Failed to save config: " + e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return DATA.enabled;
    }

    public static void setEnabled(boolean enabled) {
        DATA.enabled = enabled;
        save();
    }

    public static int getDelayTicks() {
        return Math.max(1, Math.min(40, DATA.delayTicks));
    }

    public static void setDelayTicks(int ticks) {
        DATA.delayTicks = Math.max(1, Math.min(40, ticks));
        save();
    }

    public static String getDefaultPassword() {
        return DATA.defaultPassword != null ? DATA.defaultPassword : "";
    }

    public static void setDefaultPassword(String password) {
        DATA.defaultPassword = password != null ? password : "";
        save();
    }

    public static void setPassword(String serverIp, String password) {
        if (serverIp == null || serverIp.trim().isEmpty()) return;
        DATA.passwords.put(cleanIp(serverIp), password);
        save();
    }

    public static String getPassword(String serverIp) {
        if (serverIp != null && !serverIp.trim().isEmpty()) {
            String specific = DATA.passwords.get(cleanIp(serverIp));
            if (specific != null && !specific.isEmpty()) {
                return specific;
            }
        }
        if (DATA.defaultPassword != null && !DATA.defaultPassword.isEmpty()) {
            return DATA.defaultPassword;
        }
        return null;
    }

    public static boolean removePassword(String serverIp) {
        if (serverIp == null) return false;
        String removed = DATA.passwords.remove(cleanIp(serverIp));
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public static Map<String, String> getAllPasswords() {
        return DATA.passwords;
    }

    public static String cleanIp(String ip) {
        return ip.trim().toLowerCase();
    }
}

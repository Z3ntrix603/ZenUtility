package de.pvputility.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.pvputility.PvPUtilityMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class InvseeCounterConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "pvputility", "invsee.json");
    private static InvseeCounterConfig instance;

    private boolean enabled = true;
    private final Map<String, Boolean> trackedItems = new LinkedHashMap<>();
    private boolean allContainers = true;
    private int xOffset = 6;
    private int yOffset = 4;

    public InvseeCounterConfig() {
        initDefaults();
    }

    public static synchronized InvseeCounterConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private void initDefaults() {
        if (trackedItems.isEmpty()) {
            trackedItems.put("totem_of_undying", true);
            trackedItems.put("end_crystal", true);
            trackedItems.put("enchanted_golden_apple", true);
            trackedItems.put("ender_pearl", true);
            // Anchor and glowstone removed by default per user request!
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public Map<String, Boolean> getTrackedItems() {
        return trackedItems;
    }

    public void addTrackedItem(String itemKey) {
        trackedItems.put(itemKey.toLowerCase().trim(), true);
        save();
    }

    public void removeTrackedItem(String itemKey) {
        trackedItems.remove(itemKey.toLowerCase().trim());
        save();
    }

    public void toggleTrackedItem(String itemKey) {
        Boolean cur = trackedItems.get(itemKey.toLowerCase().trim());
        if (cur != null) {
            trackedItems.put(itemKey.toLowerCase().trim(), !cur);
        } else {
            trackedItems.put(itemKey.toLowerCase().trim(), true);
        }
        save();
    }

    public boolean isAllContainers() {
        return allContainers;
    }

    public void setAllContainers(boolean allContainers) {
        this.allContainers = allContainers;
        save();
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to save invsee config: {}", e.getMessage());
        }
    }

    public static InvseeCounterConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                InvseeCounterConfig cfg = GSON.fromJson(reader, InvseeCounterConfig.class);
                if (cfg != null) {
                    cfg.initDefaults();
                    return cfg;
                }
            } catch (Exception e) {
                PvPUtilityMod.LOGGER.error("Failed to load invsee config: {}", e.getMessage());
            }
        }
        InvseeCounterConfig cfg = new InvseeCounterConfig();
        cfg.save();
        return cfg;
    }
}

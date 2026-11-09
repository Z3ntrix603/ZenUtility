package de.pvputility.totem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.pvputility.PvPUtilityMod;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TotemPopColorManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "pvputility", "totempop.json");
    private static TotemPopColorManager instance;

    public static final Map<String, Integer> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("Default", -2); // Vanilla
        PRESETS.put("Cyan", 0x00E5FF);
        PRESETS.put("Pink", 0xFF2D78);
        PRESETS.put("Purple", 0x9D4EDD);
        PRESETS.put("Red", 0xFF1E27);
        PRESETS.put("Gold", 0xFFD700);
        PRESETS.put("Lime", 0x10B981);
        PRESETS.put("Ice Blue", 0x38BDF8);
        PRESETS.put("Pure White", 0xFFFFFF);
        PRESETS.put("Rainbow", -1);
    }

    private boolean enabled = true;
    private String currentPreset = "Cyan";
    private int customColorRgb = 0x00E5FF;

    public static synchronized TotemPopColorManager getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled && !"Default".equalsIgnoreCase(currentPreset);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public void setPreset(String presetName) {
        this.currentPreset = presetName;
        Integer rgb = PRESETS.get(presetName);
        if (rgb != null && rgb > 0) {
            this.customColorRgb = rgb;
        }
        save();
    }

    public void cyclePreset() {
        List<String> keys = new ArrayList<>(PRESETS.keySet());
        int curIdx = keys.indexOf(currentPreset);
        int nextIdx = (curIdx + 1) % keys.size();
        setPreset(keys.get(nextIdx));
    }

    public int getCustomColorRgb() {
        return customColorRgb;
    }

    public void setCustomColorRgb(int rgb) {
        this.customColorRgb = rgb & 0xFFFFFF;
        this.currentPreset = "Custom";
        save();
    }

    public float[] getCurrentColorRgbFloat() {
        int rgb;
        if ("Rainbow".equalsIgnoreCase(currentPreset)) {
            float hue = (System.currentTimeMillis() % 2500L) / 2500.0f;
            Color c = Color.getHSBColor(hue, 0.85f, 1.0f);
            rgb = (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        } else {
            rgb = customColorRgb;
        }

        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new float[]{r, g, b};
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to save totem pop config: {}", e.getMessage());
        }
    }

    public static TotemPopColorManager load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                TotemPopColorManager cfg = GSON.fromJson(reader, TotemPopColorManager.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                PvPUtilityMod.LOGGER.error("Failed to load totem pop config: {}", e.getMessage());
            }
        }
        TotemPopColorManager cfg = new TotemPopColorManager();
        cfg.save();
        return cfg;
    }
}

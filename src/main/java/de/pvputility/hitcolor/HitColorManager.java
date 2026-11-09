package de.pvputility.hitcolor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import de.pvputility.PvPUtilityMod;
import de.pvputility.mixin.OverlayTextureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

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

public class HitColorManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "pvputility", "hitcolor.json");
    private static HitColorManager instance;

    public static final Map<String, Integer> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("Vanilla Red", 0xFF0000);
        PRESETS.put("Cyan", 0x00E5FF);
        PRESETS.put("Pink", 0xFF2D78);
        PRESETS.put("Purple", 0x9D4EDD);
        PRESETS.put("Gold", 0xFFD700);
        PRESETS.put("Lime", 0x10B981);
        PRESETS.put("Ice Blue", 0x38BDF8);
        PRESETS.put("Pure White", 0xFFFFFF);
        PRESETS.put("Rainbow", -1);
    }

    private boolean enabled = true;
    private String currentPreset = "Cyan";
    private int customColorRgb = 0x00E5FF;
    private int alpha = 178; // Vanilla opacity ~70% (0xB2)

    public static synchronized HitColorManager getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
        apply();
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
        if (rgb != null && rgb != -1) {
            this.customColorRgb = rgb;
        }
        save();
        apply();
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
        apply();
    }

    public int getEffectiveColorRgb() {
        if ("Rainbow".equalsIgnoreCase(currentPreset)) {
            float hue = (System.currentTimeMillis() % 2500L) / 2500.0f;
            Color c = Color.getHSBColor(hue, 0.9f, 1.0f);
            return (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        }
        return customColorRgb;
    }

    public void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        OverlayTexture ot = mc.gameRenderer.overlayTexture();
        applyToOverlayTexture(ot);
    }

    public void applyToOverlayTexture(OverlayTexture ot) {
        if (ot == null) return;
        try {
            DynamicTexture dt = ((OverlayTextureAccessor) ot).pu$getTexture();
            if (dt == null) return;
            NativeImage img = dt.getPixels();
            if (img == null) return;

            int targetColor;
            if (!enabled || "Vanilla Red".equalsIgnoreCase(currentPreset)) {
                targetColor = (0xB2 << 24) | 0xFF0000; // -1291911168 vanilla red
            } else {
                int rgb = getEffectiveColorRgb();
                targetColor = (alpha << 24) | (rgb & 0xFFFFFF);
            }

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setPixel(x, y, targetColor);
                }
            }

            dt.upload();
        } catch (Exception e) {
            PvPUtilityMod.LOGGER.error("Failed to update hit color overlay: {}", e.getMessage());
        }
    }

    public void tick() {
        if (enabled && "Rainbow".equalsIgnoreCase(currentPreset)) {
            apply();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to save hit color config: {}", e.getMessage());
        }
    }

    public static HitColorManager load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                HitColorManager cfg = GSON.fromJson(reader, HitColorManager.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                PvPUtilityMod.LOGGER.error("Failed to load hit color config: {}", e.getMessage());
            }
        }
        HitColorManager cfg = new HitColorManager();
        cfg.save();
        return cfg;
    }
}

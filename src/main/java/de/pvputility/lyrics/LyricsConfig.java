package de.pvputility.lyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class LyricsConfig {

    private static final File FILE = new File("config/pvputility/lyrics.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LyricsConfig instance;

    public static final Map<String, Integer> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("Pink", 0xFFFF66B2);
        PRESETS.put("Cyan", 0x00E5FF);
        PRESETS.put("Gold", 0xFFD700);
        PRESETS.put("Emerald", 0x50C878);
        PRESETS.put("Purple", 0xA855F7);
        PRESETS.put("White", 0xFFFFFF);
        PRESETS.put("Rainbow", -1);
    }

    private boolean enabled = true;
    private boolean inAirMode = true; // true = 3D floating in air ahead of player; false = 2D HUD
    private float inAirDistance = 4.0f; // meters/blocks in front
    private float inAirHeight = 0.5f; // vertical height offset
    private float textScale = 1.3f; // scale of 3D floating lyrics
    private boolean seeThrough = true; // whether 3D lyrics are visible through obstacles
    private boolean smoothInertia = true;
    private String activePreset = "Pink";
    private int customColorRgb = 0xFFFF66B2;
    private float syncOffsetSeconds = 0.0f;
    private boolean showTrackInfo = true;
    private boolean showProgressBar = true;
    private boolean showBackground = true;

    public static synchronized LyricsConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static LyricsConfig load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                LyricsConfig cfg = GSON.fromJson(reader, LyricsConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception ignored) {}
        }
        LyricsConfig def = new LyricsConfig();
        def.save();
        return def;
    }

    public void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {}
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }

    public boolean isInAirMode() { return inAirMode; }
    public void setInAirMode(boolean inAirMode) { this.inAirMode = inAirMode; save(); }

    public float getInAirDistance() { return inAirDistance; }
    public void setInAirDistance(float inAirDistance) {
        this.inAirDistance = Math.max(1.5f, Math.min(8.0f, inAirDistance));
        save();
    }

    public float getInAirHeight() { return inAirHeight; }
    public void setInAirHeight(float inAirHeight) {
        this.inAirHeight = Math.max(-1.0f, Math.min(3.0f, inAirHeight));
        save();
    }

    public float getTextScale() { return textScale; }
    public void setTextScale(float textScale) {
        this.textScale = Math.max(0.5f, Math.min(3.0f, textScale));
        save();
    }

    public boolean isSeeThrough() { return seeThrough; }
    public void setSeeThrough(boolean seeThrough) { this.seeThrough = seeThrough; save(); }

    public boolean isSmoothInertia() { return smoothInertia; }
    public void setSmoothInertia(boolean smoothInertia) { this.smoothInertia = smoothInertia; save(); }

    public String getActivePreset() { return activePreset; }
    public void setActivePreset(String preset) {
        this.activePreset = preset;
        if (PRESETS.containsKey(preset)) {
            int rgb = PRESETS.get(preset);
            if (rgb != -1) this.customColorRgb = rgb;
        }
        save();
    }

    public int getCustomColorRgb() { return customColorRgb; }
    public void setCustomColorRgb(int rgb) {
        this.customColorRgb = rgb;
        this.activePreset = "Custom";
        save();
    }

    public int getEffectiveColorRgb() {
        if ("Rainbow".equalsIgnoreCase(activePreset)) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
            return rgb & 0x00FFFFFF;
        }
        return customColorRgb;
    }

    public float getSyncOffsetSeconds() { return syncOffsetSeconds; }
    public void setSyncOffsetSeconds(float offset) {
        this.syncOffsetSeconds = Math.max(-10.0f, Math.min(10.0f, offset));
        save();
    }

    public boolean isShowTrackInfo() { return showTrackInfo; }
    public void setShowTrackInfo(boolean showTrackInfo) { this.showTrackInfo = showTrackInfo; save(); }

    public boolean isShowProgressBar() { return showProgressBar; }
    public void setShowProgressBar(boolean showProgressBar) { this.showProgressBar = showProgressBar; save(); }

    public boolean isShowBackground() { return showBackground; }
    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; save(); }
}

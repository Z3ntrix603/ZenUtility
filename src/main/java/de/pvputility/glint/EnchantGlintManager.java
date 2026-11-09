package de.pvputility.glint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.pvputility.PvPUtilityMod;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnchantGlintManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "pvputility", "glint.json");
    private static final String PACK_FOLDER = "PvPUtility_Active";
    private static final String PACK_ID = "file/" + PACK_FOLDER;

    private static EnchantGlintManager instance;

    public static final Map<String, Integer> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("Default", 0x8040CC);
        PRESETS.put("Red", 0xFF1E27);
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

    public static synchronized EnchantGlintManager getInstance() {
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
        applyGlintColor();
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
        applyGlintColor();
    }

    public int getCustomColorRgb() {
        return customColorRgb;
    }

    public void setCustomColorRgb(int rgb) {
        this.customColorRgb = rgb & 0xFFFFFF;
        this.currentPreset = "Custom";
        save();
        applyGlintColor();
    }

    public Path getPackDir() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return Paths.get("resourcepacks", PACK_FOLDER);
        Path rpDir = client.getResourcePackDirectory();
        if (rpDir == null) rpDir = Paths.get("resourcepacks");
        return rpDir.resolve(PACK_FOLDER);
    }

    private void initPackFolder() {
        try {
            Path dir = getPackDir();
            Files.createDirectories(dir);
            Path mcmeta = dir.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                String metaContent = "{\n  \"pack\": {\n    \"pack_format\": 34,\n    \"description\": \"PvP Utility Custom Glint\"\n  }\n}";
                Files.writeString(mcmeta, metaContent, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to init glint pack: {}", e.getMessage());
        }
    }

    public void applyGlintColor() {
        try {
            initPackFolder();
            Path packDir = getPackDir();
            Path miscDir = packDir.resolve("assets").resolve("minecraft").resolve("textures").resolve("misc");
            Files.createDirectories(miscDir);

            Path itemGlint = miscDir.resolve("enchanted_glint_item.png");
            Path armorGlint = miscDir.resolve("enchanted_glint_armor.png");
            Path itemMcmeta = miscDir.resolve("enchanted_glint_item.png.mcmeta");
            Path armorMcmeta = miscDir.resolve("enchanted_glint_armor.png.mcmeta");

            if (!enabled || "Default".equalsIgnoreCase(currentPreset)) {
                // Remove overrides so Minecraft uses default vanilla glint directly
                Files.deleteIfExists(itemGlint);
                Files.deleteIfExists(armorGlint);
                Files.deleteIfExists(itemMcmeta);
                Files.deleteIfExists(armorMcmeta);
            } else if ("Rainbow".equalsIgnoreCase(currentPreset)) {
                byte[] rainbowPng = generateVanillaRainbowGlintPng();
                if (rainbowPng.length > 0) {
                    Files.write(itemGlint, rainbowPng);
                    Files.write(armorGlint, rainbowPng);

                    String animMcmeta = "{\"animation\":{\"frametime\":2,\"interpolate\":true},\"texture\":{\"blur\":true}}";
                    Files.writeString(itemMcmeta, animMcmeta, StandardCharsets.UTF_8);
                    Files.writeString(armorMcmeta, animMcmeta, StandardCharsets.UTF_8);
                }
            } else {
                byte[] coloredPng = generateVanillaTintedGlintPng(customColorRgb);
                if (coloredPng.length > 0) {
                    Files.write(itemGlint, coloredPng);
                    Files.write(armorGlint, coloredPng);

                    String staticMcmeta = "{\"texture\":{\"blur\":true}}";
                    Files.writeString(itemMcmeta, staticMcmeta, StandardCharsets.UTF_8);
                    Files.writeString(armorMcmeta, staticMcmeta, StandardCharsets.UTF_8);
                }
            }

            ensurePackEnabledAndReload();
        } catch (Exception e) {
            PvPUtilityMod.LOGGER.error("Failed to apply glint color: {}", e.getMessage());
        }
    }

    private static BufferedImage loadVanillaGlint() {
        try (InputStream in = EnchantGlintManager.class.getResourceAsStream("/assets/pvputility/textures/vanilla_glint.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static byte[] generateVanillaTintedGlintPng(int targetRgb) {
        BufferedImage base = loadVanillaGlint();
        if (base == null) return new byte[0];

        int w = base.getWidth();
        int h = base.getHeight();
        BufferedImage tinted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int tr = (targetRgb >> 16) & 0xFF;
        int tg = (targetRgb >> 8) & 0xFF;
        int tb = targetRgb & 0xFF;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = base.getRGB(x, y);
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // Relative brightness in vanilla glint texture
                float lum = (r * 0.299f + g * 0.587f + b * 0.114f) / 125.0f;
                int nr = Math.min(255, Math.round(tr * lum));
                int ng = Math.min(255, Math.round(tg * lum));
                int nb = Math.min(255, Math.round(tb * lum));

                tinted.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }

        return toByteArray(tinted);
    }

    public static byte[] generateVanillaRainbowGlintPng() {
        BufferedImage base = loadVanillaGlint();
        if (base == null) return new byte[0];

        int w = base.getWidth();
        int h = base.getHeight();
        int frames = 16;
        BufferedImage rainbow = new BufferedImage(w, h * frames, BufferedImage.TYPE_INT_ARGB);

        for (int f = 0; f < frames; f++) {
            float hue = (float) f / (float) frames;
            Color color = Color.getHSBColor(hue, 0.85f, 1.0f);
            int tr = color.getRed();
            int tg = color.getGreen();
            int tb = color.getBlue();
            int yOffset = f * h;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = base.getRGB(x, y);
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    float lum = (r * 0.299f + g * 0.587f + b * 0.114f) / 125.0f;
                    int nr = Math.min(255, Math.round(tr * lum));
                    int ng = Math.min(255, Math.round(tg * lum));
                    int nb = Math.min(255, Math.round(tb * lum));

                    rainbow.setRGB(x, yOffset + y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                }
            }
        }

        return toByteArray(rainbow);
    }

    private static byte[] toByteArray(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private void ensurePackEnabledAndReload() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        PackRepository repo = client.getResourcePackRepository();
        if (repo != null) {
            repo.reload();
            List<String> selected = new ArrayList<>(repo.getSelectedIds());
            if (!selected.contains(PACK_ID)) {
                selected.add(PACK_ID);
                repo.setSelected(selected);
            }
        }

        client.execute(client::reloadResourcePacks);
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to save glint config: {}", e.getMessage());
        }
    }

    public static EnchantGlintManager load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                EnchantGlintManager cfg = GSON.fromJson(reader, EnchantGlintManager.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                PvPUtilityMod.LOGGER.error("Failed to load glint config: {}", e.getMessage());
            }
        }
        EnchantGlintManager cfg = new EnchantGlintManager();
        cfg.save();
        return cfg;
    }
}

package de.pvputility.scale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.pvputility.PvPUtilityMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class ItemScaleConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "pvputility", "itemscale.json");
    private static ItemScaleConfig instance;

    private boolean enabled = true;
    private final Map<String, Float> itemScales = new LinkedHashMap<>();

    public ItemScaleConfig() {
        // Standardmäßig leer per user request!
    }

    public static synchronized ItemScaleConfig getInstance() {
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
    }

    public Map<String, Float> getItemScales() {
        return itemScales;
    }

    public float getScaleForItem(ItemStack stack) {
        if (!enabled || stack == null || stack.isEmpty()) {
            return 1.0f;
        }

        Item item = stack.getItem();
        String key = BuiltInRegistries.ITEM.getKey(item).getPath();

        Float customScale = itemScales.get(key);
        if (customScale != null && customScale > 0.1f) {
            return customScale;
        }

        return 1.0f;
    }

    public void setScaleForItem(String itemKey, float scale) {
        scale = Math.max(1.0f, Math.min(5.0f, Math.round(scale * 10.0f) / 10.0f));
        itemScales.put(itemKey.toLowerCase().trim(), scale);
        save();
    }

    public void removeScaleForItem(String itemKey) {
        itemScales.remove(itemKey.toLowerCase().trim());
        save();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to save item scale config: {}", e.getMessage());
        }
    }

    public static ItemScaleConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ItemScaleConfig cfg = GSON.fromJson(reader, ItemScaleConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (Exception e) {
                PvPUtilityMod.LOGGER.error("Failed to load item scale config: {}", e.getMessage());
            }
        }
        ItemScaleConfig cfg = new ItemScaleConfig();
        cfg.save();
        return cfg;
    }
}

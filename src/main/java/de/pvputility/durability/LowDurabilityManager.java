package de.pvputility.durability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.pvputility.PvPUtilityMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LowDurabilityManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "pvputility", "durability.json");
    private static LowDurabilityManager instance;

    private boolean enabled = true;
    private int threshold = 10;
    private boolean blockMining = true;
    private boolean playSound = true;
    private boolean showMessage = true;
    private boolean titleWarning = true;

    private transient long lastAlertTime = 0;
    private transient int passiveTickCounter = 0;
    private transient Item lastItem = null;
    private transient int lastDamage = -1;

    public static synchronized LowDurabilityManager getInstance() {
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

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = Math.max(1, Math.min(threshold, 500));
        save();
    }

    public boolean isBlockMining() {
        return blockMining;
    }

    public void setBlockMining(boolean blockMining) {
        this.blockMining = blockMining;
        save();
    }

    public void toggleBlockMining() {
        setBlockMining(!blockMining);
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
        save();
    }

    public void togglePlaySound() {
        setPlaySound(!playSound);
    }

    public boolean isShowMessage() {
        return showMessage;
    }

    public void setShowMessage(boolean showMessage) {
        this.showMessage = showMessage;
        save();
    }

    public void toggleShowMessage() {
        setShowMessage(!showMessage);
    }

    public boolean isTitleWarning() {
        return titleWarning;
    }

    public void setTitleWarning(boolean titleWarning) {
        this.titleWarning = titleWarning;
        save();
    }

    public void toggleTitleWarning() {
        setTitleWarning(!titleWarning);
    }

    public int getRemainingDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return -1;
        }
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    public boolean isHoldingLowDurabilityTool() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        if (mc.player.isCreative() || mc.player.isSpectator()) return false;

        ItemStack stack = mc.player.getMainHandItem();
        int remaining = getRemainingDurability(stack);
        return remaining >= 0 && remaining <= threshold;
    }

    public void sendOverlayMessage(Component component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.hud != null) {
            mc.gui.hud.setOverlayMessage(component, false);
        }
    }

    public void showTitles(Component title, Component subtitle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.hud != null) {
            mc.gui.hud.setTitle(title);
            mc.gui.hud.setSubtitle(subtitle);
        }
    }

    public void triggerMiningBlockedWarning() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        int remaining = getRemainingDurability(stack);
        String itemName = stack.getHoverName().getString();

        long now = System.currentTimeMillis();
        if (now - lastAlertTime > 400) {
            lastAlertTime = now;

            if (playSound) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 1.0f));
            }

            if (showMessage) {
                sendOverlayMessage(Component.literal("§c§l⛔ ABBAUEN BLOCKIERT! §f" + itemName + " §chat nur noch §e" + remaining + " Dura!"));
            }

            if (titleWarning) {
                showTitles(
                        Component.literal("§c§l⚠ HALTBARKEIT KRITISCH!"),
                        Component.literal("§e" + itemName + " §7(§e" + remaining + " Dura übrig§7) §c- Abbauen gestoppt!")
                );
            }
        }
    }

    public void tick(Minecraft mc) {
        if (!enabled || mc == null || mc.player == null) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;

        ItemStack current = mc.player.getMainHandItem();
        if (isHoldingLowDurabilityTool()) {
            passiveTickCounter++;
            Item curItem = current.getItem();
            int curDamage = current.getDamageValue();
            boolean itemChanged = (curItem != lastItem || Math.abs(curDamage - lastDamage) >= 2);
            if (itemChanged || passiveTickCounter >= 80) {
                passiveTickCounter = 0;
                lastItem = curItem;
                lastDamage = curDamage;
                int remaining = getRemainingDurability(current);
                String itemName = current.getHoverName().getString();
                if (showMessage) {
                    sendOverlayMessage(Component.literal("§6§l⚠ HALTBARKEIT NIEDRIG: §f" + itemName + " §7(§e" + remaining + "§7 Dura)"));
                }
            }
        } else {
            passiveTickCounter = 0;
            lastItem = null;
            lastDamage = -1;
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PvPUtilityMod.LOGGER.error("Failed to save durability config: {}", e.getMessage());
        }
    }

    public static LowDurabilityManager load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                LowDurabilityManager cfg = GSON.fromJson(reader, LowDurabilityManager.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                PvPUtilityMod.LOGGER.error("Failed to load durability config: {}", e.getMessage());
            }
        }
        LowDurabilityManager cfg = new LowDurabilityManager();
        cfg.save();
        return cfg;
    }
}

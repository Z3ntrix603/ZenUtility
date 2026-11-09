package de.pvputility.hud;

import de.pvputility.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public class InvseeCounterHud {

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                // Don't render on local survival inventory screen (E key)
                if (screen instanceof InventoryScreen) {
                    return;
                }

                ScreenEvents.afterExtract(containerScreen).register((scr, extractor, mouseX, mouseY, delta) -> {
                    renderHud(containerScreen, extractor);
                });
            }
        });
    }

    private static void renderHud(AbstractContainerScreen<?> screen, GuiGraphicsExtractor extractor) {
        InvseeCounterConfig cfg = InvseeCounterConfig.getInstance();
        if (!cfg.isEnabled()) return;

        Map<String, Integer> activeCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : cfg.getTrackedItems().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                activeCounts.put(entry.getKey(), 0);
            }
        }

        if (activeCounts.isEmpty()) return;

        // Count items in container slots
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) continue;

            String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (activeCounts.containsKey(key)) {
                activeCounts.put(key, activeCounts.get(key) + stack.getCount());
            }
        }

        // Get container screen bounds
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.pu$getLeftPos();
        int topPos = accessor.pu$getTopPos();
        int imageWidth = accessor.pu$getImageWidth();

        int cardX = leftPos + imageWidth + cfg.getXOffset();
        int cardY = topPos + cfg.getYOffset();
        int cardW = 86;

        int rowCount = activeCounts.size();
        int cardH = 22 + rowCount * 18;

        // Clean Modern Obsidian Background
        drawRoundedBox(extractor, cardX, cardY, cardW, cardH, 0xF2090E18, 0xFF1E293B);

        // Header bar
        extractor.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 17, 0xFF0B1220);
        extractor.fill(cardX + 1, cardY + 17, cardX + cardW - 1, cardY + 18, 0xFF1E293B);

        // Header text
        Minecraft mc = Minecraft.getInstance();
        extractor.text(mc.font, "INV STATS", cardX + 8, cardY + 5, 0xFF38BDF8, true);

        // Rows
        int rowY = cardY + 21;
        for (Map.Entry<String, Integer> entry : activeCounts.entrySet()) {
            String itemKey = entry.getKey();
            int count = entry.getValue();

            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse("minecraft:" + itemKey));
            if (item != null) {
                extractor.fakeItem(item.getDefaultInstance(), cardX + 5, rowY);
            }

            String countText = "x" + count;
            int color = count > 0 ? 0xFFFFD700 : 0xFF64748B;
            extractor.text(mc.font, countText, cardX + 26, rowY + 4, color, true);
            rowY += 18;
        }
    }

    private static void drawRoundedBox(GuiGraphicsExtractor extractor, int x, int y, int w, int h, int bg, int border) {
        extractor.fill(x + 1, y, x + w - 1, y + h, bg);
        extractor.fill(x, y + 1, x + 1, y + h - 1, bg);
        extractor.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);

        extractor.fill(x + 1, y, x + w - 1, y + 1, border);
        extractor.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        extractor.fill(x, y + 1, x + 1, y + h - 1, border);
        extractor.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
    }
}

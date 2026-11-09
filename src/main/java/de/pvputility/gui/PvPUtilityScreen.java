package de.pvputility.gui;

import de.pvputility.durability.LowDurabilityManager;
import de.pvputility.glint.EnchantGlintManager;
import de.pvputility.hitcolor.HitColorManager;
import de.pvputility.hud.InvseeCounterConfig;
import de.pvputility.scale.ItemScaleConfig;
import de.pvputility.totem.TotemPopColorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class PvPUtilityScreen extends Screen {

    private static final int WIN_WIDTH = 384;
    private static final int WIN_HEIGHT = 224;
    private static final int SIDEBAR_WIDTH = 110;
    private static final int HEADER_HEIGHT = 24;

    // Obsidian Dark UI Colors
    private static final int COLOR_BACKDROP = 0x85000000;
    private static final int COLOR_WIN_BG = 0xF4090E18;
    private static final int COLOR_WIN_HEADER = 0xFF0B1220;
    private static final int COLOR_WIN_BORDER = 0xFF1E293B;

    private static final int COLOR_SIDEBAR_BG = 0xFF080D16;
    private static final int COLOR_SIDEBAR_ITEM = 0xFF0F172A;
    private static final int COLOR_SIDEBAR_SELECTED = 0xFF1E293B;
    private static final int COLOR_CYAN = 0xFF00E5FF;
    private static final int COLOR_ACCENT = 0xFF38BDF8;

    private static final int COLOR_SLOT_BG = 0xFF0D1424;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_GRAY = 0xFF94A3B8;
    private static final int COLOR_SUCCESS = 0xFF4ADE80;

    private enum Tab {
        GLINT("Enchant Glint"),
        HIT_COLOR("Hit Color"),
        TOTEM_POP("Totem Pop"),
        DURABILITY("Low Dura"),
        SCALE("Dropped Scale"),
        INVSEE("Invsee Stats");

        final String displayName;
        Tab(String displayName) { this.displayName = displayName; }
    }

    private Tab activeTab = Tab.GLINT;
    private String statusMessage = "Gespeichert.";

    // Glint tab
    private EditBox customHexBox;

    // Hit Color tab
    private EditBox hitHexBox;

    // Totem Pop tab
    private EditBox totemHexBox;

    // Scale tab
    private EditBox searchItemBox;
    private int scaleScroll = 0;

    // Invsee tab
    private EditBox searchInvseeBox;
    private int invseeScroll = 0;

    public PvPUtilityScreen() {
        super(Component.literal("PvP Utility"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildUI();
    }

    private void rebuildUI() {
        this.clearWidgets();

        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        // Close button '✕'
        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(winX + WIN_WIDTH - 18, winY + 4, 14, 14).build());

        int contentX = winX + SIDEBAR_WIDTH + 12;
        int contentY = winY + HEADER_HEIGHT + 6;
        int contentW = WIN_WIDTH - SIDEBAR_WIDTH - 20;

        if (activeTab == Tab.GLINT) {
            buildGlintUI(contentX, contentY, contentW);
        } else if (activeTab == Tab.HIT_COLOR) {
            buildHitColorUI(contentX, contentY, contentW);
        } else if (activeTab == Tab.TOTEM_POP) {
            buildTotemPopUI(contentX, contentY, contentW);
        } else if (activeTab == Tab.DURABILITY) {
            buildDurabilityUI(contentX, contentY, contentW);
        } else if (activeTab == Tab.SCALE) {
            buildScaleUI(contentX, contentY, contentW);
        } else if (activeTab == Tab.INVSEE) {
            buildInvseeUI(contentX, contentY, contentW);
        }
    }

    private void buildGlintUI(int contentX, int contentY, int contentW) {
        EnchantGlintManager glint = EnchantGlintManager.getInstance();

        String toggleText = "Glint: " + (glint.isEnabled() ? "§a[AKTIV]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
            glint.setEnabled(!glint.isEnabled());
            statusMessage = glint.isEnabled() ? "Glint aktiviert!" : "Glint deaktiviert.";
            rebuildUI();
        }).bounds(contentX, contentY + 16, 110, 16).build());

        int presetY = contentY + 46;
        int btnW = 76;
        int btnH = 15;
        int col = 0;
        int row = 0;

        for (Map.Entry<String, Integer> entry : EnchantGlintManager.PRESETS.entrySet()) {
            String name = entry.getKey();
            boolean isSel = name.equalsIgnoreCase(glint.getCurrentPreset());

            int bx = contentX + col * (btnW + 4);
            int by = presetY + row * (btnH + 4);

            String label = (isSel ? "§b▶ " : "") + name;
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                glint.setPreset(name);
                statusMessage = "Preset '" + name + "' angewendet!";
                rebuildUI();
            }).bounds(bx, by, btnW, btnH).build());

            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        int customY = presetY + (row + 1) * (btnH + 4) + 6;
        String curHex = String.format("#%06X", glint.getCustomColorRgb());
        customHexBox = new EditBox(this.font, contentX + 75, customY, 65, 15, Component.literal("Hex"));
        customHexBox.setValue(curHex);
        this.addRenderableWidget(customHexBox);

        this.addRenderableWidget(Button.builder(Component.literal("Setzen"), b -> {
            String txt = customHexBox.getValue().trim().replace("#", "");
            try {
                int rgb = Integer.parseInt(txt, 16);
                glint.setCustomColorRgb(rgb);
                statusMessage = "Custom Glint Farbe #" + txt + " gesetzt!";
                rebuildUI();
            } catch (Exception ignored) {
                statusMessage = "Ungültiger Hex-Code!";
            }
        }).bounds(contentX + 144, customY, 52, 15).build());
    }

    private void buildHitColorUI(int contentX, int contentY, int contentW) {
        HitColorManager hit = HitColorManager.getInstance();

        String toggleText = "Hit Color: " + (hit.isEnabled() ? "§a[AKTIV]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
            hit.toggle();
            statusMessage = hit.isEnabled() ? "Hit Color aktiviert!" : "Hit Color deaktiviert.";
            rebuildUI();
        }).bounds(contentX, contentY + 16, 120, 16).build());

        int presetY = contentY + 46;
        int btnW = 76;
        int btnH = 15;
        int col = 0;
        int row = 0;

        for (Map.Entry<String, Integer> entry : HitColorManager.PRESETS.entrySet()) {
            String name = entry.getKey();
            boolean isSel = name.equalsIgnoreCase(hit.getCurrentPreset());

            int bx = contentX + col * (btnW + 4);
            int by = presetY + row * (btnH + 4);

            String label = (isSel ? "§b▶ " : "") + name;
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                hit.setPreset(name);
                statusMessage = "Hit Color '" + name + "' gesetzt!";
                rebuildUI();
            }).bounds(bx, by, btnW, btnH).build());

            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        int customY = presetY + (row + 1) * (btnH + 4) + 6;
        String curHex = String.format("#%06X", hit.getCustomColorRgb());
        hitHexBox = new EditBox(this.font, contentX + 75, customY, 65, 15, Component.literal("Hex"));
        hitHexBox.setValue(curHex);
        this.addRenderableWidget(hitHexBox);

        this.addRenderableWidget(Button.builder(Component.literal("Setzen"), b -> {
            String txt = hitHexBox.getValue().trim().replace("#", "");
            try {
                int rgb = Integer.parseInt(txt, 16);
                hit.setCustomColorRgb(rgb);
                statusMessage = "Custom Hit Color #" + txt + " gesetzt!";
                rebuildUI();
            } catch (Exception ignored) {
                statusMessage = "Ungültiger Hex-Code!";
            }
        }).bounds(contentX + 144, customY, 52, 15).build());
    }

    private void buildTotemPopUI(int contentX, int contentY, int contentW) {
        TotemPopColorManager pop = TotemPopColorManager.getInstance();

        String toggleText = "Totem Pop: " + (pop.isEnabled() ? "§a[AKTIV]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
            pop.toggle();
            statusMessage = pop.isEnabled() ? "Totem Pop aktiviert!" : "Totem Pop deaktiviert.";
            rebuildUI();
        }).bounds(contentX, contentY + 16, 120, 16).build());

        int presetY = contentY + 46;
        int btnW = 76;
        int btnH = 15;
        int col = 0;
        int row = 0;

        for (Map.Entry<String, Integer> entry : TotemPopColorManager.PRESETS.entrySet()) {
            String name = entry.getKey();
            boolean isSel = name.equalsIgnoreCase(pop.getCurrentPreset());

            int bx = contentX + col * (btnW + 4);
            int by = presetY + row * (btnH + 4);

            String label = (isSel ? "§b▶ " : "") + name;
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                pop.setPreset(name);
                statusMessage = "Totem Pop '" + name + "' gesetzt!";
                rebuildUI();
            }).bounds(bx, by, btnW, btnH).build());

            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        int customY = presetY + (row + 1) * (btnH + 4) + 6;
        String curHex = String.format("#%06X", pop.getCustomColorRgb());
        totemHexBox = new EditBox(this.font, contentX + 75, customY, 65, 15, Component.literal("Hex"));
        totemHexBox.setValue(curHex);
        this.addRenderableWidget(totemHexBox);

        this.addRenderableWidget(Button.builder(Component.literal("Setzen"), b -> {
            String txt = totemHexBox.getValue().trim().replace("#", "");
            try {
                int rgb = Integer.parseInt(txt, 16);
                pop.setCustomColorRgb(rgb);
                statusMessage = "Custom Totem Pop #" + txt + " gesetzt!";
                rebuildUI();
            } catch (Exception ignored) {
                statusMessage = "Ungültiger Hex-Code!";
            }
        }).bounds(contentX + 144, customY, 52, 15).build());
    }

    private void buildDurabilityUI(int contentX, int contentY, int contentW) {
        LowDurabilityManager dura = LowDurabilityManager.getInstance();

        String toggleText = "Dura-Schutz: " + (dura.isEnabled() ? "§a[AKTIV]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
            dura.toggle();
            statusMessage = dura.isEnabled() ? "Dura-Schutz aktiviert!" : "Dura-Schutz deaktiviert.";
            rebuildUI();
        }).bounds(contentX, contentY + 16, 130, 16).build());

        // Threshold Presets
        int presetY = contentY + 48;
        int[] presets = {5, 10, 15, 25, 50};
        int btnW = 24;
        for (int i = 0; i < presets.length; i++) {
            int val = presets[i];
            boolean isSel = (dura.getThreshold() == val);
            String label = (isSel ? "§b" : "") + val;
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                dura.setThreshold(val);
                statusMessage = "Warnung ab " + val + " Haltbarkeit!";
                rebuildUI();
            }).bounds(contentX + i * (btnW + 2), presetY, btnW, 14).build());
        }

        // Stepper buttons (-5, -1, +1, +5)
        int stepY = presetY + 18;
        int stepW = 24;
        this.addRenderableWidget(Button.builder(Component.literal("-5"), b -> {
            dura.setThreshold(dura.getThreshold() - 5);
            rebuildUI();
        }).bounds(contentX, stepY, stepW, 14).build());

        this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> {
            dura.setThreshold(dura.getThreshold() - 1);
            rebuildUI();
        }).bounds(contentX + 26, stepY, stepW, 14).build());

        this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> {
            dura.setThreshold(dura.getThreshold() + 1);
            rebuildUI();
        }).bounds(contentX + 52, stepY, stepW, 14).build());

        this.addRenderableWidget(Button.builder(Component.literal("+5"), b -> {
            dura.setThreshold(dura.getThreshold() + 5);
            rebuildUI();
        }).bounds(contentX + 78, stepY, stepW, 14).build());

        // Option 1: Abbauen blockieren
        int optY1 = stepY + 22;
        String blockText = "Abbauen blockieren: " + (dura.isBlockMining() ? "§a[AN]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(blockText), b -> {
            dura.toggleBlockMining();
            statusMessage = dura.isBlockMining() ? "Abbauen wird blockiert!" : "Abbauen nicht blockiert.";
            rebuildUI();
        }).bounds(contentX, optY1, 140, 15).build());

        // Option 2: Warn-Sound
        int optY2 = optY1 + 18;
        String soundText = "Warn-Sound: " + (dura.isPlaySound() ? "§a[AN]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(soundText), b -> {
            dura.togglePlaySound();
            statusMessage = dura.isPlaySound() ? "Audio-Warnung an." : "Audio-Warnung aus.";
            rebuildUI();
        }).bounds(contentX, optY2, 140, 15).build());

        // Option 3: Titel-Warnung
        int optY3 = optY2 + 18;
        String titleText = "Titel-Warnung: " + (dura.isTitleWarning() ? "§a[AN]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(titleText), b -> {
            dura.toggleTitleWarning();
            statusMessage = dura.isTitleWarning() ? "Titel-Warnung an." : "Titel-Warnung aus.";
            rebuildUI();
        }).bounds(contentX, optY3, 140, 15).build());
    }

    private void buildScaleUI(int contentX, int contentY, int contentW) {
        ItemScaleConfig cfg = ItemScaleConfig.getInstance();

        String toggleText = "Item Scale: " + (cfg.isEnabled() ? "§a[AKTIV]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
            cfg.setEnabled(!cfg.isEnabled());
            statusMessage = cfg.isEnabled() ? "Item Scale aktiviert!" : "Item Scale deaktiviert.";
            rebuildUI();
        }).bounds(contentX, contentY + 16, 120, 16).build());

        int sy = contentY + 38;
        searchItemBox = new EditBox(this.font, contentX, sy, 165, 15, Component.literal("Item Suche"));
        this.addRenderableWidget(searchItemBox);

        this.addRenderableWidget(Button.builder(Component.literal("+ Item"), b -> {
            String query = searchItemBox.getValue().trim().toLowerCase().replace(" ", "_");
            if (!query.isEmpty()) {
                cfg.setScaleForItem(query, 3.0f);
                statusMessage = "Item '" + query + "' hinzugefügt (3.0x)";
                rebuildUI();
            }
        }).bounds(contentX + 170, sy, 65, 15).build());

        int listY = sy + 22;
        List<Map.Entry<String, Float>> entries = new ArrayList<>(cfg.getItemScales().entrySet());
        int visibleCount = 5;

        for (int i = 0; i < Math.min(visibleCount, entries.size() - scaleScroll); i++) {
            int idx = i + scaleScroll;
            if (idx >= entries.size()) break;

            Map.Entry<String, Float> e = entries.get(idx);
            String itemKey = e.getKey();
            float scale = e.getValue();
            int iy = listY + i * 20;

            this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                float n = Math.max(1.0f, scale - 0.5f);
                cfg.setScaleForItem(itemKey, n);
                rebuildUI();
            }).bounds(contentX + 130, iy, 14, 14).build());

            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                float n = Math.min(5.0f, scale + 0.5f);
                cfg.setScaleForItem(itemKey, n);
                rebuildUI();
            }).bounds(contentX + 178, iy, 14, 14).build());

            this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                cfg.removeScaleForItem(itemKey);
                rebuildUI();
            }).bounds(contentX + 196, iy, 14, 14).build());
        }
    }

    private void buildInvseeUI(int contentX, int contentY, int contentW) {
        InvseeCounterConfig cfg = InvseeCounterConfig.getInstance();

        String toggleText = "Invsee HUD: " + (cfg.isEnabled() ? "§a[AKTIV]" : "§c[AUS]");
        this.addRenderableWidget(Button.builder(Component.literal(toggleText), b -> {
            cfg.setEnabled(!cfg.isEnabled());
            statusMessage = cfg.isEnabled() ? "Invsee HUD aktiviert!" : "Invsee HUD deaktiviert.";
            rebuildUI();
        }).bounds(contentX, contentY + 16, 110, 16).build());

        int sy = contentY + 38;
        searchInvseeBox = new EditBox(this.font, contentX, sy, 80, 15, Component.literal("Item Suche"));
        this.addRenderableWidget(searchInvseeBox);

        this.addRenderableWidget(Button.builder(Component.literal("+ Item"), b -> {
            String query = searchInvseeBox.getValue().trim().toLowerCase().replace(" ", "_");
            if (!query.isEmpty()) {
                cfg.addTrackedItem(query);
                statusMessage = "Item '" + query + "' zum Counter hinzugefügt!";
                rebuildUI();
            }
        }).bounds(contentX + 84, sy, 50, 15).build());

        int listY = sy + 22;
        List<Map.Entry<String, Boolean>> entries = new ArrayList<>(cfg.getTrackedItems().entrySet());
        int visibleCount = 5;

        for (int i = 0; i < Math.min(visibleCount, entries.size() - invseeScroll); i++) {
            int idx = i + invseeScroll;
            if (idx >= entries.size()) break;

            Map.Entry<String, Boolean> e = entries.get(idx);
            String itemKey = e.getKey();
            boolean isTracked = Boolean.TRUE.equals(e.getValue());
            int iy = listY + i * 20;

            this.addRenderableWidget(Button.builder(Component.literal(isTracked ? "§aAN" : "§cAUS"), b -> {
                cfg.toggleTrackedItem(itemKey);
                rebuildUI();
            }).bounds(contentX + 90, iy, 32, 14).build());

            this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                cfg.removeTrackedItem(itemKey);
                rebuildUI();
            }).bounds(contentX + 125, iy, 14, 14).build());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab == Tab.SCALE) {
            int total = ItemScaleConfig.getInstance().getItemScales().size();
            int maxScroll = Math.max(0, total - 5);
            if (verticalAmount > 0 && scaleScroll > 0) {
                scaleScroll--;
                rebuildUI();
                return true;
            } else if (verticalAmount < 0 && scaleScroll < maxScroll) {
                scaleScroll++;
                rebuildUI();
                return true;
            }
        } else if (activeTab == Tab.INVSEE) {
            int total = InvseeCounterConfig.getInstance().getTrackedItems().size();
            int maxScroll = Math.max(0, total - 5);
            if (verticalAmount > 0 && invseeScroll > 0) {
                invseeScroll--;
                rebuildUI();
                return true;
            } else if (verticalAmount < 0 && invseeScroll < maxScroll) {
                invseeScroll++;
                rebuildUI();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractor.fill(0, 0, this.width, this.height, COLOR_BACKDROP);

        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        drawRoundedBox(extractor, winX, winY, WIN_WIDTH, WIN_HEIGHT, COLOR_WIN_BG, COLOR_WIN_BORDER);

        extractor.fill(winX + 2, winY + 2, winX + WIN_WIDTH - 2, winY + HEADER_HEIGHT, COLOR_WIN_HEADER);
        extractor.fill(winX + 2, winY + HEADER_HEIGHT, winX + WIN_WIDTH - 2, winY + HEADER_HEIGHT + 1, COLOR_WIN_BORDER);
        extractor.text(this.font, "PvP Utility", winX + 10, winY + 7, COLOR_TEXT_WHITE, true);

        // Sidebar Background
        int sidebarY = winY + HEADER_HEIGHT + 1;
        extractor.fill(winX + 2, sidebarY, winX + SIDEBAR_WIDTH, winY + WIN_HEIGHT - 2, COLOR_SIDEBAR_BG);
        extractor.fill(winX + SIDEBAR_WIDTH, sidebarY, winX + SIDEBAR_WIDTH + 1, winY + WIN_HEIGHT - 2, COLOR_WIN_BORDER);

        // Sidebar Tabs
        Tab[] tabs = Tab.values();
        int tabH = 19;
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int ty = sidebarY + 4 + i * (tabH + 3);
            boolean isSel = (t == activeTab);
            boolean isHov = mouseX >= winX + 4 && mouseX < winX + SIDEBAR_WIDTH - 4 && mouseY >= ty && mouseY < ty + tabH;

            int bg = isSel ? COLOR_SIDEBAR_SELECTED : (isHov ? COLOR_SIDEBAR_ITEM : COLOR_SIDEBAR_BG);
            drawRoundedBox(extractor, winX + 4, ty, SIDEBAR_WIDTH - 8, tabH, bg, isSel ? COLOR_ACCENT : COLOR_WIN_BORDER);

            int textColor = isSel ? COLOR_TEXT_WHITE : (isHov ? COLOR_CYAN : COLOR_TEXT_GRAY);
            extractor.text(this.font, t.displayName, winX + 8, ty + 5, textColor, isSel);
        }

        int contentX = winX + SIDEBAR_WIDTH + 12;
        int contentY = winY + HEADER_HEIGHT + 6;
        extractor.text(this.font, activeTab.displayName, contentX, contentY + 2, COLOR_TEXT_WHITE, true);

        if (activeTab == Tab.GLINT) {
            renderGlintContent(extractor, contentX, contentY);
        } else if (activeTab == Tab.HIT_COLOR) {
            renderHitColorContent(extractor, contentX, contentY);
        } else if (activeTab == Tab.TOTEM_POP) {
            renderTotemPopContent(extractor, contentX, contentY);
        } else if (activeTab == Tab.DURABILITY) {
            renderDurabilityContent(extractor, contentX, contentY);
        } else if (activeTab == Tab.SCALE) {
            renderScaleContent(extractor, contentX, contentY);
        } else if (activeTab == Tab.INVSEE) {
            renderInvseeContent(extractor, contentX, contentY);
        }

        int statusY = winY + WIN_HEIGHT - 16;
        extractor.text(this.font, statusMessage, contentX, statusY, COLOR_SUCCESS, true);

        super.extractRenderState(extractor, mouseX, mouseY, delta);
    }

    private void renderGlintContent(GuiGraphicsExtractor extractor, int contentX, int contentY) {
        extractor.text(this.font, "1:1 Vanilla-Glint Farbfilter:", contentX, contentY + 35, COLOR_TEXT_GRAY, false);

        int customY = contentY + 114;
        extractor.text(this.font, "Custom HEX:", contentX, customY + 3, COLOR_TEXT_WHITE, false);

        EnchantGlintManager glint = EnchantGlintManager.getInstance();
        int previewX = contentX + 160;
        int previewY = contentY + 10;
        int previewSize = 36;
        drawRoundedBox(extractor, previewX, previewY, previewSize, previewSize, COLOR_SLOT_BG, COLOR_ACCENT);

        ItemStack sword = Items.NETHERITE_SWORD.getDefaultInstance();
        extractor.fakeItem(sword, previewX + 10, previewY + 10);

        int colorRgb = glint.getCustomColorRgb();
        int colorPreview = 0xFF000000 | colorRgb;
        extractor.fill(contentX + 200, customY, contentX + 215, customY + 15, colorPreview);
        drawRoundedBox(extractor, contentX + 200, customY, 15, 15, 0x00000000, COLOR_TEXT_WHITE);
    }

    private void renderHitColorContent(GuiGraphicsExtractor extractor, int contentX, int contentY) {
        extractor.text(this.font, "Schadens-Flash Farbe (Damage Indicator):", contentX, contentY + 35, COLOR_TEXT_GRAY, false);

        int customY = contentY + 114;
        extractor.text(this.font, "Custom HEX:", contentX, customY + 3, COLOR_TEXT_WHITE, false);

        HitColorManager hit = HitColorManager.getInstance();
        int previewX = contentX + 160;
        int previewY = contentY + 10;
        int previewSize = 36;
        int hitColor = 0xFF000000 | hit.getEffectiveColorRgb();

        drawRoundedBox(extractor, previewX, previewY, previewSize, previewSize, COLOR_SLOT_BG, hitColor);
        extractor.fill(previewX + 4, previewY + 4, previewX + previewSize - 4, previewY + previewSize - 4, (0xB2 << 24) | (hitColor & 0xFFFFFF));
        extractor.text(this.font, "HIT!", previewX + 8, previewY + 14, COLOR_TEXT_WHITE, true);

        extractor.fill(contentX + 200, customY, contentX + 215, customY + 15, hitColor);
        drawRoundedBox(extractor, contentX + 200, customY, 15, 15, 0x00000000, COLOR_TEXT_WHITE);

        extractor.text(this.font, "Hotkey: [H] (in Steuerungs-Settings änderbar)", contentX, contentY + 155, COLOR_TEXT_GRAY, false);
    }

    private void renderTotemPopContent(GuiGraphicsExtractor extractor, int contentX, int contentY) {
        extractor.text(this.font, "Totem Pop Partikel- & Flash-Farbe:", contentX, contentY + 35, COLOR_TEXT_GRAY, false);

        int customY = contentY + 114;
        extractor.text(this.font, "Custom HEX:", contentX, customY + 3, COLOR_TEXT_WHITE, false);

        TotemPopColorManager pop = TotemPopColorManager.getInstance();
        int previewX = contentX + 160;
        int previewY = contentY + 10;
        int previewSize = 36;
        float[] rgb = pop.getCurrentColorRgbFloat();
        int previewColor = 0xFF000000 | (((int)(rgb[0] * 255)) << 16) | (((int)(rgb[1] * 255)) << 8) | ((int)(rgb[2] * 255));

        drawRoundedBox(extractor, previewX, previewY, previewSize, previewSize, COLOR_SLOT_BG, previewColor);
        extractor.fakeItem(Items.TOTEM_OF_UNDYING.getDefaultInstance(), previewX + 10, previewY + 10);

        extractor.fill(contentX + 200, customY, contentX + 215, customY + 15, previewColor);
        drawRoundedBox(extractor, contentX + 200, customY, 15, 15, 0x00000000, COLOR_TEXT_WHITE);

        extractor.text(this.font, "Hotkey: [J] (in Steuerungs-Settings änderbar)", contentX, contentY + 155, COLOR_TEXT_GRAY, false);
    }

    private void renderDurabilityContent(GuiGraphicsExtractor extractor, int contentX, int contentY) {
        LowDurabilityManager dura = LowDurabilityManager.getInstance();
        extractor.text(this.font, "Warnung & Abbau-Stopp ab: §e" + dura.getThreshold() + " Dura", contentX, contentY + 36, COLOR_TEXT_WHITE, false);

        int cardX = contentX + 148;
        int cardY = contentY + 16;
        int cardW = 92;
        int cardH = 92;
        drawRoundedBox(extractor, cardX, cardY, cardW, cardH, 0xF2090E18, 0xFF1E293B);
        extractor.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 17, 0xFF0B1220);
        extractor.fill(cardX + 1, cardY + 17, cardX + cardW - 1, cardY + 18, 0xFF1E293B);
        extractor.text(this.font, "HELD TOOL", cardX + 8, cardY + 5, 0xFF38BDF8, true);

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            if (held != null && !held.isEmpty()) {
                extractor.fakeItem(held, cardX + 38, cardY + 24);
                int rem = dura.getRemainingDurability(held);
                if (rem >= 0) {
                    int max = held.getMaxDamage();
                    int duraColor = (rem <= dura.getThreshold()) ? 0xFFFF4444 : 0xFF4ADE80;
                    String dStr = rem + " / " + max;
                    int dw = this.font.width(dStr);
                    extractor.text(this.font, dStr, cardX + (cardW - dw) / 2, cardY + 48, duraColor, false);

                    String statusText = (rem <= dura.getThreshold() && dura.isEnabled() && dura.isBlockMining()) ? "⛔ BLOCKIERT" : "✓ SICHER";
                    int stColor = (rem <= dura.getThreshold()) ? 0xFFFF4444 : 0xFF4ADE80;
                    int stw = this.font.width(statusText);
                    extractor.text(this.font, statusText, cardX + (cardW - stw) / 2, cardY + 66, stColor, true);
                } else {
                    String noDura = "Keine Dura";
                    int nw = this.font.width(noDura);
                    extractor.text(this.font, noDura, cardX + (cardW - nw) / 2, cardY + 50, COLOR_TEXT_GRAY, false);
                }
            } else {
                String empty = "Kein Item";
                int ew = this.font.width(empty);
                extractor.text(this.font, empty, cardX + (cardW - ew) / 2, cardY + 50, COLOR_TEXT_GRAY, false);
            }
        }
    }

    private void renderScaleContent(GuiGraphicsExtractor extractor, int contentX, int contentY) {
        int listY = contentY + 60;
        ItemScaleConfig cfg = ItemScaleConfig.getInstance();
        List<Map.Entry<String, Float>> entries = new ArrayList<>(cfg.getItemScales().entrySet());

        if (entries.isEmpty()) {
            extractor.text(this.font, "Keine Items konfiguriert.", contentX, listY + 10, COLOR_TEXT_GRAY, false);
            extractor.text(this.font, "Füge oben Items hinzu (z.B. mace, totem).", contentX, listY + 24, COLOR_TEXT_GRAY, false);
            return;
        }

        int visibleCount = 5;
        for (int i = 0; i < Math.min(visibleCount, entries.size() - scaleScroll); i++) {
            int idx = i + scaleScroll;
            if (idx >= entries.size()) break;

            Map.Entry<String, Float> e = entries.get(idx);
            String itemKey = e.getKey();
            float scale = e.getValue();
            int iy = listY + i * 20;

            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse("minecraft:" + itemKey));
            if (item != null) {
                extractor.fakeItem(item.getDefaultInstance(), contentX, iy - 2);
            }

            String displayKey = formatItemName(itemKey);
            extractor.text(this.font, displayKey, contentX + 20, iy + 2, COLOR_TEXT_WHITE, false);
            extractor.text(this.font, String.format(Locale.ROOT, "%.1fx", scale), contentX + 148, iy + 2, COLOR_CYAN, true);
        }

        if (entries.size() > visibleCount) {
            extractor.text(this.font, "Mausrad zum Scrollen", contentX, contentY + 164, COLOR_TEXT_GRAY, false);
        }
    }

    private void renderInvseeContent(GuiGraphicsExtractor extractor, int contentX, int contentY) {
        int listY = contentY + 60;
        InvseeCounterConfig cfg = InvseeCounterConfig.getInstance();
        List<Map.Entry<String, Boolean>> entries = new ArrayList<>(cfg.getTrackedItems().entrySet());

        int visibleCount = 5;
        for (int i = 0; i < Math.min(visibleCount, entries.size() - invseeScroll); i++) {
            int idx = i + invseeScroll;
            if (idx >= entries.size()) break;

            Map.Entry<String, Boolean> e = entries.get(idx);
            String itemKey = e.getKey();
            int iy = listY + i * 20;

            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse("minecraft:" + itemKey));
            if (item != null) {
                extractor.fakeItem(item.getDefaultInstance(), contentX, iy - 2);
            }

            String displayKey = formatItemName(itemKey);
            extractor.text(this.font, displayKey, contentX + 20, iy + 2, COLOR_TEXT_WHITE, false);
        }

        if (entries.size() > visibleCount) {
            extractor.text(this.font, "Mausrad zum Scrollen", contentX, contentY + 164, COLOR_TEXT_GRAY, false);
        }

        int cardX = contentX + 146;
        int cardY = contentY + 16;
        int cardW = 86;

        List<String> activeKeys = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : entries) {
            if (Boolean.TRUE.equals(e.getValue())) activeKeys.add(e.getKey());
        }

        if (!activeKeys.isEmpty()) {
            int cardH = 22 + Math.min(activeKeys.size(), 6) * 18;
            drawRoundedBox(extractor, cardX, cardY, cardW, cardH, 0xF2090E18, 0xFF1E293B);
            extractor.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + 17, 0xFF0B1220);
            extractor.fill(cardX + 1, cardY + 17, cardX + cardW - 1, cardY + 18, 0xFF1E293B);
            extractor.text(this.font, "INV STATS", cardX + 8, cardY + 5, 0xFF38BDF8, true);

            int ry = cardY + 21;
            for (int i = 0; i < Math.min(activeKeys.size(), 6); i++) {
                String k = activeKeys.get(i);
                Item it = BuiltInRegistries.ITEM.getValue(Identifier.tryParse("minecraft:" + k));
                if (it != null) {
                    extractor.fakeItem(it.getDefaultInstance(), cardX + 5, ry);
                }
                extractor.text(this.font, "x64", cardX + 26, ry + 4, 0xFFFFD700, true);
                ry += 18;
            }
        }
    }

    private String formatItemName(String key) {
        String[] parts = key.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        String res = sb.toString().trim();
        if (res.length() > 11) res = res.substring(0, 9) + "..";
        return res;
    }

    private void drawRoundedBox(GuiGraphicsExtractor extractor, int x, int y, int w, int h, int bg, int border) {
        extractor.fill(x + 1, y, x + w - 1, y + h, bg);
        extractor.fill(x, y + 1, x + 1, y + h - 1, bg);
        extractor.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);

        extractor.fill(x + 1, y, x + w - 1, y + 1, border);
        extractor.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        extractor.fill(x, y + 1, x + 1, y + h - 1, border);
        extractor.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        double mouseX = event.x();
        double mouseY = event.y();

        int winX = (this.width - WIN_WIDTH) / 2;
        int winY = (this.height - WIN_HEIGHT) / 2;

        int sidebarY = winY + HEADER_HEIGHT + 1;
        if (mouseX >= winX + 4 && mouseX < winX + SIDEBAR_WIDTH - 4) {
            Tab[] tabs = Tab.values();
            int tabH = 19;
            for (int i = 0; i < tabs.length; i++) {
                int ty = sidebarY + 4 + i * (tabH + 3);
                if (mouseY >= ty && mouseY < ty + tabH) {
                    activeTab = tabs[i];
                    rebuildUI();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

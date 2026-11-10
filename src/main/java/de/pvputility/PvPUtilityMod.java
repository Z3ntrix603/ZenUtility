package de.pvputility;

import de.pvputility.durability.LowDurabilityManager;
import de.pvputility.glint.EnchantGlintManager;
import de.pvputility.gui.PvPUtilityScreen;
import de.pvputility.hitcolor.HitColorManager;
import de.pvputility.hud.InvseeCounterHud;
import de.pvputility.lyrics.LyricsConfig;
import de.pvputility.lyrics.LyricsManager;
import de.pvputility.lyrics.LyricsRenderer;
import de.pvputility.scale.ItemScaleConfig;
import de.pvputility.totem.TotemPopColorManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvPUtilityMod implements ClientModInitializer {

    public static final String MOD_ID = "pvputility";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("zenutility", "main")
    );

    private static KeyMapping openGuiKey;
    private static KeyMapping toggleHitColorKey;
    private static KeyMapping toggleTotemPopKey;
    private static KeyMapping cycleHitColorKey;
    private static KeyMapping cycleTotemPopKey;
    private static KeyMapping toggleLowDuraKey;
    private static KeyMapping toggleLyricsKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ZenUtility initializing...");

        // Load configs
        ItemScaleConfig.getInstance();
        EnchantGlintManager.getInstance();
        HitColorManager.getInstance();
        TotemPopColorManager.getInstance();
        LowDurabilityManager.getInstance();
        LyricsManager.getInstance();
        InvseeCounterHud.init();

        // Register In-Game HUD Elements
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"),
                new LyricsRenderer()
        );

        // Register Keybindings in custom dedicated ZenUtility category
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                CATEGORY
        ));

        toggleHitColorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.toggle_hitcolor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));

        toggleTotemPopKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.toggle_totempop",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        cycleHitColorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.cycle_hitcolor",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        cycleTotemPopKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.cycle_totempop",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        toggleLowDuraKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.toggle_low_dura",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        toggleLyricsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvputility.toggle_lyrics",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        // Client Tick Handler for Keybinds and Rainbow ticks
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new PvPUtilityScreen());
                }
            }

            while (toggleHitColorKey.consumeClick()) {
                HitColorManager.getInstance().toggle();
                boolean on = HitColorManager.getInstance().isEnabled();
                if (client.gui != null) {
                    client.gui.setOverlayMessage(Component.literal("§d[ZenUtility] §fHit Color: " + (on ? "§aAKTIV (" + HitColorManager.getInstance().getCurrentPreset() + ")" : "§cDEAKTIVIERT")), false);
                }
            }

            while (toggleTotemPopKey.consumeClick()) {
                TotemPopColorManager.getInstance().toggle();
                boolean on = TotemPopColorManager.getInstance().isEnabled();
                if (client.gui != null) {
                    client.gui.setOverlayMessage(Component.literal("§d[ZenUtility] §fTotem Pop: " + (on ? "§aAKTIV (" + TotemPopColorManager.getInstance().getCurrentPreset() + ")" : "§cDEAKTIVIERT")), false);
                }
            }

            while (cycleHitColorKey.consumeClick()) {
                HitColorManager.getInstance().cyclePreset();
                if (client.gui != null) {
                    client.gui.setOverlayMessage(Component.literal("§d[ZenUtility] §fHit Color: §e" + HitColorManager.getInstance().getCurrentPreset()), false);
                }
            }

            while (cycleTotemPopKey.consumeClick()) {
                TotemPopColorManager.getInstance().cyclePreset();
                if (client.gui != null) {
                    client.gui.setOverlayMessage(Component.literal("§d[ZenUtility] §fTotem Pop: §e" + TotemPopColorManager.getInstance().getCurrentPreset()), false);
                }
            }

            while (toggleLowDuraKey.consumeClick()) {
                LowDurabilityManager.getInstance().toggle();
                boolean on = LowDurabilityManager.getInstance().isEnabled();
                if (client.gui != null) {
                    client.gui.setOverlayMessage(Component.literal("§d[ZenUtility] §fDura-Schutz: " + (on ? "§aAKTIV (≤ " + LowDurabilityManager.getInstance().getThreshold() + " Dura)" : "§cDEAKTIVIERT")), false);
                }
            }

            while (toggleLyricsKey.consumeClick()) {
                LyricsConfig.getInstance().setEnabled(!LyricsConfig.getInstance().isEnabled());
                boolean on = LyricsConfig.getInstance().isEnabled();
                if (client.gui != null) {
                    client.gui.setOverlayMessage(Component.literal("§d[ZenUtility] §fSong Lyrics: " + (on ? "§aAKTIV" : "§cDEAKTIVIERT")), false);
                }
            }

            HitColorManager.getInstance().tick();
            LowDurabilityManager.getInstance().tick(client);
        });

        // Register Slash Commands: /pvputils, /zen, and /utility
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pvputils").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new PvPUtilityScreen());
                });
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("zen").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new PvPUtilityScreen());
                });
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("utility").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new PvPUtilityScreen());
                });
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("lyrics").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    PvPUtilityScreen scr = new PvPUtilityScreen();
                    scr.setActiveTab(PvPUtilityScreen.Tab.LYRICS);
                    Minecraft.getInstance().setScreen(scr);
                });
                return 1;
            }));
        });

        LOGGER.info("ZenUtility initialized successfully.");
    }
}

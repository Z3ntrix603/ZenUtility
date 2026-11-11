package de.pvputility.lyrics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

public class LyricsWorldRenderer {

    public static class FormattedLines {
        public final String primary;
        public final String secondary;

        public FormattedLines(String primary, String secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    public static FormattedLines formatLines(String currentLineText, String nextLineText) {
        if (currentLineText == null) currentLineText = "";
        currentLineText = currentLineText.trim();
        if (currentLineText.isEmpty()) {
            return new FormattedLines("♪  ♪  ♪", "");
        }

        // Split long lines gracefully around the middle space for the viral aesthetic layout
        if (currentLineText.length() > 20 && currentLineText.contains(" ")) {
            int mid = currentLineText.length() / 2;
            int bestSpace = -1;
            int bestDist = Integer.MAX_VALUE;
            for (int i = 0; i < currentLineText.length(); i++) {
                if (currentLineText.charAt(i) == ' ') {
                    int dist = Math.abs(i - mid);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestSpace = i;
                    }
                }
            }
            if (bestSpace > 0 && bestSpace < currentLineText.length() - 1) {
                String l1 = currentLineText.substring(0, bestSpace).trim();
                String l2 = currentLineText.substring(bestSpace + 1).trim();
                return new FormattedLines(l1, l2);
            }
        }

        String sub = (nextLineText != null && !nextLineText.trim().isEmpty()) ? nextLineText.trim() : "";
        if (sub.length() > 36) {
            sub = sub.substring(0, 34) + "...";
        }
        return new FormattedLines(currentLineText, sub);
    }

    public static void render3D(WorldRenderContext context) {
        LyricsConfig cfg = LyricsConfig.getInstance();
        if (!cfg.isEnabled() || !cfg.isInAirMode()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        LyricsManager mgr = LyricsManager.getInstance();
        if (mgr.getCurrentTrack() == null && mgr.getCurrentLyrics() == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;
        Vec3 camPos = camera.position();
        if (camPos == null) return;

        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        // Camera direction from pitch & yaw
        double yawRad = Math.toRadians(camera.yRot());
        double pitchRad = Math.toRadians(camera.xRot());

        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);

        float dist = cfg.getInAirDistance();
        float height = cfg.getInAirHeight();

        double targetX = camX + fx * dist;
        double targetY = camY + fy * dist + height;
        double targetZ = camZ + fz * dist;

        if (!mgr.hasInAirPos) {
            mgr.currentInAirX = targetX;
            mgr.currentInAirY = targetY;
            mgr.currentInAirZ = targetZ;
            mgr.hasInAirPos = true;
        } else if (cfg.isSmoothInertia()) {
            double lerp = 0.08;
            mgr.currentInAirX += (targetX - mgr.currentInAirX) * lerp;
            mgr.currentInAirY += (targetY - mgr.currentInAirY) * lerp;
            mgr.currentInAirZ += (targetZ - mgr.currentInAirZ) * lerp;
        } else {
            mgr.currentInAirX = targetX;
            mgr.currentInAirY = targetY;
            mgr.currentInAirZ = targetZ;
        }

        // Atmospheric floating wave & dreamy tilt
        long now = System.currentTimeMillis();
        double waveY = Math.sin(now * 0.0022) * 0.12;
        double waveX = Math.cos(now * 0.0016) * 0.08;
        float tiltRoll = (float) (Math.sin(now * 0.0015) * 2.5);

        double renderX = (mgr.currentInAirX + waveX) - camX;
        double renderY = (mgr.currentInAirY + waveY) - camY;
        double renderZ = (mgr.currentInAirZ) - camZ;

        PoseStack poseStack = context.matrices();
        MultiBufferSource consumers = context.consumers();
        if (poseStack == null || consumers == null) return;

        Font font = mc.font;

        poseStack.pushPose();
        poseStack.translate(renderX, renderY, renderZ);

        // Billboard towards camera
        poseStack.mulPose(camera.rotation());

        // Subtle dreamy 3D roll
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltRoll));

        // Scale pop on line change
        long elapsed = now - mgr.getLastLineChangeTimestamp();
        float animProgress = Math.min(1.0f, elapsed / 350.0f);
        float popMultiplier = 0.90f + 0.10f * (1.0f - (float) Math.pow(1.0f - animProgress, 3));

        float baseScale = -0.040f * cfg.getTextScale();
        float scale = baseScale * popMultiplier;
        poseStack.scale(scale, scale, scale);

        // Fetch lyric texts
        String currentLine = "";
        String nextLine = "";
        if (mgr.getCurrentLyrics() != null && !mgr.getCurrentLyrics().lines.isEmpty()) {
            int lineIdx = mgr.getCurrentLineIndex();
            currentLine = mgr.getCurrentLyrics().lines.get(lineIdx).text;
            if (lineIdx + 1 < mgr.getCurrentLyrics().lines.size()) {
                nextLine = mgr.getCurrentLyrics().lines.get(lineIdx + 1).text;
            }
        } else if (mgr.getCurrentTrack() != null) {
            currentLine = mgr.getCurrentTrack().artist + " - " + mgr.getCurrentTrack().title;
        }

        FormattedLines display = formatLines(currentLine, nextLine);

        int baseColor = cfg.getEffectiveColorRgb();
        int activeColor = baseColor | 0xFF000000;
        int subColor = ((baseColor & 0x00FFFFFF) | 0x99000000);

        Font.DisplayMode mode = cfg.isSeeThrough() ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

        // Draw primary line: Bold + Italic for aesthetic cursive look
        FormattedCharSequence seq1 = Component.literal("§o§l" + display.primary).getVisualOrderText();
        int w1 = font.width(seq1);
        font.drawInBatch(seq1, -w1 / 2.0f, display.secondary.isEmpty() ? -4.5f : -9.0f, activeColor, true, poseStack.last().pose(), consumers, mode, 0, 0xF000F0);

        // Draw secondary line: Italic softer glow
        if (!display.secondary.isEmpty()) {
            FormattedCharSequence seq2 = Component.literal("§o" + display.secondary).getVisualOrderText();
            int w2 = font.width(seq2);
            font.drawInBatch(seq2, -w2 / 2.0f, 3.0f, subColor, true, poseStack.last().pose(), consumers, mode, 0, 0xF000F0);
        }

        poseStack.popPose();
    }
}

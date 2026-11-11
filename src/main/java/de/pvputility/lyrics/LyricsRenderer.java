package de.pvputility.lyrics;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class LyricsRenderer implements HudElement {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        LyricsConfig cfg = LyricsConfig.getInstance();
        if (!cfg.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        LyricsManager mgr = LyricsManager.getInstance();
        if (mgr.getCurrentTrack() == null && mgr.getCurrentLyrics() == null) {
            return;
        }

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int renderX;
        int renderY;
        float distanceScale = 1.0f;

        if (cfg.isInAirMode()) return;

        if (false) {
            // Camera position and orientation
            double camX = mc.player.getX();
            double camY = mc.player.getEyeY();
            double camZ = mc.player.getZ();

            float yaw = mc.player.getYRot();
            float pitch = mc.player.getXRot();

            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);

            double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
            double fy = -Math.sin(pitchRad);
            double fz = Math.cos(yawRad) * Math.cos(pitchRad);

            double rx = Math.cos(yawRad);
            double ry = 0.0;
            double rz = Math.sin(yawRad);

            double ux = -rz * fy;
            double uy = rz * fx - rx * fz;
            double uz = rx * fy;

            double dist = cfg.getInAirDistance();
            double hOff = cfg.getInAirHeight();

            double targetX = camX + fx * dist + ux * hOff;
            double targetY = camY + fy * dist + uy * hOff;
            double targetZ = camZ + fz * dist + uz * hOff;

            if (!mgr.hasInAirPos) {
                mgr.currentInAirX = targetX;
                mgr.currentInAirY = targetY;
                mgr.currentInAirZ = targetZ;
                mgr.hasInAirPos = true;
            } else {
                double lerp = cfg.isSmoothInertia() ? 0.16 : 1.0;
                mgr.currentInAirX += (targetX - mgr.currentInAirX) * lerp;
                mgr.currentInAirY += (targetY - mgr.currentInAirY) * lerp;
                mgr.currentInAirZ += (targetZ - mgr.currentInAirZ) * lerp;
            }

            double dx = mgr.currentInAirX - camX;
            double dy = mgr.currentInAirY - camY;
            double dz = mgr.currentInAirZ - camZ;

            double localZ = dx * fx + dy * fy + dz * fz;
            double localX = dx * rx + dy * ry + dz * rz;
            double localY = dx * ux + dy * uy + dz * uz;

            if (localZ <= 0.18) return; // Behind camera

            double fov = mc.options.fov().get();
            double tanHalfFov = Math.tan(Math.toRadians(fov * 0.5));
            double aspect = (double) screenWidth / (double) screenHeight;

            double ndcX = localX / (localZ * tanHalfFov * aspect);
            double ndcY = -localY / (localZ * tanHalfFov);

            renderX = (int) (screenWidth * 0.5 * (1.0 + ndcX));
            renderY = (int) (screenHeight * 0.5 * (1.0 + ndcY));
            distanceScale = (float) Math.max(0.70, Math.min(1.30, dist / Math.max(1.0, localZ)));
        } else {
            // 2D HUD Mode
            renderX = screenWidth / 2;
            renderY = (int) (screenHeight * 0.74);
            distanceScale = 1.0f;
        }

        // Check screen bounds
        if (renderX < -150 || renderX > screenWidth + 150 || renderY < -100 || renderY > screenHeight + 100) {
            return;
        }

        // Determine lyrics lines
        String prevLine = "";
        String activeLine = "";
        String nextLine = "";
        String nextNextLine = "";

        LrclibClient.LyricsResult lyrics = mgr.getCurrentLyrics();
        if (lyrics != null && !lyrics.lines.isEmpty()) {
            int curIdx = mgr.getCurrentLineIndex();
            List<LrclibClient.LyricLine> lines = lyrics.lines;

            if (curIdx > 0) prevLine = lines.get(curIdx - 1).text;
            activeLine = lines.get(curIdx).text;
            if (curIdx + 1 < lines.size()) nextLine = lines.get(curIdx + 1).text;
            if (curIdx + 2 < lines.size()) nextNextLine = lines.get(curIdx + 2).text;

            if (activeLine.trim().isEmpty()) activeLine = "♪  ♪  ♪";
        } else if (mgr.getCurrentTrack() != null) {
            SpotifyDetector.TrackInfo t = mgr.getCurrentTrack();
            activeLine = "♪ " + t.artist + " - " + t.title + " ♪";
        }

        // Apply scaling if necessary
        boolean needScale = Math.abs(distanceScale - 1.0f) > 0.02f;
        if (needScale) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float) renderX, (float) renderY);
            guiGraphics.pose().scale(distanceScale, distanceScale);
            guiGraphics.pose().translate((float) -renderX, (float) -renderY);
        }

        int activeColor = cfg.getEffectiveColorRgb() | 0xFF000000;
        int maxTextW = font.width(activeLine);
        if (!nextLine.isEmpty()) maxTextW = Math.max(maxTextW, font.width(nextLine));

        int cardW = Math.max(190, maxTextW + 36);
        cardW = Math.min(320, cardW);
        int cardH = 68;
        int left = renderX - cardW / 2;
        int top = renderY - cardH / 2;

        if (cfg.isShowBackground()) {
            guiGraphics.fill(left, top, left + cardW, top + cardH, 0x85090E18);
            // Outline with 4 lines
            int border = 0x2238BDF8;
            guiGraphics.fill(left, top, left + cardW, top + 1, border);
            guiGraphics.fill(left, top + cardH - 1, left + cardW, top + cardH, border);
            guiGraphics.fill(left, top, left + 1, top + cardH, border);
            guiGraphics.fill(left + cardW - 1, top, left + cardW, top + cardH, border);
        }

        int yOff = top + 5;
        if (cfg.isShowTrackInfo() && mgr.getCurrentTrack() != null) {
            SpotifyDetector.TrackInfo t = mgr.getCurrentTrack();
            String titleStr = (t.isPlaying ? "🎵 " : "⏸ ") + t.artist + " - " + t.title;
            if (font.width(titleStr) > cardW - 12) {
                titleStr = font.plainSubstrByWidth(titleStr, cardW - 20) + "...";
            }
            guiGraphics.drawCenteredString(font, titleStr, renderX, yOff, 0xFF94A3B8);
            yOff += 11;
        }

        if (cfg.isShowProgressBar() && lyrics != null && lyrics.durationMs > 0) {
            int barW = cardW - 24;
            int barH = 2;
            int barX = renderX - barW / 2;
            guiGraphics.fill(barX, yOff, barX + barW, yOff + barH, 0x33FFFFFF);

            float prog = (float) mgr.getCurrentPositionMs() / (float) lyrics.durationMs;
            prog = Math.max(0.0f, Math.min(1.0f, prog));
            guiGraphics.fill(barX, yOff, barX + (int) (barW * prog), yOff + barH, activeColor);
            yOff += 6;
        }

        if (!prevLine.isEmpty()) {
            guiGraphics.drawCenteredString(font, prevLine, renderX, yOff, 0x55FFFFFF);
        }
        yOff += 11;

        // Active line with glowing highlight
        guiGraphics.drawCenteredString(font, "✦ " + activeLine + " ✦", renderX, yOff, activeColor);
        yOff += 12;

        if (!nextLine.isEmpty()) {
            guiGraphics.drawCenteredString(font, nextLine, renderX, yOff, 0xAAFFFFFF);
        }
        yOff += 10;

        if (!nextNextLine.isEmpty()) {
            guiGraphics.drawCenteredString(font, nextNextLine, renderX, yOff, 0x44FFFFFF);
        }

        if (needScale) {
            guiGraphics.pose().popMatrix();
        }
    }
}

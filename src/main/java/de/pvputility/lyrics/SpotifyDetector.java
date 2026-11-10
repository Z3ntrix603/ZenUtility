package de.pvputility.lyrics;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;

public class SpotifyDetector {

    public static class TrackInfo {
        public final String artist;
        public final String title;
        public final boolean isPlaying;

        public TrackInfo(String artist, String title, boolean isPlaying) {
            this.artist = artist != null ? artist : "";
            this.title = title != null ? title : "";
            this.isPlaying = isPlaying;
        }

        @Override
        public String toString() {
            return isPlaying ? artist + " - " + title : "[PAUSED]";
        }
    }

    public static TrackInfo detect() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return null;
        }

        final TrackInfo[] found = new TrackInfo[1];

        try {
            User32.INSTANCE.EnumWindows((hWnd, data) -> {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
                String title = Native.toString(windowText).trim();

                if (!title.isEmpty()) {
                    char[] className = new char[256];
                    User32.INSTANCE.GetClassName(hWnd, className, 256);
                    String cls = Native.toString(className);

                    // Spotify main window uses class Chrome_WidgetWin_0
                    if (cls.contains("Chrome_WidgetWin")) {
                        IntByReference pid = new IntByReference();
                        User32.INSTANCE.GetWindowThreadProcessId(hWnd, pid);
                        int p = pid.getValue();

                        boolean isSpotify = ProcessHandle.of(p)
                                .flatMap(ph -> ph.info().command())
                                .map(String::toLowerCase)
                                .map(cmd -> cmd.contains("spotify"))
                                .orElse(false);

                        if (isSpotify) {
                            if (title.contains(" - ") 
                                    && !title.equalsIgnoreCase("Spotify") 
                                    && !title.equalsIgnoreCase("Spotify Free") 
                                    && !title.equalsIgnoreCase("Spotify Premium")) {
                                int dashIdx = title.indexOf(" - ");
                                String artist = title.substring(0, dashIdx).trim();
                                String song = title.substring(dashIdx + 3).trim();
                                found[0] = new TrackInfo(artist, song, true);
                                return false; // Stop enumeration
                            } else if (title.equalsIgnoreCase("Spotify") 
                                    || title.equalsIgnoreCase("Spotify Free") 
                                    || title.equalsIgnoreCase("Spotify Premium")) {
                                if (found[0] == null) {
                                    found[0] = new TrackInfo("", "", false);
                                }
                            }
                        }
                    }
                }
                return true;
            }, null);
        } catch (Throwable ignored) {}

        return found[0];
    }
}

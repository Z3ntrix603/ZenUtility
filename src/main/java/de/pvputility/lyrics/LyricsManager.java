package de.pvputility.lyrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LyricsManager {

    private static final LyricsManager INSTANCE = new LyricsManager();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ZenUtility-SpotifyDetector");
        t.setDaemon(true);
        return t;
    });

    private SpotifyDetector.TrackInfo currentTrack;
    private LrclibClient.LyricsResult currentLyrics;

    private boolean isPlaying = false;
    private long playbackStartTime = 0;
    private long pausedAtPositionMs = 0;
    private String lastFetchedKey = "";

    // 3D In-Air Lerp Coordinates
    public double currentInAirX = 0;
    public double currentInAirY = 0;
    public double currentInAirZ = 0;
    public boolean hasInAirPos = false;

    private LyricsManager() {
        // Start background polling for Spotify
        scheduler.scheduleWithFixedDelay(this::pollSpotify, 1000, 600, TimeUnit.MILLISECONDS);
    }

    public static LyricsManager getInstance() {
        return INSTANCE;
    }

    private void pollSpotify() {
        if (!LyricsConfig.getInstance().isEnabled()) return;

        try {
            SpotifyDetector.TrackInfo track = SpotifyDetector.detect();
            if (track != null) {
                if (track.isPlaying) {
                    String trackKey = track.artist.toLowerCase() + " - " + track.title.toLowerCase();
                    if (!trackKey.equals(lastFetchedKey)) {
                        lastFetchedKey = trackKey;
                        currentTrack = track;
                        isPlaying = true;
                        playbackStartTime = System.currentTimeMillis();
                        pausedAtPositionMs = 0;

                        // Fetch lyrics asynchronously
                        fetchLyricsAsync(track.artist, track.title);
                    } else if (!isPlaying) {
                        // Resumed playback from paused
                        isPlaying = true;
                        playbackStartTime = System.currentTimeMillis() - pausedAtPositionMs;
                    }
                } else {
                    // Spotify is paused or idle
                    if (isPlaying) {
                        pausedAtPositionMs = getRawPlaybackPositionMs();
                        isPlaying = false;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void fetchLyricsAsync(String artist, String title) {
        new Thread(() -> {
            LrclibClient.LyricsResult res = LrclibClient.fetch(artist, title);
            if (res != null) {
                currentLyrics = res;
            }
        }, "ZenUtility-LyricsFetcher").start();
    }

    public void loadDemoSong() {
        List<LrclibClient.LyricLine> lines = new ArrayList<>();
        lines.add(new LrclibClient.LyricLine(0, "Is this the real life? Is this just fantasy?"));
        lines.add(new LrclibClient.LyricLine(6500, "Caught in a landslide, no escape from reality"));
        lines.add(new LrclibClient.LyricLine(14000, "Open your eyes, look up to the skies and see"));
        lines.add(new LrclibClient.LyricLine(24000, "I'm just a poor boy, I need no sympathy"));
        lines.add(new LrclibClient.LyricLine(29500, "Because I'm easy come, easy go, little high, little low"));
        lines.add(new LrclibClient.LyricLine(38000, "Any way the wind blows doesn't really matter to me..."));
        lines.add(new LrclibClient.LyricLine(48000, "Mama, just killed a man"));
        lines.add(new LrclibClient.LyricLine(54000, "Put a gun against his head, pulled my trigger, now he's dead"));
        lines.add(new LrclibClient.LyricLine(61000, "Mama, life had just begun"));
        lines.add(new LrclibClient.LyricLine(67000, "But now I've gone and thrown it all away"));
        lines.add(new LrclibClient.LyricLine(73000, "Mama, ooh, didn't mean to make you cry"));
        lines.add(new LrclibClient.LyricLine(82000, "If I'm not back again this time tomorrow"));
        lines.add(new LrclibClient.LyricLine(86000, "Carry on, carry on as if nothing really matters"));

        currentTrack = new SpotifyDetector.TrackInfo("Queen", "Bohemian Rhapsody", true);
        currentLyrics = new LrclibClient.LyricsResult("Bohemian Rhapsody", "Queen", 355000, lines, true);
        lastFetchedKey = "queen - bohemian rhapsody";
        isPlaying = true;
        playbackStartTime = System.currentTimeMillis();
        pausedAtPositionMs = 0;
    }

    public long getRawPlaybackPositionMs() {
        if (!isPlaying) return pausedAtPositionMs;
        return System.currentTimeMillis() - playbackStartTime;
    }

    public long getCurrentPositionMs() {
        long raw = getRawPlaybackPositionMs();
        long offset = (long) (LyricsConfig.getInstance().getSyncOffsetSeconds() * 1000f);
        return Math.max(0, raw + offset);
    }

    public void adjustOffset(float deltaSeconds) {
        LyricsConfig cfg = LyricsConfig.getInstance();
        cfg.setSyncOffsetSeconds(cfg.getSyncOffsetSeconds() + deltaSeconds);
    }

    public void restartTrackTimer() {
        playbackStartTime = System.currentTimeMillis();
        pausedAtPositionMs = 0;
        isPlaying = true;
    }

    public void togglePlayPause() {
        if (isPlaying) {
            pausedAtPositionMs = getRawPlaybackPositionMs();
            isPlaying = false;
        } else {
            playbackStartTime = System.currentTimeMillis() - pausedAtPositionMs;
            isPlaying = true;
        }
    }

    public int getCurrentLineIndex() {
        if (currentLyrics == null || currentLyrics.lines.isEmpty()) return -1;
        long pos = getCurrentPositionMs();
        List<LrclibClient.LyricLine> lines = currentLyrics.lines;

        if (pos < lines.get(0).timeMs) return 0;

        for (int i = lines.size() - 1; i >= 0; i--) {
            if (pos >= lines.get(i).timeMs) {
                return i;
            }
        }
        return 0;
    }

    public SpotifyDetector.TrackInfo getCurrentTrack() { return currentTrack; }
    public LrclibClient.LyricsResult getCurrentLyrics() { return currentLyrics; }
    public boolean isPlaying() { return isPlaying; }
}

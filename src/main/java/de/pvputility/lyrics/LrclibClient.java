package de.pvputility.lyrics;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrclibClient {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static final Pattern LRC_LINE_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]\\s*(.*)");

    public static class LyricLine {
        public final long timeMs;
        public final String text;

        public LyricLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    public static class LyricsResult {
        public final String trackTitle;
        public final String artist;
        public final long durationMs;
        public final List<LyricLine> lines;
        public final boolean isSynced;

        public LyricsResult(String trackTitle, String artist, long durationMs, List<LyricLine> lines, boolean isSynced) {
            this.trackTitle = trackTitle;
            this.artist = artist;
            this.durationMs = durationMs;
            this.lines = lines;
            this.isSynced = isSynced;
        }
    }

    public static LyricsResult fetch(String artist, String title) {
        try {
            String cleanArtist = cleanQuery(artist);
            String cleanTitle = cleanQuery(title);

            String encodedArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8);
            String encodedTitle = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8);
            String url = "https://lrclib.net/api/get?artist_name=" + encodedArtist + "&track_name=" + encodedTitle;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "ZenUtility/1.0.4 (Minecraft Mod; +https://github.com/Z3ntrix603/ZenUtility)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                LyricsResult res = parseJson(response.body(), title, artist);
                if (res != null) return res;
            }

            // Fallback search
            String searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(cleanArtist + " " + cleanTitle, StandardCharsets.UTF_8);
            HttpRequest searchReq = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("User-Agent", "ZenUtility/1.0.4 (Minecraft Mod; +https://github.com/Z3ntrix603/ZenUtility)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> searchResp = client.send(searchReq, HttpResponse.BodyHandlers.ofString());
            if (searchResp.statusCode() == 200 && searchResp.body() != null) {
                return parseJsonSearch(searchResp.body(), title, artist);
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static String cleanQuery(String s) {
        if (s == null) return "";
        // Remove trailing "(Remastered)", "(feat. ...)", etc.
        return s.replaceAll("(?i)\\s*\\(feat\\..*?\\)", "")
                .replaceAll("(?i)\\s*\\(remaster.*?\\)", "")
                .replaceAll("(?i)\\s*\\[.*?\\]", "")
                .trim();
    }

    private static LyricsResult parseJson(String json, String fallbackTitle, String fallbackArtist) {
        String syncedLyrics = extractJsonString(json, "syncedLyrics");
        String plainLyrics = extractJsonString(json, "plainLyrics");
        double durationSec = extractJsonNumber(json, "duration");
        long durationMs = (long) (durationSec * 1000);

        List<LyricLine> lines = new ArrayList<>();
        if (syncedLyrics != null && !syncedLyrics.trim().isEmpty()) {
            for (String rawLine : syncedLyrics.split("\\r?\\n")) {
                Matcher m = LRC_LINE_PATTERN.matcher(rawLine.trim());
                if (m.matches()) {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    String msStr = m.group(3);
                    long ms = msStr.length() == 2 ? Long.parseLong(msStr) * 10 : Long.parseLong(msStr);
                    long totalMs = min * 60000 + sec * 1000 + ms;
                    String text = m.group(4).trim();
                    lines.add(new LyricLine(totalMs, text));
                }
            }
            if (!lines.isEmpty()) {
                return new LyricsResult(fallbackTitle, fallbackArtist, durationMs, lines, true);
            }
        }

        if (plainLyrics != null && !plainLyrics.trim().isEmpty()) {
            String[] rawLines = plainLyrics.split("\\r?\\n");
            long step = durationMs > 0 && rawLines.length > 0 ? durationMs / rawLines.length : 3500;
            for (int i = 0; i < rawLines.length; i++) {
                String line = rawLines[i].trim();
                if (!line.isEmpty()) {
                    lines.add(new LyricLine(i * step, line));
                }
            }
            return new LyricsResult(fallbackTitle, fallbackArtist, durationMs, lines, false);
        }

        return null;
    }

    private static LyricsResult parseJsonSearch(String jsonArray, String fallbackTitle, String fallbackArtist) {
        if (!jsonArray.startsWith("[")) return null;
        int syncedIdx = jsonArray.indexOf("\"syncedLyrics\"");
        if (syncedIdx > 0) {
            int startObj = jsonArray.lastIndexOf("{", syncedIdx);
            int endObj = jsonArray.indexOf("}", syncedIdx);
            if (startObj >= 0 && endObj > startObj) {
                return parseJson(jsonArray.substring(startObj, endObj + 1), fallbackTitle, fallbackArtist);
            }
        }
        return null;
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start < json.length() && json.charAt(start) == '"') {
            start++;
            StringBuilder sb = new StringBuilder();
            boolean esc = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (esc) {
                    if (c == 'n') sb.append('\n');
                    else if (c == 'r') sb.append('\r');
                    else if (c == 't') sb.append('\t');
                    else sb.append(c);
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }
        }
        return null;
    }

    private static double extractJsonNumber(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int start = idx + search.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception ignored) {
            return 0;
        }
    }
}

package com.trousseau.service;

import com.trousseau.model.DayForecast;
import com.trousseau.model.User;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches a 16-day forecast from Open-Meteo for the weekly planner.
 *
 * <p><strong>Entirely optional.</strong> A user with no coordinates set gets no
 * forecast, and every failure path — no route to the internet, a timeout, a bad
 * response — returns an empty map rather than propagating. The planner then falls
 * back to its original freshness-and-colour scoring. That matters because Trousseau
 * is built to run on a LAN with no outbound access at all.</p>
 *
 * <p>Open-Meteo needs no API key and no account. Responses are cached in memory for
 * {@link #CACHE_TTL_MINUTES} minutes per location so that reloading the planner, and
 * the Sunday mail run iterating over every user, do not each hit the network.</p>
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class WeatherService {

    private static final Logger LOG = Logger.getLogger(WeatherService.class.getName());

    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final int CACHE_TTL_MINUTES = 60;
    private static final int MAX_CACHE_ENTRIES = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private HttpClient http;

    /** location key -> cached forecast. Access is synchronised on the map itself. */
    private final Map<String, CacheEntry> cache =
            Collections.synchronizedMap(new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });

    @PostConstruct
    void init() {
        http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** True when the user has coordinates and weather scoring can apply. */
    public boolean isConfigured(User user) {
        return user != null && user.getLatitude() != null && user.getLongitude() != null;
    }

    /**
     * Forecast by date for the user's location, or an empty map when weather is
     * unconfigured or unavailable. Never throws.
     */
    public Map<LocalDate, DayForecast> getForecast(User user) {
        if (!isConfigured(user)) {
            return Collections.emptyMap();
        }
        return getForecast(user.getLatitude(), user.getLongitude());
    }

    public Map<LocalDate, DayForecast> getForecast(double latitude, double longitude) {
        String key = String.format("%.3f,%.3f", latitude, longitude);

        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.forecast;
        }

        try {
            Map<LocalDate, DayForecast> fresh = fetch(latitude, longitude);
            cache.put(key, new CacheEntry(fresh));
            return fresh;
        } catch (Exception e) {
            LOG.log(Level.FINE, "Weather lookup failed for " + key + "; planner will score without it", e);
            // Cache the failure briefly too, so an offline server does not retry on
            // every single page render.
            cache.put(key, new CacheEntry(Collections.emptyMap()));
            return Collections.emptyMap();
        }
    }

    private Map<LocalDate, DayForecast> fetch(double latitude, double longitude) throws Exception {
        String url = ENDPOINT
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum"
                // 16 days, not 7: the planner's useful case is *next* week, and the
                // Sunday mail plans the week starting tomorrow. A 7-day window would
                // cover only the first few days of it, and almost none of the current
                // week, most of which is already in the past.
                + "&timezone=auto&forecast_days=16";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Open-Meteo returned HTTP " + response.statusCode());
        }
        return parse(response.body());
    }

    private Map<LocalDate, DayForecast> parse(String body) {
        Map<LocalDate, DayForecast> result = new HashMap<>();
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject root = reader.readObject();
            if (!root.containsKey("daily")) {
                return result;
            }
            JsonObject daily = root.getJsonObject("daily");
            JsonArray days = daily.getJsonArray("time");
            JsonArray max = daily.getJsonArray("temperature_2m_max");
            JsonArray min = daily.getJsonArray("temperature_2m_min");
            JsonArray precip = daily.getJsonArray("precipitation_sum");

            for (int i = 0; i < days.size(); i++) {
                LocalDate date = LocalDate.parse(days.getString(i));
                result.put(date, new DayForecast(
                        date,
                        numberAt(max, i),
                        numberAt(min, i),
                        numberAt(precip, i)));
            }
        }
        return result;
    }

    /** Open-Meteo sends null for a value it has no data for. */
    private double numberAt(JsonArray array, int index) {
        if (array == null || index >= array.size() || array.isNull(index)) {
            return 0d;
        }
        return array.getJsonNumber(index).doubleValue();
    }

    private static final class CacheEntry {
        final Map<LocalDate, DayForecast> forecast;
        final Instant fetchedAt = Instant.now();

        CacheEntry(Map<LocalDate, DayForecast> forecast) {
            this.forecast = forecast;
        }

        boolean isExpired() {
            return fetchedAt.plus(Duration.ofMinutes(CACHE_TTL_MINUTES)).isBefore(Instant.now());
        }
    }
}

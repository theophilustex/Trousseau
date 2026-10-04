package com.trousseau.service;

import com.trousseau.dao.PlannedOutfitDao;
import com.trousseau.model.ClothingItem;
import com.trousseau.model.DayForecast;
import com.trousseau.model.Outfit;
import com.trousseau.model.PlannedOutfit;
import com.trousseau.model.User;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.*;

/**
 * Stateless service that encapsulates the weekly outfit recommendation algorithm.
 * Used by both the WeeklyPlannerBean (JSF) and WeeklyPlannerEmailScheduler (EJB timer).
 */
@Stateless
public class WeeklyPlannerService {

    private static final Map<String, Set<String>> COLOR_FAMILIES = new HashMap<>();

    static {
        COLOR_FAMILIES.put("red",    new HashSet<>(Arrays.asList("warm", "bright", "bold")));
        COLOR_FAMILIES.put("pink",   new HashSet<>(Arrays.asList("warm", "bright", "light")));
        COLOR_FAMILIES.put("orange", new HashSet<>(Arrays.asList("warm", "bright", "bold")));
        COLOR_FAMILIES.put("yellow", new HashSet<>(Arrays.asList("warm", "bright", "light")));
        COLOR_FAMILIES.put("green",  new HashSet<>(Arrays.asList("cool", "nature", "muted")));
        COLOR_FAMILIES.put("teal",   new HashSet<>(Arrays.asList("cool", "nature", "muted")));
        COLOR_FAMILIES.put("blue",   new HashSet<>(Arrays.asList("cool", "calm", "muted")));
        COLOR_FAMILIES.put("navy",   new HashSet<>(Arrays.asList("cool", "calm", "dark")));
        COLOR_FAMILIES.put("purple", new HashSet<>(Arrays.asList("cool", "bold", "rich")));
        COLOR_FAMILIES.put("brown",  new HashSet<>(Arrays.asList("warm", "neutral", "dark")));
        COLOR_FAMILIES.put("beige",  new HashSet<>(Arrays.asList("warm", "neutral", "light")));
        COLOR_FAMILIES.put("cream",  new HashSet<>(Arrays.asList("warm", "neutral", "light")));
        COLOR_FAMILIES.put("white",  new HashSet<>(Arrays.asList("neutral", "light", "bright")));
        COLOR_FAMILIES.put("grey",   new HashSet<>(Arrays.asList("neutral", "muted", "calm")));
        COLOR_FAMILIES.put("gray",   new HashSet<>(Arrays.asList("neutral", "muted", "calm")));
        COLOR_FAMILIES.put("black",  new HashSet<>(Arrays.asList("neutral", "dark", "bold")));
    }

    @Inject
    private OutfitService outfitService;

    @Inject
    private PlannedOutfitDao plannedOutfitDao;

    @Inject
    private WeatherService weatherService;

    /**
     * Generates a 7-element list of recommended outfits (Mon–Sun).
     * Each element may be null if the user has no outfits.
     */
    public List<Outfit> generateWeeklyPlan(User user) {
        return generateWeeklyPlan(user, LocalDate.now().with(java.time.DayOfWeek.MONDAY));
    }

    /**
     * Generates a plan for the week starting {@code weekStart}. Knowing the dates lets
     * each day be scored against its own forecast; without a forecast the scoring is
     * exactly what it was before weather existed.
     */
    public List<Outfit> generateWeeklyPlan(User user, LocalDate weekStart) {
        List<Outfit> candidates = new ArrayList<>(outfitService.findByCreator(user));

        // Shuffle before scoring so ties resolve differently on each call (makes Regenerate useful)
        Collections.shuffle(candidates);

        Map<LocalDate, DayForecast> forecast = weatherService.getForecast(user);

        List<Outfit> plan = new ArrayList<>();
        Outfit prev = null;

        for (int day = 0; day < 7; day++) {
            DayForecast weather = forecast.get(weekStart.plusDays(day));
            Outfit chosen = pickBest(candidates, prev, weather);
            plan.add(chosen);
            prev = chosen;
        }
        return plan;
    }

    private Outfit pickBest(List<Outfit> candidates, Outfit prev, DayForecast weather) {
        if (candidates.isEmpty()) return null;
        Outfit best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Outfit c : candidates) {
            double score = score(c, prev, weather);
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    /**
     * Weighted blend of three signals. Without a forecast the weather term drops out
     * and the remaining two keep their original 0.7 / 0.3 balance, so an offline
     * server plans exactly as it did before.
     */
    private double score(Outfit outfit, Outfit prev, DayForecast weather) {
        double freshness = freshnessScore(outfit);
        double dissimilarity = prev != null
                ? jaccardDistance(colorFamilies(outfit), colorFamilies(prev))
                : 0.5;

        if (weather == null) {
            return (freshness * 0.7) + (dissimilarity * 0.3);
        }
        return (freshness * 0.45) + (weatherScore(outfit, weather) * 0.35) + (dissimilarity * 0.20);
    }

    /**
     * How well an outfit suits the day's forecast, in [0,1].
     *
     * <p>Two independent signals: whether the outfit's season tags match the
     * temperature band, and whether it has outerwear when rain is expected. An outfit
     * with no season tags scores neutral rather than badly — absent information is not
     * evidence of a bad match.</p>
     */
    private double weatherScore(Outfit outfit, DayForecast weather) {
        double seasonFit = seasonFit(outfit.getSeasons(), weather.getMaxTempC());

        double rainFit = 0.5;
        if (weather.isWet()) {
            rainFit = hasOuterwear(outfit) ? 1.0 : 0.2;
        }

        return (seasonFit * 0.65) + (rainFit * 0.35);
    }

    /**
     * Typical temperature each season tag is dressed for, in degrees Celsius.
     *
     * <p>Graded distance rather than discrete bands, because bands leave dead zones:
     * with a "mild" bucket spanning 8-18&deg;C, a Winter outfit and a Summer outfit
     * both scored the same at 9&deg;C, which is plainly wrong. Scoring by distance from
     * a centre keeps the ordering sensible at every temperature.</p>
     */
    private static final Map<String, Double> SEASON_CENTRE_C = new HashMap<>();
    static {
        SEASON_CENTRE_C.put("Winter",  2.0);
        SEASON_CENTRE_C.put("Fall",   12.0);
        SEASON_CENTRE_C.put("Spring", 16.0);
        SEASON_CENTRE_C.put("Summer", 26.0);
    }

    /** How far from a season's centre still counts as wearable, in degrees. */
    private static final double SEASON_TOLERANCE_C = 14.0;

    /**
     * How well an outfit's season tags suit the day's temperature, in [0,1].
     *
     * <p>An outfit is scored by its best-matching tag, so tagging something both Spring
     * and Summer widens its range rather than averaging it into mediocrity. Untagged
     * outfits score neutral: absent information is not evidence of a bad match.</p>
     */
    private double seasonFit(Set<String> seasons, double maxTempC) {
        if (seasons == null || seasons.isEmpty()) {
            return 0.5;
        }
        double best = 0.0;
        boolean matched = false;
        for (String season : seasons) {
            Double centre = SEASON_CENTRE_C.get(season);
            if (centre == null) {
                continue;
            }
            matched = true;
            double fit = 1.0 - (Math.abs(maxTempC - centre) / SEASON_TOLERANCE_C);
            best = Math.max(best, Math.max(0.0, Math.min(1.0, fit)));
        }
        // Only unrecognised season names — treat as untagged rather than as a bad match.
        return matched ? best : 0.5;
    }

    private boolean hasOuterwear(Outfit outfit) {
        for (ClothingItem item : outfit.getItems()) {
            String category = item.getCategory();
            if (category != null && category.equalsIgnoreCase("Outerwear")) {
                return true;
            }
        }
        return false;
    }

    private double freshnessScore(Outfit outfit) {
        List<ClothingItem> items = outfit.getItems();
        if (items.isEmpty()) return 0.5;
        double total = 0;
        for (ClothingItem item : items) {
            int threshold = item.getWashAfterWears() > 0 ? item.getWashAfterWears() : 3;
            if (item.isNeedsWash()) {
                total -= 1.0;
            } else if (item.getWearsSinceWash() == 0) {
                total += 1.0;
            } else {
                total += 1.0 - ((double) item.getWearsSinceWash() / threshold);
            }
        }
        return total / items.size();
    }

    private Set<String> colorFamilies(Outfit outfit) {
        Set<String> families = new HashSet<>();
        for (ClothingItem item : outfit.getItems()) {
            if (item.getColor() == null) continue;
            Set<String> fam = COLOR_FAMILIES.get(item.getColor().toLowerCase().trim());
            if (fam != null) families.addAll(fam);
        }
        return families;
    }

    private double jaccardDistance(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.5;
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return 1.0 - ((double) intersection.size() / union.size());
    }

    // -------------------------------------------------------------------------
    // Persistence
    //
    // The plan is stored rather than recomputed on every view, so that a manual
    // override survives a page reload and the Sunday email sends the same plan the
    // user is looking at. Generation is the fallback, not the default.
    // -------------------------------------------------------------------------

    /**
     * The stored plan for the week beginning {@code weekStart}, generating and saving
     * one first if that week has no plan yet. Returns at most seven rows, ordered by
     * date; a day is absent when the user has no outfits to fill it with.
     */
    public List<PlannedOutfit> getOrCreateWeekPlan(User user, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<PlannedOutfit> existing = plannedOutfitDao.findByUserAndDateRange(user, weekStart, weekEnd);
        if (!existing.isEmpty()) {
            initialiseAll(existing);
            return existing;
        }
        return createAndStorePlan(user, weekStart);
    }

    /** Discards the stored plan for the week and generates a fresh one in its place. */
    public List<PlannedOutfit> regenerateWeekPlan(User user, LocalDate weekStart) {
        plannedOutfitDao.deleteByUserAndDateRange(user, weekStart, weekStart.plusDays(6));
        return createAndStorePlan(user, weekStart);
    }

    /**
     * Pins a specific outfit to a specific day, replacing whatever was planned. The row
     * is flagged as manually chosen so the UI can distinguish it from a suggestion.
     */
    public PlannedOutfit setPlanForDay(User user, LocalDate date, Outfit outfit) {
        PlannedOutfit plan = plannedOutfitDao.findByUserAndDate(user, date);
        if (plan == null) {
            plan = new PlannedOutfit();
            plan.setUser(user);
            plan.setPlanDate(date);
        }
        plan.setOutfit(outfit);
        plan.setManuallyChosen(true);
        PlannedOutfit saved = plannedOutfitDao.save(plan);
        initialise(saved.getOutfit());
        return saved;
    }

    /** Empties a single day. Regenerating the week will fill it again. */
    public void clearPlanForDay(User user, LocalDate date) {
        PlannedOutfit plan = plannedOutfitDao.findByUserAndDate(user, date);
        if (plan != null) {
            plannedOutfitDao.delete(plan);
        }
    }

    private List<PlannedOutfit> createAndStorePlan(User user, LocalDate weekStart) {
        List<Outfit> suggested = generateWeeklyPlan(user, weekStart);
        List<PlannedOutfit> stored = new ArrayList<>();

        for (int day = 0; day < 7 && day < suggested.size(); day++) {
            Outfit outfit = suggested.get(day);
            if (outfit == null) {
                continue;
            }
            PlannedOutfit plan = new PlannedOutfit();
            plan.setUser(user);
            plan.setOutfit(outfit);
            plan.setPlanDate(weekStart.plusDays(day));
            plan.setManuallyChosen(false);
            stored.add(plannedOutfitDao.save(plan));
        }
        initialiseAll(stored);
        return stored;
    }

    /**
     * Touches each outfit's lazy collections while the transaction is still open, so the
     * view can render item thumbnails and freshness badges without a lazy-init error.
     */
    private void initialiseAll(List<PlannedOutfit> plans) {
        for (PlannedOutfit plan : plans) {
            initialise(plan.getOutfit());
        }
    }

    private void initialise(Outfit outfit) {
        if (outfit != null) {
            outfit.getItems().size();
            outfit.getSeasons().size();
        }
    }
}

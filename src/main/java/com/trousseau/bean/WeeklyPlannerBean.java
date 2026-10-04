package com.trousseau.bean;

import com.trousseau.model.DayForecast;
import com.trousseau.model.Outfit;
import com.trousseau.model.OutfitWearLog;
import com.trousseau.model.PlannedOutfit;
import com.trousseau.model.User;
import com.trousseau.service.OutfitService;
import com.trousseau.service.OutfitWearLogService;
import com.trousseau.service.WeatherService;
import com.trousseau.service.WeeklyPlannerService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Named
@ViewScoped
public class WeeklyPlannerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private WeeklyPlannerService weeklyPlannerService;
    @Inject private OutfitService outfitService;
    @Inject private OutfitWearLogService outfitWearLogService;
    @Inject private WeatherService weatherService;
    @Inject private SessionBean sessionBean;

    private List<DayPlan> weekPlan;
    private List<Outfit> allOutfits;
    private LocalDate weekStart;
    private Map<LocalDate, DayForecast> forecast = new HashMap<>();
    private boolean weatherConfigured;

    @PostConstruct
    public void init() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        loadPlan();
    }

    /**
     * Loads the stored plan for the current week, creating one on first visit.
     * Unlike the previous behaviour this does not re-suggest on every page load —
     * what you saw last time is what you see now.
     */
    public void loadPlan() {
        User currentUser = sessionBean.getCurrentUser();
        allOutfits = outfitService.findByCreator(currentUser);

        weatherConfigured = weatherService.isConfigured(currentUser);
        forecast = weatherService.getForecast(currentUser);

        List<PlannedOutfit> stored = weeklyPlannerService.getOrCreateWeekPlan(currentUser, weekStart);
        buildWeek(stored);
    }

    /** Throws the stored week away and suggests a new one. Manual picks are replaced too. */
    public void generatePlan() {
        User currentUser = sessionBean.getCurrentUser();
        allOutfits = outfitService.findByCreator(currentUser);

        List<PlannedOutfit> stored = weeklyPlannerService.regenerateWeekPlan(currentUser, weekStart);
        buildWeek(stored);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "New plan generated for this week", null));
    }

    private void buildWeek(List<PlannedOutfit> stored) {
        Map<LocalDate, PlannedOutfit> byDate = new HashMap<>();
        for (PlannedOutfit plan : stored) {
            byDate.put(plan.getPlanDate(), plan);
        }

        Set<String> wornKeys = loadWornKeys();

        weekPlan = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            PlannedOutfit plan = byDate.get(date);

            DayPlan dp = new DayPlan();
            dp.date = date;
            dp.dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            dp.outfit = plan != null ? plan.getOutfit() : null;
            dp.manuallyChosen = plan != null && plan.isManuallyChosen();
            dp.today = date.equals(LocalDate.now());
            dp.worn = dp.outfit != null && wornKeys.contains(wornKey(date, dp.outfit.getId()));
            dp.forecast = forecast.get(date);
            weekPlan.add(dp);
        }
    }

    /**
     * Wear entries already logged this week, keyed by date and outfit, so each day can
     * show whether the plan was actually followed.
     */
    private Set<String> loadWornKeys() {
        Set<String> keys = new HashSet<>();
        List<OutfitWearLog> logs = outfitWearLogService.findByUserAndDateRange(
                sessionBean.getCurrentUser(), weekStart, weekStart.plusDays(6));
        for (OutfitWearLog log : logs) {
            keys.add(wornKey(log.getWornDate(), log.getOutfit().getId()));
        }
        return keys;
    }

    private String wornKey(LocalDate date, Long outfitId) {
        return date + "#" + outfitId;
    }

    // --- week navigation -----------------------------------------------------

    public void previousWeek() {
        weekStart = weekStart.minusWeeks(1);
        loadPlan();
    }

    public void nextWeek() {
        weekStart = weekStart.plusWeeks(1);
        loadPlan();
    }

    public void thisWeek() {
        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        loadPlan();
    }

    public boolean isCurrentWeek() {
        return weekStart.equals(LocalDate.now().with(DayOfWeek.MONDAY));
    }

    // --- per-day actions -----------------------------------------------------

    /** Pins the outfit selected in the day's dropdown, and stores the choice. */
    public void applyChange(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= weekPlan.size()) return;
        DayPlan dp = weekPlan.get(dayIndex);
        Long outfitId = dp.getSelectedOutfitId();
        if (outfitId == null) return;

        allOutfits.stream()
                .filter(o -> outfitId.equals(o.getId()))
                .findFirst()
                .ifPresent(outfit -> {
                    weeklyPlannerService.setPlanForDay(
                            sessionBean.getCurrentUser(), dp.date, outfit);
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_INFO,
                                    outfit.getName() + " planned for " + dp.dayName, null));
                });

        dp.selectedOutfitId = null;
        loadPlan();
    }

    public void clearDay(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= weekPlan.size()) return;
        DayPlan dp = weekPlan.get(dayIndex);
        weeklyPlannerService.clearPlanForDay(sessionBean.getCurrentUser(), dp.date);
        loadPlan();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, dp.dayName + " cleared", null));
    }

    /**
     * Logs the planned outfit as actually worn on that day, which also advances the
     * wear and wash counters of every item in it.
     */
    public void recordWear(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= weekPlan.size()) return;
        DayPlan dp = weekPlan.get(dayIndex);
        if (dp.outfit == null) return;

        outfitWearLogService.recordWear(dp.outfit, sessionBean.getCurrentUser(), dp.date);
        loadPlan();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Recorded " + dp.outfit.getName() + " as worn on " + dp.date, null));
    }

    // --- getters -------------------------------------------------------------

    public List<DayPlan> getWeekPlan() { return weekPlan; }
    public List<Outfit> getAllOutfits() { return allOutfits; }
    public LocalDate getWeekStart() { return weekStart; }
    public LocalDate getWeekEnd() { return weekStart.plusDays(6); }

    /** True when the user has set coordinates, whether or not the fetch succeeded. */
    public boolean isWeatherConfigured() { return weatherConfigured; }

    /** True when a forecast was actually retrieved for this week. */
    public boolean isWeatherAvailable() {
        if (!weatherConfigured || weekPlan == null) return false;
        for (DayPlan d : weekPlan) {
            if (d.forecast != null) return true;
        }
        return false;
    }

    /** Configured but nothing came back — offline, or the week is beyond the forecast range. */
    public boolean isWeatherUnavailable() {
        return weatherConfigured && !isWeatherAvailable();
    }

    public String getLocationName() {
        User u = sessionBean.getCurrentUser();
        if (u == null) return null;
        if (u.getLocationName() != null && !u.getLocationName().isEmpty()) {
            return u.getLocationName();
        }
        return (u.getLatitude() != null && u.getLongitude() != null)
                ? String.format("%.2f, %.2f", u.getLatitude(), u.getLongitude())
                : null;
    }

    public static class DayPlan implements Serializable {
        private static final long serialVersionUID = 1L;
        private LocalDate date;
        private String dayName;
        private Outfit outfit;
        private Long selectedOutfitId;
        private boolean manuallyChosen;
        private boolean worn;
        private boolean today;
        private DayForecast forecast;

        public LocalDate getDate() { return date; }
        public String getDayName() { return dayName; }
        public Outfit getOutfit() { return outfit; }
        public Long getSelectedOutfitId() { return selectedOutfitId; }
        public void setSelectedOutfitId(Long selectedOutfitId) { this.selectedOutfitId = selectedOutfitId; }
        public boolean isManuallyChosen() { return manuallyChosen; }
        public boolean isWorn() { return worn; }
        public boolean isToday() { return today; }
        public DayForecast getForecast() { return forecast; }
        public boolean isHasForecast() { return forecast != null; }
    }
}
